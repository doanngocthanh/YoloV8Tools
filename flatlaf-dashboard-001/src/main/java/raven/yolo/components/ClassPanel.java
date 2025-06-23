package raven.yolo.components;

import com.formdev.flatlaf.FlatClientProperties;
import raven.yolo.model.ClassManager;

import javax.swing.*;
import java.awt.*;

/**
 * Panel for managing YOLO classes
 */
public class ClassPanel extends JPanel {
    private ClassManager classManager;
    private DefaultListModel<String> listModel;
    private JList<String> classList;
    private JTextField classNameField;
    private JButton addButton;
    private JButton removeButton;
    private JButton editButton;
    
    public ClassPanel(ClassManager classManager) {
        this.classManager = classManager;
        initComponents();
        setupLayout();
        setupEventListeners();
        updateClassList();
    }
    
    private void initComponents() {
        setLayout(new BorderLayout(5, 5));
        setBorder(BorderFactory.createTitledBorder("Classes"));
        
        // Class list
        listModel = new DefaultListModel<>();
        classList = new JList<>(listModel);
        classList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        classList.putClientProperty(FlatClientProperties.STYLE, "background:$Panel.background");
        
        // Input field
        classNameField = new JTextField();
        classNameField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Enter class name...");
        
        // Buttons
        addButton = new JButton("Add");
        removeButton = new JButton("Remove");
        editButton = new JButton("Edit");
        
        addButton.putClientProperty(FlatClientProperties.STYLE, "background:$Component.accentColor");
        removeButton.putClientProperty(FlatClientProperties.STYLE, "background:$Component.error.focusedBorderColor");
    }
    
    private void setupLayout() {
        // Top panel for input and add button
        JPanel inputPanel = new JPanel(new BorderLayout(5, 5));
        inputPanel.add(classNameField, BorderLayout.CENTER);
        inputPanel.add(addButton, BorderLayout.EAST);
        
        // Bottom panel for buttons
        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(editButton);
        buttonPanel.add(removeButton);
        
        // Scroll pane for list
        JScrollPane scrollPane = new JScrollPane(classList);
        scrollPane.setPreferredSize(new Dimension(200, 150));
        scrollPane.putClientProperty(FlatClientProperties.STYLE, "border:1,1,1,1,$Component.borderColor,,5");
        
        add(inputPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
    }
    
    private void setupEventListeners() {
        addButton.addActionListener(e -> addClass());
        
        removeButton.addActionListener(e -> removeClass());
        
        editButton.addActionListener(e -> editClass());
        
        classNameField.addActionListener(e -> addClass());
        
        classList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int selectedIndex = classList.getSelectedIndex();
                if (selectedIndex >= 0) {
                    classManager.setSelectedClassId(selectedIndex);
                    classNameField.setText(classManager.getSelectedClassName());
                }
                updateButtonStates();
            }
        });
    }
    
    private void addClass() {
        String className = classNameField.getText().trim();
        if (!className.isEmpty()) {
            classManager.addClass(className);
            updateClassList();
            classNameField.setText("");
            classNameField.requestFocus();
        }
    }
    
    private void removeClass() {
        int selectedIndex = classList.getSelectedIndex();
        if (selectedIndex >= 0) {
            int result = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to remove this class?",
                "Confirm Removal",
                JOptionPane.YES_NO_OPTION
            );
            if (result == JOptionPane.YES_OPTION) {
                classManager.removeClass(selectedIndex);
                updateClassList();
            }
        }
    }
    
    private void editClass() {
        int selectedIndex = classList.getSelectedIndex();
        String newName = classNameField.getText().trim();
        if (selectedIndex >= 0 && !newName.isEmpty()) {
            classManager.updateClass(selectedIndex, newName);
            updateClassList();
            classList.setSelectedIndex(selectedIndex);
        }
    }
    
    private void updateClassList() {
        listModel.clear();
        for (String className : classManager.getClasses()) {
            listModel.addElement(className);
        }
        
        // Select current class
        int selectedClassId = classManager.getSelectedClassId();
        if (selectedClassId < listModel.getSize()) {
            classList.setSelectedIndex(selectedClassId);
        }
        
        updateButtonStates();
    }
    
    private void updateButtonStates() {
        boolean hasSelection = classList.getSelectedIndex() >= 0;
        removeButton.setEnabled(hasSelection);
        editButton.setEnabled(hasSelection && !classNameField.getText().trim().isEmpty());
    }
    
    public void refreshClassList() {
        updateClassList();
    }
}
