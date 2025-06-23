package raven.yolo.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages YOLO class labels
 */
public class ClassManager {
    private List<String> classes;
    private int selectedClassId;
    
    public ClassManager() {
        this.classes = new ArrayList<>();
        this.selectedClassId = 0;
        initializeDefaultClasses();
    }
    
    private void initializeDefaultClasses() {
        // Add some default classes
        classes.add("person");
        classes.add("car");
        classes.add("truck");
        classes.add("bus");
        classes.add("motorcycle");
        classes.add("bicycle");
        classes.add("dog");
        classes.add("cat");
        classes.add("bird");
        classes.add("object");
    }
    
    public void addClass(String className) {
        if (!classes.contains(className)) {
            classes.add(className);
        }
    }
    
    public void removeClass(int classId) {
        if (classId >= 0 && classId < classes.size()) {
            classes.remove(classId);
            if (selectedClassId >= classes.size()) {
                selectedClassId = Math.max(0, classes.size() - 1);
            }
        }
    }
    
    public void updateClass(int classId, String newName) {
        if (classId >= 0 && classId < classes.size()) {
            classes.set(classId, newName);
        }
    }
    
    public String getClassName(int classId) {
        if (classId >= 0 && classId < classes.size()) {
            return classes.get(classId);
        }
        return "unknown";
    }
    
    public int getClassId(String className) {
        return classes.indexOf(className);
    }
    
    public List<String> getClasses() {
        return new ArrayList<>(classes);
    }
    
    public int getSelectedClassId() {
        return selectedClassId;
    }
    
    public void setSelectedClassId(int selectedClassId) {
        if (selectedClassId >= 0 && selectedClassId < classes.size()) {
            this.selectedClassId = selectedClassId;
        }
    }
    
    public String getSelectedClassName() {
        return getClassName(selectedClassId);
    }
    
    public int getClassCount() {
        return classes.size();
    }
    
    // Export classes to classes.txt format
    public String exportClassesToString() {
        StringBuilder sb = new StringBuilder();
        for (String className : classes) {
            sb.append(className).append("\n");
        }
        return sb.toString();
    }
    
    // Import classes from classes.txt format
    public void importClassesFromString(String classesText) {
        classes.clear();
        String[] lines = classesText.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (!line.isEmpty()) {
                classes.add(line);
            }
        }
        selectedClassId = 0;
    }
}
