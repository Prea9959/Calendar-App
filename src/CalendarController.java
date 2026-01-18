import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

public class CalendarController {
    
    // State Management
    public enum ViewMode { CALENDAR, LIST }
    public enum TimeScale { DAY, WEEK, MONTH }

    private ViewMode currentMode = ViewMode.CALENDAR;
    private TimeScale currentScale = TimeScale.MONTH;
    private LocalDate referenceDate = LocalDate.now();
    
    private List<Event> events = new ArrayList<>();
    private FileHandler fileHandler = new FileHandler();

    public CalendarController() {
        try {
            events = fileHandler.loadEvents();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // --- View State Getters/Setters ---
    public ViewMode getMode() { return currentMode; }
    public void setMode(ViewMode mode) { this.currentMode = mode; }
    
    public TimeScale getScale() { return currentScale; }
    public void setScale(TimeScale scale) { this.currentScale = scale; }
    
    public LocalDate getReferenceDate() { return referenceDate; }
    
    public void setReferenceDate(LocalDate date) { 
        this.referenceDate = date; 
    }
    
    public void setReferenceDateFromSelectors(int monthIndex, String yearStr) {
        int month = monthIndex + 1;
        int year = Integer.parseInt(yearStr);
        this.referenceDate = LocalDate.of(year, month, 1);
    }

    // --- Navigation Logic ---
    public void navigate(int direction) {
        switch (currentScale) {
            case DAY: referenceDate = referenceDate.plusDays(direction); break;
            case WEEK: referenceDate = referenceDate.plusWeeks(direction); break;
            case MONTH: referenceDate = referenceDate.plusMonths(direction); break;
        }
    }

    // --- Core Data Logic ---
    public List<Event> getEventsOnDate(LocalDate date) {
        return events.stream().filter(e -> e.occursOn(date)).collect(Collectors.toList());
    }

    private void addOrUpdateEvent(Event event) {
        events.removeIf(e -> e.getId() == event.getId());
        events.add(event);
        save();
    }

    public void deleteEvent(Event event) {
        events.remove(event);
        save();
    }

    public int getNextEventId() {
        return events.stream().mapToInt(Event::getId).max().orElse(0) + 1;
    }

    private void save() {
        try { 
            fileHandler.saveEvents(events); 
        } catch (Exception e) { 
            e.printStackTrace(); 
        }
    }

    // --- INPUT PROCESSING (New Logic Moved from View) ---
    
    /**
     * Handles the logic for creating or updating an event from raw UI strings.
     * Parses times, validates constraints, and updates the model.
     */
    public void processEventUpsert(Event existingEvent, LocalDate targetDate, 
                                   String title, String desc, 
                                   String startStr, String endStr, 
                                   String recurType, String recurCountStr) throws IllegalArgumentException {
        
        try {
            LocalTime startTime = LocalTime.parse(startStr);
            LocalTime endTime = LocalTime.parse(endStr);
            LocalDateTime startDT = LocalDateTime.of(targetDate, startTime);
            LocalDateTime endDT = LocalDateTime.of(targetDate, endTime);

            // Business Logic Validation
            if (endDT.isBefore(startDT)) {
                throw new IllegalArgumentException("End time must be after start time!");
            }

            int id = (existingEvent != null) ? existingEvent.getId() : getNextEventId();
            int count = Integer.parseInt(recurCountStr);

            Event newEvent = new Event(id, title, desc, startDT, endDT, recurType, count);
            addOrUpdateEvent(newEvent);
            
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid time format. Please use HH:mm (e.g., 14:30)");
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Recurrence count must be a number.");
        }
    }

    /**
     * Handles logic for parsing date strings and running a search.
     */
    public List<Event> processDateRangeSearch(String startStr, String endStr) throws IllegalArgumentException {
        try {
            LocalDate start = LocalDate.parse(startStr);
            LocalDate end = LocalDate.parse(endStr);
            return searchEventsByDate(start, end);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid date format. Use YYYY-MM-DD.");
        }
    }

    // --- REPORT GENERATION (Formatted Strings for View) ---

    public String getNotificationSummary() {
        List<Event> upcoming = getUpcomingEvents(24);
        
        if (upcoming.isEmpty()) {
            return "No upcoming events in the next 24 hours.";
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
            sb.append("  In: ").append(hoursUntil).append("h ")
              .append(minutesUntil).append("m\n\n");
        }
        return sb.toString();
    }

    public String getSearchResultsSummary(List<Event> results) {
        if (results.isEmpty()) return "No events found.";
        
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
        return sb.toString();
    }

    // --- Range Calculations ---
    public LocalDate getStartOfRange() {
        switch (currentScale) {
            case DAY: return referenceDate;
            case WEEK: return referenceDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));
            default: return referenceDate.withDayOfMonth(1);
        }
    }

