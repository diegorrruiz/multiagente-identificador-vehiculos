package es.upm.agents;

import es.upm.idvehiculos.AgentBase;
import es.upm.idvehiculos.AgentModel;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class UIAgent extends AgentBase {

    public static final String NICKNAME = "UIAgent";

    @Override
    protected void setup() {
        this.type = AgentModel.UI;
        super.setup();
        log("Iniciado");
        registerAgentDF();
        addBehaviour(new DetectionResultReceiverBehaviour());
    }

    private class DetectionResultReceiverBehaviour extends CyclicBehaviour {
        @Override
        public void action() {
            MessageTemplate mt = MessageTemplate.and(
                    MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                    MessageTemplate.MatchOntology("detection-result")
            );

            ACLMessage msg = myAgent.receive(mt);

            if (msg != null) {
                String result = msg.getContent();
                log("Resultado de detección recibido: " + result);

                // Aquí podrías actualizar una GUI, enviar a web, etc.
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
