package es.upm.interfaz;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MainUIFrame extends JFrame {

    private static final Color BG_DARK       = new Color(18, 18, 30);
    private static final Color BG_PANEL      = new Color(26, 26, 42);
    private static final Color BG_SIDEBAR    = new Color(22, 22, 36);
    private static final Color BG_ITEM_ODD   = new Color(26, 26, 42);
    private static final Color BG_ITEM_EVEN  = new Color(30, 30, 48);
    private static final Color BG_SELECTED   = new Color(45, 45, 80);
    private static final Color ACCENT        = new Color(100, 180, 255);
    private static final Color ACCENT_GREEN  = new Color(100, 220, 140);
    private static final Color ACCENT_RED    = new Color(220, 100, 100);
    private static final Color TEXT_PRIMARY  = new Color(230, 230, 240);
    private static final Color TEXT_MUTED    = new Color(140, 140, 165);
    private static final Color DIVIDER       = new Color(40, 40, 60);

    public static class DetectionEntry {
        public final String id;
        public final String imagePath;
        public String result;
        public final long timestamp;

        public DetectionEntry(String id, String imagePath, String result) {
            this.id        = id;
            this.imagePath = imagePath;
            this.result    = result;
            this.timestamp = System.currentTimeMillis();
        }

        /** true si se detectó al menos un vehículo */
        public boolean hasVehicles() {
            return result != null && !result.isBlank()
                    && !result.contains("No se detectaron");
        }
    }

    private final DefaultListModel<DetectionEntry> listModel     = new DefaultListModel<>();
    private final JList<DetectionEntry>            sidebarList;
    private final ImageSectionPanel                detailPanel   = new ImageSectionPanel(null, null);
    private final JLabel statusLabel   = new JLabel("Sistema listo — esperando imágenes en la carpeta monitoreada...");
    private final JLabel counterLabel  = new JLabel("0 procesadas");
    private int processedCount = 0;

    public MainUIFrame() {
        super("Detector de Vehículos — Sistema Multiagente JADE");

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 720);
        setMinimumSize(new Dimension(800, 500));
        setLocationRelativeTo(null);
        getContentPane().setBackground(BG_DARK);
        setLayout(new BorderLayout(0, 0));

        add(buildHeader(), BorderLayout.NORTH);

        sidebarList = new JList<>(listModel);
        sidebarList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        sidebarList.setCellRenderer(new SidebarCellRenderer());
        sidebarList.setBackground(BG_SIDEBAR);
        sidebarList.setFixedCellHeight(72);
        sidebarList.setBorder(null);
        sidebarList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                DetectionEntry sel = sidebarList.getSelectedValue();
                if (sel != null) detailPanel.updateContent(sel.imagePath, sel.result);
            }
        });

        JScrollPane sidebarScroll = new JScrollPane(sidebarList);
        sidebarScroll.setPreferredSize(new Dimension(270, 0));
        sidebarScroll.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, DIVIDER));
        sidebarScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        sidebarScroll.getViewport().setBackground(BG_SIDEBAR);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, sidebarScroll, detailPanel);
        split.setDividerLocation(270);
        split.setDividerSize(1);
        split.setBorder(null);
        split.setBackground(BG_DARK);
        split.setUI(new javax.swing.plaf.basic.BasicSplitPaneUI() {
            @Override public javax.swing.plaf.basic.BasicSplitPaneDivider createDefaultDivider() {
                javax.swing.plaf.basic.BasicSplitPaneDivider div = super.createDefaultDivider();
                div.setBackground(DIVIDER);
                return div;
            }
        });

        add(split, BorderLayout.CENTER);
        add(buildStatusBar(), BorderLayout.SOUTH);

        setVisible(true);
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(14, 14, 24));
        header.setBorder(new CompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, DIVIDER),
                BorderFactory.createEmptyBorder(14, 20, 14, 20)
        ));

        JLabel title = new JLabel("  Detector de Vehículos");
        title.setFont(new Font("SansSerif", Font.BOLD, 17));
        title.setForeground(TEXT_PRIMARY);

        JLabel badge = new JLabel("YOLOv8 · JADE MAS");
        badge.setFont(new Font("Monospaced", Font.PLAIN, 11));
        badge.setForeground(ACCENT);
        badge.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ACCENT, 1, true),
                BorderFactory.createEmptyBorder(2, 8, 2, 8)
        ));

        counterLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        counterLabel.setForeground(TEXT_MUTED);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        right.setOpaque(false);
        right.add(counterLabel);
        right.add(badge);

        header.add(title, BorderLayout.WEST);
        header.add(right,  BorderLayout.EAST);
        return header;
    }

    private JPanel buildStatusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(new Color(14, 14, 24));
        bar.setBorder(new CompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, DIVIDER),
                BorderFactory.createEmptyBorder(5, 12, 5, 12)
        ));

        statusLabel.setFont(new Font("Monospaced", Font.PLAIN, 11));
        statusLabel.setForeground(TEXT_MUTED);

        bar.add(statusLabel, BorderLayout.WEST);
        return bar;
    }

    /**
     * Añade o actualiza una entrada en la sidebar y actualiza el panel de detalle si
     * la entrada está seleccionada. Llamar siempre desde el EDT (ya garantizado por UIAgent).
     */
    public void updateTab(String tabId, String imagePath, String result) {
        for (int i = 0; i < listModel.size(); i++) {
            DetectionEntry e = listModel.get(i);
            if (e.id.equals(tabId)) {
                e.result = result;
                listModel.set(i, e); // fuerza repaint de la celda
                if (sidebarList.getSelectedIndex() == i) {
                    detailPanel.updateContent(imagePath, result);
                }
                updateStatus(tabId, result);
                return;
            }
        }

        processedCount++;
        DetectionEntry entry = new DetectionEntry(tabId, imagePath, result);
        listModel.addElement(entry);

        if (sidebarList.getSelectedIndex() == -1) {
            sidebarList.setSelectedIndex(listModel.size() - 1);
        }

        counterLabel.setText(processedCount + " procesada" + (processedCount != 1 ? "s" : ""));
        updateStatus(tabId, result);
    }

    private void updateStatus(String filename, String result) {
        boolean ok = result != null && !result.isBlank() && !result.contains("No se detectaron");
        String icon = ok ? "✔" : "–";
        String detail = ok ? result : "sin detecciones";
        statusLabel.setText(icon + "  Última imagen: " + filename + "  |  " + detail);
        statusLabel.setForeground(ok ? ACCENT_GREEN : TEXT_MUTED);
    }

    private static class SidebarCellRenderer implements ListCellRenderer<DetectionEntry> {

        @Override
        public Component getListCellRendererComponent(
                JList<? extends DetectionEntry> list,
                DetectionEntry entry,
                int index,
                boolean isSelected,
                boolean cellHasFocus) {

            JPanel row = new JPanel(new BorderLayout(10, 0));
            row.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
            row.setBackground(isSelected ? BG_SELECTED
                    : (index % 2 == 0 ? BG_ITEM_EVEN : BG_ITEM_ODD));

            JPanel dot = new JPanel() {
                @Override protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Graphics2D g2 = (Graphics2D) g;
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(entry.hasVehicles() ? ACCENT_GREEN : ACCENT_RED);
                    g2.fillOval(0, getHeight() / 2 - 4, 8, 8);
                }
            };
            dot.setOpaque(false);
            dot.setPreferredSize(new Dimension(12, 12));

            JPanel text = new JPanel();
            text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
            text.setOpaque(false);
            String filename = new File(entry.imagePath != null ? entry.imagePath : entry.id).getName();
            JLabel nameLabel = new JLabel(filename);
            nameLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
            nameLabel.setForeground(isSelected ? Color.WHITE : TEXT_PRIMARY);

            String preview = entry.result == null || entry.result.isBlank()
                    ? "Sin detecciones"
                    : entry.result;
            if (preview.length() > 32) preview = preview.substring(0, 32) + "…";
            JLabel resultLabel = new JLabel(preview);
            resultLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
            resultLabel.setForeground(entry.hasVehicles()
                    ? new Color(100, 200, 120) : new Color(160, 100, 100));

            text.add(nameLabel);
            text.add(Box.createRigidArea(new Dimension(0, 3)));
            text.add(resultLabel);

            JLabel num = new JLabel(String.valueOf(index + 1));
            num.setFont(new Font("Monospaced", Font.BOLD, 10));
            num.setForeground(isSelected ? ACCENT : TEXT_MUTED);
            num.setPreferredSize(new Dimension(22, 22));
            num.setHorizontalAlignment(SwingConstants.RIGHT);

            row.add(dot,  BorderLayout.WEST);
            row.add(text, BorderLayout.CENTER);
            row.add(num,  BorderLayout.EAST);

            return row;
        }
    }
}
