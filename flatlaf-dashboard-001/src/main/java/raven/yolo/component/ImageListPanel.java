package raven.yolo.component;

import com.formdev.flatlaf.FlatClientProperties;
import net.miginfocom.swing.MigLayout;
import raven.yolo.manager.ProjectManager;
import raven.yolo.model.YoloImage;
import raven.yolo.model.YoloProject;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import javax.imageio.ImageIO;

public class ImageListPanel extends JPanel {
    private DefaultListModel<YoloImage> imageListModel;
    private JList<YoloImage> imageList;
    private JButton addImageButton;    // Thumbnail cache to avoid UI freezing
    private static final Map<String, ImageIcon> thumbnailCache = new ConcurrentHashMap<>();
    private static final ImageIcon defaultIcon = createDefaultIcon();
    private JButton removeImageButton;
    private JLabel titleLabel;
    
    public interface ImageSelectionListener {
        void onImageSelected(YoloImage image);
    }
    
    private ImageSelectionListener imageSelectionListener;
    
    public ImageListPanel() {
        initComponents();
        setupLayout();
        setupEventHandlers();
        loadProjectImages();
        
        // Listen for project changes
        ProjectManager.getInstance().addProjectListener(this::onProjectChanged);
    }
      private void initComponents() {        imageListModel = new DefaultListModel<>();
        imageList = new JList<>(imageListModel);
        imageList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION); // Allow multiple selection
        imageList.setCellRenderer(new ImageListCellRenderer());
        
        addImageButton = new JButton("Add Images");
        addImageButton.putClientProperty(FlatClientProperties.STYLE, "arc:5");
          removeImageButton = new JButton("Remove Selected");
        removeImageButton.putClientProperty(FlatClientProperties.STYLE, "arc:5;background:$Component.errorColor");
        removeImageButton.setEnabled(false);
        
