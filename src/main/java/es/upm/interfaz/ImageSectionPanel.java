package es.upm.interfaz;

import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import javax.imageio.ImageIO;

public class ImageSectionPanel extends JPanel {

    private static final Color BG_MAIN      = new Color(26, 26, 42);
    private static final Color BG_FOOTER    = new Color(18, 18, 30);
    private static final Color ACCENT       = new Color(100, 180, 255);
    private static final Color ACCENT_GREEN = new Color(100, 220, 140);
    private static final Color ACCENT_RED   = new Color(220, 100, 100);
    private static final Color TEXT_MUTED   = new Color(140, 140, 165);
    private static final Color DIVIDER      = new Color(40, 40, 60);
    private static final Color IMG_BORDER   = new Color(50, 50, 75);

    private final JLabel imageLabel    = new JLabel();
    private final JLabel filenameLabel = new JLabel();
    private final JLabel vehiclesLabel = new JLabel();
    private final JLabel emptyState;

    private Image   originalImage = null;
    private boolean isEmpty       = true;

    public ImageSectionPanel(String imagePath, String text) {
        setLayout(new BorderLayout(0, 0));
        setBackground(BG_MAIN);

        JPanel center = new JPanel(new GridBagLayout());
        center.setBackground(BG_MAIN);

        emptyState = buildEmptyState();
        center.add(emptyState);

        imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        imageLabel.setVisible(false);
        imageLabel.setBorder(BorderFactory.createLineBorder(IMG_BORDER, 1));
        center.add(imageLabel);

        add(center,       BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);

        if (imagePath != null) loadContent(imagePath, text);
    }

    private JLabel buildEmptyState() {
        JLabel lbl = new JLabel(
                "<html><div style='text-align:center;'>"
                        + "<span style='font-size:32px;'>🔍</span><br><br>"
                        + "Selecciona una imagen del panel lateral<br>"
                        + "<span style='color:#888;font-size:11px;'>"
                        + "Los resultados aparecerán aquí cuando el PerceptionAgent<br>"
                        + "detecte nuevas imágenes en la carpeta monitoreada."
                        + "</span></div></html>",
                SwingConstants.CENTER
        );
        lbl.setFont(new Font("SansSerif", Font.PLAIN, 14));
        lbl.setForeground(TEXT_MUTED);
        return lbl;
    }

