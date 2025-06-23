import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            LoginFrame login = new LoginFrame();
            login.setLocationRelativeTo(null);
            login.setVisible(true);
        });
    }
}