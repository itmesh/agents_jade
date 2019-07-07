package agents_jade;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.ParseException;
import jade.lang.acl.UnreadableException;

/*
 * Name pattern Agent 
 * 
 * DEVICENAME_#UID_1_#P_1
 * 
 * [DEVICENAME] device name, it can be any name
 * [#UID_n] unique ID of agent, important in communication, UID can be integer or String
 * 
 */

public class DeviceAgent extends Agent {

	private static final String AGENT_PREFIX = "AGENT_";
	private static final String PREFIX_UID = "#UID";
	private static final String PREFIX_PRIORITY = "#P";
	private static final String SENDING_MESSAGE_LOG = "Wysy³am wiadomoœæ do agenta: ";
	private static final DateFormat DATE_FORMAT = new SimpleDateFormat("HH:mm:ss");
	private static final String SD_TYPE = "DeviceAgent";

	private static final long TOKEN_CHECK_PERIOD = 3000;

	private String agentUID;
	private int agentPriority;

	private boolean hasTokenFlag;
	private String nextAgentUID;
	private MessageTask messageTask = null;

	@Override
	protected void setup() {
		super.setup();
		addBehaviour(new Receiver());
		addBehaviour(new TokenProvider(this, TOKEN_CHECK_PERIOD));

		// if (getArguments().length > 0)
		// agentPriority = Integer.valueOf((String) getArguments()[0]);

		agentUID = getLocalName();
		agentPriority = extractPriority();

		System.out.println("Agent UID: " + agentUID);
		System.out.println("Agent priority: " + agentPriority);
		try {
			registerAgentInRing();
		} catch (FIPAException e) {
			e.printStackTrace();
		}
	}

	class Receiver extends CyclicBehaviour {
		@Override
		public void action() {
			ACLMessage msg = receive();
			if (msg != null) {
				System.out.println(
						"\n Time: " + DATE_FORMAT.format(new Date()) + "  From: " + msg.getSender().getLocalName()
								+ "  To: " + myAgent.getLocalName() + "  length: " + msg.getContent().length() + "\n");

				if (msg.getContent().contains(MessageTypeSygnature.MSGTASK)) {
					messageTask = new MessageTask(msg.getContent());
				} else if (msg.getContent().contains(MessageTypeSygnature.TOKEN)) {
					System.out.println("---------------------------Agent " + getLocalName() + " has token");
					hasTokenFlag = true;
				} else if (msg.getContent().contains(MessageTypeSygnature.NEXTUID)) {
					String nextUID = msg.getContent().substring(MessageTypeSygnature.NEXTUID.length());
					System.out.println("Setting new nextUID: " + nextUID);
					nextAgentUID = nextUID;
				}

			}
			/*
			 * if (msg.getSender().getLocalName().contains(PREFIX_AGENT)) {
			 * System.out.println("\n Time: " + dateFormat.format(new Date()) + "  From: " +
			 * msg.getSender().getLocalName() + "  To: " + myAgent.getLocalName() +
			 * "  length: " + msg.getContent().length() + "\n"); }
			 */
			block();
		}

	}

	class TokenProvider extends TickerBehaviour {

		public TokenProvider(Agent a, long period) {
			super(a, period);
		}

		@Override
		protected void onTick() {
			if (hasTokenFlag) {
				if (messageTask != null)
					executeMessageTask();
				rotateToken();
			}
		}

	}

	@Override
	public void doDelete() {
		super.doDelete();
		System.out.println("doDelete");
	}

	@Override
	public void doActivate() {
		super.doActivate();
		System.out.println("doActivate");
	}

	@Override
	public void doSuspend() {
		super.doSuspend();
		System.out.println("doSuspend");
	}
	/*
	 * public void exec() { if (requestTokenFlag) { ACLMessage message = new
	 * ACLMessage(ACLMessage.REQUEST); message.addReceiver(message.getAID());
	 * message.setContent(OPEN_MESSAGE);
	 * message.setPerformative(ACLMessage.PROPAGATE); send(message);
	 * requestTokenFlag = false; onNext.onNext(); } }
	 */

