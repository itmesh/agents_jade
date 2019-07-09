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
	//nastêpny agent w ringu
	private String nextAgentUID;
	//task który mo¿e zostaæ przypisany do agenta w celu jego wykonania,
	//taskiem jest wys³anie wiadomoœci do innego agenta
	private MessageTask messageTask = null;

	@Override
	protected void setup() {
		super.setup();
		//rejestracja odbioru wiadomoœci
		addBehaviour(new Receiver());
		//rejestracja taska odpowiadaj¹cego za sprawdzenie 
		//czy agent posiada tokena, czy ma task do wykonania i za przekazanie tokena dalej
		addBehaviour(new TokenProvider(this, TOKEN_CHECK_PERIOD));
		
		//ustawianie ID agenta czyli ca³a jego nazwa
		agentUID = getLocalName();
		//pobranie priorytetu z nazwy
		agentPriority = extractPriority();

		System.out.println("Agent UID: " + agentUID);
		System.out.println("Agent priority: " + agentPriority);
		try {
			//rejestracja agenta w ringu
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
					//tworzenie taska aby w momencie tokena wys³aæ wiadomoœæ do jakiegoœ agenta
					//dane taska s¹ wysy³ane z poziomu GUI jade 
					//przyklad wiadomoœæ do zdefiniowania taska (wys³anie wiadomoœci do agenta AGENT_1_#P1)
					//				#MSGTASK wiadomoœæ #UID AGENT_1_#P1
					messageTask = new MessageTask(msg.getContent());
				} else if (msg.getContent().contains(MessageTypeSygnature.TOKEN)) {
					// wiadomoœæ ¿e agent posiada token
					System.out.println("-------------------------------------------------Agent " + getLocalName() + " has token");
					hasTokenFlag = true;
				} else if (msg.getContent().contains(MessageTypeSygnature.NEXTUID)) {
					//wiadomoœæ o aktualizacji nastêpnika w ringu
					String nextUID = msg.getContent().substring(MessageTypeSygnature.NEXTUID.length());
					System.out.println("Setting new nextUID: " + nextUID);
					nextAgentUID = nextUID;
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
			//sprawdza czy ma token, jak tak to sprawdz czy jest task do wykonania, je¿eli tak wykonaj go,
			//nastêpnie przeka¿ token dalej
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

	//wykonanie zdefiniowanego taska
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
		
		//reset taska
		messageTask = null;
	}

	//przekazanie tokena do nastêpnego agenta
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

	//rejestracja Servisu aby mieæ dostêp do listy wszystkich agentów
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

		//pobranie wszystkich agentow
		DFAgentDescription[] result = DFService.search(this, dfd);

		if (result != null && result.length > 0) {
			//sortowanie wed³ug priorytetu
			Arrays.sort(result, new Comparator<DFAgentDescription>() {

				@Override
				public int compare(DFAgentDescription o1, DFAgentDescription o2) {
					return extractPriority(o1.getName().getLocalName()) <= extractPriority(o2.getName().getLocalName())
							? 1
							: -1;
				}

			});
			
			for (DFAgentDescription df : result) {
				System.out.println(df.getName().getLocalName());
			}
			//przypisywanie nastêpników do odpowienich agentów na podstawie priorytetów
			//
			for (int i = 0; i < result.length; i++) {
				if (extractPriority(result[i].getName().getLocalName()) <= agentPriority || i == result.length - 1) {
					

					ACLMessage message = new ACLMessage(ACLMessage.INFORM);
					message.setContent(MessageTypeSygnature.NEXTUID + getLocalName());
					message.addReceiver(result[i].getName());
					send(message);
					if (i == 0) {
						nextAgentUID = result[result.length - 1].getName().getLocalName();
						System.out.println();
					} else if (i - 1 < result.length) {
						nextAgentUID = result[i - 1].getName().getLocalName();
					} else {
						nextAgentUID = result[0].getName().getLocalName();
					}
					System.out.println("Next agent: " + nextAgentUID);
					break;
				}

			}
		} else {
			//je¿eli to jest 1 agent w ringu ustaw token dla tego agenta
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
}
