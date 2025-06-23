package raven.yolo.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Workspace configuration model
 * Lưu trữ thông tin về workspace và các dự án gần đây
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class WorkspaceConfig {
    
    @JsonProperty("workspace_path")
    private String workspacePath;
    
    @JsonProperty("last_opened")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastOpened;
    
    @JsonProperty("recent_projects")
    private List<RecentProject> recentProjects;
    
    @JsonProperty("settings")
    private WorkspaceSettings settings;
    
    public WorkspaceConfig() {
        this.recentProjects = new ArrayList<>();
        this.settings = new WorkspaceSettings();
        this.lastOpened = LocalDateTime.now();
    }
    
    public WorkspaceConfig(String workspacePath) {
        this();
        this.workspacePath = workspacePath;
    }
    
    // Getters and Setters
    public String getWorkspacePath() {
        return workspacePath;
    }
    
    public void setWorkspacePath(String workspacePath) {
        this.workspacePath = workspacePath;
    }
    
    public LocalDateTime getLastOpened() {
        return lastOpened;
    }
    
    public void setLastOpened(LocalDateTime lastOpened) {
        this.lastOpened = lastOpened;
    }
    
    public List<RecentProject> getRecentProjects() {
        return recentProjects;
    }
    
    public void setRecentProjects(List<RecentProject> recentProjects) {
        this.recentProjects = recentProjects;
    }
    
    public WorkspaceSettings getSettings() {
        return settings;
    }
    
    public void setSettings(WorkspaceSettings settings) {
        this.settings = settings;
    }
    
    /**
     * Add a project to recent projects list
     */
    public void addRecentProject(String projectName, String projectPath) {
        // Remove existing entry if exists
        recentProjects.removeIf(rp -> rp.getProjectPath().equals(projectPath));
        
        // Add to beginning
        recentProjects.add(0, new RecentProject(projectName, projectPath, LocalDateTime.now()));
        
        // Keep only last 10 projects
        if (recentProjects.size() > 10) {
            recentProjects = recentProjects.subList(0, 10);
        }
    }
    
    /**
     * Remove a project from recent projects list
     */
    public void removeRecentProject(String projectPath) {
        recentProjects.removeIf(rp -> rp.getProjectPath().equals(projectPath));
    }
    
    /**
     * Recent project model
     */
    public static class RecentProject {
        @JsonProperty("project_name")
        private String projectName;
        
        @JsonProperty("project_path")
        private String projectPath;
        
        @JsonProperty("last_opened")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime lastOpened;
        
        public RecentProject() {}
        
        public RecentProject(String projectName, String projectPath, LocalDateTime lastOpened) {
            this.projectName = projectName;
            this.projectPath = projectPath;
            this.lastOpened = lastOpened;
        }
        
        // Getters and Setters
        public String getProjectName() {
            return projectName;
        }
        
        public void setProjectName(String projectName) {
            this.projectName = projectName;
        }
        
        public String getProjectPath() {
            return projectPath;
        }
        
        public void setProjectPath(String projectPath) {
            this.projectPath = projectPath;
        }
        
        public LocalDateTime getLastOpened() {
            return lastOpened;
        }
        
        public void setLastOpened(LocalDateTime lastOpened) {
            this.lastOpened = lastOpened;
        }
    }    /**
     * Workspace settings model
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class WorkspaceSettings {
        @JsonProperty("default_image_format")
        private String defaultImageFormat = "jpg";
        
        @JsonProperty("annotation_opacity")
        private float annotationOpacity = 0.3f;
        
        @JsonProperty("show_class_names")
        private boolean showClassNames = true;
        
        @JsonProperty("theme")
        private String theme = "system";
        
        public WorkspaceSettings() {}        
        // Getters and Setters
        public String getDefaultImageFormat() {
            return defaultImageFormat;
        }
        
        public void setDefaultImageFormat(String defaultImageFormat) {
            this.defaultImageFormat = defaultImageFormat;
        }
        
        public float getAnnotationOpacity() {
            return annotationOpacity;
        }
        
        public void setAnnotationOpacity(float annotationOpacity) {
            this.annotationOpacity = annotationOpacity;
        }
        
        public boolean isShowClassNames() {
            return showClassNames;
        }
        
        public void setShowClassNames(boolean showClassNames) {
            this.showClassNames = showClassNames;
        }
        
        public String getTheme() {
            return theme;
        }
        
        public void setTheme(String theme) {
            this.theme = theme;
        }
    }
}
