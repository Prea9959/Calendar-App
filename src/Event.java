import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Event {
    private int id;
    private String title;
    private String description;
    private LocalDateTime start;
    private LocalDateTime end;
    
    // New fields for recurrence
    private String recurType; // "NONE", "DAILY", "WEEKLY", "MONTHLY"
    private int recurCount;   // How many times to repeat

    public static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    public Event(int id, String title, String description, LocalDateTime start, LocalDateTime end, String recurType, int recurCount) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.start = start;
        this.end = end;
        this.recurType = recurType;
        this.recurCount = recurCount;
    }

    public String toCSV() {
        return id + "," + title + "," + description + "," + start.format(FMT) + "," + end.format(FMT) + "," + recurType + "," + recurCount;
    }

    public static Event fromCSV(String line) {
        String[] p = line.split(",");
        if (p.length < 7) return null;
        return new Event(
            Integer.parseInt(p[0]), p[1], p[2], 
            LocalDateTime.parse(p[3], FMT), LocalDateTime.parse(p[4], FMT),
            p[5], Integer.parseInt(p[6])
        );
    }

    // Logic to check if this event occurs on a specific date
    public boolean occursOn(java.time.LocalDate date) {
        for (int i = 0; i < recurCount; i++) {
            LocalDateTime occurrenceStart = getOccurrence(i);
            if (occurrenceStart.toLocalDate().equals(date)) return true;
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

    // Getters and Setters
    public int getId() { return id; }
    public String getTitle() { return title; }
    public LocalDateTime getStart() { return start; }
    public LocalDateTime getEnd() { return end; }
    public String getRecurType() { return recurType; }
    public int getRecurCount() { return recurCount; }
    public void setTitle(String t) { this.title = t; }
    public void setStart(LocalDateTime s) { this.start = s; }
    public void setEnd(LocalDateTime e) { this.end = e; }
}