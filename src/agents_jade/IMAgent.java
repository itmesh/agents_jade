package agents_jade;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.ParseException;
import jade.lang.acl.UnreadableException;

public class IMAgent extends Agent {

	public static final String PREFIX_AGENT = "IM_AGENT_";
	private static final String REPLY_MESSAGE = "Wszystko w porz¹dku";

	@Override
	protected void setup() {
		super.setup();
		register();
		addBehaviour(new ResponseSender());
	}

	class ResponseSender extends CyclicBehaviour {

		@Override
		public void action() {
			ACLMessage msg = receive();
			if (msg != null) {
				if (msg.getSender().getLocalName().contains(IMMasterAgent.PREFIX_AGENT)) {
					System.out.println("\n Wiadomoœæ dla agenta " + myAgent.getLocalName() + " od "
							+ msg.getSender().getLocalName() + "\n\t" + msg.getContent() + "\n");
					if (msg.getPerformative() == ACLMessage.REQUEST) {
						ACLMessage reply = msg.createReply();
						reply.setPerformative(ACLMessage.INFORM);
						reply.setContent(REPLY_MESSAGE);
						send(reply);
					}
				}
			}

			block();
		}
	}

	public static DFAgentDescription[] getDFAgents(Agent agent) throws FIPAException {
		DFAgentDescription dfd = new DFAgentDescription();
		ServiceDescription sd = new ServiceDescription();
		sd.setType(PREFIX_AGENT);
		dfd.addServices(sd);

		DFAgentDescription[] result = DFService.search(agent, dfd);

		return result;
	}

	private void register() {
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setName(getLocalName());
		sd.setType(PREFIX_AGENT);
		dfd.addServices(sd);
		try {
			DFService.register(this, dfd);
		} catch (FIPAException e) {
			e.printStackTrace();
		}
	}
}
