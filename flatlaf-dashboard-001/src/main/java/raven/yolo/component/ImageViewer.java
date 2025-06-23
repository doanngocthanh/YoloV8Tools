package raven.yolo.component;

import raven.yolo.model.YoloAnnotation;
import raven.yolo.model.YoloImage;
import raven.yolo.manager.ProjectManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

public class ImageViewer extends JPanel {
    
    private BufferedImage originalImage;
    private BufferedImage scaledImage;
    private YoloImage currentYoloImage;
    private double scale = 1.0;
    private int offsetX = 0;
    private int offsetY = 0;
    
    // Annotation variables
    private boolean isDrawing = false;
    private Point startPoint;
    private Point endPoint;
    private Rectangle currentRect;
    private int currentClassId = 0;
    private String currentClassName = "";
    private List<String> classNames = new ArrayList<>();
    
    // Colors for different classes
    private final Color[] classColors = {
        Color.RED, Color.BLUE, Color.GREEN, Color.YELLOW, Color.MAGENTA,
        Color.CYAN, Color.ORANGE, Color.PINK, Color.LIGHT_GRAY, Color.DARK_GRAY
    };
    
    public ImageViewer() {
        setBackground(Color.DARK_GRAY);
        setPreferredSize(new Dimension(800, 600));
        
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (originalImage != null && currentYoloImage != null) {
                    startPoint = e.getPoint();
                    isDrawing = true;
                }
            }
            
