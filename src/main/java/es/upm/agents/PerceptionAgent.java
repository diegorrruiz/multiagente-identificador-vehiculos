package es.upm.agents;

import es.upm.idvehiculos.AgentBase;
import es.upm.idvehiculos.AgentModel;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.lang.acl.MessageTemplate;

public class PerceptionAgent extends AgentBase {
    public static final String NICKNAME = "PerceptionAgent";

    @Override
    protected void setup() {
        this.type = AgentModel.PERCEPTION;
        super.setup();
        log("Iniciado");
        registerAgentDF();
        addBehaviour(new ImageRequestReceiverBehaviour());
    }

    private class ImageRequestReceiverBehaviour extends CyclicBehaviour {
        @Override
        public void action() {
            MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.REQUEST), MessageTemplate.MatchOntology("image-request"));

            ACLMessage msg = myAgent.blockingReceive(mt);

            if (msg != null) {
                log("Petición de imagen recibida de " + msg.getSender().getLocalName());

                String imagePath = msg.getContent();

                log("Ruta recibida: " + imagePath);

                myAgent.addBehaviour(new SendImageToProcessingBehaviour(imagePath));
            }
        }
    }

    private class SendImageToProcessingBehaviour extends OneShotBehaviour {
        private final String imagePath;

        public SendImageToProcessingBehaviour(String imagePath) {
            this.imagePath = imagePath;
        }

        @Override
        public void action() {
            log("Enviando imagen al ProcessingAgent...");

            ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
            msg.setOntology("process-image");
            msg.setContent(imagePath);

            DFAgentDescription[] processingAgents = getAgentsDF(AgentModel.PROCESSING);

            if (processingAgents.length > 0) {
                msg.addReceiver(processingAgents[0].getName());
                myAgent.send(msg);
                log("Imagen enviada a " + processingAgents[0].getName().getLocalName());
            } else {
                loge("No se encontró ProcessingAgent en el DF");
            }
        }
    }

    @Override
    protected void takeDown() {
        deregisterAgentDF();
        log("Terminando PerceptionAgent...");
    }
}
