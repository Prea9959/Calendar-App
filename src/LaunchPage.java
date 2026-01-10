import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.io.File;

public class LaunchPage extends JFrame implements ActionListener {
    
    private CalendarController controller;

    // UI Components
    JPanel headerPanel, contentPanel;
    TransitionPanel bodyPanel;
    JComboBox<CalendarController.ViewMode> viewToggle;
    JComboBox<CalendarController.TimeScale> scaleToggle;
    JComboBox<String> monthSelector, yearSelector;
    JButton buttonPrev, buttonNext;

    // Design
    Font dayFont = new Font("Arial", Font.BOLD, 24);
    Font labelFont = new Font("Arial", Font.BOLD, 18);
    private final DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    public LaunchPage() {
        this.controller = new CalendarController();
        this.setSize(1100, 800);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setTitle("Personal Calendar - Multi-View Edition");

        setupHeader();

        bodyPanel = new TransitionPanel();
        contentPanel = new JPanel(new BorderLayout());
        bodyPanel.add(new JScrollPane(contentPanel), BorderLayout.CENTER);

        this.add(headerPanel, BorderLayout.NORTH);
        this.add(bodyPanel, BorderLayout.CENTER); 

        refreshUI();
        
        // Show notifications on startup
        showUpcomingNotifications();
        
        this.setVisible(true);
    }

    private void setupHeader() {
        headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(240, 240, 240));

        JPanel leftContainer = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        JPanel rightContainer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        leftContainer.setOpaque(false);
        rightContainer.setOpaque(false);

        // Left side: View toggles and Month/Year selectors
        viewToggle = new JComboBox<>(CalendarController.ViewMode.values());
        scaleToggle = new JComboBox<>(CalendarController.TimeScale.values());
        scaleToggle.setSelectedItem(CalendarController.TimeScale.MONTH);

        // Month and Year Selectors
        String[] months = {"January", "February", "March", "April", "May", "June", 
                          "July", "August", "September", "October", "November", "December"};
        monthSelector = new JComboBox<>(months);
        
        // Generate years (current year ± 10 years)
        int currentYear = LocalDate.now().getYear();
        String[] years = new String[21];
        for (int i = 0; i < 21; i++) {
            years[i] = String.valueOf(currentYear - 10 + i);
        }
        yearSelector = new JComboBox<>(years);
        
        // Set to current month and year
        LocalDate today = LocalDate.now();
        monthSelector.setSelectedIndex(today.getMonthValue() - 1);
        yearSelector.setSelectedItem(String.valueOf(today.getYear()));

        viewToggle.addActionListener(e -> { 
            controller.setMode((CalendarController.ViewMode)viewToggle.getSelectedItem()); 
            refreshUI(); 
        });
        scaleToggle.addActionListener(e -> { 
            controller.setScale((CalendarController.TimeScale)scaleToggle.getSelectedItem()); 
            refreshUI(); 
        });
        
        // When month/year selector changes, update the controller's reference date
        monthSelector.addActionListener(e -> {
            if (monthSelector.getSelectedIndex() >= 0) {
                updateReferenceDate();
            }
        });
        yearSelector.addActionListener(e -> {
            if (yearSelector.getSelectedItem() != null) {
                updateReferenceDate();
            }
        });

        // Add to left container
        leftContainer.add(new JLabel("View:"));
        leftContainer.add(viewToggle);
        leftContainer.add(scaleToggle);
        leftContainer.add(new JSeparator(SwingConstants.VERTICAL));
        leftContainer.add(new JLabel("Date:"));
        leftContainer.add(monthSelector);
        leftContainer.add(yearSelector);

        // Right side: Navigation and Actions menu
        buttonPrev = new JButton("<");
        buttonNext = new JButton(">");
        buttonPrev.setFocusable(false);
        buttonNext.setFocusable(false);
        buttonPrev.addActionListener(this);
        buttonNext.addActionListener(this);

        JButton actionsButton = new JButton("Actions ▼");
        actionsButton.setFocusable(false);

        JPopupMenu actionsMenu = new JPopupMenu();

