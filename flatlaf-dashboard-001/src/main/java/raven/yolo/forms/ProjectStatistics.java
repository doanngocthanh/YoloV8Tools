package raven.yolo.forms;

import com.formdev.flatlaf.FlatClientProperties;
import net.miginfocom.swing.MigLayout;
import raven.yolo.manager.ProjectManager;
import raven.yolo.model.YoloProject;
import raven.yolo.model.YoloImage;
import raven.yolo.model.YoloAnnotation;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class ProjectStatistics extends JPanel {
    
    private JLabel projectNameLabel;
    private JLabel totalImagesLabel;
    private JLabel labeledImagesLabel;
    private JLabel totalAnnotationsLabel;
    private JLabel totalClassesLabel;
    private JPanel classDistributionPanel;
    private JProgressBar progressBar;
    
    public ProjectStatistics() {
        initComponents();
        setupLayout();
        updateStatistics();
        
        // Listen for project changes
        ProjectManager.getInstance().addProjectListener(project -> updateStatistics());
    }
    
    private void initComponents() {
        setLayout(new MigLayout("fill,insets 20", "[fill]", "[][][][][][fill]"));
        
        // Title
        JLabel titleLabel = new JLabel("Project Statistics");
        titleLabel.putClientProperty(FlatClientProperties.STYLE, "font:bold +6");
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        // Statistics labels
        projectNameLabel = new JLabel("Project: No project loaded");
        projectNameLabel.putClientProperty(FlatClientProperties.STYLE, "font:bold +2");
        
        totalImagesLabel = new JLabel("Total Images: 0");
        labeledImagesLabel = new JLabel("Labeled Images: 0");
        totalAnnotationsLabel = new JLabel("Total Annotations: 0");
        totalClassesLabel = new JLabel("Total Classes: 0");
        
        // Progress bar
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setString("0% Complete");
        progressBar.putClientProperty(FlatClientProperties.STYLE, "arc:10");
        
        // Class distribution panel
        classDistributionPanel = new JPanel(new MigLayout("fill,insets 10", "[fill]", "[]"));
        classDistributionPanel.setBorder(BorderFactory.createTitledBorder("Class Distribution"));
        classDistributionPanel.putClientProperty(FlatClientProperties.STYLE, "arc:10");
        
        add(titleLabel, "wrap 20");
        add(projectNameLabel, "wrap 10");
        add(createStatisticsPanel(), "wrap 10");
        add(progressBar, "wrap 20");
        add(classDistributionPanel, "");
    }
    
    private JPanel createStatisticsPanel() {
        JPanel statsPanel = new JPanel(new MigLayout("fill,insets 10", "[fill][fill]", "[][]"));
        statsPanel.setBorder(BorderFactory.createTitledBorder("Overview"));
        statsPanel.putClientProperty(FlatClientProperties.STYLE, "arc:10");
        
        // Style labels
        Font labelFont = new Font(Font.SANS_SERIF, Font.PLAIN, 14);
        totalImagesLabel.setFont(labelFont);
        labeledImagesLabel.setFont(labelFont);
        totalAnnotationsLabel.setFont(labelFont);
        totalClassesLabel.setFont(labelFont);
        
        statsPanel.add(totalImagesLabel, "");
        statsPanel.add(labeledImagesLabel, "wrap");
        statsPanel.add(totalAnnotationsLabel, "");
        statsPanel.add(totalClassesLabel, "");
        
        return statsPanel;
    }
    
    private void setupLayout() {
        setBackground(Color.WHITE);
    }
    
    private void updateStatistics() {
        YoloProject project = ProjectManager.getInstance().getCurrentProject();
        
        if (project == null) {
            projectNameLabel.setText("Project: No project loaded");
            totalImagesLabel.setText("Total Images: 0");
            labeledImagesLabel.setText("Labeled Images: 0");
            totalAnnotationsLabel.setText("Total Annotations: 0");
            totalClassesLabel.setText("Total Classes: 0");
            progressBar.setValue(0);
            progressBar.setString("0% Complete");
            classDistributionPanel.removeAll();
            classDistributionPanel.revalidate();
            classDistributionPanel.repaint();
            return;
        }
        
        // Calculate statistics
        int totalImages = project.getImages().size();
        int labeledImages = 0;
        int totalAnnotations = 0;
        Map<String, Integer> classDistribution = new HashMap<>();
        
        for (YoloImage image : project.getImages()) {
            if (image.isLabeled()) {
                labeledImages++;
            }
            totalAnnotations += image.getAnnotations().size();
            
            // Count class distribution
            for (YoloAnnotation annotation : image.getAnnotations()) {
                String className = annotation.getClassName();
                classDistribution.put(className, classDistribution.getOrDefault(className, 0) + 1);
            }
        }
        
        int totalClasses = project.getClasses().size();
        double completionPercentage = totalImages > 0 ? (double) labeledImages / totalImages * 100 : 0;
        
        // Update labels
        projectNameLabel.setText("Project: " + project.getName());
        totalImagesLabel.setText("Total Images: " + totalImages);
        labeledImagesLabel.setText("Labeled Images: " + labeledImages);
        totalAnnotationsLabel.setText("Total Annotations: " + totalAnnotations);
        totalClassesLabel.setText("Total Classes: " + totalClasses);
        
        // Update progress bar
        progressBar.setValue((int) completionPercentage);
        progressBar.setString(String.format("%.1f%% Complete", completionPercentage));
        
        // Update class distribution
        updateClassDistribution(classDistribution);
    }
    
    private void updateClassDistribution(Map<String, Integer> classDistribution) {
        classDistributionPanel.removeAll();
        
        if (classDistribution.isEmpty()) {
            JLabel noDataLabel = new JLabel("No annotations yet");
            noDataLabel.setHorizontalAlignment(SwingConstants.CENTER);
            noDataLabel.setForeground(Color.GRAY);
            classDistributionPanel.add(noDataLabel, "center");
        } else {
            // Find max count for scaling
            int maxCount = classDistribution.values().stream().mapToInt(Integer::intValue).max().orElse(1);
            
            // Color palette for classes
            Color[] colors = {
                Color.decode("#FF6B6B"), Color.decode("#4ECDC4"), Color.decode("#45B7D1"),
                Color.decode("#96CEB4"), Color.decode("#FFEAA7"), Color.decode("#DDA0DD"),
                Color.decode("#98D8C8"), Color.decode("#F7DC6F"), Color.decode("#BB8FCE"),
                Color.decode("#85C1E9")
            };
            
            int colorIndex = 0;
            for (Map.Entry<String, Integer> entry : classDistribution.entrySet()) {
                String className = entry.getKey();
                int count = entry.getValue();
                
                JPanel classPanel = new JPanel(new MigLayout("fill,insets 5", "[grow 0][fill][grow 0]", "[]"));
                classPanel.putClientProperty(FlatClientProperties.STYLE, "arc:5");
                classPanel.setBackground(colors[colorIndex % colors.length]);
                
                // Class name
                JLabel nameLabel = new JLabel(className);
                nameLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
                nameLabel.setForeground(Color.WHITE);
                
                // Progress bar for this class
                JProgressBar classProgressBar = new JProgressBar(0, maxCount);
                classProgressBar.setValue(count);
                classProgressBar.setStringPainted(true);
                classProgressBar.setString(count + " annotations");
                classProgressBar.putClientProperty(FlatClientProperties.STYLE, "arc:3");
                
                // Count label
                JLabel countLabel = new JLabel(String.valueOf(count));
                countLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
                countLabel.setForeground(Color.WHITE);
                
                classPanel.add(nameLabel, "w 80!");
                classPanel.add(classProgressBar, "");
                classPanel.add(countLabel, "w 30!");
                
                classDistributionPanel.add(classPanel, "wrap 5");
                colorIndex++;
            }
        }
        
        classDistributionPanel.revalidate();
        classDistributionPanel.repaint();
    }
}
