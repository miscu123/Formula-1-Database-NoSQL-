import javax.swing.*;
import java.awt.*;
import java.awt.image.*;
import javax.imageio.ImageIO;
import java.io.IOException;
import java.util.Objects;

import com.formdev.flatlaf.FlatDarkLaf;
import com.mongodb.client.MongoCollection;
import org.bson.Document;

public class LoginFrame extends JFrame {
    private JTextField txtUser;
    private JPasswordField txtPass;
    private JButton btnLogin;
    private JButton btnRegister;
    private Image backgroundImage;

    public LoginFrame() {
        super("Login");

        try {
            UIManager.setLookAndFeel(new FlatDarkLaf());
        } catch (Exception e) {
            System.err.println("Nu s-a putut seta FlatDarkLaf.");
        }

        setUndecorated(true);
        setOpacity(0f); // fade-in
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(551, 310);
        setLocationRelativeTo(null);

        // Load background image
        backgroundImage = new ImageIcon(Objects.requireNonNull(getClass().getResource("/logo.jpg"))).getImage();

        JPanel backgroundPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);

                int panelWidth = getWidth();
                int panelHeight = getHeight();

                int imgWidth = backgroundImage.getWidth(this);
                int imgHeight = backgroundImage.getHeight(this);

                if (imgWidth > 0 && imgHeight > 0) {
                    double scaleX = (double) panelWidth / imgWidth;
                    double scaleY = (double) panelHeight / imgHeight;
                    double scale = Math.min(scaleX, scaleY);

                    int finalWidth = (int) (imgWidth * scale);
                    int finalHeight = (int) (imgHeight * scale);

                    int x = (panelWidth - finalWidth) / 2;
                    int y = (panelHeight - finalHeight) / 2;

                    g.drawImage(backgroundImage, x, y, finalWidth, finalHeight, this);
                }
            }
        };
        backgroundPanel.setLayout(new BoxLayout(backgroundPanel, BoxLayout.Y_AXIS));
        backgroundPanel.setBorder(BorderFactory.createEmptyBorder(20, 40, 20, 40));
        backgroundPanel.setOpaque(false);

        txtUser = new RoundedTextField(15);
        txtPass = new RoundedPasswordField(15);

        txtUser.setBackground(new Color(50, 50, 50));
        txtUser.setForeground(Color.RED);
        txtPass.setBackground(new Color(50, 50, 50));
        txtPass.setForeground(Color.RED);

        btnLogin = createStyledButton("Login");
        btnRegister = createStyledButton("Register");

        JLabel title = new JLabel("Autentificare");
        title.setFont(new Font("SansSerif", Font.BOLD, 22));
        title.setForeground(Color.RED);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        title.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));

        backgroundPanel.add(title);
        backgroundPanel.add(createLabeledField("Username:", txtUser));
        backgroundPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        backgroundPanel.add(createLabeledField("Password:", txtPass));
        backgroundPanel.add(Box.createRigidArea(new Dimension(0, 20)));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        buttonPanel.setOpaque(false);
        buttonPanel.add(btnLogin);
        buttonPanel.add(btnRegister);
        backgroundPanel.add(buttonPanel);

        add(backgroundPanel);

        btnLogin.addActionListener(e -> doLogin());
        btnRegister.addActionListener(e -> new RegisterFrame().setVisible(true));

        fadeInWindow();
    }

    private JPanel createLabeledField(String label, JComponent field) {
        JPanel p = new JPanel(new BorderLayout(5, 5));
        p.setOpaque(false);
        JLabel lbl = new JLabel(label);
        lbl.setForeground(Color.WHITE);
        p.add(lbl, BorderLayout.WEST);
        p.add(field, BorderLayout.CENTER);
        return p;
    }

    private JButton createStyledButton(String text) {
        JButton btn = new JButton(text);
        btn.setBackground(new Color(30, 30, 30));
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setFont(new Font("SansSerif", Font.PLAIN, 14));
        return btn;
    }

    private void fadeInWindow() {
        setVisible(true);
        Timer timer = new Timer(30, null);
        timer.addActionListener(e -> {
            float opacity = getOpacity();
            if (opacity < 1f) {
                setOpacity(Math.min(opacity + 0.05f, 1f));
            } else {
                timer.stop();
            }
        });
        timer.start();
    }

    private void doLogin() {
        String username = txtUser.getText().trim();
        String password = new String(txtPass.getPassword()).trim();

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Completează toate câmpurile.");
            return;
        }

        try {
            Formula1DAO db = new Formula1DAO();
            MongoCollection<Document> users = db.getUtilizatoriCollection();

            String passwordHash = sha256(password);

            Document query = new Document("username", username)
                    .append("passwordHash", passwordHash);

            Document user = users.find(query).first();

            if (user != null) {
                JOptionPane.showMessageDialog(this, "Login reușit!");
                new ChoiceFrame(db).setVisible(true);
                this.dispose();
            } else {
                JOptionPane.showMessageDialog(this, "Credentiale incorecte");
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Eroare la conectare cu baza de date.");
        }
    }

    private String sha256(String base) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(base.getBytes("UTF-8"));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
