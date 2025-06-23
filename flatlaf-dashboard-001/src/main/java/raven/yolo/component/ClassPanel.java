package raven.yolo.component;

import com.formdev.flatlaf.FlatClientProperties;
import net.miginfocom.swing.MigLayout;
import raven.yolo.manager.ProjectManager;
import raven.yolo.model.YoloProject;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.List;

public class ClassPanel extends JPanel {
    
    private DefaultListModel<String> classListModel;
    private JList<String> classList;
    private JTextField classNameField;
    private JButton addButton;
    private JButton removeButton;
    private int selectedClassIndex = 0;
    
    // Listener for class selection changes
    public interface ClassSelectionListener {
        void onClassSelected(int classId, String className);
    }
    
    private ClassSelectionListener classSelectionListener;
    
    public ClassPanel() {
        initComponents();
        setupLayout();
        setupEventHandlers();
        loadProjectClasses();
        
        // Listen for project changes
        ProjectManager.getInstance().addProjectListener(this::onProjectChanged);
    }
    
    private void initComponents() {
        classListModel = new DefaultListModel<>();
        classList = new JList<>(classListModel);
        classList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        classList.setCellRenderer(new ClassListCellRenderer());
        
        classNameField = new JTextField();
        classNameField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Enter class name...");
        
        addButton = new JButton("Add");
        addButton.putClientProperty(FlatClientProperties.STYLE, "arc:5");
        
        removeButton = new JButton("Remove");
        removeButton.putClientProperty(FlatClientProperties.STYLE, "arc:5");
        removeButton.setEnabled(false);
    }
    
    private void setupLayout() {
        setLayout(new MigLayout("fill,insets 10", "[fill]", "[grow 0][fill][grow 0]"));
        
        // Title
        JLabel title = new JLabel("Classes");
        title.putClientProperty(FlatClientProperties.STYLE, "font:bold +2");
        add(title, "wrap");
        
        // Class list
        JScrollPane scrollPane = new JScrollPane(classList);
        scrollPane.setPreferredSize(new Dimension(200, 200));
        scrollPane.putClientProperty(FlatClientProperties.STYLE, "arc:5");
        add(scrollPane, "wrap");
        
        // Input panel
        JPanel inputPanel = new JPanel(new MigLayout("fill,insets 0", "[fill][grow 0][grow 0]", "[]"));
        inputPanel.add(classNameField, "");
        inputPanel.add(addButton, "w 60!");
        inputPanel.add(removeButton, "w 70!");
        
        add(inputPanel, "");
    }
    
    private void setupEventHandlers() {
        addButton.addActionListener(e -> addClass());
        removeButton.addActionListener(e -> removeClass());
        
        classNameField.addActionListener(e -> addClass());
        
        classList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateSelection();
            }
        });
    }
    
    private void addClass() {
        String className = classNameField.getText().trim();
        if (!className.isEmpty()) {
            try {
                ProjectManager.getInstance().addClass(className);
                classNameField.setText("");
                loadProjectClasses();
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Error adding class: " + e.getMessage(), 
                                            "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private void removeClass() {
        String selectedClass = classList.getSelectedValue();
        if (selectedClass != null) {
            int result = JOptionPane.showConfirmDialog(this, 
                "Are you sure you want to remove class '" + selectedClass + "'?", 
                "Confirm Remove", JOptionPane.YES_NO_OPTION);
            
            if (result == JOptionPane.YES_OPTION) {
                try {
                    ProjectManager.getInstance().removeClass(selectedClass);
                    loadProjectClasses();
                } catch (IOException e) {
                    JOptionPane.showMessageDialog(this, "Error removing class: " + e.getMessage(), 
                                                "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }
      private void updateSelection() {
        int selectedIndex = classList.getSelectedIndex();
        String selectedClass = classList.getSelectedValue();
        
        removeButton.setEnabled(selectedIndex >= 0);
        
        if (selectedIndex >= 0 && selectedClass != null) {
            selectedClassIndex = selectedIndex;
            System.out.println("ClassPanel: Selection updated - Index: " + selectedIndex + ", Class: " + selectedClass);
            if (classSelectionListener != null) {
                classSelectionListener.onClassSelected(selectedIndex, selectedClass);
            } else {
                System.out.println("ClassPanel: No selection listener set!");
            }
        }
    }
      private void loadProjectClasses() {
        classListModel.clear();
        YoloProject project = ProjectManager.getInstance().getCurrentProject();
        if (project != null) {
            List<String> classes = project.getClasses();
            for (String className : classes) {
                classListModel.addElement(className);
            }
            
            // Select first class by default
            if (!classes.isEmpty()) {
                classList.setSelectedIndex(0);
                // Force update selection to notify listener
                SwingUtilities.invokeLater(this::updateSelection);
            }
        }
    }
      private void onProjectChanged(YoloProject project) {
        SwingUtilities.invokeLater(() -> {
            loadProjectClasses();
            // Additional delay to ensure everything is set up
            SwingUtilities.invokeLater(this::updateSelection);
        });
    }
      public void setClassSelectionListener(ClassSelectionListener listener) {
        this.classSelectionListener = listener;
        System.out.println("ClassPanel: Selection listener set!");
        
        // If we already have a selection, notify the listener
        SwingUtilities.invokeLater(() -> {
            if (classList.getSelectedIndex() >= 0) {
                updateSelection();
            }
        });
    }
    
    public int getSelectedClassIndex() {
        return selectedClassIndex;
    }
    
    public String getSelectedClassName() {
        return classList.getSelectedValue();
    }
    
    // Custom cell renderer for class list
    private static class ClassListCellRenderer extends DefaultListCellRenderer {
        private final Color[] classColors = {
            Color.RED, Color.BLUE, Color.GREEN, Color.YELLOW, Color.MAGENTA,
            Color.CYAN, Color.ORANGE, Color.PINK, Color.LIGHT_GRAY, Color.DARK_GRAY
        };
        
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                    boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            
            // Add color indicator
            Color classColor = classColors[index % classColors.length];
            
            if (!isSelected) {
                setBackground(list.getBackground());
                setForeground(list.getForeground());
            }
            
            // Create colored square icon
            Icon colorIcon = new Icon() {
                @Override
                public void paintIcon(Component c, Graphics g, int x, int y) {
                    g.setColor(classColor);
                    g.fillRect(x, y, getIconWidth(), getIconHeight());
                    g.setColor(Color.BLACK);
                    g.drawRect(x, y, getIconWidth(), getIconHeight());
                }
                
                @Override
                public int getIconWidth() { return 12; }
                
                @Override
                public int getIconHeight() { return 12; }
            };
            
            setIcon(colorIcon);
            setText(String.format("[%d] %s", index, value.toString()));
            
            return this;
        }
    }
}
