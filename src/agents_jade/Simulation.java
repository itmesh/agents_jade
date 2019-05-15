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
		AgentContainer container = getContainerController();
		try {
			for (int i = 0; i < IM_AGENT_COUNT; i++) {
				AgentController imAgent = container.createNewAgent(IMAgent.PREFIX_AGENT + i, "agents_jade.IMAgent",
						null);
				imAgent.start();
			}
			AgentController imMasterAgent = container.createNewAgent(IMMasterAgent.PREFIX_AGENT + 0,
					"agents_jade.IMMasterAgent", null);
			imMasterAgent.start();
		} catch (StaleProxyException e) {
			e.printStackTrace();
		}
	}
}
