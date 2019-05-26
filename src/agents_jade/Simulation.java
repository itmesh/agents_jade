package agents_jade;

import jade.core.Agent;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import jade.wrapper.StaleProxyException;

public class Simulation extends Agent {

	public static int IM_AGENT_COUNT = 5;

	@Override
	protected void setup() {
		super.setup();
		RingControler.init();
		AgentContainer container = getContainerController();
		try {
			for (int i = 0; i < IM_AGENT_COUNT; i++) {
				AgentController imAgent = container.createNewAgent(IMAgent.PREFIX_AGENT + (i + 1),
						"agents_jade.IMAgent", null);
				imAgent.start();
			}
		} catch (StaleProxyException e) {
			e.printStackTrace();
		}
	}

}