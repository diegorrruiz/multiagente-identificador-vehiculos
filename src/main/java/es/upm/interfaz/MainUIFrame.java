package es.upm.interfaz;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class MainUIFrame extends JFrame {

    private final JTabbedPane tabbedPane;
    private final Map<String, ImageSectionPanel> panels;

    public MainUIFrame() {
        super("Detector de Vehículos");

        this.panels = new HashMap<>();

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 600);
        setLocationRelativeTo(null);

        tabbedPane = new JTabbedPane(JTabbedPane.TOP);

        setLayout(new BorderLayout());
        add(tabbedPane, BorderLayout.CENTER);

        setVisible(true);
    }

    public void updateTab(String tabId, String imagePath, String text) {
        if (panels.containsKey(tabId)) {
            panels.get(tabId).updateContent(imagePath, text);
        } else {
            ImageSectionPanel panel = new ImageSectionPanel(imagePath, text);
            panels.put(tabId, panel);
            tabbedPane.addTab(tabId, wrapCentered(panel));
        }
    }

    private JScrollPane wrapCentered(JPanel panel) {
        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setBackground(Color.WHITE);

        wrapper.add(panel);

        JScrollPane scroll = new JScrollPane(wrapper);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(Color.WHITE);

        return scroll;
    }
}