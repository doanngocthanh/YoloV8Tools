package raven.yolo.forms;

import com.formdev.flatlaf.FlatClientProperties;
import net.miginfocom.swing.MigLayout;
import raven.components.FormManager;
import raven.yolo.manager.ProjectManager;
import raven.yolo.model.YoloProject;

import javax.swing.*;
import java.awt.*;
import java.io.File;

public class QuickStartPanel extends JPanel {
    
    private JButton createProjectCard;
    private JButton openProjectCard;
    private JButton startAnnotationCard;
    private JButton importDatasetCard;
    private JLabel statusLabel;
    private JPanel recentProjectsPanel;
    
    public QuickStartPanel() {
        initComponents();
        setupLayout();
        setupEventHandlers();
        updateRecentProjects();
        
        // Listen for project changes
        ProjectManager.getInstance().addProjectListener(this::onProjectChanged);
    }
    
    private void initComponents() {
        setOpaque(false);
        
        // Create large card-style buttons
        createProjectCard = createCard("Create New Project", 
            "Start a new YOLO annotation project", 
            "🆕", new Color(76, 175, 80));
            
        openProjectCard = createCard("Open Project", 
            "Open an existing project", 
            "📂", new Color(33, 150, 243));
            
        startAnnotationCard = createCard("Start Annotation", 
            "Begin annotating images", 
            "✏️", new Color(255, 152, 0));
            
        importDatasetCard = createCard("Import Dataset", 
            "Import existing YOLO dataset", 
            "📥", new Color(156, 39, 176));
        
        statusLabel = new JLabel("No project loaded. Create or open a project to get started.");
        statusLabel.putClientProperty(FlatClientProperties.STYLE, 
            "font:+1;foreground:$Label.disabledForeground");
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        recentProjectsPanel = new JPanel(new MigLayout("fillx,insets 0", "[fill]", "[]"));
        recentProjectsPanel.setOpaque(false);
    }
    
    private JButton createCard(String title, String description, String icon, Color accentColor) {
        JButton card = new JButton();
        card.setLayout(new MigLayout("fill,insets 20", "[grow 0][fill]", "[][grow 0]"));
        card.setPreferredSize(new Dimension(280, 120));
        card.putClientProperty(FlatClientProperties.STYLE, 
            "arc:15;borderWidth:1;focusWidth:2;innerFocusWidth:0");
        
        // Icon
        JLabel iconLabel = new JLabel(icon);
        iconLabel.putClientProperty(FlatClientProperties.STYLE, "font:bold +8");
        iconLabel.setForeground(accentColor);
        
        // Title
        JLabel titleLabel = new JLabel(title);
        titleLabel.putClientProperty(FlatClientProperties.STYLE, "font:bold +2");
        
        // Description
        JLabel descLabel = new JLabel(description);
        descLabel.putClientProperty(FlatClientProperties.STYLE, 
            "font:-1;foreground:$Label.disabledForeground");
        
        card.add(iconLabel, "spany 2,aligny top");
        card.add(titleLabel, "wrap");
        card.add(descLabel, "");
        
        return card;
    }
    
    private void setupLayout() {
        setLayout(new MigLayout("fill,insets 30", "[fill]", "[][20][fill][20][fill]"));
        
        // Welcome header
        JLabel welcomeLabel = new JLabel("Welcome to YOLO v8 Annotation Tool");
        welcomeLabel.putClientProperty(FlatClientProperties.STYLE, "font:bold +6");
        welcomeLabel.setHorizontalAlignment(SwingConstants.CENTER);
        add(welcomeLabel, "wrap");
        
        add(statusLabel, "wrap");
        
        // Quick action cards
        JPanel cardsPanel = new JPanel(new MigLayout("fill,insets 0", "[fill][fill]", "[fill][fill]"));
        cardsPanel.setOpaque(false);
        
        cardsPanel.add(createProjectCard, "");
        cardsPanel.add(openProjectCard, "wrap");
        cardsPanel.add(startAnnotationCard, "");
        cardsPanel.add(importDatasetCard, "");
        
        add(cardsPanel, "wrap");
        
        // Recent projects section
        JLabel recentLabel = new JLabel("Recent Projects");
        recentLabel.putClientProperty(FlatClientProperties.STYLE, "font:bold +2");
        add(recentLabel, "wrap 10");
        
        JScrollPane recentScrollPane = new JScrollPane(recentProjectsPanel);
        recentScrollPane.setOpaque(false);
        recentScrollPane.getViewport().setOpaque(false);
        recentScrollPane.setBorder(BorderFactory.createEmptyBorder());
        recentScrollPane.setPreferredSize(new Dimension(0, 150));
        add(recentScrollPane, "");
    }
    
