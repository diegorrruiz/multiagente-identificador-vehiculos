package es.upm.agents;

import es.upm.idvehiculos.AgentBase;
import es.upm.idvehiculos.AgentModel;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import org.opencv.core.*;
import org.opencv.dnn.*;
import org.opencv.imgcodecs.Imgcodecs;

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
            log("Procesando imagen con YOLO...");

            // Cargar imagen
            Mat img = Imgcodecs.imread(imagePath);
            if (img.empty()) {
                loge("ERROR: No se pudo cargar la imagen: " + imagePath);
                return;
            }

            // Cargar modelo yolov8n.onnx
            String modelPath = "src/main/resources/models/yolov8n.onnx";
            Net net = Dnn.readNetFromONNX(modelPath);

            // Crear blob
            Mat blob = Dnn.blobFromImage(
                    img,
                    1 / 255.0,
                    new Size(640, 640),
                    new Scalar(0, 0, 0),
                    true,
                    false
            );

            net.setInput(blob);

            // Forward pass
            Mat output = net.forward();

            // Procesar detecciones
            List<String> detectedVehicles = new ArrayList<>();

            List<String> vehicleClasses = Arrays.asList(
                    "bicycle", "car", "motorcycle", "bus", "train",
                    "truck", "boat", "airplane"
            );

            for (int i = 0; i < output.rows(); i++) {
                float confidence = (float) output.get(i, 4)[0];

                if (confidence > 0.5) {
                    int classId = -1;
                    float maxScore = 0;

                    for (int j = 5; j < output.cols(); j++) {
                        float score = (float) output.get(i, j)[0];
                        if (score > maxScore) {
                            maxScore = score;
                            classId = j - 5;
                        }
                    }

                    if (maxScore > 0.5 && classId < classNames.size()) {
                        String className = classNames.get(classId);

                        if (vehicleClasses.contains(className)) {
                            detectedVehicles.add(className);
                        }
                    }
                }
            }

            // Crear mensaje de resultado
            String detectionResult = detectedVehicles.isEmpty()
                    ? "No se detectaron vehículos."
                    : "Vehículos detectados: " + detectedVehicles;

            ACLMessage resultMsg = new ACLMessage(ACLMessage.INFORM);
            resultMsg.setOntology("detection-result");
            resultMsg.setContent(detectionResult);

            DFAgentDescription[] uiAgents = getAgentsDF(AgentModel.UI);
            if (uiAgents.length > 0) {
                resultMsg.addReceiver(uiAgents[0].getName());
                myAgent.send(resultMsg);
                log("Resultado enviado a " + uiAgents[0].getName().getLocalName());
            } else {
                loge("No se encontró UIAgent en el DF");
            }
        }
    }

    @Override
    protected void takeDown() {
        deregisterAgentDF();
        log("Terminando ProcessingAgent...");
    }
}
