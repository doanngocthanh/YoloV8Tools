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
import javax.imageio.ImageIO;

public class ImageListPanel extends JPanel {
    
    private DefaultListModel<YoloImage> imageListModel;
    private JList<YoloImage> imageList;
    private JButton addImageButton;
    private JButton removeImageButton;
    
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
    
    private void initComponents() {
        imageListModel = new DefaultListModel<>();
        imageList = new JList<>(imageListModel);
        imageList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        imageList.setCellRenderer(new ImageListCellRenderer());
        
        addImageButton = new JButton("Add Images");
        addImageButton.putClientProperty(FlatClientProperties.STYLE, "arc:5");
        
        removeImageButton = new JButton("Remove");
        removeImageButton.putClientProperty(FlatClientProperties.STYLE, "arc:5");
        removeImageButton.setEnabled(false);
    }
    
    private void setupLayout() {
        setLayout(new MigLayout("fill,insets 10", "[fill]", "[grow 0][fill][grow 0]"));
        
        // Title
        JLabel title = new JLabel("Images");
        title.putClientProperty(FlatClientProperties.STYLE, "font:bold +2");
        add(title, "wrap");
        
        // Image list
        JScrollPane scrollPane = new JScrollPane(imageList);
        scrollPane.setPreferredSize(new Dimension(250, 300));
        scrollPane.putClientProperty(FlatClientProperties.STYLE, "arc:5");
        add(scrollPane, "wrap");
        
        // Button panel
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
    }
    
    private void removeImage() {
        YoloImage selectedImage = imageList.getSelectedValue();
        if (selectedImage != null) {
            int result = JOptionPane.showConfirmDialog(this, 
                "Are you sure you want to remove image '" + selectedImage.getFilename() + "'?", 
                "Confirm Remove", JOptionPane.YES_NO_OPTION);
            
            if (result == JOptionPane.YES_OPTION) {
                try {
                    ProjectManager.getInstance().removeImageFromProject(selectedImage);
                    loadProjectImages();
                } catch (IOException e) {
                    JOptionPane.showMessageDialog(this, "Error removing image: " + e.getMessage(), 
                                                "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }
    
    private void updateSelection() {
        YoloImage selectedImage = imageList.getSelectedValue();
        removeImageButton.setEnabled(selectedImage != null);
        
        if (selectedImage != null && imageSelectionListener != null) {
            imageSelectionListener.onImageSelected(selectedImage);
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
                
                // Try to load thumbnail
                try {
                    File imageFile = new File(image.getPath());
                    if (imageFile.exists()) {
                        BufferedImage originalImage = ImageIO.read(imageFile);
                        if (originalImage != null) {
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
                            
                            setIcon(new ImageIcon(thumbnail));
                        }
                    }
                } catch (IOException e) {
                    // Use default icon if thumbnail can't be loaded
                    setIcon(UIManager.getIcon("FileView.fileIcon"));
                }
                
                // Set preferred size for better layout
                setPreferredSize(new Dimension(200, 60));
            }
            
            return this;
        }
    }
}