        JMenuItem itemAdd = new JMenuItem("+ Add Event");
        JMenuItem itemSearch = new JMenuItem("🔍 Search Events");
        JMenuItem itemWeekList = new JMenuItem("📋 Week List View");
        JMenuItem itemNotifications = new JMenuItem("🔔 View Notifications");
        JMenuItem itemBackup = new JMenuItem("💾 Backup Data");
        JMenuItem itemRestore = new JMenuItem("📥 Restore Data");

        itemAdd.addActionListener(e -> createOrUpdateEvent(null, controller.getReferenceDate()));
        itemSearch.addActionListener(e -> handleSearch());
        itemWeekList.addActionListener(e -> showWeekListView());
        itemNotifications.addActionListener(e -> showUpcomingNotifications());
        itemBackup.addActionListener(e -> handleBackup());
        itemRestore.addActionListener(e -> handleRestore());

        actionsMenu.add(itemAdd);
        actionsMenu.addSeparator();
        actionsMenu.add(itemSearch);
        actionsMenu.add(itemWeekList);
        actionsMenu.add(itemNotifications);
        actionsMenu.addSeparator();
        actionsMenu.add(itemBackup);
        actionsMenu.add(itemRestore);

        actionsButton.addActionListener(e -> actionsMenu.show(actionsButton, 0, actionsButton.getHeight()));

        // Add to right container
        rightContainer.add(buttonPrev);
        rightContainer.add(buttonNext);
        rightContainer.add(actionsButton);

