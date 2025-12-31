import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

public class LaunchPage extends JFrame implements ActionListener{  
    JPanel headerPanel;
    JLabel headerLabel;
    JPanel gridPanel;
    JButton buttonPrev;
    JButton buttonNext;
    JButton dayButtons;
    ImageIcon icon;

    public LaunchPage() {
        this.setSize(800, 600);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        icon = new ImageIcon("src/resources/icon.png");
        this.setIconImage(icon.getImage());
        this.setTitle("Calendar App");
        
        // Header setup
        headerPanel = new JPanel();    
        headerPanel.setPreferredSize(new Dimension(400, 100));
        headerPanel.setBackground(Color.BLUE);
        headerPanel.setLayout(new BorderLayout());
        
        // Header Label
        buttonPrev = new JButton("<");
        buttonPrev.setFocusable(false);
    

        headerLabel = new JLabel("Month Year");
        headerLabel.setForeground(Color.WHITE);
        headerLabel.setFont(new Font("Arial", Font.BOLD, 24));
        headerLabel.setHorizontalAlignment(SwingConstants.CENTER);
        headerLabel.setVerticalAlignment(SwingConstants.CENTER);

        buttonNext = new JButton(">");
        buttonNext.setFocusable(false);

        headerPanel.add(headerLabel);
        headerPanel.add(buttonPrev, BorderLayout.WEST);
        headerPanel.add(buttonNext, BorderLayout.EAST);
        
        // Grid Setup
        gridPanel = new JPanel();
        gridPanel.setPreferredSize(new Dimension(400, 400));
        gridPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10,10));
        gridPanel.setLayout(new GridLayout(7,5));
        for (int i = 1; i <= 35; i++) {
            dayButtons = new JButton(String.valueOf(i));
            dayButtons.setFocusable(false);
            gridPanel.add(dayButtons);
        }

        this.add(headerPanel, BorderLayout.NORTH);
        this.add(gridPanel, BorderLayout.CENTER);
        this.setVisible(true);
    }

    @Override
    public void actionPerformed(java.awt.event.ActionEvent e) {
        // Handle action events here
    }
}
