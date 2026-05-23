package es.upm.idvehiculos;

import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.wrapper.StaleProxyException;
import java.io.IOException;

import es.upm.agents.PerceptionAgent;
import es.upm.agents.UIAgent;

public class Main {
    private static jade.wrapper.AgentContainer cc;

    private static void loadBoot() {
        jade.core.Runtime rt = jade.core.Runtime.instance();
        rt.setCloseVM(true);

        Profile profile = new ProfileImpl(null, 1200, null);
        cc = rt.createMainContainer(profile);

        try {
            ProfileImpl pContainer = new ProfileImpl(null, 1200, null);
            rt.createAgentContainer(pContainer);

            cc.createNewAgent("rma", "jade.tools.rma.rma", new Object[0]).start();
            cc.createNewAgent(PerceptionAgent.NICKNAME, PerceptionAgent.class.getName(), new Object[]{}).start();
            cc.createNewAgent(UIAgent.NICKNAME,         UIAgent.class.getName(),         new Object[]{}).start();

        } catch (StaleProxyException e) {
            System.err.println("Error durante el arranque del sistema multiagente.");
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static void main(String[] args) throws IOException {
        System.out.println("Arrancando sistema multiagente...");
        loadBoot();
        System.out.println("Sistema multiagente iniciado.");
    }
}
