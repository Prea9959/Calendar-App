import javax.swing.UIManager;

public class Main {
    public static void main(String[] args) {
    try {
        UIManager.put("Button.focus", new java.awt.Color(0, 0, 0, 0));
    } catch (Exception e) {
        e.printStackTrace();
    }
        new LaunchPage();
    }
}
