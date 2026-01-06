import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.event.*;
import java.time.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class LaunchPage extends JFrame implements ActionListener {
    JPanel headerPanel, gridPanel;
    JComboBox<String> buttonMonth, buttonYear;
    JButton buttonPrev, buttonNext, buttonConflict;
    FileHandler fileHandler = new FileHandler();
    List<Event> events = new ArrayList<>();
    boolean showConflicts = false;
    
    Font dayFont = new Font("Arial", Font.BOLD, 25);
    Font labelFont = new Font("Arial", Font.BOLD, 22);

    public LaunchPage() {
        this.setSize(900, 700);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setTitle("Personal Calendar App");

        try { events = fileHandler.loadEvents(); } catch (Exception e) { e.printStackTrace(); }

        headerPanel = new JPanel(new BorderLayout());
        headerPanel.setPreferredSize(new Dimension(900, 100));

        JPanel buttonGroup = new JPanel();
        buttonMonth = new JComboBox<>(new String[]{"January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"});
        buttonYear = new JComboBox<>(new String[]{"2026", "2027", "2028", "2029", "2030"});
        buttonMonth.addActionListener(this);
        buttonYear.addActionListener(this);
        buttonGroup.add(buttonMonth);
        buttonGroup.add(buttonYear);
        headerPanel.add(buttonGroup, BorderLayout.WEST);

        JPanel navButton = new JPanel();
        buttonConflict = new JButton("Check for Conflicts");
        buttonPrev = new JButton("<");
        buttonNext = new JButton(">");

        buttonConflict.addActionListener(this);
        buttonPrev.addActionListener(this);
        buttonNext.addActionListener(this);

        navButton.add(buttonConflict);
        navButton.add(buttonPrev);
        navButton.add(buttonNext);
        headerPanel.add(navButton, BorderLayout.EAST);

        gridPanel = new JPanel(new GridLayout(0, 7, 5, 5));
        this.add(headerPanel, BorderLayout.NORTH);
        this.add(new JScrollPane(gridPanel), BorderLayout.CENTER);
        
        updateCalendar();
        this.setVisible(true);
    }

    private void updateCalendar() {
        gridPanel.removeAll();
        int month = buttonMonth.getSelectedIndex() + 1;
        int year = Integer.parseInt((String) buttonYear.getSelectedItem());
        LocalDate today = LocalDate.now();

        LocalDate firstDay = LocalDate.of(year, month, 1);
        int startDay = firstDay.getDayOfWeek().getValue() % 7;
        
        String[] days = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
        for (String d : days) {
            JLabel lbl = new JLabel(d, SwingConstants.CENTER);
            lbl.setFont(labelFont);
            gridPanel.add(lbl);
        }

        for (int i = 0; i < startDay; i++) gridPanel.add(new JLabel(""));

        for (int i = 1; i <= firstDay.lengthOfMonth(); i++) {
            final int day = i;
            JButton dayBtn = new JButton(String.valueOf(i));
            dayBtn.setFont(dayFont);
            LocalDate dateObj = LocalDate.of(year, month, day);

            List<Event> dayEvents = events.stream()
                .filter(e -> e.getStart().toLocalDate().equals(dateObj))
                .collect(Collectors.toList());

            if (!dayEvents.isEmpty()) {
                if (showConflicts) {
                    boolean hasConflict = checkForConflictOnDate(dayEvents);
                    Color conflictColor = new Color(255, 102, 102);
                    Color conflictDark = new Color(178, 34, 34);
                    Color okColor = new Color(144, 238, 144);
                    Color okDark = new Color(34, 139, 34);

                    if (hasConflict) {
                        dayBtn.setBackground(conflictColor);
                        dayBtn.setOpaque(true);
                        dayBtn.setBorder(BorderFactory.createCompoundBorder(
                                BorderFactory.createLineBorder(conflictDark, 2),
                                BorderFactory.createEmptyBorder(2, 2, 2, 2)
                        ));
                    } else {
                        dayBtn.setBackground(okColor);
                        dayBtn.setOpaque(true);
                        dayBtn.setBorder(BorderFactory.createCompoundBorder(
                                BorderFactory.createLineBorder(okDark, 2),
                                BorderFactory.createEmptyBorder(2, 2, 2, 2)
                        ));
                    }
                    dayBtn.setBorderPainted(true);
                } else {
                    Color blueColor = new Color(173, 216, 230);
                    Color blueDark = new Color(70, 130, 180);
                    dayBtn.setBackground(blueColor);
                    dayBtn.setOpaque(true);
                    dayBtn.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(blueDark, 2),
                            BorderFactory.createEmptyBorder(2, 2, 2, 2)
                    ));
                    dayBtn.setBorderPainted(true);
                }
            }

            // Subtler Today highlight (Yellow Border)
            if (dateObj.equals(today)) {
                dayBtn.setBorder(BorderFactory.createLineBorder(Color.BLACK, 3));
            }

            dayBtn.addActionListener(e -> showDayEvents(year, month, day));
            gridPanel.add(dayBtn);
        }
        gridPanel.revalidate();
        gridPanel.repaint();
    }

    private boolean checkForConflictOnDate(List<Event> dayEvents) {
        for (int i = 0; i < dayEvents.size(); i++) {
            for (int j = i + 1; j < dayEvents.size(); j++) {
                Event a = dayEvents.get(i);
                Event b = dayEvents.get(j);
                // Standard overlap logic
                if (a.getStart().isBefore(b.getEnd()) && a.getEnd().isAfter(b.getStart())) {
                    return true;
                }
            }
        }
        return false;
    }

    private void showDayEvents(int y, int m, int d) {
        LocalDate date = LocalDate.of(y, m, d);
        List<Event> dayEvents = events.stream()
            .filter(e -> e.getStart().toLocalDate().equals(date))
            .collect(Collectors.toList());

        StringBuilder sb = new StringBuilder("Events for " + date + ":\n");
        if (dayEvents.isEmpty()) sb.append("No events scheduled.");
        
        for (int i = 0; i < dayEvents.size(); i++) {
            Event e = dayEvents.get(i);
            sb.append(i + 1).append(". ").append(e.getStart().toLocalTime())
              .append(" - ").append(e.getTitle()).append("\n");
        }

        Object[] options = {"Add New", "Delete Event", "Close"};
        int choice = JOptionPane.showOptionDialog(this, sb.toString(), "Day View",
                JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null, options, options[2]);

        if (choice == 0) {
            createNewEvent(date);
        } else if (choice == 1 && !dayEvents.isEmpty()) {
            String input = JOptionPane.showInputDialog("Enter the number of the event to delete:");
            try {
                int index = Integer.parseInt(input) - 1;
                events.remove(dayEvents.get(index));
                fileHandler.saveEvents(events);
                updateCalendar();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Invalid selection.");
            }
        }
    }

    private void createNewEvent(LocalDate targetDate) {
        JTextField titleField = new JTextField();
        JTextField startField = new JTextField("12:00");
        JTextField endField = new JTextField("13:00");
        
        Object[] message = {
            "Title:", titleField,
            "Start Time (HH:mm):", startField,
            "End Time (HH:mm):", endField
        };

        int option = JOptionPane.showConfirmDialog(null, message, "New Event", JOptionPane.OK_CANCEL_OPTION);
        if (option == JOptionPane.OK_OPTION) {
            try {
                LocalTime startTime = LocalTime.parse(startField.getText());
                LocalTime endTime = LocalTime.parse(endField.getText());

                if (endTime.isBefore(startTime)) {
                    JOptionPane.showMessageDialog(this, "Error: End time must be later than start time.");
                    return;
                }

                LocalDateTime startDT = LocalDateTime.of(targetDate, startTime);
                LocalDateTime endDT = LocalDateTime.of(targetDate, endTime);

                events.add(new Event(events.size() + 1, titleField.getText(), "Manual Entry", startDT, endDT));
                fileHandler.saveEvents(events);
                updateCalendar();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Invalid Time Format. Use HH:mm (e.g., 14:30)");
            }
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == buttonConflict) {
            showConflicts = !showConflicts;
            buttonConflict.setText(showConflicts ? "Hide Conflicts" : "Check for Conflicts");
            updateCalendar();
        } else if (e.getSource() == buttonPrev) {
            navigateMonth(-1);
        } else if (e.getSource() == buttonNext) {
            navigateMonth(1);
        } else if (e.getSource() == buttonMonth || e.getSource() == buttonYear) {
            updateCalendar();
        }
    }

    private void navigateMonth(int diff) {
        // Capture current entire content pane rendering (so the slide covers full area)
        Container content = this.getContentPane();
        Dimension csize = content.getSize();
        if (csize.width <= 0 || csize.height <= 0) csize = this.getSize();
        BufferedImage before = new BufferedImage(csize.width, csize.height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = before.createGraphics();
        content.paint(g);
        g.dispose();

        int month = buttonMonth.getSelectedIndex() + diff;
        if (month < 0) {
            month = 11;
            int yearIdx = buttonYear.getSelectedIndex();
            if (yearIdx > 0) buttonYear.setSelectedIndex(yearIdx - 1);
        } else if (month > 11) {
            month = 0;
            int yearIdx = buttonYear.getSelectedIndex();
            if (yearIdx < buttonYear.getItemCount() - 1) buttonYear.setSelectedIndex(yearIdx + 1);
        }
        buttonMonth.setSelectedIndex(month);

        // Force calendar rebuild and capture after state change
        updateCalendar();
        Container content2 = this.getContentPane();
        Dimension csize2 = content2.getSize();
        if (csize2.width <= 0 || csize2.height <= 0) csize2 = this.getSize();
        BufferedImage after = new BufferedImage(csize2.width, csize2.height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = after.createGraphics();
        content2.paint(g2);
        g2.dispose();

        // Determine direction: moving forward => slide left
        Animator.Direction dir = diff > 0 ? Animator.Direction.LEFT : Animator.Direction.RIGHT;
        Animator.slideTransition(this, gridPanel, before, after, dir, 300);
    }
}