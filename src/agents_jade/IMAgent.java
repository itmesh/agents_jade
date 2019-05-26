package agents_jade;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

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
	public static final String OPEN_MESSAGE = "Wysy³am wiadomoœæ";
	// private static final String REPLY_MESSAGE = "Otrzymana";
	DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

	public IMAgent messageReceiver;
	public boolean requestTokenFlag = false;
	private OnNext onNext;

	@Override
	protected void setup() {
		super.setup();
		addBehaviour(new Receiver());
		RingControler.instance.addAgent(this);
	}

	class Receiver extends CyclicBehaviour {

		@Override
		public void action() {
			ACLMessage msg = receive();
			if (msg != null) {
				if (msg.getSender().getLocalName().contains(PREFIX_AGENT)) {
					System.out.println("\n Time: " + dateFormat.format(new Date()) + "  From: "
							+ msg.getSender().getLocalName() + "  To: " + myAgent.getLocalName() + "  length: "
							+ msg.getContent().length() + "\n");
				}
			}

			block();
		}
	}

	@Override
	public void doDelete() {
		super.doDelete();
		RingControler.instance.deleteAgent(this);
		if(requestTokenFlag) {
			requestTokenFlag = false;
			onNext.onNext();
		}
	}

	public void queueMessageToAgent(IMAgent agent, OnNext onNext) {
		messageReceiver = agent;
		this.onNext = onNext;
		requestTokenFlag = true;
	}

	@Override
	public void doActivate() {
		super.doActivate();
		RingControler.instance.addAgent(this);
	}
	
	@Override
	public void doSuspend() {
		super.doSuspend();
		RingControler.instance.deleteAgent(this);
		if(requestTokenFlag) {
			requestTokenFlag = false;
			onNext.onNext();
		}
	}
	
	public void exec() {
		if (requestTokenFlag) {
			ACLMessage wiadomosc = new ACLMessage(ACLMessage.REQUEST);
			wiadomosc.addReceiver(messageReceiver.getAID());
			wiadomosc.setContent(OPEN_MESSAGE);
			send(wiadomosc);
			requestTokenFlag = false;
			onNext.onNext();
		}
	}
}