        // Create title label
        titleLabel = new JLabel("Images");
        titleLabel.putClientProperty(FlatClientProperties.STYLE, "font:bold +2");
    }    private void setupLayout() {
        setLayout(new MigLayout("fill,insets 10", "[fill]", "[grow 0][fill][grow 0][grow 0]"));
        
        // Title
        add(titleLabel, "wrap");
        
        // Image list
        JScrollPane scrollPane = new JScrollPane(imageList);
        scrollPane.setPreferredSize(new Dimension(250, 300));
        scrollPane.putClientProperty(FlatClientProperties.STYLE, "arc:5");
        add(scrollPane, "wrap");
        
        // Selection control panel
        JPanel selectionPanel = new JPanel(new MigLayout("fill,insets 0", "[fill][fill]", "[]"));
        JButton selectAllButton = new JButton("Select All");
        selectAllButton.putClientProperty(FlatClientProperties.STYLE, "arc:5;font:-1");
        selectAllButton.addActionListener(e -> selectAllImages());
        
        JButton clearSelectionButton = new JButton("Clear");
        clearSelectionButton.putClientProperty(FlatClientProperties.STYLE, "arc:5;font:-1");
        clearSelectionButton.addActionListener(e -> clearSelection());
        
        selectionPanel.add(selectAllButton, "");
        selectionPanel.add(clearSelectionButton, "");
        add(selectionPanel, "wrap");
        
        // Main button panel
        JPanel buttonPanel = new JPanel(new MigLayout("fill,insets 0", "[fill][fill]", "[]"));
        buttonPanel.add(addImageButton, "");
        buttonPanel.add(removeImageButton, "");
        
        add(buttonPanel, "");
    }
    
    private void setupEventHandlers() {
        addImageButton.addActionListener(e -> addImages());
        removeImageButton.addActionListener(e -> removeImage());
        
        imageList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateSelection();
            }
        });
    }
    
    private void addImages() {
        if (ProjectManager.getInstance().getCurrentProject() == null) {
            JOptionPane.showMessageDialog(this, "Please create or open a project first.", 
                                        "No Project", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setMultiSelectionEnabled(true);
        fileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
            @Override
            public boolean accept(File f) {
                if (f.isDirectory()) return true;
                String name = f.getName().toLowerCase();
                return name.endsWith(".jpg") || name.endsWith(".jpeg") || 
                       name.endsWith(".png") || name.endsWith(".bmp");
            }
            
            @Override
            public String getDescription() {
                return "Image files (*.jpg, *.jpeg, *.png, *.bmp)";
            }
        });
        
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File[] selectedFiles = fileChooser.getSelectedFiles();
            
            SwingWorker<Void, Integer> worker = new SwingWorker<Void, Integer>() {                @Override
                protected Void doInBackground() throws Exception {
                    for (int i = 0; i < selectedFiles.length; i++) {
                        final int currentIndex = i;
                        try {
                            ProjectManager.getInstance().addImageToProject(selectedFiles[i]);
                            publish((currentIndex + 1) * 100 / selectedFiles.length);
                        } catch (IOException e) {
                            final int errorIndex = currentIndex;
                            SwingUtilities.invokeLater(() -> 
                                JOptionPane.showMessageDialog(ImageListPanel.this, 
                                    "Error adding image " + selectedFiles[errorIndex].getName() + ": " + e.getMessage(), 
                                    "Error", JOptionPane.ERROR_MESSAGE));
                        }
                    }
                    return null;
                }
                
                @Override
                protected void process(List<Integer> chunks) {
                    // Update progress if needed
                }
                
                @Override
                protected void done() {
                    loadProjectImages();
                }
            };
            
            worker.execute();
        }
    }    private void removeImage() {
        List<YoloImage> selectedImages = imageList.getSelectedValuesList();
        if (!selectedImages.isEmpty()) {
            String message;
            if (selectedImages.size() == 1) {
                message = "Are you sure you want to remove image '" + selectedImages.get(0).getFilename() + "'?";
            } else {
                message = String.format("Are you sure you want to remove %d selected images?", selectedImages.size());
            }
            int result = JOptionPane.showConfirmDialog(this, message, "Confirm Remove", JOptionPane.YES_NO_OPTION);
            if (result == JOptionPane.YES_OPTION) {
                removeImageButton.setEnabled(false);
                addImageButton.setEnabled(false);
                if (selectedImages.size() == 1) {
                    removeImageButton.setText("Removing...");
                } else {
                    removeImageButton.setText("Removing " + selectedImages.size() + "...");
                }
                // Đảm bảo chỉ tạo 1 SwingWorker cho mỗi thao tác
                SwingWorker<String, Integer> worker = new SwingWorker<String, Integer>() {
                    @Override
                    protected String doInBackground() throws Exception {
                        for (int i = selectedImages.size() - 1; i >= 0; i--) {
                            YoloImage imageToRemove = selectedImages.get(i);
                            ProjectManager.getInstance().removeImageFromProject(imageToRemove);
                            int progress = ((selectedImages.size() - i) * 100) / selectedImages.size();
                            publish(progress);
                        }
                        return "SUCCESS";
                    }
                    @Override
                    protected void process(List<Integer> chunks) {
                        if (!chunks.isEmpty()) {
                            int progress = chunks.get(chunks.size() - 1);
                            removeImageButton.setText("Removing... " + progress + "%");
                        }
                    }
                    @Override
                    protected void done() {
                        try {
                            get();
                            loadProjectImages();
                            // Clear thumbnail cache nếu cần
                            thumbnailCache.clear();
                            if (selectedImages.size() > 1) {
                                JOptionPane.showMessageDialog(ImageListPanel.this, String.format("Successfully removed %d images.", selectedImages.size()), "Success", JOptionPane.INFORMATION_MESSAGE);
                            }
                        } catch (Exception e) {
                            String errorMessage = e.getCause() instanceof IOException ? e.getCause().getMessage() : e.getMessage();
                            JOptionPane.showMessageDialog(ImageListPanel.this, "Error removing images: " + errorMessage, "Error", JOptionPane.ERROR_MESSAGE);
                            loadProjectImages();
                        } finally {
                            addImageButton.setEnabled(true);
                            removeImageButton.setEnabled(false);
                            removeImageButton.setText("Remove Selected");
                            updateSelection();
                            // Nếu có custom dialog, luôn dispose trên EDT
                            // Example: if (customDialog != null) SwingUtilities.invokeLater(() -> customDialog.dispose());
                        }
                    }
                };
                worker.execute();
            }
        }
    }
      private void updateSelection() {
        List<YoloImage> selectedImages = imageList.getSelectedValuesList();
        removeImageButton.setEnabled(!selectedImages.isEmpty());
        
        // Update title with selection info
        int totalImages = imageListModel.getSize();
        if (selectedImages.isEmpty()) {
            titleLabel.setText(String.format("Images (%d)", totalImages));
            removeImageButton.setText("Remove Selected");
        } else if (selectedImages.size() == 1) {
            titleLabel.setText(String.format("Images (%d) - 1 selected", totalImages));
            removeImageButton.setText("Remove Selected");
        } else {
            titleLabel.setText(String.format("Images (%d) - %d selected", totalImages, selectedImages.size()));
            removeImageButton.setText("Remove " + selectedImages.size() + " Images");
        }
        
        // For single selection, still notify listener
        if (selectedImages.size() == 1 && imageSelectionListener != null) {
            imageSelectionListener.onImageSelected(selectedImages.get(0));
        }
    }
      private void loadProjectImages() {
        imageListModel.clear();
        YoloProject project = ProjectManager.getInstance().getCurrentProject();
        if (project != null) {
            List<YoloImage> images = project.getImages();
            for (YoloImage image : images) {
                imageListModel.addElement(image);
            }
        }
        
        // Update title and UI after loading
        updateSelection();
    }
    
    private void onProjectChanged(YoloProject project) {
        SwingUtilities.invokeLater(this::loadProjectImages);
    }
    
    public void setImageSelectionListener(ImageSelectionListener listener) {
        this.imageSelectionListener = listener;
    }
    
    public YoloImage getSelectedImage() {
        return imageList.getSelectedValue();
    }
    
    // Custom cell renderer for image list
    private static class ImageListCellRenderer extends DefaultListCellRenderer {
        
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                    boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            
            if (value instanceof YoloImage) {
                YoloImage image = (YoloImage) value;
                
                // Create display text
                String displayText = String.format("<html><b>%s</b><br/>%dx%d<br/>%d annotations%s</html>", 
                    image.getFilename(),
                    image.getWidth(), image.getHeight(),
                    image.getAnnotations().size(),
                    image.isLabeled() ? " ✓" : "");
                  setText(displayText);
                
                // Load thumbnail asynchronously
                loadThumbnailAsync(image, this);
                
                // Set preferred size for better layout
                setPreferredSize(new Dimension(200, 60));
            }            return this;
        }
        
        private void loadThumbnailAsync(YoloImage image, JLabel label) {
            String imagePath = image.getPath();
            
            // Check cache first
            ImageIcon cachedIcon = thumbnailCache.get(imagePath);
            if (cachedIcon != null) {
                label.setIcon(cachedIcon);
                return;
            }
            
            // Set default icon immediately
            label.setIcon(defaultIcon);
            
            // Load thumbnail in background
            SwingWorker<ImageIcon, Void> worker = new SwingWorker<ImageIcon, Void>() {
                @Override
                protected ImageIcon doInBackground() throws Exception {
                    try {
                        File imageFile = new File(imagePath);
                        if (!imageFile.exists()) {
                            return defaultIcon;
                        }
                        
                        BufferedImage originalImage = ImageIO.read(imageFile);
                        if (originalImage == null) {
                            return defaultIcon;
                        }
                        
                        // Create thumbnail
                        int thumbSize = 48;
                        int thumbWidth = thumbSize;
                        int thumbHeight = thumbSize;
                        
                        if (originalImage.getWidth() > originalImage.getHeight()) {
                            thumbHeight = (int) ((double) thumbSize * originalImage.getHeight() / originalImage.getWidth());
                        } else {
                            thumbWidth = (int) ((double) thumbSize * originalImage.getWidth() / originalImage.getHeight());
                        }
                        
                        BufferedImage thumbnail = new BufferedImage(thumbWidth, thumbHeight, BufferedImage.TYPE_INT_RGB);
                        Graphics2D g2d = thumbnail.createGraphics();
                        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                        g2d.drawImage(originalImage, 0, 0, thumbWidth, thumbHeight, null);
                        g2d.dispose();
                        
                        ImageIcon icon = new ImageIcon(thumbnail);
                        
                        // Cache the thumbnail
                        thumbnailCache.put(imagePath, icon);
                        
                        return icon;
                    } catch (Exception e) {
                        return defaultIcon;
                    }
                }
                
                @Override
                protected void done() {
                    try {
                        ImageIcon icon = get();
                        if (icon != null && label.isDisplayable()) {
                            label.setIcon(icon);
                            label.repaint();
                        }
                    } catch (Exception e) {
                        // Ignore errors
                    }
                }
            };
            
            worker.execute();
        }
    }
    
    /**
     * Select all images in the list
     */
    private void selectAllImages() {
        if (imageListModel.getSize() > 0) {
            imageList.setSelectionInterval(0, imageListModel.getSize() - 1);
        }
    }
    
    /**
     * Clear current selection
     */
    private void clearSelection() {
        imageList.clearSelection();
    }
    
    private static ImageIcon createDefaultIcon() {
        // Create a simple default icon instead of using UIManager to avoid casting issues
        BufferedImage defaultImage = new BufferedImage(16, 16, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = defaultImage.createGraphics();
        g2d.setColor(Color.LIGHT_GRAY);
        g2d.fillRect(0, 0, 16, 16);
        g2d.setColor(Color.DARK_GRAY);
        g2d.drawRect(0, 0, 15, 15);
        g2d.dispose();
        return new ImageIcon(defaultImage);
    }
}
