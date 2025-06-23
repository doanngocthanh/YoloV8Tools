package raven.yolo.components;

import raven.yolo.model.BoundingBox;
import raven.yolo.model.ClassManager;
import raven.yolo.model.YoloImage;
import raven.yolo.model.YoloAnnotation;
import raven.yolo.model.YoloProject;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Enhanced ImageViewer component for YOLO annotation with drag/resize capabilities
 */
public class ImageViewer extends JPanel {
    private BufferedImage image;
    private YoloImage currentYoloImage;
    private List<BoundingBox> boundingBoxes;
    private ClassManager classManager;
      // Current class selection
    private int currentClassId = -1; // -1 means no class selected
    private String currentClassName = null;
    
    // Drawing state
    private boolean isDrawing = false;
    private boolean isDragging = false;
    private boolean isResizing = false;
    private Point startPoint;
    private Point endPoint;
    private Point dragStartPoint;
    private Rectangle currentRect;
    private BoundingBox selectedBox;
    private BoundingBox draggedBox;
    private int resizeHandle = -1; // 0-7 for 8 resize handles
      // Scaling and offset for image display
    private double scale = 1.0;
    private int offsetX = 0;
    private int offsetY = 0;
    
    public ImageViewer(ClassManager classManager) {
        this.classManager = classManager;
        this.boundingBoxes = new ArrayList<>();
        setBackground(Color.DARK_GRAY);
        setFocusable(true); // Allow keyboard input
        setupMouseListeners();
        setupKeyboardListeners();
    }
    
    public ImageViewer() {
        this(null);
    }
    
    private void setupMouseListeners() {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (image == null) return;
                
                Point imagePoint = screenToImageCoordinates(e.getPoint());
                if (imagePoint == null) return;
                
                if (SwingUtilities.isLeftMouseButton(e)) {
                    // Check if clicking on existing bounding box
                    BoundingBox clickedBox = findBoundingBoxAt(imagePoint);
                    if (clickedBox != null) {
                        selectBoundingBox(clickedBox);
                        
                        // Check if clicking on resize handle
                        resizeHandle = getResizeHandle(clickedBox, imagePoint);
                        if (resizeHandle >= 0) {
                            isResizing = true;
                            dragStartPoint = imagePoint;
                        } else {
                            // Start dragging the box
                            isDragging = true;
                            draggedBox = clickedBox;
                            dragStartPoint = imagePoint;
                        }
                    } else {
                        // Start drawing new bounding box
                        deselectAllBoxes();
                        startPoint = imagePoint;
                        isDrawing = true;
                    }
                } else if (SwingUtilities.isRightMouseButton(e)) {
                    // Right click to show context menu
                    BoundingBox clickedBox = findBoundingBoxAt(imagePoint);
                    if (clickedBox != null) {
                        selectBoundingBox(clickedBox);
                        showContextMenu(e.getPoint());
                    }
                }
            }
            
