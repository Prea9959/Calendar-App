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
        this.setSize(1000, 700);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setTitle("Personal Calendar App - Pro Version");

        try { events = fileHandler.loadEvents(); } catch (Exception e) { e.printStackTrace(); }

        headerPanel = new JPanel(new BorderLayout());
        headerPanel.setPreferredSize(new Dimension(900, 100));

        JPanel buttonGroup = new JPanel();
        buttonMonth = new JComboBox<>(new String[]{"January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"});
        
        // Dynamic Year Generation
        int currentYear = LocalDate.now().getYear();
        String[] yearRange = new String[11];
        for(int i=0; i<11; i++) yearRange[i] = String.valueOf(currentYear - 2 + i);
        buttonYear = new JComboBox<>(yearRange);
        buttonYear.setSelectedItem(String.valueOf(currentYear));

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

        buttonMonth.setSelectedIndex(LocalDate.now().getMonthValue() - 1);
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

        Object[] message = { "Title:", titleField, "Start Time (HH:mm):", startField, "End Time (HH:mm):", endField, "Repeat Type:", recurBox, "Repeat Times:", countField };

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
                    events.add(new Event(newId, titleField.getText(), "Manual Entry", startDT, endDT, (String)recurBox.getSelectedItem(), Integer.parseInt(countField.getText())));
                }
                saveAndRefresh();
            } catch (Exception ex) { JOptionPane.showMessageDialog(this, "Use HH:mm format."); }
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
                if (a.getStart().toLocalTime().isBefore(b.getEnd().toLocalTime()) && 
                    a.getEnd().toLocalTime().isAfter(b.getStart().toLocalTime())) return true;
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
                JOptionPane.showMessageDialog(this, "Saved to calendar_backup.zip");
            } catch (Exception ex) { ex.printStackTrace(); }
        } else if (e.getSource() == buttonRestore) {
            try {
                fileHandler.restore("calendar_backup.zip");
                events = fileHandler.loadEvents();
                updateCalendar();
                JOptionPane.showMessageDialog(this, "Restored!");
            } catch (Exception ex) { ex.printStackTrace(); }
        } else if (e.getSource() == buttonSearch) {
            searchEvents();
        } else if (e.getSource() == buttonPrev) {
            navigateMonth(-1);
        } else if (e.getSource() == buttonNext) {
            navigateMonth(1);
        } else if (e.getSource() == buttonMonth || e.getSource() == buttonYear) {
            updateCalendar();
        }
    }

    private void searchEvents() {
        JTextField startField = new JTextField(LocalDate.now().toString());
        JTextField endField = new JTextField(LocalDate.now().plusMonths(1).toString());

        Object[] message = {
            "Start Date (yyyy-MM-dd):", startField,
            "End Date (yyyy-MM-dd):", endField
        };

        int option = JOptionPane.showConfirmDialog(this, message, "Search by Date Interval", JOptionPane.OK_CANCEL_OPTION);
        
        if (option == JOptionPane.OK_OPTION) {
            try {
                LocalDate startDate = LocalDate.parse(startField.getText());
                LocalDate endDate = LocalDate.parse(endField.getText());

                if (endDate.isBefore(startDate)) {
                    JOptionPane.showMessageDialog(this, "End date cannot be before start date.");
                    return;
                }

                // Filter events that have at least one occurrence within the range
                List<Event> results = events.stream()
                        .filter(event -> {
                            for (int i = 0; i < event.getRecurCount(); i++) {
                                LocalDate occurrence = event.getOccurrence(i).toLocalDate();
                                if (!occurrence.isBefore(startDate) && !occurrence.isAfter(endDate)) {
                                    return true;
                                }
                            }
                            return false;
                        })
                        .collect(Collectors.toList());

                if (results.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "No events found between " + startDate + " and " + endDate);
                } else {
                    StringBuilder sb = new StringBuilder("Events from " + startDate + " to " + endDate + ":\n\n");
                    for (Event e : results) {
                        sb.append("• ").append(e.getTitle()).append("\n");
                    }
                    
                    // Show scrollable results if list is long
                    JTextArea textArea = new JTextArea(sb.toString());
                    textArea.setEditable(false);
                    JScrollPane scrollPane = new JScrollPane(textArea);
                    scrollPane.setPreferredSize(new Dimension(300, 400));
                    JOptionPane.showMessageDialog(this, scrollPane, "Search Results", JOptionPane.INFORMATION_MESSAGE);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Invalid date format. Please use yyyy-MM-dd.");
            }
        }
    }

    private void navigateMonth(int diff) {
        buttonPrev.setEnabled(false);
        buttonNext.setEnabled(false);

        Container content = this.getContentPane();
        BufferedImage before = new BufferedImage(content.getWidth(), content.getHeight(), BufferedImage.TYPE_INT_ARGB);
        content.paint(before.getGraphics());

        int month = buttonMonth.getSelectedIndex() + diff;
        if (month < 0) {
            month = 11;
            if (buttonYear.getSelectedIndex() > 0) buttonYear.setSelectedIndex(buttonYear.getSelectedIndex() - 1);
        } else if (month > 11) {
            month = 0;
            if (buttonYear.getSelectedIndex() < buttonYear.getItemCount() - 1) buttonYear.setSelectedIndex(buttonYear.getSelectedIndex() + 1);
        }
        buttonMonth.setSelectedIndex(month);
        updateCalendar();

        BufferedImage after = new BufferedImage(content.getWidth(), content.getHeight(), BufferedImage.TYPE_INT_ARGB);
        content.paint(after.getGraphics());

        Animator.slideTransition(this, gridPanel, before, after, diff > 0 ? Animator.Direction.LEFT : Animator.Direction.RIGHT, 300, () -> {
            buttonPrev.setEnabled(true);
            buttonNext.setEnabled(true);
        });
    }
}