package raven.yolo.utils;

import raven.yolo.model.DetectionResult;
import raven.yolo.model.DetectionResult.BoundingBox;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;

/**
 * Utility class to render detection results on images
 */
public class ImageRenderer {
    
    private static final int STROKE_WIDTH = 2;
    private static final Font LABEL_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 12);
    
    /**
     * Render detection results on image
     */
    public static BufferedImage renderDetections(BufferedImage originalImage, List<DetectionResult> detections) {
        if (originalImage == null || detections == null || detections.isEmpty()) {
            return originalImage;
        }
        
        // Create a copy of the original image
        BufferedImage resultImage = new BufferedImage(
            originalImage.getWidth(), 
            originalImage.getHeight(), 
            BufferedImage.TYPE_INT_ARGB
        );
        
        Graphics2D g2d = resultImage.createGraphics();
        
        // Enable anti-aliasing
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        
        // Draw original image
        g2d.drawImage(originalImage, 0, 0, null);
        
        // Draw each detection
        for (DetectionResult detection : detections) {
            renderDetection(g2d, detection, originalImage.getWidth(), originalImage.getHeight());
        }
        
        g2d.dispose();
        return resultImage;
    }
    
    /**
     * Render a single detection
     */
    private static void renderDetection(Graphics2D g2d, DetectionResult detection, int imageWidth, int imageHeight) {
        BoundingBox bbox = detection.getBoundingBox();
        if (bbox == null) return;
        
        // Scale bounding box to image dimensions if needed
        BoundingBox scaledBbox = bbox;
        
        // Get colors
        Color fillColor = detection.getConfidenceColor();
        Color borderColor = detection.getBorderColor();
        
        // Draw bounding box
        Rectangle rect = scaledBbox.getRectangle();
        
        // Fill with transparent color
        g2d.setColor(fillColor);
        g2d.fillRect(rect.x, rect.y, rect.width, rect.height);
        
        // Draw border
        g2d.setStroke(new BasicStroke(STROKE_WIDTH));
        g2d.setColor(borderColor);
        g2d.drawRect(rect.x, rect.y, rect.width, rect.height);
        
        // Draw label
        String label = String.format("%s: %.1f%%", 
            detection.getClassName(), 
            detection.getConfidence() * 100);
        
        drawLabel(g2d, label, rect.x, rect.y, borderColor);
    }
    
    /**
     * Draw detection label
     */
    private static void drawLabel(Graphics2D g2d, String label, int x, int y, Color borderColor) {
        g2d.setFont(LABEL_FONT);
        FontMetrics fm = g2d.getFontMetrics();
        
        int labelWidth = fm.stringWidth(label);
        int labelHeight = fm.getHeight();
        int padding = 4;
        
        // Position label above the bounding box
        int labelX = x;
        int labelY = y - labelHeight;
        
        // If label would be outside image, position it inside the box
        if (labelY < 0) {
            labelY = y + labelHeight + padding;
        }
        
        // Draw label background
        g2d.setColor(new Color(borderColor.getRed(), borderColor.getGreen(), borderColor.getBlue(), 200));
        g2d.fillRect(labelX, labelY - fm.getAscent(), labelWidth + padding * 2, labelHeight);
        
        // Draw label text
        g2d.setColor(Color.WHITE);
        g2d.drawString(label, labelX + padding, labelY);
    }
    
    /**
     * Create a scaled version of the image for display
     */
    public static BufferedImage scaleImage(BufferedImage originalImage, int maxWidth, int maxHeight) {
        if (originalImage == null) return null;
        
        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();
        
        // Calculate scaling factor
        double scaleX = (double) maxWidth / originalWidth;
        double scaleY = (double) maxHeight / originalHeight;
        double scale = Math.min(scaleX, scaleY);
        
        // Don't upscale
        if (scale > 1.0) {
            scale = 1.0;
        }
        
        int newWidth = (int) (originalWidth * scale);
        int newHeight = (int) (originalHeight * scale);
        
        BufferedImage scaledImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = scaledImage.createGraphics();
        
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.drawImage(originalImage, 0, 0, newWidth, newHeight, null);
        g2d.dispose();
        
        return scaledImage;
    }
    
    /**
     * Scale detection results to match scaled image
     */
    public static List<DetectionResult> scaleDetections(List<DetectionResult> detections, 
                                                       double scaleX, double scaleY) {
        if (detections == null) return null;
        
        for (DetectionResult detection : detections) {
            BoundingBox bbox = detection.getBoundingBox();
            if (bbox != null) {
                detection.setBoundingBox(bbox.scale(scaleX, scaleY));
            }
        }
        
        return detections;
    }
    
    /**
     * Get summary text for detections
     */
    public static String getDetectionSummary(List<DetectionResult> detections) {
        if (detections == null || detections.isEmpty()) {
            return "No detections found";
        }
        
        StringBuilder summary = new StringBuilder();
        summary.append("Detection Results:\n");
        summary.append("==================\n\n");
        
        for (int i = 0; i < detections.size(); i++) {
            DetectionResult detection = detections.get(i);
            summary.append(String.format("Detection %d:\n", i + 1));
            summary.append(String.format("  Class ID: %d\n", detection.getClassId()));
            summary.append(String.format("  Class: %s\n", detection.getClassName()));
            summary.append(String.format("  Confidence: %.2f%%\n", detection.getConfidence() * 100));
            summary.append(String.format("  Bounding Box: %s\n", detection.getBoundingBox()));
            summary.append("\n");
        }
        
        summary.append(String.format("Total detections: %d", detections.size()));
        
        return summary.toString();
    }
}
