package raven.application;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.fonts.roboto.FlatRobotoFont;
import com.formdev.flatlaf.themes.FlatMacDarkLaf;
import raven.components.MainForm;
import raven.yolo.manager.WorkspaceManager;
import raven.yolo.forms.WorkspaceSetupDialog;

import javax.swing.*;
import java.awt.*;

public class Application extends JFrame {

    private MainForm mainForm = new MainForm();

    public Application() {
        init();
        initializeWorkspace();
    }    private void init() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        getRootPane().putClientProperty(FlatClientProperties.FULL_WINDOW_CONTENT, true);
        
        // Set to full screen
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        
        // Fallback size if maximized doesn't work
        GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice device = env.getDefaultScreenDevice();
        Rectangle bounds = device.getDefaultConfiguration().getBounds();
        setSize(bounds.width, bounds.height);
        setLocation(0, 0);
        
        getContentPane().add(mainForm);
    }
    
    private void initializeWorkspace() {
        SwingUtilities.invokeLater(() -> {
            try {
                // Initialize workspace manager
                WorkspaceManager workspaceManager = WorkspaceManager.getInstance();
                
                // Scan workspace for existing projects
                workspaceManager.scanWorkspaceForProjects();
                
            } catch (Exception e) {
                e.printStackTrace();
                // Show workspace setup dialog if there's an issue
                showWorkspaceSetupDialog(true);
            }
        });
    }
    
    private void showWorkspaceSetupDialog(boolean isFirstTime) {
        WorkspaceSetupDialog dialog = new WorkspaceSetupDialog(this, isFirstTime);
        dialog.setVisible(true);
        
        if (!dialog.isConfirmed() && isFirstTime) {
            // If user cancels on first time, exit application
            System.exit(0);
        }
    }

    public static void main(String[] args) {
        FlatRobotoFont.install();
        UIManager.put("defaultFont", new Font(FlatRobotoFont.FAMILY, Font.PLAIN, 13));
        FlatMacDarkLaf.setup();
        EventQueue.invokeLater(() -> new Application().setVisible(true));
    }
}
