package raven.components;

import com.formdev.flatlaf.FlatClientProperties;
import net.miginfocom.swing.MigLayout;
import raven.swing.blur.BlurChild;
import raven.swing.blur.style.GradientColor;
import raven.swing.blur.style.Style;
import raven.swing.blur.style.StyleBorder;
import raven.swing.blur.style.StyleOverlay;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Point2D;

public class Title extends BlurChild {

    private JLabel titleLabel;
    private JButton closeAllButton;
    private JLabel memoryLabel;

    public Title() {
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
        setLayout(new MigLayout("insets 10", "[grow][]20[]", "[]"));
        
        // Title
        titleLabel = new JLabel("YOLO v8 Annotation Tool");
        titleLabel.putClientProperty(FlatClientProperties.STYLE, "font:bold +2");
        
        // Memory usage label
        memoryLabel = new JLabel();
        memoryLabel.putClientProperty(FlatClientProperties.STYLE, "font:-1;foreground:$Label.disabledForeground");
        updateMemoryUsage();
        
        // Close all button
        closeAllButton = new JButton("Close All Forms");
        closeAllButton.putClientProperty(FlatClientProperties.STYLE, "arc:5");
        closeAllButton.setToolTipText("Close all forms except Dashboard to free memory");
        closeAllButton.addActionListener(e -> {
            FormManager.getInstance().closeAllForms();
            updateMemoryUsage();
        });
        
        add(titleLabel, "grow");
        add(memoryLabel);
        add(closeAllButton);
        
        // Update memory usage every 5 seconds
        Timer memoryTimer = new Timer(5000, e -> updateMemoryUsage());
        memoryTimer.start();
    }
    
    private void updateMemoryUsage() {
        SwingUtilities.invokeLater(() -> {
            Runtime runtime = Runtime.getRuntime();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            
            double usedMB = usedMemory / (1024.0 * 1024.0);
            double totalMB = totalMemory / (1024.0 * 1024.0);
            
            memoryLabel.setText(String.format("Memory: %.1f/%.1f MB", usedMB, totalMB));
        });
    }
}
