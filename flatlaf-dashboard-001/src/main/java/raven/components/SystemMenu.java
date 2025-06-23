package raven.components;

import com.formdev.flatlaf.FlatClientProperties;
import net.miginfocom.swing.MigLayout;
import raven.drawer.component.header.SimpleHeader;
import raven.drawer.component.header.SimpleHeaderData;
import raven.drawer.component.menu.*;
import raven.drawer.component.menu.data.Item;
import raven.forms.YoloDashboard;
import raven.yolo.forms.YoloAnnotationForm;
import raven.yolo.forms.ExportDatasetDialog;
import raven.yolo.forms.ProjectStatistics;
import raven.yolo.forms.ProjectSettings;
import raven.yolo.forms.PythonSetupDialog;
import raven.swing.AvatarIcon;
import raven.swing.blur.BlurChild;
import raven.swing.blur.style.GradientColor;
import raven.swing.blur.style.Style;
import raven.swing.blur.style.StyleBorder;
import raven.swing.blur.style.StyleOverlay;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Point2D;

public class SystemMenu extends BlurChild {

    public SystemMenu() {
        super(new Style()
                .setBlur(30)
                .setBorder(new StyleBorder(10)
                        .setOpacity(0.15f)
                        .setBorderWidth(1.2f)
                        .setBorderColor(new GradientColor(new Color(200, 200, 200), new Color(150, 150, 150), new Point2D.Float(0, 0), new Point2D.Float(1f, 0)))
                )
                .setOverlay(new StyleOverlay(new Color(0, 0, 0), 0.2f))
        );
        init();
    }

    private void init() {
        setLayout(new MigLayout("wrap,fill", "[fill]", "[grow 0][fill]"));
        simpleMenu = new SimpleMenu(getMenuOption());

        simpleMenu.setOpaque(false);
        JScrollPane scrollPane = new JScrollPane(simpleMenu);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getVerticalScrollBar().setOpaque(false);
        scrollPane.getVerticalScrollBar().setUnitIncrement(10);
        scrollPane.getVerticalScrollBar().putClientProperty(FlatClientProperties.STYLE, "" +
                "trackArc:999;" +
                "width:5;" +
                "thumbInsets:0,0,0,0");

        // header
        SimpleHeader header = new SimpleHeader(getHeaderData());
        header.setOpaque(false);
        add(header);
        add(scrollPane);
    }    private SimpleHeaderData getHeaderData() {
        return new SimpleHeaderData()
                .setTitle("YOLO v8")
                .setDescription("Annotation Tool")
                .setIcon(new AvatarIcon(getClass().getResource("/raven/images/profile.png"), 60, 60, 999));
    }    private SimpleMenuOption getMenuOption() {
        raven.drawer.component.menu.data.MenuItem items[] = new raven.drawer.component.menu.data.MenuItem[]{
                new Item.Label("YOLO ANNOTATION"),
                new Item("Dashboard", "dashboard.svg"),
                new Item("Annotation Tool", "chart.svg"),
                new Item.Label("PROJECT"),
                new Item("Export Dataset", "page.svg"),
                new Item.Label("ENVIRONMENT"),
                new Item("Python Setup", "chat.svg"),
                new Item.Label("TOOLS"),
                new Item("Statistics", "ui.svg"),
                new Item("Settings", "icon.svg")
        };
        return new SimpleMenuOption()
                .setBaseIconPath("raven/menu")
                .setIconScale(0.5f)
                .setMenus(items)
                .setMenuStyle(new SimpleMenuStyle() {
                    @Override
                    public void styleMenuPanel(JPanel panel, int[] index) {
                        panel.setOpaque(false);
                    }

                    @Override
                    public void styleMenuItem(JButton menu, int[] index) {
                        menu.setContentAreaFilled(false);
                    }
                })                .addMenuEvent(new MenuEvent() {
                    @Override
                    public void selected(MenuAction menuAction, int[] ints) {
                        System.out.println("Menu selected: " + java.util.Arrays.toString(ints));
                        if (ints.length == 1) {
                            int index = ints[0];                            switch (index) {
                                case 0: // Dashboard - don't close previous forms, allow multiple instances
                                    FormManager.getInstance().showForm("YOLO Dashboard", new YoloDashboard(), false);
                                    break;
                                case 1: // Annotation Tool - close previous non-dashboard forms
                                    FormManager.getInstance().showForm("YOLO Annotation Tool", new YoloAnnotationForm(), true);
                                    break;
                                case 2: // Export Dataset
                                    showExportDatasetDialog();
                                    break;
                                case 3: // Python Setup
                                    showPythonSetupDialog();
                                    break;
                                case 4: // Statistics - close previous non-dashboard forms
                                    showProjectStatistics();
                                    break;
                                case 5: // Settings - close previous non-dashboard forms
                                    showProjectSettings();
                                    break;
                            }
                        }
                    }
                })
                ;

    }    private SimpleMenu simpleMenu;
    
