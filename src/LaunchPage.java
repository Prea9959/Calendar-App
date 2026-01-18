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

    // Colour Palette
    final Color THEME_LIGHTEST = new Color(252, 248, 248);  
    final Color THEME_LIGHT = new Color(251, 239, 239);     
    final Color THEME_MEDIUM = new Color(249, 223, 223);    
    final Color THEME_DARK = new Color(245, 175, 175);      

    final Color HEADER_BG = THEME_LIGHT;            
    final Color DAY_HEADER_BG = THEME_DARK;         
    final Color EVENT_BG = THEME_MEDIUM;            
    final Color EVENT_BORDER = THEME_DARK;          
    final Color TODAY_BORDER = THEME_DARK;          
    
    final Color NO_CONFLICT_BG = THEME_LIGHTEST;    
    final Color CONFLICT_BG = THEME_DARK;

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
        headerPanel.setBackground(HEADER_BG);

        JPanel leftContainer = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        JPanel rightContainer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        leftContainer.setOpaque(false);
        rightContainer.setOpaque(false);

        // Left side: View toggles and Month/Year selectors
        viewToggle = new JComboBox<>(CalendarController.ViewMode.values());
        scaleToggle = new JComboBox<>(CalendarController.TimeScale.values());
        scaleToggle.setSelectedItem(CalendarController.TimeScale.MONTH);

        String[] months = {"January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"};
        monthSelector = new JComboBox<>(months);
        
        int currentYear = LocalDate.now().getYear();
        String[] years = new String[21];
        for (int i = 0; i < 21; i++) {
            years[i] = String.valueOf(currentYear - 10 + i);
        }
        yearSelector = new JComboBox<>(years);
        
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
        
        // Listeners for selectors
        ActionListener dateUpdateListener = e -> {
            if (monthSelector.getSelectedIndex() >= 0 && yearSelector.getSelectedItem() != null) {
                controller.setReferenceDateFromSelectors(
                    monthSelector.getSelectedIndex(), 
                    (String) yearSelector.getSelectedItem()
                );
                refreshUI();
            }
        };
        monthSelector.addActionListener(dateUpdateListener);
        yearSelector.addActionListener(dateUpdateListener);

        leftContainer.add(new JLabel("View:"));
        leftContainer.add(viewToggle);
        leftContainer.add(scaleToggle);
        leftContainer.add(new JSeparator(SwingConstants.VERTICAL));
        leftContainer.add(new JLabel("Date:"));
        leftContainer.add(monthSelector);
        leftContainer.add(yearSelector);

        // Right side: Navigation and Actions
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

        rightContainer.add(buttonPrev);
        rightContainer.add(buttonNext);
        rightContainer.add(actionsButton);

        headerPanel.add(leftContainer, BorderLayout.WEST);
        headerPanel.add(rightContainer, BorderLayout.EAST);
    }
    
    private void syncSelectors() {
        LocalDate refDate = controller.getReferenceDate();
        
        // Disable listeners temporarily to prevent loops
        ActionListener[] monthListeners = monthSelector.getActionListeners();
        ActionListener[] yearListeners = yearSelector.getActionListeners();
        for (ActionListener al : monthListeners) monthSelector.removeActionListener(al);
        for (ActionListener al : yearListeners) yearSelector.removeActionListener(al);
        
        monthSelector.setSelectedIndex(refDate.getMonthValue() - 1);
        yearSelector.setSelectedItem(String.valueOf(refDate.getYear()));
        
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

    // --- VIEW RENDERING (Unchanged View Logic) ---
    private void renderCalendarView() {
        if (controller.getScale() == CalendarController.TimeScale.DAY) {
            renderDayTimelineView();
            return;
        }
        
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
    
    private void renderDayTimelineView() {
        LocalDate date = controller.getReferenceDate();
        List<Event> dayEvents = controller.getEventsOnDate(date);
        
        JPanel mainPanel = new JPanel(new BorderLayout());
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(DAY_HEADER_BG);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JLabel headerLabel = new JLabel(date.getDayOfWeek() + ", " + date);
        headerLabel.setFont(new Font("Arial", Font.BOLD, 20));
        headerLabel.setForeground(Color.WHITE);
        headerPanel.add(headerLabel, BorderLayout.CENTER);
        
        JButton addEventBtn = new JButton("+ Add Event");
        addEventBtn.setFocusable(false);
        addEventBtn.addActionListener(e -> createOrUpdateEvent(null, date));
        headerPanel.add(addEventBtn, BorderLayout.EAST);
        
        mainPanel.add(headerPanel, BorderLayout.NORTH);
        
        JPanel timelinePanel = new JPanel();
        timelinePanel.setLayout(new BoxLayout(timelinePanel, BoxLayout.Y_AXIS));
        timelinePanel.setBackground(Color.WHITE);
        
        for (int hour = 0; hour < 24; hour++) {
            JPanel hourSlot = createLargeHourSlot(hour, dayEvents, date);
            timelinePanel.add(hourSlot);
        }
        
        JScrollPane scrollPane = new JScrollPane(timelinePanel);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        contentPanel.add(mainPanel, BorderLayout.CENTER);
    }
    
    private JPanel createLargeHourSlot(int hour, List<Event> dayEvents, LocalDate date) {
        JPanel slot = new JPanel(new BorderLayout());
        slot.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY));
        slot.setBackground(Color.WHITE);
        slot.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        slot.setPreferredSize(new Dimension(800, 80));
        
        if (date.equals(LocalDate.now()) && LocalTime.now().getHour() == hour) {
            slot.setBackground(new Color(255, 255, 220)); 
        }
        
        JPanel timePanel = new JPanel(new BorderLayout());
        timePanel.setBackground(slot.getBackground());
        timePanel.setPreferredSize(new Dimension(80, 80));
        JLabel timeLabel = new JLabel(String.format("%02d:00", hour), SwingConstants.CENTER);
        timeLabel.setFont(new Font("Monospaced", Font.BOLD, 16));
        timePanel.add(timeLabel, BorderLayout.CENTER);
        slot.add(timePanel, BorderLayout.WEST);
        
        JPanel eventPanel = new JPanel();
        eventPanel.setLayout(new BoxLayout(eventPanel, BoxLayout.Y_AXIS));
        eventPanel.setBackground(slot.getBackground());
        eventPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        
        boolean hasEvents = false;
        for (Event event : dayEvents) {
            if (event.getStart().getHour() <= hour && hour < event.getEnd().getHour()) {
                hasEvents = true;
                JPanel eventBar = new JPanel(new BorderLayout());
                eventBar.setBackground(EVENT_BG);
                eventBar.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(EVENT_BORDER, 2),
                    BorderFactory.createEmptyBorder(5, 8, 5, 8)
                ));
                eventBar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                eventBar.addMouseListener(new MouseAdapter() {
                    public void mouseClicked(MouseEvent e) { createOrUpdateEvent(event, date); }
                });
                
                JLabel titleLabel = new JLabel("<html><b>" + event.getTitle() + "</b></html>");
                eventBar.add(titleLabel, BorderLayout.CENTER);
                eventPanel.add(eventBar);
                eventPanel.add(Box.createVerticalStrut(3));
            }
        }
        if (!hasEvents) eventPanel.add(new JLabel(" ")); 
        
        slot.add(eventPanel, BorderLayout.CENTER);
        return slot;
    }

    private JButton createDayButton(LocalDate date) {
        List<Event> dayEvents = controller.getEventsOnDate(date);
        
        String text = "<html><center>" + date.getDayOfMonth();
        if (!dayEvents.isEmpty()) text += "<br><font size='2'>● " + dayEvents.size() + " Event(s)</font>";
        text += "</center></html>";

        JButton btn = new JButton(text);
        btn.setFocusable(false);
        btn.setFont(dayFont);
        
        if (date.equals(LocalDate.now())) btn.setBorder(BorderFactory.createLineBorder(TODAY_BORDER, 2));
        
        if (!dayEvents.isEmpty()) {
            boolean conflict = controller.checkForConflictOnDate(dayEvents);
            btn.setBackground(conflict ? CONFLICT_BG : NO_CONFLICT_BG);
        }

        btn.addActionListener(e -> showDayEvents(date));
        return btn;
    }

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
        JLabel title = new JLabel("<html><b>" + e.getTitle() + "</b> (" + timeInfo + ")</html>");
        title.setBorder(BorderFactory.createEmptyBorder(5,10,5,10));
        
        if (controller.hasConflict(e)) row.setBackground(new Color(255, 200, 200));

        JButton editBtn = new JButton("View");
        editBtn.setFocusable(false);
        editBtn.addActionListener(ex -> createOrUpdateEvent(e, e.getStart().toLocalDate()));

        row.add(title, BorderLayout.CENTER);
        row.add(editBtn, BorderLayout.EAST);
        return row;
    }

    private void navigate(int direction) {
        bodyPanel.animate(direction, () -> {
            controller.navigate(direction);
            syncSelectors();
            refreshUI();
        });
    }

    // --- DIALOGS (Heavy Logic Moved to Controller) ---
    private void showDayEvents(LocalDate date) {
        // Simplified to just show options; timeline rendering logic remains in Helper below
        List<Event> dayEvents = controller.getEventsOnDate(date);
        JPanel dayViewPanel = createDayScheduleView(date, dayEvents);
        
        Object[] options = {"Add New Event", "Close"};
        int choice = JOptionPane.showOptionDialog(this, dayViewPanel, 
            "Schedule for " + date, JOptionPane.DEFAULT_OPTION, 
            JOptionPane.PLAIN_MESSAGE, null, options, options[1]);

        if (choice == 0) createOrUpdateEvent(null, date);
    }
    
    // Helper for visual timeline (UI only)
    private JPanel createDayScheduleView(LocalDate date, List<Event> dayEvents) {
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setPreferredSize(new Dimension(400, 400));
        
        // Simplified timeline for the popup
        JTextArea area = new JTextArea();
        area.setEditable(false);
        StringBuilder sb = new StringBuilder();
        if(dayEvents.isEmpty()) sb.append("No events.");
        for(Event e : dayEvents) sb.append(e.getStart().toLocalTime()).append(" - ").append(e.getTitle()).append("\n");
        area.setText(sb.toString());
        
        mainPanel.add(new JScrollPane(area), BorderLayout.CENTER);
        return mainPanel;
    }

    private void createOrUpdateEvent(Event existing, LocalDate targetDate) {
        // 1. Prepare UI inputs
        JTextField titleField = new JTextField(existing != null ? existing.getTitle() : "");
        JTextField descField = new JTextField(existing != null ? existing.getDescription() : "");
        JTextField startField = new JTextField(existing != null ? existing.getStart().toLocalTime().toString() : "09:00");
        JTextField endField = new JTextField(existing != null ? existing.getEnd().toLocalTime().toString() : "10:00");
        JComboBox<String> recurBox = new JComboBox<>(new String[]{"NONE", "DAILY", "WEEKLY", "MONTHLY"});
        if (existing != null) recurBox.setSelectedItem(existing.getRecurType());
        JTextField countField = new JTextField(existing != null ? String.valueOf(existing.getRecurCount()) : "0");

        Object[] message = { 
            "Title:", titleField, "Description:", descField,
            "Start (HH:mm):", startField, "End (HH:mm):", endField, 
            "Repeat:", recurBox, "Times:", countField 
        };
        
        // 2. Show Dialog
        int option = JOptionPane.showConfirmDialog(this, message, "Event Details", JOptionPane.OK_CANCEL_OPTION);
        
        // 3. Pass data to Controller for Logic & Validation
        if (option == JOptionPane.OK_OPTION) {
            try {
                controller.processEventUpsert(
                    existing, targetDate,
                    titleField.getText(), descField.getText(),
                    startField.getText(), endField.getText(),
                    (String) recurBox.getSelectedItem(), countField.getText()
                );
                refreshUI();
                JOptionPane.showMessageDialog(this, "Event saved successfully!");
            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Input Error", JOptionPane.ERROR_MESSAGE);
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
            results = controller.searchEvents(query);
            
        } else if (choice == 1) {
            JTextField startField = new JTextField(LocalDate.now().toString());
            JTextField endField = new JTextField(LocalDate.now().plusMonths(1).toString());
            Object[] message = { "Start Date (YYYY-MM-DD):", startField, "End Date (YYYY-MM-DD):", endField };

            int option = JOptionPane.showConfirmDialog(this, message, "Date Range Search", JOptionPane.OK_CANCEL_OPTION);
            if (option == JOptionPane.OK_OPTION) {
                try {
                    results = controller.processDateRangeSearch(startField.getText(), endField.getText());
                } catch (IllegalArgumentException ex) {
                    JOptionPane.showMessageDialog(this, ex.getMessage());
                    return;
                }
            }
        }

        if (choice != 2) {
            showScrollableMessage("Search Results", controller.getSearchResultsSummary(results));
        }
    }
    
    private void showWeekListView() {
        String input = JOptionPane.showInputDialog(this, "Enter week start date (YYYY-MM-DD):", controller.getReferenceDate().toString());
        if (input == null) return;
        
        try {
            LocalDate weekStart = LocalDate.parse(input);
            String report = controller.generateWeekListView(weekStart);
            showScrollableMessage("Week List View", report);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Invalid date format!");
        }
    }
    
    private void showUpcomingNotifications() {
        String report = controller.getNotificationSummary();
        // Only show if it's not the empty message, or if specifically requested via menu
        if (!report.startsWith("No upcoming") || isVisible()) { 
             showScrollableMessage("Event Notifications", report);
        }
    }
    
    // Generic Helper for text popups
    private void showScrollableMessage(String title, String content) {
        JTextArea textArea = new JTextArea(content);
        textArea.setEditable(false);
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(500, 400));
        JOptionPane.showMessageDialog(this, scrollPane, title, JOptionPane.INFORMATION_MESSAGE);
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
                JOptionPane.showMessageDialog(this, "Backup completed!\n" + path);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Backup failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private void handleRestore() {
        String[] opts = {"Append to existing", "Replace all", "Cancel"};
        int choice = JOptionPane.showOptionDialog(this, "Restore Options", "Restore", 
                     JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, opts, opts[2]);
        
        if (choice == 2 || choice == -1) return;
        
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileNameExtensionFilter("ZIP files", "zip"));
        
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                controller.performRestore(fileChooser.getSelectedFile().getAbsolutePath(), choice == 0);
                refreshUI();
                JOptionPane.showMessageDialog(this, "Restore complete!");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Restore failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == buttonPrev) navigate(-1);
        else if (e.getSource() == buttonNext) navigate(1);
    }
}