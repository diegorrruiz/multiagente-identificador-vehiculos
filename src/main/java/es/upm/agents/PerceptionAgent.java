package es.upm.agents;

import es.upm.idvehiculos.AgentBase;
import es.upm.idvehiculos.AgentModel;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import jade.domain.FIPAAgentManagement.DFAgentDescription;

public class PerceptionAgent extends AgentBase {

    public static final String NICKNAME = "PerceptionAgent";
    // añadir PerceptionAgent


    // añadir ImageRequestReceiverBehaviour

    private class SendImageToProcessingBehaviour extends OneShotBehaviour {

        private final String imageData;

        public SendImageToProcessingBehaviour(String imageData) {
            this.imageData = imageData;
        }

        @Override
        public void action() {
            log("Enviando imagen al ProcessingAgent...");

            ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
            msg.setOntology("process-image");
            msg.setContent(imageData);

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