    private void showExportDatasetDialog() {
        // Check if there's a current project
        if (raven.yolo.manager.ProjectManager.getInstance().getCurrentProject() == null) {
            JOptionPane.showMessageDialog(this, 
                "Please open or create a project first before exporting dataset.", 
                "No Project", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        SwingUtilities.invokeLater(() -> {
            Frame parentFrame = (Frame) SwingUtilities.getAncestorOfClass(Frame.class, this);
            ExportDatasetDialog dialog = new ExportDatasetDialog(parentFrame);
            dialog.setVisible(true);
        });
    }
    
    private void showProjectStatistics() {
        // Check if there's a current project
        if (raven.yolo.manager.ProjectManager.getInstance().getCurrentProject() == null) {
            JOptionPane.showMessageDialog(this, 
                "Please open or create a project first to view statistics.", 
                "No Project", JOptionPane.WARNING_MESSAGE);
            return;
        }
          SwingUtilities.invokeLater(() -> {
            FormManager.getInstance().showForm("Project Statistics", new ProjectStatistics(), true);
        });
    }
    
    private void showProjectSettings() {
        // Check if there's a current project
        if (raven.yolo.manager.ProjectManager.getInstance().getCurrentProject() == null) {
            JOptionPane.showMessageDialog(this, 
                "Please open or create a project first to access settings.", 
                "No Project", JOptionPane.WARNING_MESSAGE);
            return;
        }
          SwingUtilities.invokeLater(() -> {
            FormManager.getInstance().showForm("Project Settings", new ProjectSettings(), true);
        });
    }
      private void showPythonSetupDialog() {
        SwingUtilities.invokeLater(() -> {
            Frame parentFrame = (Frame) SwingUtilities.getAncestorOfClass(Frame.class, this);
            
            // Ensure project context is set for Python setup
            try {
                raven.yolo.manager.ProjectManager projectManager = raven.yolo.manager.ProjectManager.getInstance();
                raven.yolo.model.YoloProject currentProject = projectManager.getCurrentProject();
                if (currentProject != null) {
                    String workspacePath = raven.yolo.manager.WorkspaceManager.getInstance().getCurrentWorkspacePath();
                    raven.yolo.training.PythonSetupManager.getInstance().setCurrentProject(currentProject.getId(), workspacePath);
                } else {
                    // Set default project context if no project is open
                    String workspacePath = raven.yolo.manager.WorkspaceManager.getInstance().getCurrentWorkspacePath();
                    raven.yolo.training.PythonSetupManager.getInstance().setCurrentProject("default", workspacePath);
                }
            } catch (Exception e) {
                e.printStackTrace();
                // Fallback to default project
                raven.yolo.training.PythonSetupManager.getInstance().setCurrentProject("default", System.getProperty("user.home"));
            }
            
            PythonSetupDialog dialog = new PythonSetupDialog(parentFrame);
            dialog.setVisible(true);
        });
    }
}