    private JPanel buildFooter() {
        JPanel footer = new JPanel();
        footer.setLayout(new BoxLayout(footer, BoxLayout.Y_AXIS));
        footer.setBackground(BG_FOOTER);
        footer.setBorder(new CompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, DIVIDER),
                BorderFactory.createEmptyBorder(14, 24, 14, 24)
        ));

        filenameLabel.setFont(new Font("Monospaced", Font.BOLD, 13));
        filenameLabel.setForeground(ACCENT);
        filenameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lbl = new JLabel("Vehículos detectados");
        lbl.setFont(new Font("SansSerif", Font.PLAIN, 11));
        lbl.setForeground(TEXT_MUTED);
        lbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        vehiclesLabel.setFont(new Font("SansSerif", Font.BOLD, 15));
        vehiclesLabel.setForeground(TEXT_MUTED);
        vehiclesLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        footer.add(filenameLabel);
        footer.add(Box.createRigidArea(new Dimension(0, 6)));
        footer.add(lbl);
        footer.add(Box.createRigidArea(new Dimension(0, 4)));
        footer.add(vehiclesLabel);

        return footer;
    }

    public void loadContent(String imagePath, String text) {
        setImage(imagePath);
        setText(text);
        filenameLabel.setText(imagePath != null ? new File(imagePath).getName() : "—");
    }

    public void updateContent(String imagePath, String text) {
        loadContent(imagePath, text);
        revalidate();
        repaint();
    }

    private void setImage(String path) {
        if (path == null) { showEmpty(); return; }
        try {
            BufferedImage img = loadImage(path);
            originalImage = img;
            isEmpty       = false;
            emptyState.setVisible(false);
            imageLabel.setVisible(true);
            imageLabel.setText(null);
            updateScaledImage();
        } catch (Exception e) {
            showEmpty();
            imageLabel.setText("No se pudo cargar: " + new File(path).getName());
            imageLabel.setForeground(ACCENT_RED);
            imageLabel.setVisible(true);
            emptyState.setVisible(false);
        }
    }

    /**
     * Carga una imagen desde disco.
     * Primero intenta ImageIO (JPG, PNG, JPEG, GIF…).
     * Si devuelve null (p.ej. WebP), usa OpenCV como fallback.
     * OpenCV ya está cargado por ProcessingAgent antes de que llegue
     * cualquier resultado a la UI, por lo que el fallback es seguro.
     */
    private static BufferedImage loadImage(String path) throws Exception {
        BufferedImage img = ImageIO.read(new File(path));
        if (img != null) return img;

        try {
            Mat mat = Imgcodecs.imread(path);
            if (mat == null || mat.empty()) {
                throw new Exception("OpenCV: imagen vacía o formato no soportado — " + path);
            }
            BufferedImage result = matToBufferedImage(mat);
            mat.release();
            return result;
        } catch (UnsatisfiedLinkError e) {
            throw new Exception("Formato no soportado y OpenCV no disponible para: " + path);
        }
    }

    /**
     * Convierte un Mat de OpenCV (BGR) a BufferedImage (TYPE_3BYTE_BGR).
     * Ambos usan el mismo orden de bytes interno, por lo que la copia
     * es directa sin necesidad de convertir canales.
     */
    private static BufferedImage matToBufferedImage(Mat mat) {
        int type = mat.channels() == 1
                ? BufferedImage.TYPE_BYTE_GRAY
                : BufferedImage.TYPE_3BYTE_BGR;
        BufferedImage bImg = new BufferedImage(mat.cols(), mat.rows(), type);
        byte[] data = ((DataBufferByte) bImg.getRaster().getDataBuffer()).getData();
        mat.get(0, 0, data);
        return bImg;
    }


    private void setText(String text) {
        if (text == null || text.isBlank()) {
            vehiclesLabel.setText("No se detectaron vehículos");
            vehiclesLabel.setForeground(ACCENT_RED);
        } else {
            vehiclesLabel.setText("<html><center>" + text.replace(",", " · ") + "</center></html>");
            vehiclesLabel.setForeground(ACCENT_GREEN);
        }
    }

    private void showEmpty() {
        isEmpty       = true;
        originalImage = null;
        imageLabel.setVisible(false);
        imageLabel.setIcon(null);
        imageLabel.setText(null);
        emptyState.setVisible(true);
    }

    /** Recalcula la escala manteniendo la relación de aspecto. */
    private void updateScaledImage() {
        if (originalImage == null || isEmpty) return;

        int panelW = Math.max(getWidth(),         300);
        int panelH = Math.max(getHeight() - 100,  200);
        int maxW   = (int) (panelW * 0.88);
        int maxH   = (int) (panelH * 0.88);

        int srcW = originalImage.getWidth(null);
        int srcH = originalImage.getHeight(null);
        if (srcW <= 0 || srcH <= 0) return;

        double scale = Math.min((double) maxW / srcW, (double) maxH / srcH);
        scale = Math.min(scale, 1.0); // no ampliar imágenes más pequeñas que el panel

        int newW = Math.max(1, (int) (srcW * scale));
        int newH = Math.max(1, (int) (srcH * scale));

        imageLabel.setIcon(new ImageIcon(originalImage.getScaledInstance(newW, newH, Image.SCALE_SMOOTH)));
    }

    @Override
    public void invalidate() {
        super.invalidate();
        if (!isEmpty) updateScaledImage();
    }
}
