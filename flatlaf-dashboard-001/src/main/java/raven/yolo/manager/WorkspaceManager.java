package raven.yolo.manager;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import raven.yolo.model.WorkspaceConfig;

import javax.swing.filechooser.FileSystemView;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Workspace Manager
 * Quản lý workspace, cấu hình và các dự án gần đây
 */
public class WorkspaceManager {
    
    private static WorkspaceManager instance;
    private final ObjectMapper objectMapper;
    private WorkspaceConfig workspaceConfig;
    private String configFilePath;
    
    // Default workspace in user home directory
    private static final String DEFAULT_WORKSPACE_DIR = "YoloAnnotationTool";
    private static final String CONFIG_FILE_NAME = "workspace-config.json";
    
    private WorkspaceManager() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.writerWithDefaultPrettyPrinter();
        
        initializeWorkspace();
    }
    
    public static WorkspaceManager getInstance() {
        if (instance == null) {
            instance = new WorkspaceManager();
        }
        return instance;
    }
    
    /**
     * Initialize workspace - tạo workspace mặc định nếu chưa có
     */
    private void initializeWorkspace() {
        try {
            // Get user home directory
            String userHome = System.getProperty("user.home");
            String defaultWorkspacePath = Paths.get(userHome, DEFAULT_WORKSPACE_DIR).toString();
            
            // Create workspace directory if not exists
            File workspaceDir = new File(defaultWorkspacePath);
            if (!workspaceDir.exists()) {
                workspaceDir.mkdirs();
            }
            
            // Configuration file path
            configFilePath = Paths.get(defaultWorkspacePath, CONFIG_FILE_NAME).toString();
            File configFile = new File(configFilePath);
            
            if (configFile.exists()) {
                // Load existing configuration
                loadWorkspaceConfig();
            } else {
                // Create new configuration
                createDefaultWorkspaceConfig(defaultWorkspacePath);
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            // Fallback to minimal configuration
            workspaceConfig = new WorkspaceConfig(System.getProperty("user.home"));
        }
    }
    
    /**
     * Load workspace configuration từ file JSON
     */
    private void loadWorkspaceConfig() throws IOException {
        File configFile = new File(configFilePath);
        if (configFile.exists()) {
            workspaceConfig = objectMapper.readValue(configFile, WorkspaceConfig.class);
            workspaceConfig.setLastOpened(LocalDateTime.now());
            saveWorkspaceConfig();
        }
    }
    
    /**
     * Tạo workspace configuration mặc định
     */
    private void createDefaultWorkspaceConfig(String workspacePath) throws IOException {
        workspaceConfig = new WorkspaceConfig(workspacePath);
        saveWorkspaceConfig();
    }
    
    /**
     * Save workspace configuration to JSON file
     */
    public void saveWorkspaceConfig() throws IOException {
        if (workspaceConfig != null && configFilePath != null) {
            File configFile = new File(configFilePath);
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(configFile, workspaceConfig);
        }
    }
    
    /**
     * Set workspace path và cập nhật cấu hình
     */
    public void setWorkspacePath(String workspacePath) throws IOException {
        if (workspaceConfig == null) {
            workspaceConfig = new WorkspaceConfig();
        }
        
        workspaceConfig.setWorkspacePath(workspacePath);
        
        // Update config file path
        configFilePath = Paths.get(workspacePath, CONFIG_FILE_NAME).toString();
        
        // Create workspace directory if not exists
        File workspaceDir = new File(workspacePath);
        if (!workspaceDir.exists()) {
            workspaceDir.mkdirs();
        }
        
        saveWorkspaceConfig();
    }
    
    /**
     * Get current workspace configuration
     */
    public WorkspaceConfig getWorkspaceConfig() {
        return workspaceConfig;
    }
    
    /**
     * Get workspace path
     */
    public String getWorkspacePath() {
        return workspaceConfig != null ? workspaceConfig.getWorkspacePath() : System.getProperty("user.home");
    }
    
    /**
     * Get current workspace path
     */
    public String getCurrentWorkspacePath() {
        return getWorkspacePath();
    }
    
    /**
     * Add project to recent projects
     */
    public void addRecentProject(String projectName, String projectPath) {
        if (workspaceConfig != null) {
            workspaceConfig.addRecentProject(projectName, projectPath);
            try {
                saveWorkspaceConfig();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Remove project from recent projects
     */
    public void removeRecentProject(String projectPath) {
        if (workspaceConfig != null) {
            workspaceConfig.removeRecentProject(projectPath);
            try {
                saveWorkspaceConfig();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Get recent projects
     */
    public List<WorkspaceConfig.RecentProject> getRecentProjects() {
        return workspaceConfig != null ? workspaceConfig.getRecentProjects() : List.of();
    }
    
    /**
     * Get workspace settings
     */
    public WorkspaceConfig.WorkspaceSettings getSettings() {
        return workspaceConfig != null ? workspaceConfig.getSettings() : new WorkspaceConfig.WorkspaceSettings();
    }
    
    /**
     * Update workspace settings
     */
    public void updateSettings(WorkspaceConfig.WorkspaceSettings settings) {
        if (workspaceConfig != null) {
            workspaceConfig.setSettings(settings);
            try {
                saveWorkspaceConfig();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Check if project exists in workspace
     */
    public boolean projectExists(String projectName) {
        String projectPath = Paths.get(getWorkspacePath(), projectName).toString();
        File projectFile = new File(projectPath, "project.json");
        return projectFile.exists();
    }
    
    /**
     * Get full project path in workspace
     */
    public String getProjectPath(String projectName) {
        return Paths.get(getWorkspacePath(), projectName).toString();
    }
    
    /**
     * Create projects directory structure in workspace
     */
    public void createProjectDirectory(String projectName) throws IOException {
        String projectPath = getProjectPath(projectName);
        File projectDir = new File(projectPath);
        
        if (!projectDir.exists()) {
            projectDir.mkdirs();
        }
        
        // Create subdirectories
        new File(projectDir, "images").mkdirs();
        new File(projectDir, "labels").mkdirs();
        new File(projectDir, "exports").mkdirs();
    }
    
    /**
     * Delete project directory and files
     */
    public void deleteProjectDirectory(String projectPath) throws IOException {
        File projectDir = new File(projectPath);
        if (projectDir.exists()) {
            deleteDirectory(projectDir);
        }
        
        // Remove from recent projects
        removeRecentProject(projectPath);
    }
    
    /**
     * Recursively delete directory
     */
    private void deleteDirectory(File directory) throws IOException {
        if (directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    deleteDirectory(file);
                }
            }
        }
        if (!directory.delete()) {
            throw new IOException("Failed to delete " + directory.getAbsolutePath());
        }
    }
    
    /**
     * Scan workspace for existing projects
     */
    public void scanWorkspaceForProjects() {
        try {
            File workspaceDir = new File(getWorkspacePath());
            if (workspaceDir.exists() && workspaceDir.isDirectory()) {
                File[] subdirs = workspaceDir.listFiles(File::isDirectory);
                if (subdirs != null) {
                    for (File subdir : subdirs) {
                        File projectFile = new File(subdir, "project.json");
                        if (projectFile.exists()) {
                            // Add to recent projects if not already there
                            String projectPath = subdir.getAbsolutePath();
                            boolean exists = getRecentProjects().stream()
                                    .anyMatch(rp -> rp.getProjectPath().equals(projectPath));
                            
                            if (!exists) {
                                addRecentProject(subdir.getName(), projectPath);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
