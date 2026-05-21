package es.upm.agents;

import es.upm.idvehiculos.AgentBase;
import es.upm.idvehiculos.AgentModel;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import org.opencv.core.*;
import org.opencv.dnn.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class ProcessingAgent extends AgentBase {
    public static final String NICKNAME = "ProcessingAgent";

    private List<String> classNames;

    @Override
    protected void setup() {
        this.type = AgentModel.PROCESSING;
        super.setup();
        log("Iniciado");
        registerAgentDF();

        // Cargar nombres de clases COCO
        try {
            classNames = Files.readAllLines(Paths.get("src/main/resources/models/coco.names"));
        } catch (Exception e) {
            loge("ERROR cargando coco.names: " + e.getMessage());
            classNames = new ArrayList<>();
        }

        addBehaviour(new ImageProcessingReceiverBehaviour());
    }

    private class ImageProcessingReceiverBehaviour extends CyclicBehaviour {
        @Override
        public void action() {
            MessageTemplate mt = MessageTemplate.and(
                    MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
                    MessageTemplate.MatchOntology("process-image")
            );

            ACLMessage msg = myAgent.blockingReceive(mt);
            if (msg != null) {
                String imagePath = msg.getContent();
                log("Imagen recibida para procesar: " + imagePath);

                myAgent.addBehaviour(new ProcessImageBehaviour(imagePath));
            }
        }
    }

    private class ProcessImageBehaviour extends OneShotBehaviour {
        private final String imagePath;

        public ProcessImageBehaviour(String imagePath) {
            this.imagePath = imagePath;
        }

        @Override
        public void action() {
        }

    }
}