package es.upm.agents;

import es.upm.idvehiculos.AgentBase;
import es.upm.idvehiculos.AgentModel;
import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;

import java.io.File;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

public class PerceptionAgent extends AgentBase {

    public static final String NICKNAME = "PerceptionAgent";
    private static final String IMAGE_FOLDER = "imagenes/";

    // Registro de imágenes ya leídas para evitar repetirlas
    private final Set<String> processedImages = new HashSet<>();

    // Cola de imágenes en espera de ser procesadas
    private final Queue<String> pendingImages = new LinkedList<>();

    // Conjunto de nombres locales de los ProcessingAgents que están activos actualmente
    private final Set<String> activeProcessors = new HashSet<>();

    @Override
    protected void setup() {
        this.type = AgentModel.PERCEPTION;
        super.setup();
        log("Iniciado");
        registerAgentDF();

        // Escanear la carpeta cada 5 segundos (5000 ms)
        addBehaviour(new ImageScannerBehaviour(this, 5000));

        addBehaviour(new ProcessorFeedbackReceiverBehaviour());
    }

    private class ImageScannerBehaviour extends TickerBehaviour {

        public ImageScannerBehaviour(PerceptionAgent agent, long period) {
            super(agent, period);
        }

        @Override
        protected void onTick() {
            File folder = new File(IMAGE_FOLDER);

            if (!folder.exists() || !folder.isDirectory()) {
                loge("La carpeta 'imagenes/' no existe");
                return;
            }

            File[] files = folder.listFiles((dir, name) ->
                    name.endsWith(".jpg") || name.endsWith(".png") || name.endsWith(".jpeg") || name.endsWith(".webp")
            );

            if (files == null || files.length == 0) {
                log("No hay imágenes en la carpeta");
                return;
            }

            boolean newImagesAdded = false;
            for (File file : files) {
                String path = file.getAbsolutePath();

                // Si es la primera vez que vemos esta imagen
                if (!processedImages.contains(path)) {
                    processedImages.add(path);
                    pendingImages.add(path);
                    log("Nueva imagen detectada y encolada: " + file.getName());
                    newImagesAdded = true;
                }
            }

            // Si hay imágenes en cola, intentamos lanzar procesadores
            if (newImagesAdded || !pendingImages.isEmpty()) {
                checkAndSpawnProcessors();
            }
        }
    }

    /**
     * Método para lanzar agentes de procesamiento si no se supera el límite de 10 concurrentes
     */
    private synchronized void checkAndSpawnProcessors() {
        // Cambiamos 'while' por 'if' para lanzar estrictamente un único agente por tick (cada 5 segundos)
        if (activeProcessors.size() < 10 && !pendingImages.isEmpty()) {
            String imagePath = pendingImages.poll();

            // Generamos el nombre del agente inmediatamente para el registro
            String agentName = "Processor-" + System.currentTimeMillis() + "-" + activeProcessors.size();
            activeProcessors.add(agentName);

            // Lanzamos el comportamiento
            addBehaviour(new CreateProcessingAgentBehaviour(imagePath, agentName));
        }
    }

    private class CreateProcessingAgentBehaviour extends OneShotBehaviour {

        private final String imagePath;
        private final String agentName;

        // Ahora recibe el nombre pre-generado
        public CreateProcessingAgentBehaviour(String imagePath, String agentName) {
            this.imagePath = imagePath;
            this.agentName = agentName;
        }

        @Override
        public void action() {
            try {
                ContainerController container = getContainerController();

                // Usamos el nombre que ya reservamos en la lista
                AgentController procAgent =
                        container.createNewAgent(agentName,
                                "es.upm.agents.ProcessingAgent",
                                null);

                procAgent.start();
                log("Creado agente de procesamiento: " + agentName + " (Activos: " + activeProcessors.size() + "/10)");

                // Enviar mensaje REQUEST con la ruta de la imagen
                ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
                msg.addReceiver(new AID(agentName, AID.ISLOCALNAME));
                msg.setOntology("process-image");
                msg.setContent(imagePath);

                send(msg);
                log("Mensaje enviado a " + agentName + " para la imagen " + new File(imagePath).getName());

            } catch (Exception e) {
                e.printStackTrace();
                loge("Error creando agente de procesamiento: " + agentName);

                // Si el agente falla al crearse, lo removemos para no perder ese slot para siempre
                synchronized (myAgent) {
                    activeProcessors.remove(agentName);
                }
            }
        }
    }

    /**
     * Escucha los mensajes de confirmación de fin de procesamiento para liberar slots
     */
    private class ProcessorFeedbackReceiverBehaviour extends CyclicBehaviour {
        @Override
        public void action() {
            MessageTemplate mt = MessageTemplate.and(
                    MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                    MessageTemplate.MatchOntology("processor-finished")
            );

            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                String senderName = msg.getSender().getLocalName();
                log("Feedback de finalización recibido de " + senderName);

                synchronized (myAgent) {
                    // Quitamos al agente de la lista de activos, pero NO lanzamos otro desde aquí
                    activeProcessors.remove(senderName);
                    log("Liberado slot de " + senderName + ". (Activos restantes: " + activeProcessors.size() + "/10)");
                }
            } else {
                block();
            }
        }
    }

    @Override
    protected void takeDown() {
        deregisterAgentDF();
        log("Terminando PerceptionAgent...");
    }
}