    private void setupEventHandlers() {
        createProjectCard.addActionListener(e -> createNewProject());
        openProjectCard.addActionListener(e -> openProject());
        startAnnotationCard.addActionListener(e -> startAnnotation());
        importDatasetCard.addActionListener(e -> importDataset());
    }
    
    private void createNewProject() {
        SwingUtilities.invokeLater(() -> {
            Frame parentFrame = (Frame) SwingUtilities.getAncestorOfClass(Frame.class, this);
            ProjectCreationDialog dialog = new ProjectCreationDialog(parentFrame);
            dialog.setVisible(true);
            
            if (dialog.isConfirmed()) {
                updateRecentProjects();
                updateStatus();
            }
        });
    }
    
    private void openProject() {
        SwingUtilities.invokeLater(() -> {
            Frame parentFrame = (Frame) SwingUtilities.getAncestorOfClass(Frame.class, this);
            ProjectManagerDialog dialog = new ProjectManagerDialog(parentFrame);
            dialog.setVisible(true);
            
            if (dialog.isConfirmed() && dialog.getSelectedProject() != null) {
                ProjectManager.getInstance().setCurrentProject(dialog.getSelectedProject());
                updateStatus();
            }
        });
    }
    
    private void startAnnotation() {
        YoloProject currentProject = ProjectManager.getInstance().getCurrentProject();
        if (currentProject == null) {
            JOptionPane.showMessageDialog(this, 
                "Please create or open a project first before starting annotation.", 
                "No Project", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        FormManager.getInstance().showForm("YOLO Annotation Tool", new YoloAnnotationForm());
    }
    
    private void importDataset() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fileChooser.setDialogTitle("Select YOLO Dataset Directory");
        
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedDir = fileChooser.getSelectedFile();
            
            // Check if it's a valid YOLO dataset structure
            File imagesDir = new File(selectedDir, "images");
            File labelsDir = new File(selectedDir, "labels");
            
            if (!imagesDir.exists() || !labelsDir.exists()) {
                int option = JOptionPane.showConfirmDialog(this,
                    "This doesn't appear to be a standard YOLO dataset structure.\n" +
                    "Expected: images/ and labels/ folders.\n" +
                    "Do you want to create a new project and import images from this directory?",
                    "Import Dataset", JOptionPane.YES_NO_OPTION);
                
                if (option == JOptionPane.YES_OPTION) {
                    createProjectFromDirectory(selectedDir);
                }
            } else {
                importYoloDataset(selectedDir);
            }
        }
    }
    
    private void createProjectFromDirectory(File directory) {        SwingUtilities.invokeLater(() -> {
            Frame parentFrame = (Frame) SwingUtilities.getAncestorOfClass(Frame.class, this);
            ProjectCreationDialog dialog = new ProjectCreationDialog(parentFrame);
            // TODO: Add setImportDirectory method to ProjectCreationDialog
            dialog.setVisible(true);
            
            if (dialog.isConfirmed()) {
                updateRecentProjects();
                updateStatus();
            }
        });
    }
    
