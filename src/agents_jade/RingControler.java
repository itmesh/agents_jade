package agents_jade;

import java.util.ArrayList;
import java.util.List;

public class RingControler {

	public static RingControler instance;
	public List<DeviceAgent> IMAgentList;

	public static void init() {
		instance = new RingControler();
		instance.IMAgentList = new ArrayList<>();
		new RingSimulation().start();
	};

	synchronized public void addAgent(DeviceAgent agent) {
		/*
		 * if (instance.IMAgentList.size() > 0) { IMAgent firstAgent =
		 * instance.IMAgentList.get(0); IMAgent lastAgent =
		 * instance.IMAgentList.get(instance.IMAgentList.size() - 1);
		 * lastAgent.nextAgent = agent; agent.nextAgent = firstAgent;
		 * instance.IMAgentList.add(agent); } else { instance.IMAgentList.add(agent); }
		 */
		instance.IMAgentList.add(agent);
	}

	public void deleteAgent(DeviceAgent imAgent) {
		// TODO Auto-generated method stub
		instance.IMAgentList.remove(imAgent);
	}
}

class RingSimulation extends Thread {

	public int currentTokenID = 0;
	// public boolean pending = false;
	public boolean firstMessageGenerated = false;

	public RingSimulation() {
		// TODO Auto-generated constructor stub
	}
/*
	@Override
	public void run() {
		super.run();

		while (true) {
			try {
				sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			if (RingControler.instance.IMAgentList.size() > 1) {
				if (!firstMessageGenerated) {
					generateMessage();
					firstMessageGenerated = true;
				}
				System.out.println(
						"# Token for Agent " + RingControler.instance.IMAgentList.get(currentTokenID).getLocalName());

				if (RingControler.instance.IMAgentList.get(currentTokenID).requestTokenFlag) {
					RingControler.instance.IMAgentList.get(currentTokenID).exec();
				}

				if (currentTokenID >= RingControler.instance.IMAgentList.size() - 1) {
					currentTokenID = 0;
				} else
					currentTokenID++;

			}
		}
	}

	void generateMessage() {
		int fromAgent = (int) (Math.random() * RingControler.instance.IMAgentList.size());
		int toAgent = fromAgent;
		while (toAgent == fromAgent) {
			toAgent = (int) (Math.random() * RingControler.instance.IMAgentList.size());
		}
		RingControler.instance.IMAgentList.get(fromAgent)
				.queueMessageToAgent(RingControler.instance.IMAgentList.get(toAgent), new OnNext() {
					@Override
					public void onNext() {
						if (RingControler.instance.IMAgentList.size() > 1)
							generateMessage();
						else
							firstMessageGenerated = false;
					}
				});
	}
	*/
}
