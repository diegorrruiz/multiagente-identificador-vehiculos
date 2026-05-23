package es.upm.agents;

import es.upm.idvehiculos.AgentBase;
import es.upm.idvehiculos.AgentModel;
import jade.core.AID;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import org.opencv.core.*;
import org.opencv.dnn.*;
import org.opencv.imgcodecs.Imgcodecs;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class ProcessingAgent extends AgentBase {

    public static final String NICKNAME = "ProcessingAgent";

    private static final String MODEL_PATH      = "src/main/resources/models/yolov8n.onnx";
    private static final String COCO_NAMES_PATH = "src/main/resources/models/coco.names";
    private static final String OPENCV_DLL_PATH = "src/main/resources/models/opencv_java4120.dll";

    static {
        try {
            System.load(new File(OPENCV_DLL_PATH).getAbsolutePath());
            System.out.println(System.currentTimeMillis() + ": [OpenCV] Librería nativa cargada correctamente.");
        } catch (UnsatisfiedLinkError e) {
            System.err.println(System.currentTimeMillis() + ": [OpenCV] ERROR cargando librería nativa: " + e.getMessage());
        }
    }

    private static volatile List<String> sharedClassNames = null;
    private static final Object CLASS_NAMES_LOCK = new Object();

    private static List<String> getClassNames() {
        if (sharedClassNames == null) {
            synchronized (CLASS_NAMES_LOCK) {
                if (sharedClassNames == null) {
                    try {
                        sharedClassNames = Collections.unmodifiableList(
                                Files.readAllLines(Paths.get(COCO_NAMES_PATH)));
                    } catch (Exception e) {
                        System.err.println("[ProcessingAgent] ERROR cargando coco.names: " + e.getMessage());
                        sharedClassNames = Collections.emptyList();
                    }
                }
            }
        }
        return sharedClassNames;
    }

    @Override
    protected void setup() {
        this.type = AgentModel.PROCESSING;
        super.setup();
        log("Iniciado");

        if (params.length == 0 || params[0] == null || params[0].isBlank()) {
            loge("No se recibió ruta de imagen como argumento. Terminando.");
            doDelete();
            return;
        }
        String imagePath = params[0];

        Net net;
        try {
            net = Dnn.readNetFromONNX(MODEL_PATH);
            log("Modelo YOLOv8 listo para: " + new File(imagePath).getName());
        } catch (Exception e) {
            loge("No se pudo cargar el modelo YOLO: " + e.getMessage());
            doDelete();
            return;
        }

        addBehaviour(new ProcessImageBehaviour(imagePath, net));
    }

    private class ProcessImageBehaviour extends OneShotBehaviour {

        private final String imagePath;
        private final Net net;

        public ProcessImageBehaviour(String imagePath, Net net) {
            this.imagePath = imagePath;
            this.net = net;
        }

        @Override
        public void action() {
            log("Procesando imagen con YOLOv8: " + new File(imagePath).getName());

            Mat img         = null;
            Mat blob        = null;
            Mat output      = null;
            Mat predictions = null;
            Mat predictionsT = null;

            try {
                img = Imgcodecs.imread(imagePath);
                if (img.empty()) {
                    loge("No se pudo cargar la imagen: " + imagePath);
                    return;
                }

                blob = Dnn.blobFromImage(img, 1 / 255.0, new Size(640, 640),
                        new Scalar(0, 0, 0), true, false);
                net.setInput(blob);

                output      = net.forward();
                predictions = output.reshape(1, 84);
                predictionsT = new Mat();
                Core.transpose(predictions, predictionsT);

                List<String> detectedVehicles = new ArrayList<>();
                List<String> vehicleClasses   = Arrays.asList(
                        "bicycle", "car", "motorcycle", "bus", "train",
                        "truck", "boat", "airplane"
                );
                List<String> classNames = getClassNames();

                int rows = predictionsT.rows();
                int cols = predictionsT.cols();

                for (int i = 0; i < rows; i++) {
                    float[] rowData = new float[cols];
                    predictionsT.get(i, 0, rowData);

                    float maxScore = 0;
                    int classId = -1;
                    for (int j = 4; j < cols; j++) {
                        if (rowData[j] > maxScore) {
                            maxScore = rowData[j];
                            classId  = j - 4;
                        }
                    }

                    if (maxScore > 0.5 && classId >= 0 && classId < classNames.size()) {
                        String className = classNames.get(classId);
                        if (vehicleClasses.contains(className) && !detectedVehicles.contains(className)) {
                            detectedVehicles.add(className);
                        }
                    }
                }

                String json = "{ \"resultado\": \""
                        + (detectedVehicles.isEmpty()
                                ? "No se detectaron vehículos."
                                : "Vehículos detectados: " + detectedVehicles)
                        + "\", \"imagen\": \"" + imagePath + "\" }";

                ACLMessage resultMsg = new ACLMessage(ACLMessage.INFORM);
                resultMsg.setOntology("detection-result");
                resultMsg.setContent(json);

                DFAgentDescription[] uiAgents = getAgentsDF(AgentModel.UI);
                if (uiAgents.length > 0) {
                    resultMsg.addReceiver(uiAgents[0].getName());
                    myAgent.send(resultMsg);
                    log("Resultado enviado a UIAgent: " + json);
                } else {
                    loge("UIAgent no encontrado en el DF. Resultado: " + json);
                }

            } catch (Exception e) {
                loge("Excepción durante el procesamiento: " + e.getMessage());
                e.printStackTrace();
            } finally {
                if (img != null)          img.release();
                if (blob != null)         blob.release();
                if (output != null)       output.release();
                if (predictions != null)  predictions.release();
                if (predictionsT != null) predictionsT.release();

                ACLMessage feedbackMsg = new ACLMessage(ACLMessage.INFORM);
                feedbackMsg.addReceiver(new AID(PerceptionAgent.NICKNAME, AID.ISLOCALNAME));
                feedbackMsg.setOntology("processor-finished");
                myAgent.send(feedbackMsg);
                log("Feedback de finalización enviado al PerceptionAgent");

                myAgent.doDelete();
            }
        }
    }

    @Override
    protected void takeDown() {
        log("Terminando ProcessingAgent.");
    }
}