	/*
	 * private String extractAgentUID() { return extractAgentUID(getLocalName()); }
	 * 
	 * private String extractAgentUID(String agentName) { int startIndex =
	 * agentName.indexOf(PREFIX_UID) + PREFIX_UID.length(); int endIndex =
	 * agentName.indexOf(PREFIX_PRIORITY) - 1; System.out.println("UID: " +
	 * agentName.substring(startIndex, endIndex)); return
	 * agentName.substring(startIndex, endIndex); }
	 */

	private int extractPriority(String agentName) {
		int startIndex = agentName.indexOf(PREFIX_PRIORITY) + PREFIX_PRIORITY.length();
		return Integer.valueOf(agentName.substring(startIndex));
	}

	private int extractPriority() {
		return extractPriority(getLocalName());
	}

	private void executeMessageTask() {
		System.out.println("Sending message to agent: " + messageTask.receiverUID);
		System.out.println("\t\tmessage: " + messageTask.message);
		try {
			ACLMessage message = new ACLMessage(ACLMessage.INFORM);
			message.addReceiver(new AID(messageTask.receiverUID, AID.ISLOCALNAME));
			message.setContent(MessageTypeSygnature.INFORM + messageTask.message);
			// send(message);
		} catch (Exception e) {
			e.printStackTrace(System.out);
		}
		messageTask = null;
	}

	private void rotateToken() {
		if (nextAgentUID != null && !nextAgentUID.equals(getLocalName())) {
			System.out.println("Rotating token to Agent: " + nextAgentUID);
			try {
				ACLMessage message = new ACLMessage(ACLMessage.INFORM);
				message.addReceiver(new AID(nextAgentUID, AID.ISLOCALNAME));
				message.setContent(MessageTypeSygnature.TOKEN);
				hasTokenFlag = false;
				send(message);
			} catch (Exception e) {
				e.printStackTrace(System.out);
			}
		} else {
			System.out.println("There is no next agent in token ring");
		}
	}

	public void register() {
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setName(getLocalName());
		sd.setType(SD_TYPE);
		dfd.addServices(sd);
		try {
			DFService.register(this, dfd);
		} catch (FIPAException e) {
			e.printStackTrace();
		}
	}

	public void registerAgentInRing() throws FIPAException {
		DFAgentDescription dfd = new DFAgentDescription();
		ServiceDescription sd = new ServiceDescription();
		sd.setType(SD_TYPE);
		dfd.addServices(sd);

		DFAgentDescription[] result = DFService.search(this, dfd);

		if (result != null && result.length > 0) {
			Arrays.sort(result, new Comparator<DFAgentDescription>() {

				@Override
				public int compare(DFAgentDescription o1, DFAgentDescription o2) {
					return extractPriority(o1.getName().getLocalName()) <= extractPriority(o2.getName().getLocalName())
							? 1
							: -1;
				}

			});
			System.out.println(result);
			for (int i = 0; i < result.length; i++) {
				if (extractPriority(result[i].getName().getLocalName()) <= agentPriority) {

					ACLMessage message = new ACLMessage(ACLMessage.INFORM);
					message.setContent(MessageTypeSygnature.NEXTUID + getLocalName());
					message.addReceiver(result[i].getName());
					send(message);
					if (i == 0) {
						nextAgentUID = result[result.length - 1].getName().getLocalName();
					} else if (i - 1 < result.length) {
						nextAgentUID = result[i + 1].getName().getLocalName();
					} else {
						nextAgentUID = result[0].getName().getLocalName();
					}
					break;
				}

			}
		} else {
			hasTokenFlag = true;
		}

		register();
	}
}

class MessageTask {
	private static final String UID_INDEX = "#UID ";

	String message;
	String receiverUID;

	MessageTask(String content) {
		int startMessageIndex = MessageTypeSygnature.MSGTASK.length();
		int endMessageIndex = content.indexOf(UID_INDEX) - 2;
		int startReceiverUIDIndex = content.indexOf(UID_INDEX) + UID_INDEX.length();
		message = content.substring(startMessageIndex, endMessageIndex);
		receiverUID = content.substring(startReceiverUIDIndex);
	}
}

class MessageTypeSygnature {
	public static final String INFORM = "#INFORM ";
	public static final String TOKEN = "#TOKEN ";
	public static final String PREVUID = "#PREVUID ";
	public static final String NEXTUID = "#NEXTUID ";
	public static final String MSGTASK = "#MSGTASK ";
}
