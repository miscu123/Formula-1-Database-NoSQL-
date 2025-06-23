import javax.swing.*;
import java.awt.*;
import java.util.Objects;
import com.formdev.flatlaf.FlatDarkLaf;

public class ChoiceFrame extends JFrame {
    private final Formula1DAO db;
    private Image backgroundImage;

    public ChoiceFrame(Formula1DAO db) {
        super("Formula 1 - Dashboard");
        this.db = db;

        try {
            UIManager.setLookAndFeel(new FlatDarkLaf());
        } catch (Exception ex) {
            System.err.println("Nu s-a putut seta FlatLaf");
        }

        // Încarcă imaginea din resurse (src folder)
        backgroundImage = new ImageIcon(Objects.requireNonNull(getClass().getResource("/good.jpg"))).getImage();

        setUndecorated(true);
        setSize(600, 360);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setOpacity(0f);

        // Panel personalizat cu fundal imagine
        JPanel mainPanel = new JPanel() {
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
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 40, 20, 40));
        mainPanel.setOpaque(false);  // permite să vedem imaginea de fundal

        JLabel title = new JLabel("Alege o categorie");
        title.setFont(new Font("SansSerif", Font.BOLD, 22));
        title.setForeground(Color.RED);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        title.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));
        mainPanel.add(title);

        String[] labels = {"Piloti", "Circuite", "Curse", "Rezultate", "Echipe"};
        for (String label : labels) {
            JButton button = createStyledButton(label);
            button.addActionListener(e -> FrameFactory.openFrame(label, db));
            mainPanel.add(button);
            mainPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        }

        add(mainPanel);

        fadeInWindow();
    }

    private JButton createStyledButton(String text) {
        JButton btn = new RoundedButton(text);
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.setMaximumSize(new Dimension(200, 40));
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
}
