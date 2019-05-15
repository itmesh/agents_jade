package agents_jade;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;

public class IMMasterAgent extends Agent {

	public static final String PREFIX_AGENT = "IM_MASTER_AGENT_";
	public static final int INTERVAL_REQUEST = 2000;
	public static final String OPEN_MESSAGE = "Czeœæ, co tam u Ciebie?";
	public static final String END_MESSAGE = "Super, trzymaj siê agencie numer ";
	private int imAgentCount = 0;

	@Override
	protected void setup() {
		super.setup();
		addBehaviour(new RequestSender(this, INTERVAL_REQUEST));
		addBehaviour(new MessageProcessor());
	}

	class MessageProcessor extends CyclicBehaviour {

		@Override
		public void action() {
			ACLMessage msg = receive();
			if (msg != null && msg.getSender().getLocalName().contains(IMAgent.PREFIX_AGENT)) {
				System.out.println("\n Wiadomoœæ dla agenta " + myAgent.getLocalName() + " od "
						+ msg.getSender().getLocalName() + "\n\t" + msg.getContent() + "\n");
				ACLMessage reply = msg.createReply();
				reply.setPerformative(ACLMessage.INFORM);
				String agentName = msg.getSender().getLocalName();
				reply.setContent(END_MESSAGE + (agentName.replace(IMAgent.PREFIX_AGENT, "")));
				send(reply);
			}

			block();
		}
	}

	class RequestSender extends TickerBehaviour {

		private int actualAgent = 0;

		public RequestSender(Agent a, long period) {
			super(a, period);
		}

		@Override
		protected void onTick() {
			DFAgentDescription[] result = null;
			try {
				result = IMAgent.getDFAgents(IMMasterAgent.this);
			} catch (FIPAException e) {
				e.printStackTrace();
			}
			if (result != null & result.length > 0) {
				ACLMessage wiadomosc = new ACLMessage(ACLMessage.REQUEST);
				if (actualAgent > result.length - 1)
					actualAgent = 0;
				/*
				 * for (int i = 0; i < result.length; i++) {
				 * wiadomosc.addReceiver(result[i].getName()); }
				 */
				try {
					wiadomosc.addReceiver(result[actualAgent].getName());
					actualAgent++;
					wiadomosc.setContent(OPEN_MESSAGE);
					myAgent.send(wiadomosc);
				} catch (Exception e) {
					e.printStackTrace(System.out);
				}
			}
		}
	}
}
