import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.event.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class LaunchPage extends JFrame implements ActionListener {
    JPanel headerPanel, gridPanel;
    JComboBox<String> buttonMonth, buttonYear;
    JButton buttonPrev, buttonNext, buttonConflict, buttonSearch, buttonBackup, buttonRestore;
    FileHandler fileHandler = new FileHandler();
    List<Event> events = new ArrayList<>();
    boolean showConflicts = false;

    Font dayFont = new Font("Arial", Font.BOLD, 25);
    Font labelFont = new Font("Arial", Font.BOLD, 22);
    private final DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    public LaunchPage() {
        this.setSize(1000, 700); // Widened slightly for new buttons
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setTitle("Personal Calendar App - Pro Version");

        try { events = fileHandler.loadEvents(); } catch (Exception e) { e.printStackTrace(); }

        headerPanel = new JPanel(new BorderLayout());
        headerPanel.setPreferredSize(new Dimension(900, 100));

        JPanel buttonGroup = new JPanel();
        buttonMonth = new JComboBox<>(new String[]{"January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"});
        buttonYear = new JComboBox<>(new String[]{"2025", "2026", "2027", "2028", "2029", "2030"});
        buttonMonth.addActionListener(this);
        buttonYear.addActionListener(this);
        buttonGroup.add(buttonMonth);
        buttonGroup.add(buttonYear);
        headerPanel.add(buttonGroup, BorderLayout.WEST);

        JPanel navButton = new JPanel();
        buttonSearch = new JButton("Search");
        buttonConflict = new JButton("Conflicts");
        buttonBackup = new JButton("Backup");
        buttonRestore = new JButton("Restore");
        buttonPrev = new JButton("<");
        buttonNext = new JButton(">");

        buttonSearch.addActionListener(this);
        buttonConflict.addActionListener(this);
        buttonBackup.addActionListener(this);
        buttonRestore.addActionListener(this);
        buttonPrev.addActionListener(this);
        buttonNext.addActionListener(this);

        navButton.add(buttonSearch);
        navButton.add(buttonConflict);
        navButton.add(buttonBackup);
        navButton.add(buttonRestore);
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
            final int dayNum = i;
            JButton dayBtn = new JButton(String.valueOf(i));
            dayBtn.setFont(dayFont);
            LocalDate dateObj = LocalDate.of(year, month, dayNum);

            // Filter logic now uses the new occursOn() method for recurrence
            List<Event> dayEvents = events.stream()
                    .filter(e -> e.occursOn(dateObj))
                    .collect(Collectors.toList());

            if (!dayEvents.isEmpty()) {
                if (showConflicts) {
                    boolean hasConflict = checkForConflictOnDate(dayEvents);
                    dayBtn.setBackground(hasConflict ? new Color(255, 102, 102) : new Color(144, 238, 144));
                } else {
                    dayBtn.setBackground(new Color(173, 216, 230));
                }
                dayBtn.setOpaque(true);
                dayBtn.setBorderPainted(true);
            }

            if (dateObj.equals(today)) {
                dayBtn.setBorder(BorderFactory.createLineBorder(Color.BLACK, 3));
            }

            dayBtn.addActionListener(e -> showDayEvents(year, month, dayNum));
            gridPanel.add(dayBtn);
        }
        gridPanel.revalidate();
        gridPanel.repaint();
    }

    private void showDayEvents(int y, int m, int d) {
        LocalDate date = LocalDate.of(y, m, d);
        List<Event> dayEvents = events.stream()
                .filter(e -> e.occursOn(date))
                .collect(Collectors.toList());

        StringBuilder sb = new StringBuilder("Events for " + date.format(dateFormat) + ":\n");
        if (dayEvents.isEmpty()) sb.append("No events scheduled.");
        for (int i = 0; i < dayEvents.size(); i++) {
            sb.append(i + 1).append(". ").append(dayEvents.get(i).getTitle()).append("\n");
        }

        Object[] options = {"Add New", "Edit/Update", "Delete", "Close"};
        int choice = JOptionPane.showOptionDialog(this, sb.toString(), "Day View",
                JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null, options, options[3]);

        if (choice == 0) {
            createOrUpdateEvent(null, date);
        } else if (choice == 1 && !dayEvents.isEmpty()) {
            String input = JOptionPane.showInputDialog("Enter the number of the event to edit:");
            try {
                int index = Integer.parseInt(input) - 1;
                createOrUpdateEvent(dayEvents.get(index), date);
            } catch (Exception ex) { JOptionPane.showMessageDialog(this, "Invalid selection."); }
        } else if (choice == 2 && !dayEvents.isEmpty()) {
            String input = JOptionPane.showInputDialog("Enter the number of the event to delete:");
            try {
                int index = Integer.parseInt(input) - 1;
                events.remove(dayEvents.get(index));
                saveAndRefresh();
            } catch (Exception ex) { JOptionPane.showMessageDialog(this, "Invalid selection."); }
        }
    }

    private void createOrUpdateEvent(Event existing, LocalDate targetDate) {
        JTextField titleField = new JTextField(existing != null ? existing.getTitle() : "");
        JTextField startField = new JTextField(existing != null ? existing.getStart().toLocalTime().toString() : "12:00");
        JTextField endField = new JTextField(existing != null ? existing.getEnd().toLocalTime().toString() : "13:00");
        
        JComboBox<String> recurBox = new JComboBox<>(new String[]{"NONE", "DAILY", "WEEKLY", "MONTHLY"});
        if (existing != null) recurBox.setSelectedItem(existing.getRecurType());
        
        JTextField countField = new JTextField(existing != null ? String.valueOf(existing.getRecurCount()) : "1");

        Object[] message = {
            "Title:", titleField,
            "Start Time (HH:mm):", startField,
            "End Time (HH:mm):", endField,
            "Repeat Type:", recurBox,
            "Repeat Times:", countField
        };

        int option = JOptionPane.showConfirmDialog(null, message, "Event Details", JOptionPane.OK_CANCEL_OPTION);
        if (option == JOptionPane.OK_OPTION) {
            try {
                LocalTime startTime = LocalTime.parse(startField.getText());
                LocalTime endTime = LocalTime.parse(endField.getText());
                LocalDateTime startDT = LocalDateTime.of(targetDate, startTime);
                LocalDateTime endDT = LocalDateTime.of(targetDate, endTime);

                if (existing != null) {
                    existing.setTitle(titleField.getText());
                    existing.setStart(startDT);
                    existing.setEnd(endDT);
                } else {
                    int newId = events.stream().mapToInt(Event::getId).max().orElse(0) + 1;
                    events.add(new Event(newId, titleField.getText(), "Manual Entry", startDT, endDT, 
                               (String)recurBox.getSelectedItem(), Integer.parseInt(countField.getText())));
                }
                saveAndRefresh();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Invalid format. Use HH:mm for time and numbers for count.");
            }
        }
    }

    private void saveAndRefresh() {
        try { fileHandler.saveEvents(events); } catch (Exception e) { e.printStackTrace(); }
        updateCalendar();
    }

    private boolean checkForConflictOnDate(List<Event> dayEvents) {
        for (int i = 0; i < dayEvents.size(); i++) {
            for (int j = i + 1; j < dayEvents.size(); j++) {
                Event a = dayEvents.get(i);
                Event b = dayEvents.get(j);
                if (a.getStart().isBefore(b.getEnd()) && a.getEnd().isAfter(b.getStart())) return true;
            }
        }
        return false;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == buttonConflict) {
            showConflicts = !showConflicts;
            buttonConflict.setText(showConflicts ? "Hide Conflicts" : "Conflicts");
            updateCalendar();
        } else if (e.getSource() == buttonBackup) {
            try {
                fileHandler.backup("calendar_backup.zip");
                JOptionPane.showMessageDialog(this, "Backup saved to calendar_backup.zip");
            } catch (Exception ex) { JOptionPane.showMessageDialog(this, "Backup failed."); }
        } else if (e.getSource() == buttonRestore) {
            try {
                fileHandler.restore("calendar_backup.zip");
                events = fileHandler.loadEvents();
                updateCalendar();
                JOptionPane.showMessageDialog(this, "Data restored successfully!");
            } catch (Exception ex) { JOptionPane.showMessageDialog(this, "Restore failed. Ensure calendar_backup.zip exists."); }
        } else if (e.getSource() == buttonSearch) {
            // (Keep your existing searchEvents code here)
        } else if (e.getSource() == buttonPrev) {
            navigateMonth(-1);
        } else if (e.getSource() == buttonNext) {
            navigateMonth(1);
        } else if (e.getSource() == buttonMonth || e.getSource() == buttonYear) {
            updateCalendar();
        }
    }

    private void navigateMonth(int diff) {
        // 1. Capture current UI state (Uses BufferedImage!)
        Container content = this.getContentPane();
        Dimension csize = content.getSize();
        if (csize.width <= 0 || csize.height <= 0) csize = this.getSize();
        BufferedImage before = new BufferedImage(csize.width, csize.height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = before.createGraphics();
        content.paint(g);
        g.dispose();

        // 2. Perform the Month/Year Navigation
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
        updateCalendar();

        // 3. Capture new UI state
        Container content2 = this.getContentPane();
        Dimension csize2 = content2.getSize();
        if (csize2.width <= 0 || csize2.height <= 0) csize2 = this.getSize();
        BufferedImage after = new BufferedImage(csize2.width, csize2.height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = after.createGraphics();
        content2.paint(g2);
        g2.dispose();

        // 4. Trigger Animation
        Animator.Direction dir = diff > 0 ? Animator.Direction.LEFT : Animator.Direction.RIGHT;
        Animator.slideTransition(this, gridPanel, before, after, dir, 300);
    }
}