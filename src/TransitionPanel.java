import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class TransitionPanel extends JPanel {
    private BufferedImage imgOld;
    private BufferedImage imgNew;
    private float progress = 0f;
    private boolean isAnimating = false;
    private int direction = 0; // -1 for Right (Prev), 1 for Left (Next)
    private Timer timer;

    public TransitionPanel() {
        super(new BorderLayout());
    }

    public void animate(int direction, Runnable updateLogic) {
        if (isAnimating && timer != null && timer.isRunning()) {
            timer.stop(); // Stop any existing animation
        }

        this.direction = direction;
        int w = getWidth();
        int h = getHeight();

        // 1. Snapshot "Before" state
        imgOld = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        this.paint(imgOld.getGraphics()); // Use paint() to capture all children

        // 2. Run the Logic (Change Month, Rebuild Grid)
        updateLogic.run();
        
        // 3. Force Layout to validate the new Grid immediately
        this.validate();
        this.revalidate(); // Ensure hierarchy is up to date
        this.doLayout();   // Force components to arrange

        // 4. Snapshot "After" state
        imgNew = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        this.paint(imgNew.getGraphics()); // Capture the new state

        // 5. Start Animation
        this.isAnimating = true;
        this.progress = 0f;

        // Animation Speed Configuration
        int duration = 300; // milliseconds
        int fps = 60;
        int delay = 1000 / fps;
        int steps = duration / delay;
        float stepSize = 1.0f / steps;

        timer = new Timer(delay, e -> {
            progress += stepSize;
            if (progress >= 1.0f) {
                progress = 1.0f;
                isAnimating = false;
                timer.stop();
            }
            repaint(); // Triggers paintChildren()
        });
        timer.start();
    }

    @Override
    protected void paintChildren(Graphics g) {
        // If animating, draw the sliding images INSTEAD of the buttons
        if (isAnimating) {
            Graphics2D g2 = (Graphics2D) g;
            int w = getWidth();
            
            int offset = (int) (w * progress);
            
            int oldX, newX;
            if (direction > 0) { // Next Month (Slide Left)
                oldX = -offset;
                newX = w - offset;
            } else { // Prev Month (Slide Right)
                oldX = offset;
                newX = -w + offset;
            }

            // Draw the snapshots
            if (imgOld != null) g2.drawImage(imgOld, oldX, 0, null);
            if (imgNew != null) g2.drawImage(imgNew, newX, 0, null);
            
        } else {
            // If NOT animating, draw the normal buttons/grid
            super.paintChildren(g);
        }
    }
}
