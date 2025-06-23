import javax.swing.*;

public class FrameFactory {
    public static void openFrame(String type, Formula1DAO db) {
        JFrame frame = switch (type) {
            case "Piloti" -> new PilotFrame(db);
            case "Circuite" -> new CircuitFrame(db);
            case "Curse" -> new RaceFrame(db);
            case "Rezultate" -> new ResultFrame(db);
            case "Echipe" -> new TeamFrame(db);
            default -> null;
        };
        if (frame != null) {
            frame.setVisible(true);
        }
    }
}
