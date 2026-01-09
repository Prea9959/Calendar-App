import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class LaunchPage extends JFrame implements ActionListener {
    
    private CalendarController controller;

    // UI Components
    JPanel headerPanel, contentPanel;
    TransitionPanel bodyPanel;
    JComboBox<CalendarController.ViewMode> viewToggle;
    JComboBox<CalendarController.TimeScale> scaleToggle;
    JButton buttonPrev, buttonNext, buttonSearch, buttonBackup, buttonRestore, buttonAdd;

    // Design
    Font dayFont = new Font("Arial", Font.BOLD, 24);
    Font labelFont = new Font("Arial", Font.BOLD, 18);
    // FIX: Using the dateFormat variable for window titles and logs
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
        this.setVisible(true);
    }

    private void setupHeader() {
        headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        headerPanel.setBackground(new Color(240, 240, 240));

        viewToggle = new JComboBox<>(CalendarController.ViewMode.values());
        scaleToggle = new JComboBox<>(CalendarController.TimeScale.values());
        scaleToggle.setSelectedItem(CalendarController.TimeScale.MONTH);

        // Update Controller State on Change
        viewToggle.addActionListener(e -> { 
            controller.setMode((CalendarController.ViewMode)viewToggle.getSelectedItem()); 
            refreshUI(); 
        });
        scaleToggle.addActionListener(e -> { 
            controller.setScale((CalendarController.TimeScale)scaleToggle.getSelectedItem()); 
            refreshUI(); 
        });

        buttonPrev = new JButton("<");
        buttonNext = new JButton(">");
        buttonAdd = new JButton("+ Event");
        buttonSearch = new JButton("Search");
        buttonBackup = new JButton("Backup");
        buttonRestore = new JButton("Restore");

        JButton[] buttons = {buttonPrev, buttonNext, buttonAdd, buttonSearch, buttonBackup, buttonRestore};
        for (JButton b : buttons) {
            b.setFocusable(false);
            b.addActionListener(this);
        }

        headerPanel.add(new JLabel("View:"));
        headerPanel.add(viewToggle);
        headerPanel.add(scaleToggle);
        headerPanel.add(new JSeparator(SwingConstants.VERTICAL));
        headerPanel.add(buttonPrev);
        headerPanel.add(buttonNext);
        headerPanel.add(buttonAdd);
        headerPanel.add(buttonSearch);
        headerPanel.add(buttonBackup);
        headerPanel.add(buttonRestore);
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
        int length = (controller.getScale() == CalendarController.TimeScale.MONTH) ? controller.getReferenceDate().lengthOfMonth() : 7;
        
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
        
        // Ask Controller about conflicts
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
        
        // Ask Controller for filtered list
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
        JLabel title = new JLabel("<html><b>" + e.getTitle() + "</b> (" + timeInfo + ")<br>" + e.getRecurType() + "</html>");
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
        // We pass the "Logic" as a Runnable (Lambda) to the panel
        bodyPanel.animate(direction, () -> {
            // This code runs in the middle of the snapshot process
            controller.navigate(direction);
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

        // 1. Create a list of titles for the user to pick from
        DefaultListModel<String> listModel = new DefaultListModel<>();
        for (Event e : dayEvents) {
            listModel.addElement(e.getStart().toLocalTime() + " - " + e.getTitle());
        }
        
        JList<String> eventJList = new JList<>(listModel);
        eventJList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        eventJList.setSelectedIndex(0); // Default to first item

        // 2. Wrap the list in a scroll pane in case there are many events
        JScrollPane scrollPane = new JScrollPane(eventJList);
        scrollPane.setPreferredSize(new Dimension(300, 150));

        Object[] message = {
            "Select an event:", scrollPane
        };

        Object[] options = {"Edit Selected", "Delete Selected", "Add New", "Close"};
        
        int choice = JOptionPane.showOptionDialog(
            this, 
            message, 
            "Events for " + date.format(dateFormat),
            JOptionPane.DEFAULT_OPTION, 
            JOptionPane.PLAIN_MESSAGE, 
            null, 
            options, 
            options[3]
        );

        // 3. Handle the selection logic
        int selectedIdx = eventJList.getSelectedIndex();
        
        if (choice == 0 && selectedIdx != -1) { // Edit
            createOrUpdateEvent(dayEvents.get(selectedIdx), date);
        } else if (choice == 1 && selectedIdx != -1) { // Delete
            int confirm = JOptionPane.showConfirmDialog(this, "Delete this event?");
            if (confirm == JOptionPane.YES_OPTION) {
                controller.deleteEvent(dayEvents.get(selectedIdx));
                refreshUI();
            }
        } else if (choice == 2) { // Add New
            createOrUpdateEvent(null, date);
        }
    }

    private void createOrUpdateEvent(Event existing, LocalDate targetDate) {
        JTextField titleField = new JTextField(existing != null ? existing.getTitle() : "");
        JTextField startField = new JTextField(existing != null ? existing.getStart().toLocalTime().toString() : "12:00");
        JTextField endField = new JTextField(existing != null ? existing.getEnd().toLocalTime().toString() : "13:00");
        JComboBox<String> recurBox = new JComboBox<>(new String[]{"NONE", "DAILY", "WEEKLY", "MONTHLY"});
        if (existing != null) {
            recurBox.setSelectedItem(existing.getRecurType()); // Reset the dropdown to the saved value
        }
        JTextField countField = new JTextField(existing != null ? String.valueOf(existing.getRecurCount()) : "1");

        Object[] message = { "Title:", titleField, "Start (HH:mm):", startField, "End (HH:mm):", endField, "Repeat:", recurBox, "Count:", countField };
        if (JOptionPane.showConfirmDialog(this, message, "Event Details", 2) == 0) {
            try {
                LocalDateTime startDT = LocalDateTime.of(targetDate, LocalTime.parse(startField.getText()));
                LocalDateTime endDT = LocalDateTime.of(targetDate, LocalTime.parse(endField.getText()));
                
                int id = (existing != null) ? existing.getId() : controller.getNextEventId();
                Event newEvent = new Event(id, titleField.getText(), "Manual", startDT, endDT, (String)recurBox.getSelectedItem(), Integer.parseInt(countField.getText()));
                
                controller.addOrUpdateEvent(newEvent);
                refreshUI();
            } catch (Exception ex) { JOptionPane.showMessageDialog(this, "Error: Check time format (HH:mm)"); }
        }
    }

    private void handleSearch() {
        String[] options = {"Search by Name", "Search by Date Range", "Cancel"};
        int choice = JOptionPane.showOptionDialog(this, "Select Search Type", "Search", 
                    0, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);

        List<Event> results = new ArrayList<>();

        if (choice == 0) { // By Name
            String query = JOptionPane.showInputDialog(this, "Enter event title:");
            if (query != null) results = controller.searchEvents(query);
            
        } else if (choice == 1) { // By Date Range
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

        if (choice != 2) { // If not cancelled
            displaySearchResults(results);
        }
    }

    private void displaySearchResults(List<Event> results) {
        if (results.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No events found.");
        } else {
            // We temporarily hijack the List View logic to show results
            controller.setMode(CalendarController.ViewMode.LIST);
            viewToggle.setSelectedItem(CalendarController.ViewMode.LIST);
            
            // Custom refresh to show ONLY search results
            contentPanel.removeAll();
            JPanel listPanel = new JPanel();
            listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
            
            for (Event e : results) {
                listPanel.add(createEventRow(e));
                listPanel.add(Box.createRigidArea(new Dimension(0, 5)));
            }
            
            contentPanel.add(new JScrollPane(listPanel), BorderLayout.CENTER);
            contentPanel.revalidate();
            contentPanel.repaint();
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == buttonPrev) navigate(-1);
        else if (e.getSource() == buttonNext) navigate(1);
        else if (e.getSource() == buttonAdd) createOrUpdateEvent(null, controller.getReferenceDate());
        else if (e.getSource() == buttonSearch) handleSearch();
        else if (e.getSource() == buttonBackup) {
            try {
                controller.performBackup("calendar_backup.zip");
                JOptionPane.showMessageDialog(this, "Data successfully backed up!");
            } catch (Exception ex) { JOptionPane.showMessageDialog(this, "Backup failed: " + ex.getMessage()); }
        } else if (e.getSource() == buttonRestore) {
            int confirm = JOptionPane.showConfirmDialog(this, "Overwrite current events?", "Confirm Restore", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                try {
                    controller.performRestore("calendar_backup.zip");
                    refreshUI();
                    JOptionPane.showMessageDialog(this, "Restore complete!");
                } catch (Exception ex) { JOptionPane.showMessageDialog(this, "Restore failed."); }
            }
        }
    }
}