import javax.swing.*;
import java.awt.*;

class RoundedTextField extends JTextField {
    public RoundedTextField(int columns) {
        super(columns);
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
        setForeground(Color.WHITE);
        setCaretColor(Color.WHITE);
        setFont(new Font("SansSerif", Font.PLAIN, 12));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(new Color(50, 50, 50, 120));
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
        super.paintComponent(g);
        g2.dispose();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(150, 25);  // înălțimea mai realistă
    }

    @Override
    public void setBackground(Color bg) {
        super.setBackground(new Color(bg.getRed(), bg.getGreen(), bg.getBlue(), 120));
    }
}

class RoundedPasswordField extends JPasswordField {
    public RoundedPasswordField(int columns) {
        super(columns);
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
        setForeground(Color.WHITE);
        setCaretColor(Color.WHITE);
        setFont(new Font("SansSerif", Font.PLAIN, 12));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(new Color(50, 50, 50, 120));
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
        super.paintComponent(g);
        g2.dispose();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(150, 25);
    }

    @Override
    public void setBackground(Color bg) {
        super.setBackground(new Color(bg.getRed(), bg.getGreen(), bg.getBlue(), 120));
    }
}