            @Override
            public void mouseReleased(MouseEvent e) {
                if (isDrawing && image != null) {
                    Point imagePoint = screenToImageCoordinates(e.getPoint());
                    if (imagePoint != null) {
                        endPoint = imagePoint;
                        createBoundingBox();
                    }
                    isDrawing = false;
                    currentRect = null;
                    repaint();
                } else if (isDragging || isResizing) {
                    isDragging = false;
                    isResizing = false;
                    draggedBox = null;
                    dragStartPoint = null;
                    resizeHandle = -1;
                    repaint();
                }
            }
        });
        
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (image == null) return;
                
                Point imagePoint = screenToImageCoordinates(e.getPoint());
                if (imagePoint == null) return;
                
                if (isDrawing) {
                    endPoint = imagePoint;
                    updateCurrentRect();
                    repaint();
                } else if (isDragging && draggedBox != null && dragStartPoint != null) {
                    // Move the bounding box
                    int deltaX = imagePoint.x - dragStartPoint.x;
                    int deltaY = imagePoint.y - dragStartPoint.y;
                    moveBoundingBox(draggedBox, deltaX, deltaY);
                    dragStartPoint = imagePoint;
                    repaint();
                } else if (isResizing && selectedBox != null && dragStartPoint != null) {
                    // Resize the bounding box
                    resizeBoundingBox(selectedBox, resizeHandle, imagePoint, dragStartPoint);
                    dragStartPoint = imagePoint;
                    repaint();
                }
            }
            
            @Override
            public void mouseMoved(MouseEvent e) {
                if (image == null) return;
                
                Point imagePoint = screenToImageCoordinates(e.getPoint());
                if (imagePoint == null) {
                    setCursor(Cursor.getDefaultCursor());
                    return;
                }
                
                // Update cursor based on what's under the mouse
                BoundingBox hoveredBox = findBoundingBoxAt(imagePoint);
                if (hoveredBox != null && hoveredBox.isSelected()) {
                    int handle = getResizeHandle(hoveredBox, imagePoint);
                    if (handle >= 0) {
                        setCursor(getResizeCursor(handle));
                    } else {
                        setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                    }
                } else {
                    setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
                }
            }
        });
    }
    
    /**
     * Setup keyboard listeners for shortcuts
     */
    private void setupKeyboardListeners() {
        // Add key bindings for delete
        getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke("DELETE"), "delete");
        getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke("BACK_SPACE"), "delete");
        
        getActionMap().put("delete", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                deleteSelectedBoundingBox();
            }
        });
        
        // Add key binding for escape (deselect)
        getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke("ESCAPE"), "deselect");
        getActionMap().put("deselect", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                deselectAllBoxes();
                repaint();
            }
        });
    }
    
    private Point screenToImageCoordinates(Point screenPoint) {
        if (image == null) return null;
        
        int x = (int) ((screenPoint.x - offsetX) / scale);
        int y = (int) ((screenPoint.y - offsetY) / scale);
        
        // Check if point is within image bounds
        if (x >= 0 && x < image.getWidth() && y >= 0 && y < image.getHeight()) {
            return new Point(x, y);
        }
        return null;
    }
    
    private Point imageToScreenCoordinates(Point imagePoint) {
        int x = (int) (imagePoint.x * scale + offsetX);
        int y = (int) (imagePoint.y * scale + offsetY);
        return new Point(x, y);
    }
    
    private BoundingBox findBoundingBoxAt(Point imagePoint) {
        for (BoundingBox box : boundingBoxes) {
            Rectangle bounds = box.getPixelBounds(image.getWidth(), image.getHeight());
            if (bounds.contains(imagePoint)) {
                return box;
            }
        }
        return null;
    }
    
    private void selectBoundingBox(BoundingBox box) {
        deselectAllBoxes();
        box.setSelected(true);
        selectedBox = box;
        repaint();
    }
    
    private void deselectAllBoxes() {
        for (BoundingBox box : boundingBoxes) {
            box.setSelected(false);
        }
        selectedBox = null;
    }
    
    private void updateCurrentRect() {
        if (startPoint != null && endPoint != null) {
            int x = Math.min(startPoint.x, endPoint.x);
            int y = Math.min(startPoint.y, endPoint.y);
            int width = Math.abs(endPoint.x - startPoint.x);
            int height = Math.abs(endPoint.y - startPoint.y);
            currentRect = new Rectangle(x, y, width, height);
        }
    }    private void createBoundingBox() {
        if (startPoint != null && endPoint != null && image != null) {
            updateCurrentRect();
            if (currentRect.width > 5 && currentRect.height > 5) { // Minimum size
                
                // Check if project exists and has classes
                try {
                    YoloProject currentProject = raven.yolo.manager.ProjectManager.getInstance().getCurrentProject();
                    if (currentProject == null) {
                        JOptionPane.showMessageDialog(this, 
                            "No project is loaded!\n\nPlease create or open a project first.", 
                            "No Project", 
                            JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                    
                    if (currentProject.getClasses().isEmpty()) {
                        JOptionPane.showMessageDialog(this, 
                            "No classes available in the current project!\n\nPlease add classes to the project first using the Class panel.", 
                            "No Classes Available", 
                            JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(this, 
                        "Error checking project: " + e.getMessage(), 
                        "Error", 
                        JOptionPane.ERROR_MESSAGE);
                    return;
                }
                
                // Check if any class is selected
                if (currentClassId < 0 || currentClassName == null) {
                    JOptionPane.showMessageDialog(this, 
                        "Please select a class first!\n\nGo to the Class panel and select a class before creating annotations.", 
                        "No Class Selected", 
                        JOptionPane.WARNING_MESSAGE);
                    return;
                }
                
                BoundingBox box = BoundingBox.fromPixelBounds(
                    currentClassId,
                    currentClassName,
                    currentRect,
                    image.getWidth(),
                    image.getHeight()
                );
                boundingBoxes.add(box);
                saveAnnotations();
            }
        }
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        if (image != null) {
            // Calculate scale and offset to fit image in panel
            calculateScaleAndOffset();
            
            // Draw image
            g2d.drawImage(image, offsetX, offsetY, 
                         (int)(image.getWidth() * scale), 
                         (int)(image.getHeight() * scale), null);
            
            // Draw existing bounding boxes
            drawBoundingBoxes(g2d);
            
            // Draw current rectangle being drawn
            if (isDrawing && currentRect != null) {
                drawCurrentRect(g2d);
            }
        }
        
        g2d.dispose();
    }
    
    private void calculateScaleAndOffset() {
        if (image == null) return;
        
        int panelWidth = getWidth();
        int panelHeight = getHeight();
        int imageWidth = image.getWidth();
        int imageHeight = image.getHeight();
        
        // Calculate scale to fit image in panel
        double scaleX = (double) panelWidth / imageWidth;
        double scaleY = (double) panelHeight / imageHeight;
        scale = Math.min(scaleX, scaleY);
        
        // Center image in panel
        offsetX = (panelWidth - (int)(imageWidth * scale)) / 2;
        offsetY = (panelHeight - (int)(imageHeight * scale)) / 2;
    }
    
    private void drawBoundingBoxes(Graphics2D g2d) {
        for (BoundingBox box : boundingBoxes) {
            Rectangle bounds = box.getPixelBounds(image.getWidth(), image.getHeight());
            
            // Convert to screen coordinates
            Point topLeft = imageToScreenCoordinates(bounds.getLocation());
            int width = (int)(bounds.width * scale);
            int height = (int)(bounds.height * scale);
            
            // Draw bounding box
            g2d.setColor(box.getColor());
            g2d.setStroke(box.isSelected() ? new BasicStroke(3) : new BasicStroke(2));
            g2d.drawRect(topLeft.x, topLeft.y, width, height);
            
            // Draw resize handles for selected box
            if (box.isSelected()) {
                drawResizeHandles(g2d, topLeft.x, topLeft.y, width, height, box.getColor());
            }
              // Draw class label
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 12));
            FontMetrics fm = g2d.getFontMetrics();
            String label = box.getClassName();
            int labelWidth = fm.stringWidth(label);
            int labelHeight = fm.getHeight();
            
            // Calculate label position (avoid drawing outside screen)
            int labelX = topLeft.x;
            int labelY = topLeft.y;
            
            // If label would be drawn above the image, draw it inside the box
            if (labelY - labelHeight < 0) {
                labelY = topLeft.y + labelHeight + 2; // Draw inside box
            } else {
                labelY = topLeft.y - 2; // Draw above box
            }
            
            // Draw label background
            g2d.setColor(box.getColor());
            g2d.fillRect(labelX, labelY - labelHeight, labelWidth + 4, labelHeight);
            
            // Draw label text
            g2d.setColor(Color.WHITE);
            g2d.drawString(label, labelX + 2, labelY - 2);
        }
    }
    
    /**
     * Draw resize handles for selected bounding box
     */
    private void drawResizeHandles(Graphics2D g2d, int x, int y, int width, int height, Color boxColor) {
        int handleSize = 8;
        
        // Handle positions (8 handles around the rectangle)
        Point[] handlePositions = {
            new Point(x - handleSize/2, y - handleSize/2), // NW
            new Point(x + width/2 - handleSize/2, y - handleSize/2), // N
            new Point(x + width - handleSize/2, y - handleSize/2), // NE
            new Point(x + width - handleSize/2, y + height/2 - handleSize/2), // E
            new Point(x + width - handleSize/2, y + height - handleSize/2), // SE
            new Point(x + width/2 - handleSize/2, y + height - handleSize/2), // S
            new Point(x - handleSize/2, y + height - handleSize/2), // SW
            new Point(x - handleSize/2, y + height/2 - handleSize/2) // W
        };
        
        // Draw handles
        g2d.setColor(Color.WHITE);
        g2d.setStroke(new BasicStroke(1));
        for (Point pos : handlePositions) {
            g2d.fillRect(pos.x, pos.y, handleSize, handleSize);
            g2d.setColor(boxColor);
            g2d.drawRect(pos.x, pos.y, handleSize, handleSize);
            g2d.setColor(Color.WHITE);
        }
    }
    
    private void drawCurrentRect(Graphics2D g2d) {
        if (currentRect == null) return;
        
        Point topLeft = imageToScreenCoordinates(currentRect.getLocation());
        int width = (int)(currentRect.width * scale);
        int height = (int)(currentRect.height * scale);
        
        g2d.setColor(Color.YELLOW);
        g2d.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0, new float[]{5}, 0));
        g2d.drawRect(topLeft.x, topLeft.y, width, height);
    }
    
    /**
     * Get resize handle index for a point relative to a bounding box
     * Returns -1 if not on any handle, 0-7 for the 8 handles
     */
    private int getResizeHandle(BoundingBox box, Point imagePoint) {
        Rectangle bounds = box.getPixelBounds(image.getWidth(), image.getHeight());
        int handleSize = Math.max(8, (int)(8 / scale)); // Minimum 8 pixels, scaled
        
        // Define the 8 resize handles
        Rectangle[] handles = {
            new Rectangle(bounds.x - handleSize/2, bounds.y - handleSize/2, handleSize, handleSize), // NW
            new Rectangle(bounds.x + bounds.width/2 - handleSize/2, bounds.y - handleSize/2, handleSize, handleSize), // N
            new Rectangle(bounds.x + bounds.width - handleSize/2, bounds.y - handleSize/2, handleSize, handleSize), // NE
            new Rectangle(bounds.x + bounds.width - handleSize/2, bounds.y + bounds.height/2 - handleSize/2, handleSize, handleSize), // E
            new Rectangle(bounds.x + bounds.width - handleSize/2, bounds.y + bounds.height - handleSize/2, handleSize, handleSize), // SE
            new Rectangle(bounds.x + bounds.width/2 - handleSize/2, bounds.y + bounds.height - handleSize/2, handleSize, handleSize), // S
            new Rectangle(bounds.x - handleSize/2, bounds.y + bounds.height - handleSize/2, handleSize, handleSize), // SW
            new Rectangle(bounds.x - handleSize/2, bounds.y + bounds.height/2 - handleSize/2, handleSize, handleSize) // W
        };
        
        for (int i = 0; i < handles.length; i++) {
            if (handles[i].contains(imagePoint)) {
                return i;
            }
        }
        
        return -1;
    }
    
    /**
     * Get appropriate cursor for resize handle
     */
    private Cursor getResizeCursor(int handle) {
        switch (handle) {
            case 0: case 4: return Cursor.getPredefinedCursor(Cursor.NW_RESIZE_CURSOR); // NW, SE
            case 1: case 5: return Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR);  // N, S
            case 2: case 6: return Cursor.getPredefinedCursor(Cursor.NE_RESIZE_CURSOR); // NE, SW
            case 3: case 7: return Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR);  // E, W
            default: return Cursor.getDefaultCursor();
        }
    }
    
    /**
     * Move a bounding box by the given delta
     */
    private void moveBoundingBox(BoundingBox box, int deltaX, int deltaY) {
        Rectangle bounds = box.getPixelBounds(image.getWidth(), image.getHeight());
        
        // Calculate new position
        int newX = bounds.x + deltaX;
        int newY = bounds.y + deltaY;
        
        // Keep within image bounds
        newX = Math.max(0, Math.min(newX, image.getWidth() - bounds.width));
        newY = Math.max(0, Math.min(newY, image.getHeight() - bounds.height));
        
        // Update bounding box
        Rectangle newBounds = new Rectangle(newX, newY, bounds.width, bounds.height);
        BoundingBox newBox = BoundingBox.fromPixelBounds(
            box.getClassId(),
            box.getClassName(),
            newBounds,
            image.getWidth(),
            image.getHeight()
        );
        newBox.setSelected(true);
        newBox.setColor(box.getColor());
        
        // Replace old box with new one
        int index = boundingBoxes.indexOf(box);
        if (index >= 0) {
            boundingBoxes.set(index, newBox);
            selectedBox = newBox;
            draggedBox = newBox;
            saveAnnotations();
        }
    }
    
    /**
     * Resize a bounding box using the specified handle
     */
    private void resizeBoundingBox(BoundingBox box, int handle, Point currentPoint, Point startPoint) {
        Rectangle bounds = box.getPixelBounds(image.getWidth(), image.getHeight());
        
        int deltaX = currentPoint.x - startPoint.x;
        int deltaY = currentPoint.y - startPoint.y;
        
        int newX = bounds.x;
        int newY = bounds.y;
        int newWidth = bounds.width;
        int newHeight = bounds.height;
        
        // Apply resize based on handle
        switch (handle) {
            case 0: // NW
                newX += deltaX;
                newY += deltaY;
                newWidth -= deltaX;
                newHeight -= deltaY;
                break;
            case 1: // N
                newY += deltaY;
                newHeight -= deltaY;
                break;
            case 2: // NE
                newY += deltaY;
                newWidth += deltaX;
                newHeight -= deltaY;
                break;
            case 3: // E
                newWidth += deltaX;
                break;
            case 4: // SE
                newWidth += deltaX;
                newHeight += deltaY;
                break;
            case 5: // S
                newHeight += deltaY;
                break;
            case 6: // SW
                newX += deltaX;
                newWidth -= deltaX;
                newHeight += deltaY;
                break;
            case 7: // W
                newX += deltaX;
                newWidth -= deltaX;
                break;
        }
        
        // Ensure minimum size
        int minSize = 10;
        if (newWidth < minSize) {
            if (handle == 0 || handle == 6 || handle == 7) {
                newX = bounds.x + bounds.width - minSize;
            }
            newWidth = minSize;
        }
        if (newHeight < minSize) {
            if (handle == 0 || handle == 1 || handle == 2) {
                newY = bounds.y + bounds.height - minSize;
            }
            newHeight = minSize;
        }
        
        // Keep within image bounds
        newX = Math.max(0, newX);
        newY = Math.max(0, newY);
        newWidth = Math.min(newWidth, image.getWidth() - newX);
        newHeight = Math.min(newHeight, image.getHeight() - newY);
        
        // Update bounding box
        Rectangle newBounds = new Rectangle(newX, newY, newWidth, newHeight);
        BoundingBox newBox = BoundingBox.fromPixelBounds(
            box.getClassId(),
            box.getClassName(),
            newBounds,
            image.getWidth(),
            image.getHeight()
        );
        newBox.setSelected(true);
        newBox.setColor(box.getColor());
        
        // Replace old box with new one
        int index = boundingBoxes.indexOf(box);
        if (index >= 0) {
            boundingBoxes.set(index, newBox);
            selectedBox = newBox;
            saveAnnotations();
        }
    }
    
    /**
     * Show context menu for selected bounding box
     */
    private void showContextMenu(Point screenPoint) {
        if (selectedBox == null) return;
        
        JPopupMenu menu = new JPopupMenu();
        
        JMenuItem deleteItem = new JMenuItem("Delete");
        deleteItem.addActionListener(e -> {
            boundingBoxes.remove(selectedBox);
            selectedBox = null;
            saveAnnotations();
            repaint();
        });
        menu.add(deleteItem);
          JMenuItem changeClassItem = new JMenuItem("Change Class");
        changeClassItem.addActionListener(e -> {
            try {
                YoloProject currentProject = raven.yolo.manager.ProjectManager.getInstance().getCurrentProject();
                if (currentProject == null || currentProject.getClasses().isEmpty()) {
                    JOptionPane.showMessageDialog(this, 
                        "No classes available in the current project!\n\nPlease add classes to the project first.", 
                        "No Classes Available", 
                        JOptionPane.WARNING_MESSAGE);
                    return;
                }
                
                // Show class selection dialog from project classes
                String[] classOptions = currentProject.getClasses().toArray(new String[0]);
                String newClassName = (String) JOptionPane.showInputDialog(
                    this,
                    "Select new class:",
                    "Change Class",
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    classOptions,
                    selectedBox.getClassName()
                );
                
                if (newClassName != null && !newClassName.trim().isEmpty()) {
                    // Find class ID from project classes
                    int newClassId = currentProject.getClasses().indexOf(newClassName);
                    if (newClassId >= 0) {
                        BoundingBox newBox = BoundingBox.fromPixelBounds(
                            newClassId,
                            newClassName,
                            selectedBox.getPixelBounds(image.getWidth(), image.getHeight()),
                            image.getWidth(),
                            image.getHeight()
                        );
                        newBox.setSelected(true);
                        
                        int index = boundingBoxes.indexOf(selectedBox);
                        if (index >= 0) {
                            boundingBoxes.set(index, newBox);
                            selectedBox = newBox;
                            saveAnnotations();
                            repaint();
                        }
                    }
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, 
                    "Error changing class: " + ex.getMessage(), 
                    "Error", 
                    JOptionPane.ERROR_MESSAGE);
            }
        });
        menu.add(changeClassItem);
        
        menu.show(this, screenPoint.x, screenPoint.y);
    }
    
    // Public API methods
    
    /**
     * Load a YOLO image for annotation
     */
    public void loadImage(YoloImage yoloImage) {
        try {
            this.currentYoloImage = yoloImage;
            if (yoloImage != null) {
                // Load the actual image file
                java.io.File imageFile = new java.io.File(yoloImage.getPath());
                this.image = javax.imageio.ImageIO.read(imageFile);
                
                // Load existing annotations
                loadAnnotationsFromYoloImage();
                
                repaint();
            } else {
                this.image = null;
                this.boundingBoxes.clear();
                repaint();
            }
        } catch (Exception e) {
            e.printStackTrace();
            javax.swing.JOptionPane.showMessageDialog(this, 
                "Error loading image: " + e.getMessage(), 
                "Error", javax.swing.JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /**
     * Load annotations from YoloImage to BoundingBox list
     */
    private void loadAnnotationsFromYoloImage() {
        boundingBoxes.clear();
        if (currentYoloImage != null && image != null) {
            for (YoloAnnotation annotation : currentYoloImage.getAnnotations()) {
                // Convert YOLO annotation to BoundingBox
                int centerX = (int)(annotation.getXCenter() * image.getWidth());
                int centerY = (int)(annotation.getYCenter() * image.getHeight());
                int width = (int)(annotation.getWidth() * image.getWidth());
                int height = (int)(annotation.getHeight() * image.getHeight());
                
                // Convert center-based to corner-based
                int x = centerX - width / 2;
                int y = centerY - height / 2;                Rectangle rect = new Rectangle(x, y, width, height);
                
                // Get class name safely
                String className = "Unknown";
                try {
                    YoloProject currentProject = raven.yolo.manager.ProjectManager.getInstance().getCurrentProject();
                    if (currentProject != null && annotation.getClassId() < currentProject.getClasses().size()) {
                        className = currentProject.getClasses().get(annotation.getClassId());
                    }
                } catch (Exception e) {
                    // Use default if anything goes wrong
                    className = "Class_" + annotation.getClassId();
                }
                
                BoundingBox box = BoundingBox.fromPixelBounds(
                    annotation.getClassId(),
                    className,
                    rect,
                    image.getWidth(),
                    image.getHeight()
                );
                
                boundingBoxes.add(box);
            }
        }
    }
    
    /**
     * Save current annotations back to YoloImage
     */
    private void saveAnnotations() {
        if (currentYoloImage != null && image != null) {
            List<YoloAnnotation> annotations = new ArrayList<>();
            
            for (BoundingBox box : boundingBoxes) {
                Rectangle rect = box.getPixelBounds(image.getWidth(), image.getHeight());
                
                // Convert to YOLO format (center-based, normalized)
                double centerX = (rect.x + rect.width / 2.0) / image.getWidth();
                double centerY = (rect.y + rect.height / 2.0) / image.getHeight();
                double width = (double)rect.width / image.getWidth();
                double height = (double)rect.height / image.getHeight();
                  YoloAnnotation annotation = new YoloAnnotation(
                    box.getClassId(), box.getClassName(), centerX, centerY, width, height
                );
                annotations.add(annotation);
            }
            
            currentYoloImage.setAnnotations(annotations);
            currentYoloImage.setLabeled(!annotations.isEmpty());
            
            // Auto-save to file
            try {
                raven.yolo.manager.ProjectManager.getInstance().saveImageAnnotations(currentYoloImage);
            } catch (Exception e) {
                System.err.println("Error saving annotations: " + e.getMessage());
            }
        }
    }
    
    /**
     * Get current YOLO image being displayed
     */
    public YoloImage getCurrentYoloImage() {
        return currentYoloImage;
    }
      /**
     * Set current class for new annotations
     */
    public void setCurrentClass(int classId, String className) {
        this.currentClassId = classId;
        this.currentClassName = className;
        System.out.println("ImageViewer: Set current class - ID: " + classId + ", Name: " + className);
    }
    
    /**
     * Get current class info for debugging
     */
    public String getCurrentClassInfo() {
        return "Current class - ID: " + currentClassId + ", Name: " + currentClassName;
    }
    
    /**
     * Remove last annotation
     */
    public void removeLastAnnotation() {
        if (currentYoloImage != null && !boundingBoxes.isEmpty()) {
            boundingBoxes.remove(boundingBoxes.size() - 1);
            saveAnnotations();
            repaint();
        }
    }
    
    /**
     * Clear all annotations
     */
    public void clearAnnotations() {
        if (currentYoloImage != null) {
            boundingBoxes.clear();
            selectedBox = null;
            saveAnnotations();
            repaint();
        }
    }
    
    // Basic image display methods
    
    public void setImage(BufferedImage image) {
        this.image = image;
        boundingBoxes.clear();
        repaint();
    }
    
    public BufferedImage getImage() {
        return image;
    }
    
    public List<BoundingBox> getBoundingBoxes() {
        return new ArrayList<>(boundingBoxes);
    }
    
    public void setBoundingBoxes(List<BoundingBox> boundingBoxes) {
        this.boundingBoxes = new ArrayList<>(boundingBoxes);
        repaint();
    }
    
    public void clearBoundingBoxes() {
        boundingBoxes.clear();
        repaint();
    }
    
    public BoundingBox getSelectedBoundingBox() {
        return selectedBox;
    }
      public void deleteSelectedBoundingBox() {
        if (selectedBox != null) {
            boundingBoxes.remove(selectedBox);
            selectedBox = null;
            saveAnnotations();
            repaint();
        }
    }
    
    /**
     * Set the class manager for annotation
     */
    public void setClassManager(ClassManager classManager) {
        this.classManager = classManager;
    }
    
    /**
     * Get the current class manager
     */
    public ClassManager getClassManager() {
        return classManager;
    }
}