            @Override
            public void mouseReleased(MouseEvent e) {
                if (isDrawing && startPoint != null) {
                    endPoint = e.getPoint();
                    createAnnotation();
                    isDrawing = false;
                    startPoint = null;
                    endPoint = null;
                    currentRect = null;
                    repaint();
                }
            }
        });
        
        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (isDrawing && startPoint != null) {
                    endPoint = e.getPoint();
                    updateCurrentRect();
                    repaint();
                }
            }
        });
    }
    
    public void loadImage(YoloImage yoloImage) {
        try {
            this.currentYoloImage = yoloImage;
            File imageFile = new File(yoloImage.getPath());
            this.originalImage = ImageIO.read(imageFile);
            
            // Update image dimensions if not set
            if (yoloImage.getWidth() == 0 || yoloImage.getHeight() == 0) {
                yoloImage.setWidth(originalImage.getWidth());
                yoloImage.setHeight(originalImage.getHeight());
            }
            
            calculateScale();
            repaint();
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading image: " + e.getMessage(), 
                                        "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void calculateScale() {
        if (originalImage == null) return;
        
        double panelWidth = getWidth();
        double panelHeight = getHeight();
        double imageWidth = originalImage.getWidth();
        double imageHeight = originalImage.getHeight();
        
        double scaleX = panelWidth / imageWidth;
        double scaleY = panelHeight / imageHeight;
        
        scale = Math.min(scaleX, scaleY) * 0.9; // Leave some margin
        
        // Calculate offset to center the image
        offsetX = (int) ((panelWidth - imageWidth * scale) / 2);
        offsetY = (int) ((panelHeight - imageHeight * scale) / 2);
        
        // Create scaled image
        int scaledWidth = (int) (imageWidth * scale);
        int scaledHeight = (int) (imageHeight * scale);
        scaledImage = new BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = scaledImage.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.drawImage(originalImage, 0, 0, scaledWidth, scaledHeight, null);
        g2d.dispose();
    }
    
    private void updateCurrentRect() {
        if (startPoint != null && endPoint != null) {
            int x = Math.min(startPoint.x, endPoint.x);
            int y = Math.min(startPoint.y, endPoint.y);
            int width = Math.abs(endPoint.x - startPoint.x);
            int height = Math.abs(endPoint.y - startPoint.y);
            currentRect = new Rectangle(x, y, width, height);
        }
    }
    
    private void createAnnotation() {
        if (currentRect == null || originalImage == null || currentYoloImage == null) return;
        
        // Convert screen coordinates to image coordinates
        int imageX1 = (int) ((currentRect.x - offsetX) / scale);
        int imageY1 = (int) ((currentRect.y - offsetY) / scale);
        int imageX2 = (int) ((currentRect.x + currentRect.width - offsetX) / scale);
        int imageY2 = (int) ((currentRect.y + currentRect.height - offsetY) / scale);
        
        // Ensure coordinates are within image bounds
        imageX1 = Math.max(0, Math.min(imageX1, originalImage.getWidth()));
        imageY1 = Math.max(0, Math.min(imageY1, originalImage.getHeight()));
        imageX2 = Math.max(0, Math.min(imageX2, originalImage.getWidth()));
        imageY2 = Math.max(0, Math.min(imageY2, originalImage.getHeight()));
          if (imageX2 > imageX1 && imageY2 > imageY1) {
            YoloAnnotation annotation = YoloAnnotation.fromPixelCoordinates(
                currentClassId, currentClassName,
                imageX1, imageY1, imageX2, imageY2,
                originalImage.getWidth(), originalImage.getHeight()
            );
            
            currentYoloImage.addAnnotation(annotation);
            
            // Auto-save annotations to file
            try {
                raven.yolo.manager.ProjectManager.getInstance().saveImageAnnotations(currentYoloImage);
            } catch (Exception e) {
                System.err.println("Error saving annotations: " + e.getMessage());
            }
        }
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g.create();
        
        // Draw scaled image
        if (scaledImage != null) {
            g2d.drawImage(scaledImage, offsetX, offsetY, null);
            
            // Draw existing annotations
            drawAnnotations(g2d);
            
            // Draw current drawing rectangle
            if (currentRect != null) {
                g2d.setColor(getCurrentClassColor());
                g2d.setStroke(new BasicStroke(2));
                g2d.drawRect(currentRect.x, currentRect.y, currentRect.width, currentRect.height);
                
                // Draw class name
                if (!currentClassName.isEmpty()) {
                    g2d.setColor(Color.WHITE);
                    g2d.fillRect(currentRect.x, currentRect.y - 20, 
                               g2d.getFontMetrics().stringWidth(currentClassName) + 10, 20);
                    g2d.setColor(Color.BLACK);
                    g2d.drawString(currentClassName, currentRect.x + 5, currentRect.y - 5);
                }
            }
        }
        
        g2d.dispose();
    }
    
    private void drawAnnotations(Graphics2D g2d) {
        if (currentYoloImage == null || originalImage == null) return;
        
        for (YoloAnnotation annotation : currentYoloImage.getAnnotations()) {
            // Convert normalized coordinates to screen coordinates
            int x1 = (int) (annotation.getPixelX1(originalImage.getWidth()) * scale + offsetX);
            int y1 = (int) (annotation.getPixelY1(originalImage.getHeight()) * scale + offsetY);
            int x2 = (int) (annotation.getPixelX2(originalImage.getWidth()) * scale + offsetX);
            int y2 = (int) (annotation.getPixelY2(originalImage.getHeight()) * scale + offsetY);
            
            int width = x2 - x1;
            int height = y2 - y1;
            
            Color classColor = getClassColor(annotation.getClassId());
            g2d.setColor(classColor);
            g2d.setStroke(new BasicStroke(2));
            g2d.drawRect(x1, y1, width, height);
            
            // Draw class name
            String className = annotation.getClassName();
            if (className != null && !className.isEmpty()) {
                g2d.setColor(Color.WHITE);
                g2d.fillRect(x1, y1 - 20, g2d.getFontMetrics().stringWidth(className) + 10, 20);
                g2d.setColor(Color.BLACK);
                g2d.drawString(className, x1 + 5, y1 - 5);
            }
        }
    }
    
    private Color getClassColor(int classId) {
        return classColors[classId % classColors.length];
    }
    
    private Color getCurrentClassColor() {
        return getClassColor(currentClassId);
    }
      @Override
    public void setBounds(int x, int y, int width, int height) {
        super.setBounds(x, y, width, height);
        if (originalImage != null) {
            calculateScale();
            repaint();
        }
    }
    
    // Getters and Setters
    public void setCurrentClass(int classId, String className) {
        this.currentClassId = classId;
        this.currentClassName = className;
    }
    
    public void setClassNames(List<String> classNames) {
        this.classNames = new ArrayList<>(classNames);
    }
    
    public YoloImage getCurrentYoloImage() {
        return currentYoloImage;
    }
      public void clearAnnotations() {
        if (currentYoloImage != null) {
            currentYoloImage.clearAnnotations();
            // Auto-save annotations to file
            try {
                raven.yolo.manager.ProjectManager.getInstance().saveImageAnnotations(currentYoloImage);
            } catch (Exception e) {
                System.err.println("Error saving annotations: " + e.getMessage());
            }
            repaint();
        }
    }
    
    public void removeLastAnnotation() {
        if (currentYoloImage != null && !currentYoloImage.getAnnotations().isEmpty()) {
            List<YoloAnnotation> annotations = currentYoloImage.getAnnotations();
            annotations.remove(annotations.size() - 1);
            currentYoloImage.setLabeled(!annotations.isEmpty());
            // Auto-save annotations to file
            try {
                raven.yolo.manager.ProjectManager.getInstance().saveImageAnnotations(currentYoloImage);
            } catch (Exception e) {
                System.err.println("Error saving annotations: " + e.getMessage());
            }
            repaint();
        }
    }
}
