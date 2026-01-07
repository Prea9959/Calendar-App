import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

public class FileHandler {
    private final String DATA_DIR = "data";
    private final String EVENT_FILE = "data/event.csv";

    public FileHandler() {
        new File(DATA_DIR).mkdirs();
    }

    public void saveEvents(List<Event> events) throws IOException {
        try (PrintWriter pw = new PrintWriter(new FileWriter(EVENT_FILE))) {
            pw.println("eventId|title|description|start|end|recurType|recurCount");
            for (Event e : events) pw.println(e.toCSV());
        }
    }

    public List<Event> loadEvents() throws IOException {
        List<Event> list = new ArrayList<>();
        File file = new File(EVENT_FILE);
        if (!file.exists()) return list;
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            br.readLine(); 
            String line;
            while ((line = br.readLine()) != null) {
                Event e = Event.fromCSV(line);
                if (e != null) list.add(e);
            }
        }
        return list;
    }

    public void backup(String dest) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(dest))) {
            addToZip(EVENT_FILE, zos);
        }
    }

    private void addToZip(String path, ZipOutputStream zos) throws IOException {
        File file = new File(path);
        if (!file.exists()) return;
        zos.putNextEntry(new ZipEntry(file.getName()));
        Files.copy(file.toPath(), zos);
        zos.closeEntry();
    }

    public void restore(String zipPath) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipPath))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path destPath = Paths.get(DATA_DIR, entry.getName());
                Files.copy(zis, destPath, StandardCopyOption.REPLACE_EXISTING);
                zis.closeEntry();
            }
        }
    }
}