package es.upm.interfaz;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class ImageSectionPanel extends JPanel {

    private JLabel imageLabel;
    private JLabel textLabel;

    private Image originalImage;

    public ImageSectionPanel(String imagePath, String text) {
        setLayout();
        initComponents();
        loadContent(imagePath, text);
    }

    private void setLayout() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(Color.WHITE);
        setAlignmentX(Component.CENTER_ALIGNMENT);
    }

    private void initComponents() {
        imageLabel = new JLabel();
        imageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        textLabel = new JLabel();
        textLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        textLabel.setHorizontalAlignment(SwingConstants.CENTER);

        add(Box.createVerticalGlue());
        add(imageLabel);
        add(Box.createRigidArea(new Dimension(0, 10)));
        add(textLabel);
        add(Box.createVerticalGlue());
    }

    public void loadContent(String imagePath, String text) {
        setImage(imagePath);
        setText(text);
    }

    public void updateContent(String imagePath, String text) {
        loadContent(imagePath, text);
        revalidate();
        repaint();
    }

    public void setText(String text) {
        textLabel.setText("<html><div style='text-align:center;'>" + text + "</div></html>");
    }

    public void setImage(String path) {
        try {
            BufferedImage img = ImageIO.read(new File(path));
            originalImage = img;
            updateScaledImage();
        } catch (Exception e) {
            imageLabel.setText("Imagen no encontrada");
        }
    }

    public void updateScaledImage() {
        if (originalImage == null) return;

        int maxWidth = 400;
        int maxHeight = 300;

        Image scaled = originalImage.getScaledInstance(
                maxWidth,
                maxHeight,
                Image.SCALE_SMOOTH
        );

        imageLabel.setIcon(new ImageIcon(scaled));
    }

    @Override
    public void invalidate() {
        super.invalidate();
        updateScaledImage();
    }
}