import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import com.mongodb.client.MongoCollection;
import org.bson.Document;
import static com.mongodb.client.model.Filters.eq;
import com.formdev.flatlaf.FlatDarkLaf;

public class RegisterFrame extends JFrame {
    private JTextField txtUser;
    private JPasswordField txtPass;
    private JButton btnRegister;

    public RegisterFrame() {
        super("Înregistrare");

        try {
            UIManager.setLookAndFeel(new FlatDarkLaf());
        } catch (Exception e) {
            System.err.println("Nu s-a putut seta FlatDarkLaf.");
        }

        setUndecorated(true);
        setOpacity(0f); // pentru fade-in
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(350, 200);
        setLocationRelativeTo(null);
        setResizable(false);

        initUI();
        initEvents();
        fadeIn();
    }

    private void initUI() {
        txtUser = new RoundedTextField(15);
        txtPass = new RoundedPasswordField(15);
        txtUser.setBackground(new Color(50, 50, 50));
        txtUser.setForeground(Color.WHITE);
        txtPass.setBackground(new Color(50, 50, 50));
        txtPass.setForeground(Color.WHITE);
        btnRegister = createStyledButton("Creează cont");

        JLabel lblUser = new JLabel("Utilizator:");
        JLabel lblPass = new JLabel("Parolă:");

        Font font = new Font("SansSerif", Font.PLAIN, 14);
        lblUser.setFont(font);
        lblPass.setFont(font);
        lblUser.setForeground(Color.WHITE);
        lblPass.setForeground(Color.WHITE);

        txtUser.setFont(font);
        txtPass.setFont(font);

        JPanel formPanel = new JPanel(new GridLayout(2, 2, 10, 10));
        formPanel.setOpaque(false);
        formPanel.add(lblUser);
        formPanel.add(txtUser);
        formPanel.add(lblPass);
        formPanel.add(txtPass);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        buttonPanel.setOpaque(false);
        buttonPanel.add(btnRegister);

        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BorderLayout(10, 10));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        contentPanel.setBackground(Color.BLACK);
        contentPanel.add(formPanel, BorderLayout.CENTER);
        contentPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(contentPanel);
    }

    private JButton createStyledButton(String text) {
        JButton btn = new JButton(text);
        btn.setBackground(new Color(30, 30, 30));
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setFont(new Font("SansSerif", Font.PLAIN, 14));
        return btn;
    }

    private void initEvents() {
        btnRegister.addActionListener(e -> doRegister());

        txtPass.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent ke) {
                if (ke.getKeyCode() == KeyEvent.VK_ENTER) {
                    doRegister();
                }
            }
        });
    }

    private void doRegister() {
        String username = txtUser.getText().trim();
        String password = new String(txtPass.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Completează toate câmpurile.");
            return;
        }

        try {
            Formula1DAO db = new Formula1DAO();
            MongoCollection<Document> col = db.getUtilizatoriCollection();

            Document existingUser = col.find(eq("username", username)).first();

            if (existingUser != null) {
                JOptionPane.showMessageDialog(this, "Acest username există deja.");
                return;
            }

            String passwordHash = sha256(password);

            Document userDoc = new Document("username", username)
                    .append("passwordHash", passwordHash);

            col.insertOne(userDoc);

            JOptionPane.showMessageDialog(this, "Cont creat cu succes!");
            this.dispose();
            db.close();

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Eroare la înregistrare.");
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

    private void fadeIn() {
        Timer timer = new Timer(30, null);
        timer.addActionListener(new ActionListener() {
            float opacity = 0f;

            public void actionPerformed(ActionEvent e) {
                opacity += 0.05f;
                if (opacity >= 1f) {
                    opacity = 1f;
                    timer.stop();
                }
                setOpacity(opacity);
            }
        });
        timer.start();
    }
}
