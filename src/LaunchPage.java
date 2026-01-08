import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.event.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class LaunchPage extends JFrame implements ActionListener {
    // State Management
    enum ViewMode { CALENDAR, LIST }
    enum TimeScale { DAY, WEEK, MONTH }

    private ViewMode currentMode = ViewMode.CALENDAR;
    private TimeScale currentScale = TimeScale.MONTH;
    private LocalDate referenceDate = LocalDate.now(); 

    // UI Components
    JPanel headerPanel, contentPanel;
    JComboBox<ViewMode> viewToggle;
    JComboBox<TimeScale> scaleToggle;
    JButton buttonPrev, buttonNext, buttonConflict, buttonSearch, buttonBackup, buttonRestore, buttonAdd;
    
    FileHandler fileHandler = new FileHandler();
    List<Event> events = new ArrayList<>();
    boolean showConflicts = false;

    // Design
    Font dayFont = new Font("Arial", Font.BOLD, 24);
    Font labelFont = new Font("Arial", Font.BOLD, 18);
    // FIX: Using the dateFormat variable for window titles and logs
    private final DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    public LaunchPage() {
        this.setSize(1100, 800);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setTitle("Personal Calendar - Multi-View Edition");

        try { events = fileHandler.loadEvents(); } catch (Exception e) { e.printStackTrace(); }

        setupHeader();

        contentPanel = new JPanel(new BorderLayout());
        this.add(headerPanel, BorderLayout.NORTH);
        this.add(new JScrollPane(contentPanel), BorderLayout.CENTER);

        refreshUI();
        this.setVisible(true);
    }

    private void setupHeader() {
        headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        headerPanel.setBackground(new Color(240, 240, 240));

        viewToggle = new JComboBox<>(ViewMode.values());
        scaleToggle = new JComboBox<>(TimeScale.values());
        scaleToggle.setSelectedItem(TimeScale.MONTH);

        viewToggle.addActionListener(e -> { currentMode = (ViewMode)viewToggle.getSelectedItem(); refreshUI(); });
        scaleToggle.addActionListener(e -> { currentScale = (TimeScale)scaleToggle.getSelectedItem(); refreshUI(); });

        buttonPrev = new JButton("<");
        buttonNext = new JButton(">");
        buttonAdd = new JButton("+ Event");
        buttonSearch = new JButton("Search");
        buttonConflict = new JButton("Conflicts");
        buttonBackup = new JButton("Backup");
        buttonRestore = new JButton("Restore");

        JButton[] buttons = {buttonPrev, buttonNext, buttonAdd, buttonSearch, buttonConflict, buttonBackup, buttonRestore};
        for (JButton b : buttons) b.addActionListener(this);

        headerPanel.add(new JLabel("View:"));
        headerPanel.add(viewToggle);
        headerPanel.add(scaleToggle);
        headerPanel.add(new JSeparator(SwingConstants.VERTICAL));
        headerPanel.add(buttonPrev);
        headerPanel.add(buttonNext);
        headerPanel.add(buttonAdd);
        headerPanel.add(buttonSearch);
        headerPanel.add(buttonConflict);
        headerPanel.add(buttonBackup);
        headerPanel.add(buttonRestore);
    }

    private void refreshUI() {
        contentPanel.removeAll();
        
        // Use dateFormat to format the title bar
        String dateString = referenceDate.format(dateFormat);
        this.setTitle("Calendar - " + dateString + " (" + currentMode + ")");

        if (currentMode == ViewMode.CALENDAR) {
            renderCalendarView();
        } else {
            renderListView();
        }

        contentPanel.revalidate();
        contentPanel.repaint();
    }

    // --- CALENDAR RENDERING ---
    private void renderCalendarView() {
        JPanel grid = new JPanel(new GridLayout(0, 7, 2, 2));
        String[] days = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
        for (String d : days) {
            JLabel l = new JLabel(d, SwingConstants.CENTER);
            l.setFont(labelFont);
            grid.add(l);
        }

        LocalDate start = getStartOfRange();
        int length = (currentScale == TimeScale.MONTH) ? referenceDate.lengthOfMonth() : 7;
        
        if (currentScale == TimeScale.MONTH) {
            int startPadding = referenceDate.withDayOfMonth(1).getDayOfWeek().getValue() % 7;
            for (int i = 0; i < startPadding; i++) grid.add(new JLabel(""));
        }

        for (int i = 0; i < length; i++) {
            LocalDate date = start.plusDays(i);
            JButton dayBtn = createDayButton(date);
            grid.add(dayBtn);
        }
        contentPanel.add(grid, BorderLayout.CENTER);
    }

    private JButton createDayButton(LocalDate date) {
        List<Event> dayEvents = getEventsOnDate(date);
        String text = "<html><center>" + date.getDayOfMonth();
        if (!dayEvents.isEmpty()) text += "<br><font size='2'>● " + dayEvents.size() + " Event(s)</font>";
        text += "</center></html>";

        JButton btn = new JButton(text);
        btn.setFont(dayFont);
        
        if (date.equals(LocalDate.now())) btn.setBorder(BorderFactory.createLineBorder(Color.BLUE, 2));
        
        if (!dayEvents.isEmpty()) {
            boolean conflict = showConflicts && checkForConflictOnDate(dayEvents);
            btn.setBackground(conflict ? new Color(255, 180, 180) : new Color(200, 230, 255));
        }

        btn.addActionListener(e -> showDayEvents(date));
        return btn;
    }

    private void renderListView() {
        JPanel listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        
        LocalDate start = getStartOfRange();
        LocalDate end = getEndOfRange();

        List<Event> filtered = events.stream()
                .filter(e -> occursInRange(e, start, end))
                .sorted(Comparator.comparing(Event::getStart))
                .collect(Collectors.toList());

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
        
        if (showConflicts && hasGenericConflict(e)) {
            row.setBackground(new Color(255, 200, 200));
        }

        JButton editBtn = new JButton("View");
        editBtn.addActionListener(ex -> createOrUpdateEvent(e, e.getStart().toLocalDate()));

        row.add(title, BorderLayout.CENTER);
        row.add(editBtn, BorderLayout.EAST);
        return row;
    }

    private LocalDate getStartOfRange() {
        switch (currentScale) {
            case DAY: return referenceDate;
            case WEEK: return getStartOfWeek(referenceDate);
            default: return referenceDate.withDayOfMonth(1);
        }
    }

    private LocalDate getEndOfRange() {
        switch (currentScale) {
            case DAY: return referenceDate;
            case WEEK: return getStartOfWeek(referenceDate).plusDays(6);
            default: return referenceDate.with(TemporalAdjusters.lastDayOfMonth());
        }
    }

    private LocalDate getStartOfWeek(LocalDate date) {
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));
    }

    private List<Event> getEventsOnDate(LocalDate date) {
        return events.stream().filter(e -> e.occursOn(date)).collect(Collectors.toList());
    }

    private boolean occursInRange(Event e, LocalDate start, LocalDate end) {
        for (int i = 0; i < e.getRecurCount(); i++) {
            LocalDate occurrence = e.getOccurrence(i).toLocalDate();
            if (!occurrence.isBefore(start) && !occurrence.isAfter(end)) return true;
        }
        return false;
    }

    private boolean hasGenericConflict(Event e) {
        List<Event> sameDay = getEventsOnDate(e.getStart().toLocalDate());
        return checkForConflictOnDate(sameDay);
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

    // FIX: Re-implemented BufferedImage logic for transitions
    private void navigate(int direction) {
        Container content = this.getContentPane();
        BufferedImage before = new BufferedImage(content.getWidth(), content.getHeight(), BufferedImage.TYPE_INT_ARGB);
        content.paint(before.getGraphics());

        switch (currentScale) {
            case DAY: referenceDate = referenceDate.plusDays(direction); break;
            case WEEK: referenceDate = referenceDate.plusWeeks(direction); break;
            case MONTH: referenceDate = referenceDate.plusMonths(direction); break;
        }
        
        refreshUI();

        BufferedImage after = new BufferedImage(content.getWidth(), content.getHeight(), BufferedImage.TYPE_INT_ARGB);
        content.paint(after.getGraphics());

        Animator.Direction animDir = (direction > 0) ? Animator.Direction.LEFT : Animator.Direction.RIGHT;
        Animator.slideTransition(this, contentPanel, before, after, animDir, 300, () -> {});
    }

    private void showDayEvents(LocalDate date) {
        List<Event> dayEvents = getEventsOnDate(date);
        StringBuilder sb = new StringBuilder("Events for " + date.format(dateFormat) + ":\n");
        for (int i = 0; i < dayEvents.size(); i++) sb.append(i + 1).append(". ").append(dayEvents.get(i).getTitle()).append("\n");
        
        Object[] options = {"Add New", "Edit", "Delete", "Close"};
        int choice = JOptionPane.showOptionDialog(this, sb.toString(), "Day View", 0, 1, null, options, options[3]);

        if (choice == 0) createOrUpdateEvent(null, date);
        else if (choice == 1 && !dayEvents.isEmpty()) createOrUpdateEvent(dayEvents.get(0), date);
        else if (choice == 2 && !dayEvents.isEmpty()) { events.remove(dayEvents.get(0)); saveAndRefresh(); }
    }

    private void createOrUpdateEvent(Event existing, LocalDate targetDate) {
        JTextField titleField = new JTextField(existing != null ? existing.getTitle() : "");
        JTextField startField = new JTextField(existing != null ? existing.getStart().toLocalTime().toString() : "12:00");
        JTextField endField = new JTextField(existing != null ? existing.getEnd().toLocalTime().toString() : "13:00");
        JComboBox<String> recurBox = new JComboBox<>(new String[]{"NONE", "DAILY", "WEEKLY", "MONTHLY"});
        JTextField countField = new JTextField(existing != null ? String.valueOf(existing.getRecurCount()) : "1");

        Object[] message = { "Title:", titleField, "Start (HH:mm):", startField, "End (HH:mm):", endField, "Repeat:", recurBox, "Count:", countField };
        if (JOptionPane.showConfirmDialog(this, message, "Event Details", 2) == 0) {
            try {
                LocalDateTime startDT = LocalDateTime.of(targetDate, LocalTime.parse(startField.getText()));
                LocalDateTime endDT = LocalDateTime.of(targetDate, LocalTime.parse(endField.getText()));
                if (existing != null) {
                    existing.setTitle(titleField.getText()); existing.setStart(startDT); existing.setEnd(endDT);
                } else {
                    int newId = events.stream().mapToInt(Event::getId).max().orElse(0) + 1;
                    events.add(new Event(newId, titleField.getText(), "Manual", startDT, endDT, (String)recurBox.getSelectedItem(), Integer.parseInt(countField.getText())));
                }
                saveAndRefresh();
            } catch (Exception ex) { JOptionPane.showMessageDialog(this, "Error: Check time format (HH:mm)"); }
        }
    }

    private void saveAndRefresh() {
        try { fileHandler.saveEvents(events); } catch (Exception e) { e.printStackTrace(); }
        refreshUI();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == buttonPrev) navigate(-1);
        else if (e.getSource() == buttonNext) navigate(1);
        else if (e.getSource() == buttonAdd) createOrUpdateEvent(null, referenceDate);
        else if (e.getSource() == buttonConflict) {
            showConflicts = !showConflicts;
            buttonConflict.setText(showConflicts ? "Hide Conflicts" : "Conflicts");
            refreshUI();
        } else if (e.getSource() == buttonSearch) handleSearch();
        else if (e.getSource() == buttonBackup) {
            try {
                fileHandler.backup("calendar_backup.zip");
                JOptionPane.showMessageDialog(this, "Data successfully backed up to calendar_backup.zip");
            } catch (Exception ex) { JOptionPane.showMessageDialog(this, "Backup failed: " + ex.getMessage()); }
        } else if (e.getSource() == buttonRestore) {
            int confirm = JOptionPane.showConfirmDialog(this, "Restoring will overwrite current events. Continue?", "Confirm Restore", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                try {
                    fileHandler.restore("calendar_backup.zip");
                    events = fileHandler.loadEvents();
                    refreshUI();
                    JOptionPane.showMessageDialog(this, "Restore complete!");
                } catch (Exception ex) { JOptionPane.showMessageDialog(this, "Restore failed."); }
            }
        }
    }

    private void handleSearch() {
        String query = JOptionPane.showInputDialog(this, "Search event title:");
        if (query != null && !query.trim().isEmpty()) {
            List<Event> results = events.stream()
                .filter(ev -> ev.getTitle().toLowerCase().contains(query.toLowerCase()))
                .collect(Collectors.toList());
            if (results.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No events found matching: " + query);
            } else {
                currentMode = ViewMode.LIST;
                viewToggle.setSelectedItem(ViewMode.LIST);
                displaySearchResults(results, query);
            }
        }
    }

    private void displaySearchResults(List<Event> results, String query) {
        contentPanel.removeAll();
        JPanel listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.add(new JLabel("Search Results for: " + query));
        listPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        for (Event ev : results) {
            listPanel.add(createEventRow(ev));
            listPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        }
        contentPanel.add(listPanel, BorderLayout.NORTH);
        contentPanel.revalidate();
        contentPanel.repaint();
    }
}