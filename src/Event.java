import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Event {
    private int id;
    private String title;
    private String description;
    private LocalDateTime start;
    private LocalDateTime end;

    // This is the specific 24-hour format required for your CSV file (e.g., 2025-10-05T14:30:00)
    public static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    public Event(int id, String title, String description, LocalDateTime start, LocalDateTime end) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.start = start;
        this.end = end;
    }

    // This converts the Event object into a single line of text for the event.csv file
    public String toCSV() {
        return id + "," + title + "," + description + "," + start.format(FMT) + "," + end.format(FMT);
    }

    // This takes a line from the event.csv and turns it back into a Java Object
    public static Event fromCSV(String line) {
        String[] p = line.split(",");
        if (p.length < 5) return null;
        return new Event(
            Integer.parseInt(p[0]), 
            p[1], 
            p[2], 
            LocalDateTime.parse(p[3], FMT), 
            LocalDateTime.parse(p[4], FMT)
        );
    }

    // Getters: These allow the LaunchPage to "read" the data inside the event
    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public LocalDateTime getStart() { return start; }
    public LocalDateTime getEnd() { return end; }
}