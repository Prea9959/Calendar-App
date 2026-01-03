import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

public class FileHandler {
    private final String DATA_DIR = "data";
    private final String EVENT_FILE = "data/event.csv";
    private final String RECUR_FILE = "data/recurrent.csv";

    public FileHandler() {
        new File(DATA_DIR).mkdirs();
    }

    public void saveEvents(List<Event> events) throws IOException {
        try (PrintWriter pw = new PrintWriter(new FileWriter(EVENT_FILE))) {
            pw.println("eventId,title,description,startDateTime,endDateTime");
            for (Event e : events) pw.println(e.toCSV());
        }
    }

    public List<Event> loadEvents() throws IOException {
        List<Event> list = new ArrayList<>();
        File file = new File(EVENT_FILE);
        if (!file.exists()) return list;
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            br.readLine(); // skip header
            String line;
            while ((line = br.readLine()) != null) list.add(Event.fromCSV(line));
        }
        return list;
    }

    public void backup(String dest) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(dest))) {
            addToZip(EVENT_FILE, zos);
            addToZip(RECUR_FILE, zos);
        }
    }

    private void addToZip(String path, ZipOutputStream zos) throws IOException {
        File file = new File(path);
        if (!file.exists()) return;
        zos.putNextEntry(new ZipEntry(file.getName()));
        Files.copy(file.toPath(), zos);
        zos.closeEntry();
    }
}