    public LocalDate getEndOfRange() {
        switch (currentScale) {
            case DAY: return referenceDate;
            case WEEK: return getStartOfRange().plusDays(6);
            default: return referenceDate.with(TemporalAdjusters.lastDayOfMonth());
        }
    }

    // --- Filter Logic ---
    public List<Event> getEventsInRange() {
        LocalDate start = getStartOfRange();
        LocalDate end = getEndOfRange();
        return events.stream()
                .filter(e -> occursInRange(e, start, end))
                .sorted(Comparator.comparing(Event::getStart))
                .collect(Collectors.toList());
    }

    private boolean occursInRange(Event e, LocalDate start, LocalDate end) {
        if (!e.getStart().toLocalDate().isBefore(start) && 
            !e.getStart().toLocalDate().isAfter(end)) {
            return true;
        }
        if (!"NONE".equalsIgnoreCase(e.getRecurType())) {
            for (int i = 1; i <= e.getRecurCount(); i++) {
                LocalDate occurrence = e.getOccurrence(i).toLocalDate();
                if (!occurrence.isBefore(start) && !occurrence.isAfter(end)) {
                    return true;
                }
            }
        }
        return false;
    }

    // --- Search Logic ---
    public List<Event> searchEvents(String query) {
        if (query == null || query.trim().isEmpty()) return new ArrayList<>();
        return events.stream()
                .filter(ev -> ev.getTitle().toLowerCase().contains(query.toLowerCase()) ||
                ev.getDescription().toLowerCase().contains(query.toLowerCase()))
                .collect(Collectors.toList());
    }

    public List<Event> searchEventsByDate(LocalDate start, LocalDate end) {
        return events.stream()
                .filter(e -> occursInRange(e, start, end))
                .sorted(Comparator.comparing(Event::getStart))
                .collect(Collectors.toList());
    }

    // --- Conflict Logic ---
    public boolean hasConflict(Event e) {
        List<Event> sameDay = getEventsOnDate(e.getStart().toLocalDate());
        return checkForConflictOnDate(sameDay);
    }

    public boolean checkForConflictOnDate(List<Event> dayEvents) {
        for (int i = 0; i < dayEvents.size(); i++) {
            for (int j = i + 1; j < dayEvents.size(); j++) {
                Event a = dayEvents.get(i);
                Event b = dayEvents.get(j);
                if (a.getStart().isBefore(b.getEnd()) && a.getEnd().isAfter(b.getStart())) {
                    return true;
                }
            }
        }
        return false;
    }

    // --- Backup/Restore ---
    public void performBackup(String filename) throws Exception {
        fileHandler.backup(filename);
    }

    public void performRestore(String filename, boolean append) throws Exception {
        fileHandler.restore(filename, append);
        events = fileHandler.loadEvents();
    }
    
    // --- Notification Logic Helper ---
    private List<Event> getUpcomingEvents(int hoursAhead) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime future = now.plusHours(hoursAhead);
        
        return events.stream()
                .filter(e -> {
                    if (e.getStart().isAfter(now) && e.getStart().isBefore(future)) return true;
                    if (!"NONE".equalsIgnoreCase(e.getRecurType())) {
                        for (int i = 1; i <= e.getRecurCount(); i++) {
                            LocalDateTime occurrence = e.getOccurrence(i);
                            if (occurrence.isAfter(now) && occurrence.isBefore(future)) return true;
                        }
                    }
                    return false;
                })
                .sorted(Comparator.comparing(Event::getStart))
                .collect(Collectors.toList());
    }
    
    // --- Week List View Generator ---
    public String generateWeekListView(LocalDate weekStart) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Week of ").append(weekStart).append(" ===\n\n");
        String[] dayNames = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
        
        for (int i = 0; i < 7; i++) {
            LocalDate day = weekStart.plusDays(i);
            List<Event> dayEvents = getEventsOnDate(day);
            sb.append(dayNames[i]).append(" ").append(day).append(": ");
            
            if (dayEvents.isEmpty()) {
                sb.append("No events\n");
            } else {
                sb.append("\n");
                for (Event e : dayEvents) {
                    sb.append("  • ").append(e.getStart().toLocalTime())
                      .append(" - ").append(e.getTitle());
                    if (!"NONE".equals(e.getRecurType())) {
                        sb.append(" [").append(e.getRecurType()).append("]");
                    }
                    sb.append("\n");
                }
            }
        }
        return sb.toString();
    }
}