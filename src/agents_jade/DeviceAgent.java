package agents_jade;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.AMSService;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.AMSAgentDescription;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.ParseException;
import jade.lang.acl.UnreadableException;

/*
 * Name pattern Agent 
 * 
 * AGENT_1_#P1 - taki musi byæ format abym móg³ mieæ dostêp do id agenta oraz jego priorytetu w nazwie
 * 
 * #P1 - priority 1
 */

public class DeviceAgent extends Agent {

	private static final String AGENT_PREFIX = "AGENT_";
	private static final String PREFIX_PRIORITY = "#P";
	private static final DateFormat DATE_FORMAT = new SimpleDateFormat("HH:mm:ss");
	private static final String SD_TYPE = "DeviceAgent";

	private static final long TOKEN_CHECK_PERIOD = 3000;

	private String agentUID;
	private int agentPriority;

	private boolean hasTokenFlag;
	// nastêpny agent w ringu
	private String nextAgentUID;
	private String prevAgentUID;
	// task który mo¿e zostaæ przypisany do agenta w celu jego wykonania,
	// taskiem jest wys³anie wiadomoœci do innego agenta
	private MessageTask messageTask = null;
	private boolean toRegister = false;

	@Override
	protected void setup() {
		super.setup();
		// rejestracja odbioru wiadomoœci
		addBehaviour(new Receiver());
		// rejestracja taska odpowiadaj¹cego za sprawdzenie
		// czy agent posiada tokena, czy ma task do wykonania i za przekazanie tokena
		// dalej
		addBehaviour(new TokenProvider(this, TOKEN_CHECK_PERIOD));

		// ustawianie ID agenta czyli ca³a jego nazwa
		agentUID = getLocalName();
		// pobranie priorytetu z nazwy
		agentPriority = extractPriority();

		System.out.println("Agent UID: " + agentUID);
		System.out.println("Agent priority: " + agentPriority);
		try {
			// rejestracja agenta w ringu
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
				if (msg.getContent().contains(MessageTypeSygnature.MSGTASK)) {
					System.out.println("\n Time: " + DATE_FORMAT.format(new Date()) + "  From: "
							+ msg.getSender().getLocalName() + "  To: " + myAgent.getLocalName() + "  length: "
							+ msg.getContent().length() + "\n");

					// tworzenie taska aby w momencie tokena wys³aæ wiadomoœæ do jakiegoœ agenta
					// dane taska s¹ wysy³ane z poziomu GUI jade
					// przyklad wiadomoœæ do zdefiniowania taska (wys³anie wiadomoœci do agenta
					// AGENT_1_#P1)
					// #MSGTASK wiadomoœæ #UID AGENT_1_#P1
					messageTask = new MessageTask(msg.getContent());
				} else if (msg.getContent().contains(MessageTypeSygnature.TOKEN)) {
					// wiadomoœæ ¿e agent posiada token
					System.out.println(
							"-------------------------------------------------Agent " + getLocalName() + " has token");
					hasTokenFlag = true;
				} else if (msg.getContent().contains(MessageTypeSygnature.NEXTUID)) {
					// wiadomoœæ o aktualizacji nastêpnika w ringu
					String nextUID = msg.getContent().substring(MessageTypeSygnature.NEXTUID.length());
					System.out.println("Setting new nextUID: " + nextUID + " for agent " + getLocalName());
					nextAgentUID = nextUID.equals(getLocalName()) ? null : nextUID;
				} else if (msg.getContent().contains(MessageTypeSygnature.PREVUID)) {
					// wiadomoœæ o aktualizacji poprzednika w ringu
					String prevUID = msg.getContent().substring(MessageTypeSygnature.PREVUID.length());
					System.out.println("Setting new prevUID: " + prevUID + " for agent " + getLocalName());
					prevAgentUID = prevUID.equals(getLocalName()) ? null : prevUID;
				}

			}
			block();
		}

	}

	class TokenProvider extends TickerBehaviour {

		public TokenProvider(Agent a, long period) {
			super(a, period);
		}

		@Override
		protected void onTick() {
			// sprawdza czy ma token, jak tak to sprawdz czy jest task do wykonania, je¿eli
			// tak wykonaj go,
			// nastêpnie przeka¿ token dalej
			if (hasTokenFlag) {
				if (messageTask != null)
					executeMessageTask();
				rotateToken();
			}
			if (toRegister) {
				try {
					registerAgentInRing();
				} catch (FIPAException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}

	}

	@Override
	public void doDelete() {
		System.out.println("doDelete");
		unregisterAgentFromRing();
		super.doDelete();
	}

	@Override
	public void doActivate() {
		super.doActivate();
		System.out.println("doActivate");
	}

	@Override
	public void doSuspend() {
		unregisterAgentFromRing();
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

	// wykonanie zdefiniowanego taska
	private void executeMessageTask() {
		System.out.println("Sending message to agent: " + messageTask.receiverUID);
		System.out.println("\t\tmessage: " + messageTask.message + "\n");
		try {
			ACLMessage message = new ACLMessage(ACLMessage.INFORM);
			message.addReceiver(new AID(messageTask.receiverUID, AID.ISLOCALNAME));
			message.setContent(MessageTypeSygnature.INFORM + messageTask.message);
			send(message);
		} catch (Exception e) {
			e.printStackTrace(System.out);
		}

		// reset taska
		messageTask = null;
	}

	// przekazanie tokena do nastêpnego agenta
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

	// rejestracja Servisu aby mieæ dostêp do listy wszystkich agentów
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

	public void unregisterAgentFromRing() {
		toRegister = true;
		if (nextAgentUID != null && prevAgentUID != null) {
			ACLMessage message = new ACLMessage(ACLMessage.INFORM);
			message.setContent(MessageTypeSygnature.NEXTUID + nextAgentUID);
			message.addReceiver(new AID(prevAgentUID, AID.ISLOCALNAME));
			send(message);
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			ACLMessage message2 = new ACLMessage(ACLMessage.INFORM);
			message2.setContent(MessageTypeSygnature.PREVUID + prevAgentUID);
			message2.addReceiver(new AID(nextAgentUID, AID.ISLOCALNAME));
			send(message2);

			if (hasTokenFlag) {
				hasTokenFlag = false;
				rotateToken();
			}
		}
	}

	public void registerAgentInRing() throws FIPAException {
		toRegister = false;

		AMSAgentDescription[] agents = null;
		ArrayList<AMSAgentDescription> agentsFiltered = new ArrayList<>();

		try {
			SearchConstraints c = new SearchConstraints();
			c.setMaxResults(new Long(-1));
			agents = AMSService.search(this, new AMSAgentDescription(), c);
		} catch (Exception e) {
			e.printStackTrace();
		}
		// pobranie wszystkich agentow
		if (agents != null) {
			for (AMSAgentDescription ams : agents) {
				if (ams.getName().getLocalName().contains(AGENT_PREFIX)
						&& !ams.getName().getLocalName().contains(getLocalName())
						&& ams.getState().equals(AMSAgentDescription.ACTIVE)) {
					agentsFiltered.add(ams);
				}
			}
		}
		if (agentsFiltered.size() > 0) {
			// sortowanie wed³ug priorytetu
			agentsFiltered.sort(new Comparator<AMSAgentDescription>() {

				@Override
				public int compare(AMSAgentDescription o1, AMSAgentDescription o2) {
					return extractPriority(o1.getName().getLocalName()) <= extractPriority(o2.getName().getLocalName())
							? 1
							: -1;
				}

			});

			// przypisywanie nastêpników do odpowienich agentów na podstawie priorytetów
			//
			for (int i = 0; i < agentsFiltered.size(); i++) {
				if (extractPriority(agentsFiltered.get(i).getName().getLocalName()) <= agentPriority
						|| i == agentsFiltered.size() - 1) {

					ACLMessage message = new ACLMessage(ACLMessage.INFORM);
					message.setContent(MessageTypeSygnature.NEXTUID + getLocalName());
					message.addReceiver(agentsFiltered.get(i).getName());
					send(message);

					prevAgentUID = agentsFiltered.get(i).getName().getLocalName();
					if (i == 0) {
						nextAgentUID = agentsFiltered.get(agentsFiltered.size() - 1).getName().getLocalName();
					} else if (i - 1 < agentsFiltered.size()) {
						nextAgentUID = agentsFiltered.get(i - 1).getName().getLocalName();
					} else {
						nextAgentUID = agentsFiltered.get(0).getName().getLocalName();
					}
					System.out.println("Next agent: " + nextAgentUID);
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					ACLMessage message2 = new ACLMessage(ACLMessage.INFORM);
					message2.setContent(MessageTypeSygnature.PREVUID + getLocalName());
					message2.addReceiver(new AID(nextAgentUID, AID.ISLOCALNAME));
					send(message2);
					break;
				}
			}

		} else {
			// je¿eli to jest 1 agent w ringu ustaw token dla tego agenta
			hasTokenFlag = true;
		}

		// register();
	}
}

class MessageTask {
	private static final String UID_INDEX = "#UID ";

	String message;
	String receiverUID;

	MessageTask(String content) {
		int startMessageIndex = MessageTypeSygnature.MSGTASK.length();
		int endMessageIndex = content.indexOf(UID_INDEX) - 1;
		int startReceiverUIDIndex = content.indexOf(UID_INDEX) + UID_INDEX.length();
		message = content.substring(startMessageIndex, endMessageIndex);
		receiverUID = content.substring(startReceiverUIDIndex);
	}
}

//klasa do rozró¿niania typów wiadomoœci
class MessageTypeSygnature {
	public static final String INFORM = "#INFORM ";
	public static final String TOKEN = "#TOKEN ";
	public static final String PREVUID = "#PREVUID ";
	public static final String NEXTUID = "#NEXTUID ";
	public static final String MSGTASK = "#MSGTASK ";
	public static final String NEAR = "#MSGTASK ";
}
