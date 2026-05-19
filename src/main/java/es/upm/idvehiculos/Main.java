package es.upm.idvehiculos;

import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.wrapper.StaleProxyException;
import java.io.IOException;

import es.upm.agents.PerceptionAgent;
import es.upm.agents.ProcessingAgent;
import es.upm.agents.UIAgent;

public class Main {
    private static jade.wrapper.AgentContainer cc;

    private static void loadBoot() {
        jade.core.Runtime rt = jade.core.Runtime.instance();
        rt.setCloseVM(true);
        System.out.println("Runtime created");

        Profile profile = new ProfileImpl(null, 1200, null);
        System.out.println("Profile created");

        System.out.println("Launching a whole in-process platform ... " + profile);
        cc = rt.createMainContainer(profile);

        try {
            ProfileImpl pContainer = new ProfileImpl(null, 1200, null);
            rt.createAgentContainer(pContainer);
            System.out.println("Containers created");
            System.out.println("Launching the rma agent on the main container ... ");

            cc.createNewAgent("rma", "jade.tools.rma.rma", new Object[0]).start();

            cc.createNewAgent("PerceptionAgent", PerceptionAgent.class.getName(), new Object[]{}).start();

            cc.createNewAgent("ProcessingAgent", ProcessingAgent.class.getName(), new Object[]{}).start();

            cc.createNewAgent("UIAgent", UIAgent.class.getName(), new Object[]{}).start();

        } catch (StaleProxyException e) {
            System.err.println("Error during boot !!!");
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static void main(String[] args) throws IOException {
        System.out.println("Starting ... ");
        loadBoot();
        System.out.println("MAS loaded ... ");
    }

}
