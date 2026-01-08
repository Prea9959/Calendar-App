import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

public class FileHandler {
    private final String DATA_DIR = "data";
    private final String EVENT_FILE = "data/event.csv";
    private final String RECUR_FILE = "data/recurrent.csv"; // Added this file

    public FileHandler() {
        File dir = new File(DATA_DIR);
        if (!dir.exists()) dir.mkdir();
    }

    public void saveEvents(List<Event> events) throws IOException {
        // 1. Save Basic Data to event.csv
        try (PrintWriter pw = new PrintWriter(new FileWriter(EVENT_FILE))) {
            // Header required by PDF format implies standard CSV
            for (Event e : events) {
                // PDF Format: eventId, title, description, startDateTime, endDateTime
                pw.printf("%d,%s,%s,%s,%s%n",
                    e.getId(),
                    escapeCSV(e.getTitle()),
                    escapeCSV(e.getDescription()),
                    e.getStart(),
                    e.getEnd()
                );
            }
        }

        // 2. Save Recurring Data to recurrent.csv
        try (PrintWriter pw = new PrintWriter(new FileWriter(RECUR_FILE))) {
            for (Event e : events) {
                // Only save if it is actually recurring
                if (!"NONE".equalsIgnoreCase(e.getRecurType())) {
                    // PDF Format: eventId, recurrentInterval, recurrentTimes, recurrentEndDate
                    // Converting "DAILY" -> "1d" to match PDF requirement
                    String shortCode = convertToPdfFormat(e.getRecurType());
                    
                    pw.printf("%d,%s,%d,%s%n",
                        e.getId(),
                        shortCode,
                        e.getRecurCount(),
                        "0" // PDF says put 0 for recurrentEndDate if not used
                    );
                }
            }
        }
    }

    public List<Event> loadEvents() throws IOException {
    List<Event> list = new ArrayList<>();
    File eFile = new File(EVENT_FILE);
    File rFile = new File(RECUR_FILE);

    if (!eFile.exists()) return list;

    // 1. Read Basic Events
    try (BufferedReader br = new BufferedReader(new FileReader(eFile))) {
        String line;
        while ((line = br.readLine()) != null) {
            Event e = Event.fromCSV(line);
            if (e != null) list.add(e);
        }
    }

    // 2. Read Recurring Data and MERGE
    if (rFile.exists()) {
        try (BufferedReader br = new BufferedReader(new FileReader(rFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 3) {
                    int id = Integer.parseInt(parts[0]);
                    String interval = parts[1]; // e.g., "1d"
                    int count = Integer.parseInt(parts[2]);

                    for (Event e : list) {
                        if (e.getId() == id) {
                            // This is the critical link!
                            e.setRecurType(convertFromPdfFormat(interval));
                            e.setRecurCount(count);
                        }
                    }
                }
            }
        }
    }
    return list;
}

    // --- ZIP BACKUP (Must include BOTH files) ---
    public void backup(String dest) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(dest))) {
            addToZip(EVENT_FILE, zos);
            addToZip(RECUR_FILE, zos); // Now backing up both
        }
    }

    private void addToZip(String path, ZipOutputStream zos) throws IOException {
        File file = new File(path);
        if (!file.exists()) return;
        
        // We store it simply as "event.csv" inside the zip, not "data/event.csv"
        ZipEntry entry = new ZipEntry(file.getName());
        zos.putNextEntry(entry);
        Files.copy(file.toPath(), zos);
        zos.closeEntry();
    }

    public void restore(String zipPath) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipPath))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                // Extract directly into data/ folder
                Path destPath = Paths.get(DATA_DIR, entry.getName());
                Files.copy(zis, destPath, StandardCopyOption.REPLACE_EXISTING);
                zis.closeEntry();
            }
        }
    }

    // --- Helpers for PDF Requirements ---
    private String escapeCSV(String data) {
        if (data == null) return "";
        return data.replace(",", " "); // Replace commas with spaces to prevent CSV breakage
    }

    private String convertToPdfFormat(String uiType) {
        switch (uiType) {
            case "DAILY": return "1d";
            case "WEEKLY": return "1w";
            case "MONTHLY": return "1m";
            default: return "0";
        }
    }

    private String convertFromPdfFormat(String pdfType) {
        if (pdfType.contains("d")) return "DAILY";
        if (pdfType.contains("w")) return "WEEKLY";
        if (pdfType.contains("m")) return "MONTHLY";
        return "NONE";
    }
}