    private void importYoloDataset(File datasetDir) {
        try {
            // Create project name from directory name
            String projectName = datasetDir.getName() + "_imported";
            File projectsDir = new File(System.getProperty("user.home"), "YoloProjects");
            File projectDir = new File(projectsDir, projectName);
            
            // Check if project already exists
            int counter = 1;
            while (projectDir.exists()) {
                projectDir = new File(projectsDir, projectName + "_" + counter);
                counter++;
            }
              YoloProject project = ProjectManager.getInstance().createProject(
                projectDir.getName(), "Imported from " + datasetDir.getName(), projectDir.getAbsolutePath());
            
            // Import images and labels
            importImagesAndLabels(datasetDir, project);
            
            ProjectManager.getInstance().setCurrentProject(project);
            updateStatus();
            
            JOptionPane.showMessageDialog(this, 
                "Dataset imported successfully as project: " + project.getName(), 
                "Import Complete", JOptionPane.INFORMATION_MESSAGE);
                
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, 
                "Failed to import dataset: " + e.getMessage(), 
                "Import Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void importImagesAndLabels(File datasetDir, YoloProject project) throws Exception {
        File imagesDir = new File(datasetDir, "images");
        File labelsDir = new File(datasetDir, "labels");
        
        // Copy images
        File[] imageFiles = imagesDir.listFiles((dir, name) -> 
            name.toLowerCase().endsWith(".jpg") || 
            name.toLowerCase().endsWith(".jpeg") || 
            name.toLowerCase().endsWith(".png"));
        
        if (imageFiles != null) {
            for (File imageFile : imageFiles) {
                ProjectManager.getInstance().addImageToProject(imageFile);
            }
        }
        
        // Import existing labels if available
        File[] labelFiles = labelsDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".txt"));
        if (labelFiles != null) {
            // TODO: Import label files - would need to parse YOLO format
            // This is a complex task that would require reading each .txt file
            // and converting to our annotation format
        }
    }
    
    private void updateRecentProjects() {
        recentProjectsPanel.removeAll();
        
        java.util.List<String> recentProjects = ProjectManager.getInstance().getRecentProjects();
        
        if (recentProjects.isEmpty()) {
            JLabel noProjectsLabel = new JLabel("No recent projects");
            noProjectsLabel.putClientProperty(FlatClientProperties.STYLE, 
                "foreground:$Label.disabledForeground");
            recentProjectsPanel.add(noProjectsLabel, "");
        } else {
            for (String projectPath : recentProjects) {
                try {
                    YoloProject project = ProjectManager.getInstance().loadProject(projectPath);
                    JButton projectButton = createRecentProjectButton(project);
                    recentProjectsPanel.add(projectButton, "wrap 5");
                } catch (Exception e) {
                    // Skip invalid projects
                }
            }
        }
        
        recentProjectsPanel.revalidate();
        recentProjectsPanel.repaint();
    }
    
    private JButton createRecentProjectButton(YoloProject project) {
        JButton button = new JButton();
        button.setLayout(new MigLayout("fill,insets 10", "[grow 0][fill][grow 0]", "[][]"));
        button.putClientProperty(FlatClientProperties.STYLE, 
            "arc:8;borderWidth:1");
        
        // Project icon
        JLabel iconLabel = new JLabel("📁");
        iconLabel.putClientProperty(FlatClientProperties.STYLE, "font:+2");
        
        // Project info
        JLabel nameLabel = new JLabel(project.getName());
        nameLabel.putClientProperty(FlatClientProperties.STYLE, "font:bold");
        
        JLabel pathLabel = new JLabel(project.getProjectPath());
        pathLabel.putClientProperty(FlatClientProperties.STYLE, 
            "font:-2;foreground:$Label.disabledForeground");
        
        // Stats
        int imageCount = project.getImages().size();
        int annotatedCount = (int) project.getImages().stream()
            .mapToLong(img -> img.getAnnotations().size()).count();
        JLabel statsLabel = new JLabel(imageCount + " images, " + annotatedCount + " annotated");
        statsLabel.putClientProperty(FlatClientProperties.STYLE, 
            "font:-1;foreground:$Label.disabledForeground");
        
        button.add(iconLabel, "spany 2");
        button.add(nameLabel, "wrap");
        button.add(pathLabel, "wrap");
        button.add(statsLabel, "span 2");
        
        button.addActionListener(e -> {
            ProjectManager.getInstance().setCurrentProject(project);
            updateStatus();
        });
        
        return button;
    }
    
    private void updateStatus() {
        YoloProject currentProject = ProjectManager.getInstance().getCurrentProject();
        if (currentProject != null) {
            statusLabel.setText("Current project: " + currentProject.getName() + 
                " (" + currentProject.getImages().size() + " images)");
            startAnnotationCard.setEnabled(true);
        } else {
            statusLabel.setText("No project loaded. Create or open a project to get started.");
            startAnnotationCard.setEnabled(false);
        }
    }
    
    private void onProjectChanged(YoloProject project) {
        SwingUtilities.invokeLater(() -> {
            updateStatus();
            updateRecentProjects();
        });
    }
}
