package es.upm.agents;

import es.upm.idvehiculos.AgentBase;
import es.upm.idvehiculos.AgentModel;
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
import java.util.concurrent.atomic.AtomicInteger;

public class PerceptionAgent extends AgentBase {

    public static final String NICKNAME     = "PerceptionAgent";
    private static final String IMAGE_FOLDER = "imagenes/";
    private static final int MAX_PROCESSORS  = 10;

    private final Set<String>   processedImages  = new HashSet<>();
    private final Queue<String> pendingImages    = new LinkedList<>();
    private final Set<String>   activeProcessors = new HashSet<>();
    private final AtomicInteger processorCounter = new AtomicInteger(0);

    @Override
    protected void setup() {
        this.type = AgentModel.PERCEPTION;
        super.setup();
        log("Iniciado");
        registerAgentDF();
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
                loge("La carpeta '" + IMAGE_FOLDER + "' no existe");
                return;
            }

            File[] files = folder.listFiles((dir, name) -> {
                String lower = name.toLowerCase();
                return lower.endsWith(".jpg") || lower.endsWith(".jpeg")
                        || lower.endsWith(".png") || lower.endsWith(".webp");
            });

            if (files == null || files.length == 0) {
                log("No hay imágenes en la carpeta");
                return;
            }

            boolean newImages = false;
            for (File file : files) {
                String path = file.getAbsolutePath();
                if (!processedImages.contains(path)) {
                    processedImages.add(path);
                    pendingImages.add(path);
                    log("Nueva imagen encolada: " + file.getName());
                    newImages = true;
                }
            }

            if (newImages || !pendingImages.isEmpty()) {
                checkAndSpawnProcessors();
            }
        }
    }

    private synchronized void checkAndSpawnProcessors() {
        while (activeProcessors.size() < MAX_PROCESSORS && !pendingImages.isEmpty()) {
            String imagePath = pendingImages.poll();
            String agentName = "Processor-" + processorCounter.getAndIncrement();
            activeProcessors.add(agentName);
            addBehaviour(new CreateProcessingAgentBehaviour(imagePath, agentName));
        }
    }

    private class CreateProcessingAgentBehaviour extends OneShotBehaviour {

        private final String imagePath;
        private final String agentName;

        public CreateProcessingAgentBehaviour(String imagePath, String agentName) {
            this.imagePath = imagePath;
            this.agentName = agentName;
        }

        @Override
        public void action() {
            try {
                ContainerController container = getContainerController();

                AgentController procAgent = container.createNewAgent(
                        agentName,
                        "es.upm.agents.ProcessingAgent",
                        new Object[]{imagePath}
                );
                procAgent.start();
                log("Iniciado " + agentName + " → " + new File(imagePath).getName()
                        + " (activos: " + activeProcessors.size() + "/" + MAX_PROCESSORS + ")");

            } catch (Exception e) {
                loge("Error creando agente " + agentName + ": " + e.getMessage());
                synchronized (PerceptionAgent.this) {
                    activeProcessors.remove(agentName);
                }
            }
        }
    }

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
                synchronized (PerceptionAgent.this) {
                    activeProcessors.remove(senderName);
                    log("Slot liberado por " + senderName
                            + " (activos: " + activeProcessors.size() + "/" + MAX_PROCESSORS + ")");
                }
                checkAndSpawnProcessors();
            } else {
                block();
            }
        }
    }

    @Override
    protected void takeDown() {
        deregisterAgentDF();
        log("Terminando PerceptionAgent.");
    }
}