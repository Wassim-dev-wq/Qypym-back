package org.fivy.notificationservice.application.service.impl;

import com.google.cloud.Timestamp;
import com.google.cloud.firestore.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fivy.notificationservice.application.service.ChatMessageListenerService;
import org.fivy.notificationservice.application.service.PushNotificationService;
import org.fivy.notificationservice.application.service.UserNotificationPreferencesService;
import org.fivy.notificationservice.domain.entity.UserPushToken;
import org.fivy.notificationservice.domain.repository.UserPushTokenRepository;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.*;
import java.util.concurrent.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatMessageListenerServiceImpl implements ChatMessageListenerService {

    private final Firestore firestore;
    private final UserPushTokenRepository userPushTokenRepository;
    private final PushNotificationService pushNotificationService;
    private final UserNotificationPreferencesService preferencesService;

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Map<String, NotificationGroup> pendingNotifications = new ConcurrentHashMap<>();
    private final ScheduledExecutorService notificationScheduler = Executors.newScheduledThreadPool(1);

    private final Map<String, UserSession> activeUserSessions = new ConcurrentHashMap<>();

    private ListenerRegistration messagesListener;
    private ListenerRegistration userSessionsListener;

    private static final long ACTIVE_SESSION_TIMEOUT_SECONDS = 120;
    private static final long NOTIFICATION_GROUP_INTERVAL_SECONDS = 3;

    @PostConstruct
    public void startListening() {
        executorService.submit(this::listenToNewMessages);
        executorService.submit(this::listenToUserSessions);

        notificationScheduler.scheduleAtFixedRate(
                this::sendPendingNotifications,
                0,
                NOTIFICATION_GROUP_INTERVAL_SECONDS,
                TimeUnit.SECONDS
        );

        log.info("Chat notification service started");
    }

    @PreDestroy
    public void stopListening() {
        if (messagesListener != null) {
            messagesListener.remove();
        }
        if (userSessionsListener != null) {
            userSessionsListener.remove();
        }
        executorService.shutdown();
        notificationScheduler.shutdown();
        try {
            if (!notificationScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                notificationScheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            notificationScheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        log.info("Chat notification service stopped");
    }

    private void listenToUserSessions() {
        try {
            CollectionReference userStatusRef = firestore.collection("userStatus");
            userSessionsListener = userStatusRef
                    .whereEqualTo("isOnline", true)
                    .addSnapshotListener((snapshots, e) -> {
                        if (e != null) {
                            log.error("Error listening to user sessions: {}", e.getMessage(), e);
                            return;
                        }
                        if (snapshots != null) {
                            for (DocumentSnapshot doc : snapshots) {
                                updateUserSession(doc);
                            }
                            cleanupInactiveSessions();
                        }
                    });
            log.info("User sessions listening started");
        } catch (Exception e) {
            log.error("Failed to configure session listener: {}", e.getMessage(), e);
        }
    }

    private void updateUserSession(DocumentSnapshot doc) {
        try {
            String userId = doc.getId();
            String activeChatRoomId = doc.getString("activeChatRoom");
            Timestamp lastActive = doc.getTimestamp("lastActive");
            if (lastActive == null) return;
            UserSession session = new UserSession(
                    userId,
                    activeChatRoomId,
                    lastActive.toDate(),
                    doc.getBoolean("isOnline") != null && doc.getBoolean("isOnline")
            );
            activeUserSessions.put(userId, session);
        } catch (Exception e) {
            log.error("Error updating user session: {}", e.getMessage(), e);
        }
    }

    private void cleanupInactiveSessions() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<String, UserSession>> it = activeUserSessions.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, UserSession> entry = it.next();
            UserSession session = entry.getValue();
            long lastActiveTime = session.getLastActiveTime().getTime();
            if (now - lastActiveTime > TimeUnit.SECONDS.toMillis(ACTIVE_SESSION_TIMEOUT_SECONDS)) {
                it.remove();
            }
        }
    }

    @Override
    public void listenToNewMessages() {
        try {
            Query query = firestore.collectionGroup("messages")
                    .whereGreaterThan("createdAt", Timestamp.now());
            messagesListener = query.addSnapshotListener((snapshots, e) -> {
                if (e != null) {
                    log.error("Error listening for new messages: {}", e.getMessage(), e);
                    return;
                }
                if (snapshots != null && !snapshots.isEmpty()) {
                    for (QueryDocumentSnapshot document : snapshots) {
                        processNewMessage(document);
                    }
                }
            });
            log.info("Chat new messages listening started");
        } catch (Exception e) {
            log.error("Failed to configure message listener: {}", e.getMessage(), e);
            if (e.getMessage() != null && e.getMessage().contains("index")) {
                log.info("Attempting reconnection in 30 seconds...");
                try {
                    Thread.sleep(30000);
                    listenToNewMessages();
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    private void processNewMessage(QueryDocumentSnapshot document) {
        try {
            Map<String, Object> messageData = document.getData();
            Boolean isSystem = (Boolean) messageData.getOrDefault("system", false);
            if (isSystem) return;

            String chatRoomId = document.getReference().getParent().getParent().getId();
            log.debug("Processing new message: {} in chat room: {}", document.getId(), chatRoomId);

            DocumentReference chatRoomRef = firestore.collection("chatRooms").document(chatRoomId);
            DocumentSnapshot chatRoomSnapshot = chatRoomRef.get().get();
            if (!chatRoomSnapshot.exists()) {
                log.warn("Chat room not found for message: {}", document.getId());
                return;
            }

            Map<String, Object> chatRoomData = chatRoomSnapshot.getData();
            if (chatRoomData == null) return;

            Map<String, Object> user = (Map<String, Object>) messageData.get("user");
            String senderId = user != null ? (String) user.get("_id") : null;
            String senderName = user != null ? (String) user.get("name") : "Unknown";
            String messageText = (String) messageData.get("text");
            List<String> participants = (List<String>) chatRoomData.get("participants");

            if (participants != null && senderId != null) {
                for (String participantId : participants) {
                    if (participantId.equals(senderId)) continue;

                    UUID participantUuid = UUID.fromString(participantId);
                    if (!preferencesService.shouldSendPushChatMessage(participantUuid)) {
                        log.debug("Chat notifications disabled for user: {}", participantId);
                        continue;
                    }

                    boolean needsNotification = shouldSendNotification(
                            participantId,
                            chatRoomId,
                            (Timestamp) messageData.get("createdAt"),
                            chatRoomData
                    );

                    if (needsNotification) {
                        queueNotificationForUser(
                                participantUuid,
                                senderName,
                                messageText,
                                chatRoomId
                        );
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error processing message {}: {}", document.getId(), e.getMessage(), e);
        }
    }

    private boolean shouldSendNotification(
            String participantId,
            String chatRoomId,
            Timestamp messageTime,
            Map<String, Object> chatRoomData
    ) {
        List<String> mutedUsers = (List<String>) chatRoomData.getOrDefault("mutedUsers", Collections.emptyList());
        if (mutedUsers.contains(participantId)) {
            return false;
        }
        UserSession session = activeUserSessions.get(participantId);
        if (session != null) {
            if (session.isOnline() && chatRoomId.equals(session.getActiveChatRoomId())) {
                log.debug("No notification for participant {} - active in chat {}", participantId, chatRoomId);
                return false;
            }
        }
        Map<String, Object> lastSeen = (Map<String, Object>) chatRoomData.get("lastSeen");
        Timestamp lastSeenTime = lastSeen != null ? (Timestamp) lastSeen.get(participantId) : null;
        if (lastSeenTime != null && messageTime != null) {
            boolean notYetSeen = lastSeenTime.compareTo(messageTime) < 0;
            if (!notYetSeen) {
                log.debug("No notification for participant {} - message already seen in chat {}", participantId, chatRoomId);
            }
            return notYetSeen;
        }
        return true;
    }

    private void queueNotificationForUser(UUID userId, String senderName, String messageText, String chatRoomId) {
        String key = userId.toString() + ":" + chatRoomId;
        pendingNotifications.compute(key, (k, group) -> {
            if (group == null) {
                group = new NotificationGroup(userId, chatRoomId);
            }
            group.addMessage(senderName, messageText);
            return group;
        });
        log.debug("Message queued for user {} in chat room {}", userId, chatRoomId);
    }

    private void sendPendingNotifications() {
        if (pendingNotifications.isEmpty()) {
            return;
        }
        log.debug("Sending pending notifications: {}", pendingNotifications.size());
        Map<String, NotificationGroup> notifications = new HashMap<>(pendingNotifications);
        pendingNotifications.clear();
        for (NotificationGroup group : notifications.values()) {
            sendGroupedNotification(group);
        }
    }

    private void sendGroupedNotification(NotificationGroup group) {
        try {
            if (!preferencesService.shouldSendPushChatMessage(group.getUserId())) {
                log.debug("Chat notifications now disabled for user: {}", group.getUserId());
                return;
            }

            List<UserPushToken> tokens = userPushTokenRepository.findByUserId(group.getUserId());
            if (tokens.isEmpty()) {
                log.debug("No push token found for user: {}", group.getUserId());
                return;
            }

            String userIdStr = group.getUserId().toString();
            UserSession session = activeUserSessions.get(userIdStr);
            if (session != null && session.isOnline() && group.getChatRoomId().equals(session.getActiveChatRoomId())) {
                log.debug("Notification cancelled - user now active in chat: {}", group.getChatRoomId());
                return;
            }

            DocumentReference chatRoomRef = firestore.collection("chatRooms").document(group.getChatRoomId());
            DocumentSnapshot chatRoomSnapshot = chatRoomRef.get().get();
            String chatRoomName = "Chat";
            if (chatRoomSnapshot.exists() && chatRoomSnapshot.getData() != null) {
                chatRoomName = (String) chatRoomSnapshot.getData().getOrDefault("matchTitle", "Chat");
                if (chatRoomName.length() > 30) {
                    chatRoomName = chatRoomName.substring(0, 27) + "...";
                }
            }

            String title;
            String body;
            int roomUnreadCount = group.getMessageCount();
            if (roomUnreadCount == 1) {
                MessageInfo message = group.getMessages().get(0);
                title = chatRoomName;
                body = message.getSenderName() + ": " + (message.getMessageText() != null ?
                        (message.getMessageText().length() > 100 ?
                                message.getMessageText().substring(0, 97) + "..." :
                                message.getMessageText()) :
                        "nouveau message");
            } else {
                title = chatRoomName + ": " + roomUnreadCount + " nouveaux messages";
                body = group.getMessageCount() + " nouveaux messages";
            }

            String notificationId = "chat_" + group.getChatRoomId();
            for (UserPushToken token : tokens) {
                Map<String, String> data = new HashMap<>();
                data.put("type", "CHAT_MESSAGE");
                data.put("chatRoomId", group.getChatRoomId());
                data.put("messageCount", String.valueOf(group.getMessageCount()));
                data.put("chatRoomName", chatRoomName);
                data.put("notificationId", notificationId);
                pushNotificationService.sendNotification(
                        group.getUserId(),
                        token.getExpoToken(),
                        title,
                        body,
                        data
                );
            }

            log.info("Chat notification sent to user: {} ({} messages, chat room: {})",
                    group.getUserId(), group.getMessageCount(), chatRoomName);
        } catch (Exception e) {
            log.error("Failed to send grouped notifications for user {}: {}",
                    group.getUserId(), e.getMessage(), e);
        }
    }

    private static class NotificationGroup {
        private final UUID userId;
        private final String chatRoomId;
        private final List<MessageInfo> messages = new ArrayList<>();
        private final Set<String> senderNames = new HashSet<>();

        public NotificationGroup(UUID userId, String chatRoomId) {
            this.userId = userId;
            this.chatRoomId = chatRoomId;
        }

        public void addMessage(String senderName, String messageText) {
            messages.add(new MessageInfo(senderName, messageText));
            senderNames.add(senderName);
        }

        public UUID getUserId() {
            return userId;
        }

        public String getChatRoomId() {
            return chatRoomId;
        }

        public List<MessageInfo> getMessages() {
            return messages;
        }

        public Set<String> getSenderNames() {
            return senderNames;
        }

        public int getMessageCount() {
            return messages.size();
        }
    }

    private static class MessageInfo {
        private final String senderName;
        private final String messageText;

        public MessageInfo(String senderName, String messageText) {
            this.senderName = senderName;
            this.messageText = messageText;
        }

        public String getSenderName() {
            return senderName;
        }

        public String getMessageText() {
            return messageText;
        }
    }

    private static class UserSession {
        private final String userId;
        private final String activeChatRoomId;
        private final Date lastActiveTime;
        private final boolean online;

        public UserSession(String userId, String activeChatRoomId, Date lastActiveTime, boolean online) {
            this.userId = userId;
            this.activeChatRoomId = activeChatRoomId;
            this.lastActiveTime = lastActiveTime;
            this.online = online;
        }

        public String getUserId() {
            return userId;
        }

        public String getActiveChatRoomId() {
            return activeChatRoomId;
        }

        public Date getLastActiveTime() {
            return lastActiveTime;
        }

        public boolean isOnline() {
            return online;
        }
    }
}