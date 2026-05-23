package es.upm.agents;

import es.upm.idvehiculos.AgentBase;
import es.upm.idvehiculos.AgentModel;
import jade.core.AID;
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

    // OPCIÓN 2: Cargar la librería nativa directamente por su ruta absoluta
    static {
        try {
            System.load("C:\\Users\\X421FA\\Documents\\Sistemas Inteligentes\\opencv_java4120.dll");
            System.out.println(System.currentTimeMillis() + ": [OpenCV] Librería nativa cargada correctamente desde la ruta absoluta.");
        } catch (UnsatisfiedLinkError e) {
            System.err.println(System.currentTimeMillis() + ": [OpenCV] ERROR: No se pudo cargar la librería nativa de OpenCV desde la ruta especificada.");
            System.err.println(System.currentTimeMillis() + ": [OpenCV] Asegúrate de colocar el archivo 'opencv_java4120.dll' en: C:\\Users\\X421FA\\Documents\\Sistemas Inteligentes\\");
            System.err.println(System.currentTimeMillis() + ": [OpenCV] Detalle del error: " + e.getMessage());
        }
    }

    private List<String> classNames;

    @Override
    protected void setup() {
        this.type = AgentModel.PROCESSING;
        super.setup();
        log("Iniciado");
        
        // No registramos en el DF para evitar advertencias 'not-registered' en takeDown()
        // ya que estos agentes son temporales y dinámicos.

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

            // JADE Best Practice: usar receive() + block() en lugar de blockingReceive()
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                String imagePath = msg.getContent();
                log("Imagen recibida para procesar: " + imagePath);

                myAgent.addBehaviour(new ProcessImageBehaviour(imagePath));
                
                // Removemos el receptor ya que procesará una sola imagen y terminará
                myAgent.removeBehaviour(this);
            } else {
                block();
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
            log("Procesando imagen con YOLOv8...");
            
            Mat img = null;
            Mat blob = null;
            Mat output = null;
            Mat predictions = null;
            Mat predictionsT = null;

            try {
                // Cargar imagen
                img = Imgcodecs.imread(imagePath);
                if (img.empty()) {
                    loge("ERROR: No se pudo cargar la imagen: " + imagePath);
                    return;
                }

                // Cargar modelo yolov8n.onnx
                String modelPath = "src/main/resources/models/yolov8n.onnx";
                Net net = Dnn.readNetFromONNX(modelPath);

                // Crear blob (YOLOv8 espera 640x640, escala 1/255.0, swapRB = true)
                blob = Dnn.blobFromImage(
                        img,
                        1 / 255.0,
                        new Size(640, 640),
                        new Scalar(0, 0, 0),
                        true,
                        false
                );

                net.setInput(blob);

                // Forward pass (Salida YOLOv8 shape: [1, 84, 8400])
                output = net.forward();

                // Reshape a 2D: [84, 8400]
                predictions = output.reshape(1, 84);

                // Transponer a [8400, 84] para procesar por filas
                predictionsT = new Mat();
                Core.transpose(predictions, predictionsT);

                // Procesar detecciones
                List<String> detectedVehicles = new ArrayList<>();
                List<String> vehicleClasses = Arrays.asList(
                        "bicycle", "car", "motorcycle", "bus", "train",
                        "truck", "boat", "airplane"
                );

                int rows = predictionsT.rows(); // 8400 cajas candidatas
                int cols = predictionsT.cols(); // 84 (4 coordenadas + 80 clases COCO)

                for (int i = 0; i < rows; i++) {
                    // Copiar fila nativa a array Java para alta velocidad (evita overhead de JNI)
                    float[] rowData = new float[cols];
                    predictionsT.get(i, 0, rowData);

                    // Buscar la clase con la puntuación máxima (columnas de 4 a 83)
                    float maxScore = 0;
                    int classId = -1;
                    for (int j = 4; j < cols; j++) {
                        float score = rowData[j];
                        if (score > maxScore) {
                            maxScore = score;
                            classId = j - 4;
                        }
                    }

                    // Umbral de confianza razonable para detectar vehículos
                    if (maxScore > 0.5 && classId < classNames.size()) {
                        String className = classNames.get(classId);

                        if (vehicleClasses.contains(className)) {
                            // Agregamos el tipo de vehículo si no está ya en la lista detectada de esta imagen
                            if (!detectedVehicles.contains(className)) {
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
                    log("Resultado enviado a " + uiAgents[0].getName().getLocalName() + ": " + detectionResult);
                } else {
                    loge("No se encontró UIAgent en el DF. Resultado de consola: " + detectionResult);
                }
            } catch (Exception e) {
                loge("Excepción durante el procesamiento de la imagen: " + e.getMessage());
                e.printStackTrace();
            } finally {
                // Liberación explícita de memoria nativa de OpenCV
                if (img != null) img.release();
                if (blob != null) blob.release();
                if (output != null) output.release();
                if (predictions != null) predictions.release();
                if (predictionsT != null) predictionsT.release();

                // Notificar al PerceptionAgent para que libere el slot
                ACLMessage feedbackMsg = new ACLMessage(ACLMessage.INFORM);
                feedbackMsg.addReceiver(new AID(PerceptionAgent.NICKNAME, AID.ISLOCALNAME));
                feedbackMsg.setOntology("processor-finished");
                myAgent.send(feedbackMsg);
                log("Enviado feedback de finalización al PerceptionAgent");

                // Auto-destrucción
                myAgent.doDelete();
            }
        }
    }

    @Override
    protected void takeDown() {
        log("Terminando ProcessingAgent...");
    }
}