import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import com.mongodb.client.MongoCollection;
import org.bson.Document;
import static com.mongodb.client.model.Filters.eq;
import com.formdev.flatlaf.FlatDarkLaf;
import java.util.Objects;

public class RegisterFrame extends JFrame {
    private JTextField txtUser;
    private JPasswordField txtPass;
    private JButton btnRegister;
    private Image backgroundImage;

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
        setSize(474, 315);
        setLocationRelativeTo(null);
        setResizable(false);

        // încarcă imaginea de fundal
        backgroundImage = new ImageIcon(Objects.requireNonNull(getClass().getResource("/login.jpg"))).getImage();

        initUI();
        initEvents();
        fadeIn();
    }

    private void initUI() {
        txtUser = new RoundedTextField(15);
        txtPass = new RoundedPasswordField(15);
        txtUser.setBackground(new Color(50, 50, 50));
        txtUser.setForeground(Color.RED);
        txtPass.setBackground(new Color(50, 50, 50));
        txtPass.setForeground(Color.RED);
        btnRegister = createStyledButton("Creează cont");

        JLabel lblUser = new JLabel("Username:");
        JLabel lblPass = new JLabel("Password:");

        Font font = new Font("SansSerif", Font.BOLD, 20);
        lblUser.setFont(font);
        lblPass.setFont(font);
        lblUser.setForeground(Color.RED);
        lblPass.setForeground(Color.RED);

        txtUser.setFont(font);
        txtPass.setFont(font);

        JPanel formPanel = new JPanel(new GridLayout(2, 2, 5, 5));
        formPanel.setOpaque(false);
        formPanel.add(lblUser);
        formPanel.add(txtUser);
        formPanel.add(lblPass);
        formPanel.add(txtPass);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 5));
        buttonPanel.setOpaque(false);
        buttonPanel.add(btnRegister);

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
        backgroundPanel.setLayout(new BorderLayout(10, 10));
        backgroundPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        backgroundPanel.add(formPanel, BorderLayout.CENTER);
        backgroundPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(backgroundPanel);
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
