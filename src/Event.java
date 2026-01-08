import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Event {
    private int id;
    private String title;
    private String description;
    private LocalDateTime start;
    private LocalDateTime end;
    
    // NEW FIELDS
    private String recurType; // "NONE", "DAILY", "WEEKLY", "MONTHLY"
    private int recurCount;   // How many times it repeats

    // Constructor
    public Event(int id, String title, String description, LocalDateTime start, LocalDateTime end, String recurType, int recurCount) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.start = start;
        this.end = end;
        this.recurType = recurType;
        this.recurCount = recurCount;
    }

    // Simplified Constructor (for non-recurring events)
    public Event(int id, String title, String description, LocalDateTime start, LocalDateTime end) {
        this(id, title, description, start, end, "NONE", 0);
    }

    // --- GETTERS ---
    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public LocalDateTime getStart() { return start; }
    public LocalDateTime getEnd() { return end; }
    public String getRecurType() { return recurType; }
    public int getRecurCount() { return recurCount; }

    // --- SETTERS (Required to fix "Method is undefined") ---
    public void setTitle(String title) { this.title = title; }
    public void setStart(LocalDateTime start) { this.start = start; }
    public void setEnd(LocalDateTime end) { this.end = end; }
    
    // These are the specific ones you were missing:
    public void setRecurType(String recurType) { this.recurType = recurType; }
    public void setRecurCount(int recurCount) { this.recurCount = recurCount; }

    // --- LOGIC ---
    public boolean occursOn(java.time.LocalDate date) {
        // Check original date
        if (start.toLocalDate().equals(date)) return true;
        
        // Check recurrences
        if (!"NONE".equalsIgnoreCase(recurType)) {
            for (int i = 1; i <= recurCount; i++) {
                LocalDateTime next = getOccurrence(i);
                if (next.toLocalDate().equals(date)) return true;
            }
        }
        return false;
    }

    public LocalDateTime getOccurrence(int index) {
        switch (recurType) {
            case "DAILY": return start.plusDays(index);
            case "WEEKLY": return start.plusWeeks(index);
            case "MONTHLY": return start.plusMonths(index);
            default: return start;
        }
    }

    // CSV Parsing Helper
    public static Event fromCSV(String csvLine) {
        String[] parts = csvLine.split(",");
        int id = Integer.parseInt(parts[0]);
        String title = parts[1];
        String desc = parts[2];
        LocalDateTime start = LocalDateTime.parse(parts[3]);
        LocalDateTime end = LocalDateTime.parse(parts[4]);
        
        // Default to NONE if old CSV format
        return new Event(id, title, desc, start, end, "NONE", 0);
    }
}