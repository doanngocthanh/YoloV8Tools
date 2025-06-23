package raven.components;

import com.formdev.flatlaf.FlatClientProperties;
import raven.swing.blur.BlurChild;
import raven.swing.blur.style.Style;
import raven.swing.blur.style.StyleBorder;
import raven.swing.blur.style.StyleOverlay;

import javax.swing.*;
import java.awt.*;

public class FormManager {

    private static FormManager instance;
    private JDesktopPane desktopPane;
    private JInternalFrame currentFrame;

    public static FormManager getInstance() {
        if (instance == null) {
            instance = new FormManager();
        }
        return instance;
    }

    private FormManager() {

    }

    public void setDesktopPane(JDesktopPane desktopPane) {
        this.desktopPane = desktopPane;
    }

    public void showForm(String title, Component component) {
        showForm(title, component, true);
    }
    
    public void showForm(String title, Component component, boolean closePrevious) {
        // Close previous form if requested (except Dashboard)
        if (closePrevious && currentFrame != null && !isDashboard(currentFrame)) {
            closeCurrentForm();
        }
        
        JInternalFrame frame = new JInternalFrame(title, true, true, true, true);
        frame.add(component);
        frame.setSize(new Dimension(1000, 600));
        frame.setFrameIcon(null);
        
        // Add close listener to cleanup when user closes manually
        frame.setDefaultCloseOperation(JInternalFrame.DISPOSE_ON_CLOSE);
        frame.addInternalFrameListener(new javax.swing.event.InternalFrameAdapter() {
            @Override
            public void internalFrameClosed(javax.swing.event.InternalFrameEvent e) {
                if (currentFrame == frame) {
                    currentFrame = null;
                }
                // Force garbage collection of frame content
                frame.getContentPane().removeAll();
                System.gc(); // Suggest garbage collection
            }
        });
        
        try {
            frame.setMaximum(true);
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
        frame.setVisible(true);
        desktopPane.add(frame, 0);
        currentFrame = frame;
    }

    public void showFormWithBlur(String title, Component component) {
        showFormWithBlur(title, component, true);
    }
    
    public void showFormWithBlur(String title, Component component, boolean closePrevious) {
        // Close previous form if requested (except Dashboard)
        if (closePrevious && currentFrame != null && !isDashboard(currentFrame)) {
            closeCurrentForm();
        }
        
        JInternalFrame frame = createBlurFrame(title, component);
        frame.setSize(new Dimension(1000, 600));
        
        // Add close listener to cleanup when user closes manually
        frame.setDefaultCloseOperation(JInternalFrame.DISPOSE_ON_CLOSE);
        frame.addInternalFrameListener(new javax.swing.event.InternalFrameAdapter() {
            @Override
            public void internalFrameClosed(javax.swing.event.InternalFrameEvent e) {
                if (currentFrame == frame) {
                    currentFrame = null;
                }
                // Force garbage collection of frame content
                frame.getContentPane().removeAll();
                System.gc(); // Suggest garbage collection
            }
        });
        
        try {
            frame.setMaximum(true);
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
        frame.setVisible(true);
        desktopPane.add(frame, 0);
        currentFrame = frame;
    }
    
    /**
     * Close current form and free memory
     */
    public void closeCurrentForm() {
        if (currentFrame != null) {
            try {
                currentFrame.setClosed(true);
            } catch (Exception e) {
                // If can't close properly, force dispose
                currentFrame.dispose();
                if (desktopPane != null) {
                    desktopPane.remove(currentFrame);
                }
            }
            currentFrame = null;
            
            // Clean up desktop pane
            if (desktopPane != null) {
                desktopPane.repaint();
            }
            
            // Suggest garbage collection
            System.gc();
        }
    }
    
    /**
     * Close all forms except Dashboard
     */
    public void closeAllForms() {
        if (desktopPane != null) {
            JInternalFrame[] frames = desktopPane.getAllFrames();
            for (JInternalFrame frame : frames) {
                if (!isDashboard(frame)) {
                    try {
                        frame.setClosed(true);
                    } catch (Exception e) {
                        frame.dispose();
                    }
                }
            }
            currentFrame = null;
            desktopPane.repaint();
            System.gc();
        }
    }
    
    /**
     * Check if frame is Dashboard
     */
    private boolean isDashboard(JInternalFrame frame) {
        return frame != null && 
               (frame.getTitle().contains("Dashboard") || 
                frame.getTitle().contains("YOLO Dashboard"));
    }
    
    /**
     * Get current active frame
     */
    public JInternalFrame getCurrentFrame() {
        return currentFrame;
    }

    private JInternalFrame createBlurFrame(String title, Component component) {
        JInternalFrame frame = new JInternalFrame(title, true, true, true, true);
        BlurChild child = new BlurChild(new Style()
                .setBlur(10)
                .setBorder(new StyleBorder(10)
                        .setBorderWidth(1.2f)
                        .setOpacity(0.1f)
                        .setMargin(new Insets(10, 0, 0, 0))
                        .setBorderColor(new Color(255, 255, 255))
                )
                .setOverlay(new StyleOverlay(new Color(0, 0, 0), 0.03f))
        );
        child.setLayout(new BorderLayout());
        child.add(component);
        frame.setFrameIcon(null);
        frame.putClientProperty(FlatClientProperties.STYLE, "" +
                "border:0,0,0,0");
        frame.add(child);
        return frame;
    }
}
