package org.fivy.userservice.shared.exception;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ContentModerationUtil {

    private static final Set<String> ENGLISH_PROFANITY = new HashSet<>(Arrays.asList(
            "anal", "anus", "arse", "ass", "asshole", "asswipe", "bastard", "bitch", "blowjob", "blow job",
            "bollocks", "bullshit", "cock", "coon", "crap", "cum", "cunt", "damn", "dick", "dildo", "dyke",
            "fag", "faggot", "feck", "fellate", "fellatio", "felching", "fuck", "fucking", "fudgepacker",
            "homo", "jerk", "jizz", "kike", "masturbate", "nigger", "nigga", "penis", "piss", "poop", "porn",
            "pussy", "queer", "retard", "scrotum", "sex", "shit", "slut", "smegma", "spunk", "tit", "tosser",
            "turd", "twat", "vagina", "wank", "whore", "wtf",
            "kill", "murder", "suicide", "terrorist", "bomb", "shooting", "massacre", "holocaust", "death",
            "rape", "pedo", "pedophile", "molest", "assault", "abuse"
    ));

    private static final Set<String> FRENCH_PROFANITY = new HashSet<>(Arrays.asList(
            // Common profanities
            "connard", "connasse", "salope", "pute", "putain", "merde", "enculé", "encule", "foutre", "bite",
            "couille", "nichon", "cul", "con", "batard", "bâtard", "chier", "niquer", "nique", "nike", "pd", "pédé",
            "tapette", "gouine", "négro", "nègre", "bougnoule", "nazi", "suicide", "terroriste", "viol", "violer",
            "conasse", "conard", "enfoiré", "enfoire", "fdp", "ntm", "tg", "ta gueule", "fils de pute", "ta mere",
            "ta mère", "va te faire", "vtf", "vtff", "ptn", "mrd", "bordel", "crétin", "cretin", "débile", "debile",
            "abruti", "imbécile", "imbecile", "poufiasse", "pouffiasse", "salaud", "salopard", "saloperie",
            "suce", "sucer", "baiser", "bz", "baise", "levrette", "sodomie", "fellation", "pipe", "sperme",
            "sodomiser", "branler", "masturber", "masturbation", "éjaculer", "ejaculer", "éjaculation", "ejaculation",
            "vagin", "zizi", "teub", "queue", "quequette", "pénis", "penis", "chatte", "clito", "clitoris", "seins",
            "nibard", "nichons", "téton", "teton", "couilles", "testicules", "burnes",
            "arabe", "bamboula", "bicot", "boche", "bridé", "bridée", "bridee", "chinetoque", "feuj", "juif",
            "youpin", "youtre", "métèque", "meteque", "negro", "niakoué", "niakouée", "niakouee", "nègre", "negre",
            "négresse", "negresse", "noich", "noir", "renoi", "rital", "schleu", "yougo", "youd", "pakolé", "pakole",
            "rebeu", "bougnoul", "bougnoule", "sale arabe", "sale noir", "sale juif",
            "fiotte", "tafiole", "tantouze", "tarlouze", "tarlouse", "trans", "travelo", "pédé", "pede", "pédale",
            "pedale", "homosexuel", "homo", "lesbienne", "gouine", "goudou", "lopette", "lope", "baltringue",
            "crever", "crève", "creve", "mourir", "meurs", "tuer", "buter", "flinguer", "abattre", "suicider",
            "suicide", "tuer", "déglinguer", "deglinguer", "défoncer", "defoncer", "déchirer", "dechirer", "détruire",
            "detruire", "massacrer", "égorger", "egorger", "étriper", "etriper", "frapper", "cogner", "tabasser",
            "assassiner", "meurtrier", "meurtrière", "meurtriere",
            "conn", "sal", "put", "enc", "merd", "cul", "btrd", "chier", "niq", "pd", "tap", "gou", "neg", "boug",
            "viol", "sui", "ter", "bit", "coui",
            "va te faire foutre", "va te faire enculer", "nique ta mère", "nique ta mere", "fils de pute",
            "ferme ta gueule", "ferme la", "ta gueule", "ta mère", "ta mere", "fils de", "fille de",
            "attardé", "attarde", "mongol", "mongolien", "gogol", "handicapé", "handicape", "nazi", "facho",
            "fasciste", "raciste", "pédophile", "pedophile", "terroriste", "kamikaze", "djihadiste", "jihadiste",
            "astique", "branlette", "doigte", "doigter", "finir", "juter", "lécher", "lecher", "pomper", "tringler",
            "troncher", "zigounette", "zob", "gaule", "quéquette", "quequette", "gland", "berlingue", "plotte",
            "foune", "pétasse", "petasse", "bimbo", "grognasse", "cagole", "pimbêche", "pimbeche"
    ));

    private static final Pattern EVASION_PATTERN = Pattern.compile(
            "(?i)(" +
                    // English evasions
                    "a+[s5$]+[s5$]*h+[o0]+l+e+|" +
                    "b+[i1l!]+[t7]+c+h+|" +
                    "c+[o0]+c+k+|" +
                    "c+[u\\\\/]+n+t+|" +
                    "d+[i1l!]+c+k+|" +
                    "f+[u\\\\/]+c+k+|" +
                    "n+[i1l!]+g+[g6]+[e3a4]+r+|" +
                    "n+[i1l!]+g+[g6]+a+|" +
                    "p+[u\\\\/]+[s5$]+[s5$]+[y\\\\]+|" +
                    "s+h+[i1l!]+t+|" +
                    "s+l+[u\\\\/]+t+|" +
                    "w+h+[o0]+r+e+|" +
                    "c+[o0]+[n\\\\]+n+a+[r4]+d+|" +
                    "s+a+l+[o0]+p+e+|" +
                    "p+[u\\\\/]+t+[e3a4]+|" +
                    "p+[u\\\\/]+t+a+[i1l!]+n+|" +
                    "m+[e3a4]+r+d+[e3a4]+|" +
                    "e+n+c+[u\\\\/]+l+[e3a4\\\\]+|" +
                    "n+[i1l!]+q+[u\\\\/]+[e3a4]|" +
                    "b+[i1l!]+t+[e3a4]+|" +
                    "c+[o0]+[u\\\\/]+[i1l!]+l+[l1!]+[e3a4]+|" +
                    "b+[a4]+[t7]+a+r+d+|" +
                    "s+[u\\\\/]+[i1l!]+c+[i1l!]+d+[e3a4]+|" +
                    "v+[i1l!]+[o0]+l+[e3a4]+r+)"
    );
    private static final Pattern WORD_SPLITTING_PATTERN = Pattern.compile(
            "(?i)(" +
                    "f[\\s._-]*u[\\s._-]*c[\\s._-]*k|" +
                    "s[\\s._-]*h[\\s._-]*i[\\s._-]*t|" +
                    "b[\\s._-]*i[\\s._-]*t[\\s._-]*c[\\s._-]*h|" +
                    "c[\\s._-]*o[\\s._-]*n[\\s._-]*n[\\s._-]*a[\\s._-]*r[\\s._-]*d|" +
                    "s[\\s._-]*a[\\s._-]*l[\\s._-]*o[\\s._-]*p[\\s._-]*e|" +
                    "p[\\s._-]*u[\\s._-]*t[\\s._-]*e|" +
                    "p[\\s._-]*u[\\s._-]*t[\\s._-]*a[\\s._-]*i[\\s._-]*n|" +
                    "e[\\s._-]*n[\\s._-]*c[\\s._-]*u[\\s._-]*l[\\s._-]*e)"
    );

    private static final Pattern SUSPICIOUS_PATTERN = Pattern.compile(
            "(?i)(" +
                    "[\\\\(\\\\)\\\\{\\\\}\\\\[\\\\]<>]{3,}|" +
                    "[8B]=+D|" +
                    "3+=[\\\\)Dd]+|" +
                    "[oO0]{2,}[_-]{1,}[oO0]{2,}|" +
                    "\\\\d{3,}\\\\s*-\\\\s*\\\\d{3,}\\\\s*-\\\\s*\\\\d{4}|" +
                    "(https?|ftp):\\/\\/[^\\s/$.?#].[^\\s]*)"
    );

    private static final Pattern HATE_SPEECH_PATTERN = Pattern.compile(
            "(?i)\\b(" +
                    "kill (all|the) [a-zA-Zàáâäæãåāèéêëēėęîïíīįìôöòóœøōõûüùúūÿ]+|" +
                    "death to [a-zA-Zàáâäæãåāèéêëēėęîïíīįìôöòóœøōõûüùúūÿ]+|" +
                    "hate [a-zA-Zàáâäæãåāèéêëēėęîïíīįìôöòóœøōõûüùúūÿ]+|" +
                    "exterminate [a-zA-Zàáâäæãåāèéêëēėęîïíīįìôöòóœøōõûüùúūÿ]+|" +
                    "tous les [a-zA-Zàáâäæãåāèéêëēėęîïíīįìôöòóœøōõûüùúūÿ]+ sont|" +
                    "mort aux [a-zA-Zàáâäæãåāèéêëēėęîïíīįìôöòóœøōõûüùúūÿ]+|" +
                    "je (vais|va) te [a-zA-Zàáâäæãåāèéêëēėęîïíīįìôöòóœøōõûüùúūÿ]+)" +
                    "\\b"
    );

    public static boolean containsInappropriateContent(String text) {
        if (text == null || text.trim().isEmpty()) {
            return false;
        }
        String normalizedText = normalizeText(text);
        if (containsExactMatches(normalizedText)) {
            return true;
        }
        if (containsEvasionPatterns(text)) {
            return true;
        }
        if (containsWordSplitting(text)) {
            return true;
        }
        if (containsSuspiciousPatterns(text)) {
            return true;
        }
        if (containsHateSpeech(text)) {
            return true;
        }
        return false;
    }

    private static String normalizeText(String text) {
        // Normalize Unicode characters (e.g., convert é to e)
        String normalized = Normalizer.normalize(text.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        normalized = normalized.replaceAll("[àáâäæãåā]", "a")
                .replaceAll("[èéêëēėę]", "e")
                .replaceAll("[îïíīįì]", "i")
                .replaceAll("[ôöòóœøōõ]", "o")
                .replaceAll("[ûüùúū]", "u")
                .replaceAll("[ÿ]", "y");

        return normalized;
    }

    private static boolean containsExactMatches(String text) {
        // Split into words for checking
        String[] words = text.toLowerCase()
                .replaceAll("[^a-z0-9\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim()
                .split("\\s+");
        for (String word : words) {
            if (ENGLISH_PROFANITY.contains(word) || FRENCH_PROFANITY.contains(word)) {
                log.warn("Detected exact profanity match: {}", word);
                return true;
            }
        }

        return false;
    }

    private static boolean containsEvasionPatterns(String text) {
        Matcher matcher = EVASION_PATTERN.matcher(text);
        if (matcher.find()) {
            log.warn("Detected evasion pattern: {}", matcher.group());
            return true;
        }
        return false;
    }

    private static boolean containsWordSplitting(String text) {
        Matcher matcher = WORD_SPLITTING_PATTERN.matcher(text);
        if (matcher.find()) {
            log.warn("Detected word splitting evasion: {}", matcher.group());
            return true;
        }
        return false;
    }

    private static boolean containsSuspiciousPatterns(String text) {
        Matcher matcher = SUSPICIOUS_PATTERN.matcher(text);
        if (matcher.find()) {
            log.warn("Detected suspicious pattern: {}", matcher.group());
            return true;
        }
        return false;
    }

    private static boolean containsHateSpeech(String text) {
        Matcher matcher = HATE_SPEECH_PATTERN.matcher(text);
        if (matcher.find()) {
            log.warn("Detected potential hate speech or threat: {}", matcher.group());
            return true;
        }
        return false;
    }
}