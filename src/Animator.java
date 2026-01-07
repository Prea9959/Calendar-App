import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class Animator {

    // Direction for slide animation
    public enum Direction { LEFT, RIGHT }

    /*
     * Slides from one UI snapshot to another using the frame's GlassPane.
     * This avoids moving real Swing components and keeps layout untouched.
     */
    public static void slideTransition(
            JFrame frame,
            JComponent target,
            BufferedImage from,
            BufferedImage to,
            Direction dir,
            int duration
    ) {

        // Temporary overlay that draws both images during animation
        class SlideGlass extends JComponent {
            private float progress = 0f; // 0.0 → start, 1.0 → end

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                int w = getWidth();

                if (dir == Direction.LEFT) {
                    int fx = -(int) (w * progress);
                    int tx = w - (int) (w * progress);
                    if (from != null) g2.drawImage(from, fx, 0, null);
                    if (to != null) g2.drawImage(to, tx, 0, null);
                } else {
                    int fx = (int) (w * progress);
                    int tx = -w + (int) (w * progress);
                    if (from != null) g2.drawImage(from, fx, 0, null);
                    if (to != null) g2.drawImage(to, tx, 0, null);
                }
                g2.dispose();
            }

            void setProgress(float p) {
                progress = p;
                repaint();
            }
        }

        // Attach animation overlay above all components
        SlideGlass glass = new SlideGlass();
        JRootPane root = frame.getRootPane();
        root.setGlassPane(glass);

        Dimension size = frame.getContentPane().getSize();
        glass.setBounds(0, 0, size.width, size.height);
        glass.setVisible(true);

        // Timer drives the animation on the EDT
        int steps = Math.max(1, duration / 20);
        int delay = Math.max(5, duration / steps);

        Timer timer = new Timer(delay, null);
        final int[] count = {0};

        timer.addActionListener(e -> {
            float p = Math.min(1f, ++count[0] / (float) steps);
            glass.setProgress(p);

            if (p >= 1f) {
                ((Timer) e.getSource()).stop();
                glass.setVisible(false);
            }
        });

        timer.start();
    }
}
