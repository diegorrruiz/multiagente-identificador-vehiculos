package es.upm.agents;

import es.upm.idvehiculos.AgentBase;
import es.upm.idvehiculos.AgentModel;
import es.upm.interfaz.MainUIFrame;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import javax.swing.SwingUtilities;

public class UIAgent extends AgentBase {

    public static final String NICKNAME = "UIAgent";
    private MainUIFrame ui;

    @Override
    protected void setup() {
        this.type = AgentModel.UI;
        super.setup();
        log("Iniciado");
        registerAgentDF();
        SwingUtilities.invokeLater(() -> {
            ui = new MainUIFrame();
        });
        addBehaviour(new DetectionResultReceiverBehaviour());
    }

    public void handleDetectionResult(String content) {

        String imagePath = extractField(content, "imagen");
        String resultado = extractField(content, "resultado");

        String vehiculos = extractVehicles(resultado);

        SwingUtilities.invokeLater(() -> {
            if (ui != null) {
                ui.updateTab("Default", imagePath, vehiculos);
            }
        });
    }

    private String extractField(String json, String fieldName) {
        String pattern = "\"" + fieldName + "\"";
        int startKey = json.indexOf(pattern);

        if (startKey == -1) return null;

        int colon = json.indexOf(":", startKey);
        if (colon == -1) return null;

        int firstQuote = json.indexOf("\"", colon + 1);
        if (firstQuote == -1) return null;

        int secondQuote = json.indexOf("\"", firstQuote + 1);
        if (secondQuote == -1) return null;

        return json.substring(firstQuote + 1, secondQuote);
    }

    private String extractVehicles(String text) {

        if (text == null) return "";

        int start = text.indexOf("[");
        int end = text.indexOf("]");

        if (start == -1 || end == -1 || end <= start) {
            return text;
        }

        String inside = text.substring(start + 1, end);

        return inside.trim();
    }

    public class DetectionResultReceiverBehaviour extends CyclicBehaviour {

        @Override
        public void action() {

            MessageTemplate mt = MessageTemplate.and(
                    MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                    MessageTemplate.MatchOntology("detection-result")
            );

            ACLMessage msg = myAgent.receive(mt);

            if (msg != null) {
                ((UIAgent) myAgent).handleDetectionResult(msg.getContent());
            } else {
                block();
            }
        }
    }

    @Override
    protected void takeDown() {
        deregisterAgentDF();
        log("Terminando UIAgent...");
    }
}
