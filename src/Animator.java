import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class Animator {
    public enum Direction { LEFT, RIGHT }

    // duration in milliseconds
    public static void slideTransition(JFrame frame, JComponent target, BufferedImage from, BufferedImage to, Direction dir, int duration) {
        class SlideGlass extends JComponent {
            private float progress = 0f;
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                int w = getWidth();
                int h = getHeight();
                if (from == null && to == null) return;
                if (dir == Direction.LEFT) {
                    int fx = -(int) (w * progress);
                    int tx = w - (int) (w * progress);
                    if (from != null) g2.drawImage(from, fx, 0, null);
                    if (to != null) g2.drawImage(to, tx, 0, null);
                } else { // RIGHT
                    int fx = (int) (w * progress);
                    int tx = -w + (int) (w * progress);
                    if (from != null) g2.drawImage(from, fx, 0, null);
                    if (to != null) g2.drawImage(to, tx, 0, null);
                }
                g2.dispose();
            }

            void setProgress(float p) {
                this.progress = p;
                repaint();
            }
        }

        SlideGlass glass = new SlideGlass();
        glass.setOpaque(false);

        JRootPane root = frame.getRootPane();
        root.setGlassPane(glass);
        Dimension size = frame.getContentPane().getSize();
        glass.setBounds(0, 0, size.width, size.height);
        glass.setVisible(true);

        int steps = Math.max(1, duration / 20);
        int delay = Math.max(5, duration / steps);

        Timer timer = new Timer(delay, null);
        final int[] count = {0};
        timer.addActionListener(e -> {
            count[0]++;
            float p = Math.min(1f, count[0] / (float) steps);
            glass.setProgress(p);
            if (p >= 1f) {
                ((Timer) e.getSource()).stop();
                glass.setVisible(false);
            }
        });
        timer.start();
    }
}

