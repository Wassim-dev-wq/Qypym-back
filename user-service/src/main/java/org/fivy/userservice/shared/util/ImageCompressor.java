package org.fivy.userservice.shared.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;

@Slf4j
public class ImageCompressor {

    private static final int MAX_WIDTH = 800;
    private static final int MAX_HEIGHT = 800;

    private static final float COMPRESSION_QUALITY = 0.7f;

    public static byte[] compressImage(MultipartFile file, String format) throws IOException {
        BufferedImage originalImage = ImageIO.read(file.getInputStream());
        if (originalImage == null) {
            throw new IOException("Failed to read image file");
        }
        BufferedImage resizedImage = resizeImage(originalImage);
        return compressBufferedImage(resizedImage, format);
    }

    private static BufferedImage resizeImage(BufferedImage originalImage) {
        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();

        if (originalWidth <= MAX_WIDTH && originalHeight <= MAX_HEIGHT) {
            return originalImage;
        }

        float aspectRatio = (float) originalWidth / originalHeight;
        int targetWidth = originalWidth;
        int targetHeight = originalHeight;

        if (originalWidth > MAX_WIDTH) {
            targetWidth = MAX_WIDTH;
            targetHeight = Math.round(targetWidth / aspectRatio);
        }

        if (targetHeight > MAX_HEIGHT) {
            targetHeight = MAX_HEIGHT;
            targetWidth = Math.round(targetHeight * aspectRatio);
        }

        BufferedImage resizedImage = new BufferedImage(targetWidth, targetHeight, originalImage.getType());
        Graphics2D g = resizedImage.createGraphics();
        g.drawImage(originalImage, 0, 0, targetWidth, targetHeight, null);
        g.dispose();

        return resizedImage;
    }

    private static byte[] compressBufferedImage(BufferedImage image, String format) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        if ("jpg".equalsIgnoreCase(format) || "jpeg".equalsIgnoreCase(format)) {
            // Use compression for JPEG format
            Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
            if (!writers.hasNext()) {
                throw new IOException("No JPEG image writer found");
            }

            ImageWriter writer = writers.next();
            ImageOutputStream imageOutputStream = ImageIO.createImageOutputStream(outputStream);
            writer.setOutput(imageOutputStream);

            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(COMPRESSION_QUALITY);

            // Convert to RGB if needed (JPEG doesn't support alpha channel)
            BufferedImage rgbImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
            rgbImage.createGraphics().drawImage(image, 0, 0, null);

            writer.write(null, new IIOImage(rgbImage, null, null), param);
            writer.dispose();
            imageOutputStream.close();
        } else {
            // For PNG and other formats, use standard write method
            // Note: PNG uses lossless compression
            ImageIO.write(image, format, outputStream);
        }

        return outputStream.toByteArray();
    }


    public static String getImageFormatFromContentType(String contentType) {
        if (contentType == null) {
            return "jpg"; // Default format
        }

        return switch (contentType.toLowerCase()) {
            case "image/png" -> "png";
            case "image/gif" -> "gif";
            case "image/jpeg", "image/jpg" -> "jpg";
            default -> "jpg"; // Default format
        };
    }

    public static boolean isValidImageFile(MultipartFile file) {
        try {
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return false;
            }
            BufferedImage image = ImageIO.read(file.getInputStream());
            return image != null;
        } catch (IOException e) {
            log.warn("Failed to validate image file", e);
            return false;
        }
    }

    public static String convertToBase64(byte[] imageData) {
        if (imageData == null || imageData.length == 0) {
            return null;
        }
        return java.util.Base64.getEncoder().encodeToString(imageData);
    }
}