        headerPanel.add(leftContainer, BorderLayout.WEST);
        headerPanel.add(rightContainer, BorderLayout.EAST);
    }
    
    // Helper method to update reference date when month/year selector changes
    private void updateReferenceDate() {
        int month = monthSelector.getSelectedIndex() + 1;
        int year = Integer.parseInt((String) yearSelector.getSelectedItem());
        LocalDate newDate = LocalDate.of(year, month, 1);
        controller.setReferenceDate(newDate);
        refreshUI();
    }
    
    // Helper method to sync selectors with current reference date
    private void syncSelectors() {
        LocalDate refDate = controller.getReferenceDate();
        
        // Temporarily remove listeners to avoid triggering events
        ActionListener[] monthListeners = monthSelector.getActionListeners();
        ActionListener[] yearListeners = yearSelector.getActionListeners();
        
        for (ActionListener al : monthListeners) monthSelector.removeActionListener(al);
        for (ActionListener al : yearListeners) yearSelector.removeActionListener(al);
        
        // Update selections
        monthSelector.setSelectedIndex(refDate.getMonthValue() - 1);
        yearSelector.setSelectedItem(String.valueOf(refDate.getYear()));
        
        // Re-add listeners
        for (ActionListener al : monthListeners) monthSelector.addActionListener(al);
        for (ActionListener al : yearListeners) yearSelector.addActionListener(al);
    }

    private void refreshUI() {
        contentPanel.removeAll();
        
        String dateString = controller.getReferenceDate().format(dateFormat);
        this.setTitle("Calendar - " + dateString + " (" + controller.getMode() + ")");

        if (controller.getMode() == CalendarController.ViewMode.CALENDAR) {
            renderCalendarView();
        } else {
            renderListView();
        }

        contentPanel.revalidate();
        contentPanel.repaint();
    }

    // --- CALENDAR VIEW RENDERING ---
    private void renderCalendarView() {
        JPanel grid = new JPanel(new GridLayout(0, 7, 2, 2));
        String[] days = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
        for (String d : days) {
            JLabel l = new JLabel(d, SwingConstants.CENTER);
            l.setFont(labelFont);
            grid.add(l);
        }

        LocalDate start = controller.getStartOfRange();
        int length = (controller.getScale() == CalendarController.TimeScale.MONTH) ? 
                      controller.getReferenceDate().lengthOfMonth() : 7;
        
        if (controller.getScale() == CalendarController.TimeScale.MONTH) {
            int startPadding = controller.getReferenceDate().withDayOfMonth(1).getDayOfWeek().getValue() % 7;
            for (int i = 0; i < startPadding; i++) grid.add(new JLabel(""));
        }

        for (int i = 0; i < length; i++) {
            LocalDate date = start.plusDays(i);
            JButton dayBtn = createDayButton(date);
            dayBtn.setFocusable(false);
            grid.add(dayBtn);
        }
        contentPanel.add(grid, BorderLayout.CENTER);
    }

    private JButton createDayButton(LocalDate date) {
        List<Event> dayEvents = controller.getEventsOnDate(date);
        
        String text = "<html><center>" + date.getDayOfMonth();
        if (!dayEvents.isEmpty()) text += "<br><font size='2'>● " + dayEvents.size() + " Event(s)</font>";
        text += "</center></html>";

        JButton btn = new JButton(text);
        btn.setFocusable(false);
        btn.setFont(dayFont);
        
        if (date.equals(LocalDate.now())) btn.setBorder(BorderFactory.createLineBorder(Color.BLUE, 2));
        
        if (!dayEvents.isEmpty()) {
            boolean conflict = controller.checkForConflictOnDate(dayEvents);
            btn.setBackground(conflict ? new Color(255, 180, 180) : new Color(200, 230, 255));
        }

        btn.addActionListener(e -> showDayEvents(date));
        return btn;
    }

    // --- LIST VIEW RENDERING ---
    private void renderListView() {
        JPanel listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        
        List<Event> filtered = controller.getEventsInRange();

        if (filtered.isEmpty()) {
            listPanel.add(new JLabel("No events found for this period."));
        } else {
            for (Event e : filtered) {
                listPanel.add(createEventRow(e));
                listPanel.add(Box.createRigidArea(new Dimension(0, 5)));
            }
        }
        contentPanel.add(listPanel, BorderLayout.NORTH);
    }

    private JPanel createEventRow(Event e) {
        JPanel row = new JPanel(new BorderLayout());
        row.setMaximumSize(new Dimension(2000, 60));
        row.setBorder(BorderFactory.createEtchedBorder());
        
        String timeInfo = e.getStart().toLocalTime() + " - " + e.getEnd().toLocalTime();
        String recurInfo = !"NONE".equals(e.getRecurType()) ? " [" + e.getRecurType() + "]" : "";
        JLabel title = new JLabel("<html><b>" + e.getTitle() + "</b> (" + timeInfo + ")" + recurInfo + "</html>");
        title.setBorder(BorderFactory.createEmptyBorder(5,10,5,10));
        
        if (controller.hasConflict(e)) {
            row.setBackground(new Color(255, 200, 200));
        }

        JButton editBtn = new JButton("View");
        editBtn.setFocusable(false);
        editBtn.addActionListener(ex -> createOrUpdateEvent(e, e.getStart().toLocalDate()));

        row.add(title, BorderLayout.CENTER);
        row.add(editBtn, BorderLayout.EAST);
        return row;
    }

    // --- UI TRANSITIONS ---
    private void navigate(int direction) {
        bodyPanel.animate(direction, () -> {
            controller.navigate(direction);
            syncSelectors(); // Update selectors after navigation
            refreshUI();
        });
    }

    // --- DIALOGS & USER INPUT ---
    private void showDayEvents(LocalDate date) {
        List<Event> dayEvents = controller.getEventsOnDate(date);
        
        if (dayEvents.isEmpty()) {
            int choice = JOptionPane.showConfirmDialog(this, "No events. Add one?", "Day View", JOptionPane.YES_NO_OPTION);
            if (choice == JOptionPane.YES_OPTION) createOrUpdateEvent(null, date);
            return;
        }

        DefaultListModel<String> listModel = new DefaultListModel<>();
        for (Event e : dayEvents) {
            String recurMark = !"NONE".equals(e.getRecurType()) ? " [R]" : "";
            listModel.addElement(e.getStart().toLocalTime() + " - " + e.getTitle() + recurMark);
        }
        
        JList<String> eventJList = new JList<>(listModel);
        eventJList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        eventJList.setSelectedIndex(0);

        JScrollPane scrollPane = new JScrollPane(eventJList);
        scrollPane.setPreferredSize(new Dimension(350, 150));

        Object[] message = {"Select an event:", scrollPane};
        Object[] options = {"Edit Selected", "Delete Selected", "Add New", "Close"};
        
        int choice = JOptionPane.showOptionDialog(this, message, "Events for " + date.format(dateFormat),
                JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[3]);

        int selectedIdx = eventJList.getSelectedIndex();
        
        if (choice == 0 && selectedIdx != -1) {
            createOrUpdateEvent(dayEvents.get(selectedIdx), date);
        } else if (choice == 1 && selectedIdx != -1) {
            int confirm = JOptionPane.showConfirmDialog(this, "Delete this event?");
            if (confirm == JOptionPane.YES_OPTION) {
                controller.deleteEvent(dayEvents.get(selectedIdx));
                refreshUI();
            }
        } else if (choice == 2) {
            createOrUpdateEvent(null, date);
        }
    }

    private void createOrUpdateEvent(Event existing, LocalDate targetDate) {
        JTextField titleField = new JTextField(existing != null ? existing.getTitle() : "");
        JTextField descField = new JTextField(existing != null ? existing.getDescription() : "");
        JTextField startField = new JTextField(existing != null ? existing.getStart().toLocalTime().toString() : "09:00");
        JTextField endField = new JTextField(existing != null ? existing.getEnd().toLocalTime().toString() : "10:00");
        JComboBox<String> recurBox = new JComboBox<>(new String[]{"NONE", "DAILY", "WEEKLY", "MONTHLY"});
        if (existing != null) recurBox.setSelectedItem(existing.getRecurType());
        JTextField countField = new JTextField(existing != null ? String.valueOf(existing.getRecurCount()) : "0");

        Object[] message = { 
            "Title:", titleField, 
            "Description:", descField,
            "Start (HH:mm):", startField, 
            "End (HH:mm):", endField, 
            "Repeat:", recurBox, 
            "Times:", countField 
        };
        
        if (JOptionPane.showConfirmDialog(this, message, "Event Details", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            try {
                LocalDateTime startDT = LocalDateTime.of(targetDate, LocalTime.parse(startField.getText()));
                LocalDateTime endDT = LocalDateTime.of(targetDate, LocalTime.parse(endField.getText()));
                
                if (endDT.isBefore(startDT)) {
                    JOptionPane.showMessageDialog(this, "End time must be after start time!");
                    return;
                }
                
                int id = (existing != null) ? existing.getId() : controller.getNextEventId();
                String recurType = (String)recurBox.getSelectedItem();
                int count = Integer.parseInt(countField.getText());
                
                Event newEvent = new Event(id, titleField.getText(), descField.getText(), 
                                          startDT, endDT, recurType, count);
                
                controller.addOrUpdateEvent(newEvent);
                refreshUI();
                JOptionPane.showMessageDialog(this, "Event saved successfully!");
            } catch (Exception ex) { 
                JOptionPane.showMessageDialog(this, "Error: Check time format (HH:mm)"); 
            }
        }
    }

    private void handleSearch() {
        String[] options = {"Search by Title", "Search by Date Range", "Cancel"};
        int choice = JOptionPane.showOptionDialog(this, "Select Search Type", "Search", 
                    JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);

        List<Event> results = new ArrayList<>();

        if (choice == 0) {
            String query = JOptionPane.showInputDialog(this, "Enter event title or description:");
            if (query != null) results = controller.searchEvents(query);
            
        } else if (choice == 1) {
            JTextField startField = new JTextField(LocalDate.now().toString());
            JTextField endField = new JTextField(LocalDate.now().plusMonths(1).toString());
            Object[] message = {
                "Start Date (YYYY-MM-DD):", startField,
                "End Date (YYYY-MM-DD):", endField
            };

            int option = JOptionPane.showConfirmDialog(this, message, "Date Range Search", JOptionPane.OK_CANCEL_OPTION);
            if (option == JOptionPane.OK_OPTION) {
                try {
                    LocalDate start = LocalDate.parse(startField.getText());
                    LocalDate end = LocalDate.parse(endField.getText());
                    results = controller.searchEventsByDate(start, end);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Invalid date format. Use YYYY-MM-DD.");
                    return;
                }
            }
        }

        if (choice != 2) {
            displaySearchResults(results);
        }
    }

    private void displaySearchResults(List<Event> results) {
        if (results.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No events found.");
        } else {
            StringBuilder sb = new StringBuilder("Found " + results.size() + " event(s):\n\n");
            for (Event e : results) {
                sb.append("• ").append(e.getStart().toLocalDate()).append(" ")
                  .append(e.getStart().toLocalTime()).append(" - ")
                  .append(e.getTitle());
                if (!"NONE".equals(e.getRecurType())) {
                    sb.append(" [").append(e.getRecurType()).append("]");
                }
                sb.append("\n");
            }
            
            JTextArea textArea = new JTextArea(sb.toString());
            textArea.setEditable(false);
            textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
            JScrollPane scrollPane = new JScrollPane(textArea);
            scrollPane.setPreferredSize(new Dimension(500, 400));
            
            JOptionPane.showMessageDialog(this, scrollPane, "Search Results", JOptionPane.PLAIN_MESSAGE);
        }
    }
    
    private void showWeekListView() {
        String input = JOptionPane.showInputDialog(this, 
            "Enter week start date (YYYY-MM-DD):", 
            controller.getReferenceDate().toString());
        
        if (input == null) return;
        
        try {
            LocalDate weekStart = LocalDate.parse(input);
            String weekList = controller.generateWeekListView(weekStart);
            
            JTextArea textArea = new JTextArea(weekList);
            textArea.setEditable(false);
            textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
            JScrollPane scrollPane = new JScrollPane(textArea);
            scrollPane.setPreferredSize(new Dimension(500, 400));
            
            JOptionPane.showMessageDialog(this, scrollPane, "Week List View", JOptionPane.PLAIN_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Invalid date format!");
        }
    }
    
    private void showUpcomingNotifications() {
        List<Event> upcoming = controller.getUpcomingEvents(24); // Next 24 hours
        
        if (upcoming.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No upcoming events in the next 24 hours.", 
                                        "Notifications", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        StringBuilder sb = new StringBuilder("Upcoming Events (Next 24 Hours):\n\n");
        LocalDateTime now = LocalDateTime.now();
        
        for (Event e : upcoming) {
            long minutesUntil = java.time.Duration.between(now, e.getStart()).toMinutes();
            long hoursUntil = minutesUntil / 60;
            minutesUntil = minutesUntil % 60;
            
            sb.append("• ").append(e.getTitle()).append("\n");
            sb.append("  Time: ").append(e.getStart().toLocalDate()).append(" ")
              .append(e.getStart().toLocalTime()).append("\n");
            sb.append("  In: ").append(hoursUntil).append("h ").append(minutesUntil).append("m\n\n");
        }
        
        JTextArea textArea = new JTextArea(sb.toString());
        textArea.setEditable(false);
        textArea.setFont(new Font("Arial", Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(400, 300));
        
        JOptionPane.showMessageDialog(this, scrollPane, "Event Notifications", JOptionPane.INFORMATION_MESSAGE);
    }
    
    private void handleBackup() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save Backup");
        fileChooser.setFileFilter(new FileNameExtensionFilter("ZIP files", "zip"));
        fileChooser.setSelectedFile(new File("calendar_backup_" + LocalDate.now() + ".zip"));
        
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                String path = fileChooser.getSelectedFile().getAbsolutePath();
                if (!path.endsWith(".zip")) path += ".zip";
                controller.performBackup(path);
                JOptionPane.showMessageDialog(this, 
                    "Backup completed!\n\nLocation: " + path,
                    "Backup Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Backup failed: " + ex.getMessage(), 
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private void handleRestore() {
        String[] opts = {"Append to existing", "Replace all", "Cancel"};
        int restoreChoice = JOptionPane.showOptionDialog(this,
            "How would you like to restore?\n\n" +
            "Append: Keep existing events and add imported ones\n" +
            "Replace: Delete all current events and use only imported ones",
            "Restore Options", JOptionPane.DEFAULT_OPTION, 
            JOptionPane.QUESTION_MESSAGE, null, opts, opts[2]);
        
        if (restoreChoice == 2 || restoreChoice == -1) return;
        
        boolean append = (restoreChoice == 0);
        
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select Backup File");
        fileChooser.setFileFilter(new FileNameExtensionFilter("ZIP files", "zip"));
        
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                String path = fileChooser.getSelectedFile().getAbsolutePath();
                controller.performRestore(path, append);
                refreshUI();
                JOptionPane.showMessageDialog(this, "Restore complete!", 
                    "Restore Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Restore failed: " + ex.getMessage(), 
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == buttonPrev) {
            navigate(-1);
        } else if (e.getSource() == buttonNext) {
            navigate(1);
        }
    }
}