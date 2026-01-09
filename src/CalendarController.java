import java.time.*;
import java.time.temporal.TemporalAdjusters;
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
            events = fileHandler.loadEvents(); // [cite: 24]
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // --- State Getters/Setters ---
    public ViewMode getMode() { return currentMode; }
    public void setMode(ViewMode mode) { this.currentMode = mode; }
    
    public TimeScale getScale() { return currentScale; }
    public void setScale(TimeScale scale) { this.currentScale = scale; }
    
    public LocalDate getReferenceDate() { return referenceDate; }

    // --- Date Navigation Logic ---
    public void navigate(int direction) {
        switch (currentScale) {
            case DAY: referenceDate = referenceDate.plusDays(direction); break;
            case WEEK: referenceDate = referenceDate.plusWeeks(direction); break;
            case MONTH: referenceDate = referenceDate.plusMonths(direction); break;
        }
    }

    // --- Event Logic ---
    public List<Event> getEventsOnDate(LocalDate date) {
        return events.stream().filter(e -> e.occursOn(date)).collect(Collectors.toList());
    }

    public void addOrUpdateEvent(Event event) {
        // If event exists (by ID), remove it first, then add new version
        events.removeIf(e -> e.getId() == event.getId());
        events.add(event);
        save();
    }

    public void deleteEvent(Event event) {
        events.remove(event);
        save();
    }

    public int getNextEventId() {
        return events.stream().mapToInt(Event::getId).max().orElse(0) + 1; // [cite: 17]
    }

    private void save() {
        try { fileHandler.saveEvents(events); } catch (Exception e) { e.printStackTrace(); }
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

    // --- Filter Logic for List View ---
    public List<Event> getEventsInRange() {
        LocalDate start = getStartOfRange();
        LocalDate end = getEndOfRange();
        return events.stream()
                .filter(e -> occursInRange(e, start, end))
                .sorted(Comparator.comparing(Event::getStart))
                .collect(Collectors.toList());
    }

    private boolean occursInRange(Event e, LocalDate start, LocalDate end) {
    // Check the original occurrence (index 0)
    if (!e.getStart().toLocalDate().isBefore(start) && !e.getStart().toLocalDate().isAfter(end)) {
        return true;
    }
    
    // Check all future recurrences
    for (int i = 1; i <= e.getRecurCount(); i++) {
        LocalDate occurrence = e.getOccurrence(i).toLocalDate();
        if (!occurrence.isBefore(start) && !occurrence.isAfter(end)) {
            return true;
        }
    }
    return false;
    }

    // --- Search Logic [cite: 26] ---
    public List<Event> searchEvents(String query) {
        if (query == null || query.trim().isEmpty()) return new ArrayList<>();
        return events.stream()
                .filter(ev -> ev.getTitle().toLowerCase().contains(query.toLowerCase()))
                .collect(Collectors.toList());
    }

    // --- Conflict Logic [cite: 32] ---
    public boolean hasConflict(Event e) {
        List<Event> sameDay = getEventsOnDate(e.getStart().toLocalDate());
        return checkForConflictOnDate(sameDay);
    }

    public boolean checkForConflictOnDate(List<Event> dayEvents) {
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

    // --- Backup/Restore ---
    public void performBackup(String filename) throws Exception {
        fileHandler.backup(filename); // [cite: 25]
    }

    public void performRestore(String filename) throws Exception {
        fileHandler.restore(filename); // [cite: 25]
        events = fileHandler.loadEvents();
    }

    // --- Advanced Search Logic ---
    public List<Event> searchEventsByDate(LocalDate start, LocalDate end) {
        return events.stream()
                .filter(e -> occursInRange(e, start, end))
                .sorted(Comparator.comparing(Event::getStart))
                .collect(Collectors.toList());
    }
}