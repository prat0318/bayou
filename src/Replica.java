import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * Created with IntelliJ IDEA.
 * User: bansal
 */
public class Replica extends Process {

    int clock = 0;
    public PlayList playList;
    List<BayouCommandMessage> writeLog = new ArrayList<BayouCommandMessage>();
    int csn = -1;
    boolean primary;
    ProcessId myGossiper;
    Map<String, Integer> versionVector = new HashMap<String, Integer>();

    public Replica(Env env, ProcessId me, boolean primary) {
        this.env = env;
        this.me = me;
        playList = new PlayList();
        this.primary = primary;
        setLogger();
        loadProp();
        env.addProc(me, this);
    }


    public void body() {
        if(!primary)
            giveMeAName();
        else
            start_gossip_thread();
        logger.log(messageLevel, "Here I am: " + me);

        while (!stop_request()) {
            BayouMessage rawMsg = getNextMessage();
            BayouCommandMessage msg = rawMsg.bayouCommandMessage;
            //DROP A MESSAGE IF COMMAND IS PRESENT AND IS AHEAD OF YOUR VERSION VECTOR
            if(msg.command != null) {
                //ToDo: CHECK IF COMMAND IS ALREADY EXECUTED AND PRESENT IN MY WRITE_LOG
                System.out.println(msg.command.acceptStamp.replica);
                Integer currentClock = versionVector.get(msg.command.acceptStamp.replica.name);
                if(!my_first_request_name_response(msg))
                    if((currentClock != null && currentClock < msg.command.acceptStamp.acceptClock) ||
                       (currentClock == null && msg.command.acceptStamp.acceptClock != 1) ) {
                        logger.log(messageLevel, "Dropping message, currClock="+currentClock+" "+msg.command);
                        continue;
                    }
                //ToDo: IF CANNOT BE ADDED AT THE END OF WRITE_LOG, POP ALL WRITE_LOGS AND
                //ToDo: AND STORE IN SOME STACK - HANDLE ROLLBACK OF STATES
            }

            if (msg instanceof RequestMessage) {
                RequestCommand c = (RequestCommand)((RequestMessage) msg).command;
                c.updateAcceptStamp(clock, me);
                playList.action(c);
                logger.log(messageLevel, "PERFORMED " + c + "OUTPUT :" + c.response);
                if(rawMsg.src.equals(c.client))  //Only if I was the first replica then reply
                    sendMessage(c.client, new BayouMessage(me, new ResponseMessage(c)));
                addToLog(msg);
            } else if (msg instanceof RequestNameMessage) {
                RequestNameMessage message = (RequestNameMessage) msg;
                if(message.command != null) {
                    AcceptStamp acceptStamp = message.command.acceptStamp;

                    if(message.my_original_name.equals(me.name)) {
                        assign_given_name(message);
                        start_gossip_thread();
                    }
                    //ToDo: HANDLE RETIRED MESSAGE
                    if(versionVector.get(acceptStamp.replica.name) == null)
                        versionVector.put(acceptStamp.replica.name, acceptStamp.acceptClock);
                } else {                  //Command null means sent first to you
                    message.command = new Command();
                    message.command.updateAcceptStamp(clock, me);
                    versionVector.put(message.command.acceptStamp.toString(), clock);
                    sendMessage(rawMsg.src, new BayouMessage(me, message));
//                    sendMessage(message.src, new NameAssignedMessage(me, message.command.acceptStamp.toString()));
                    //ToDo: SEND ALL YOUR WRITE LOGS TO THIS NEW REPLICA
                }
                addToLog(message);
            } else if (msg instanceof RetireMessage) {
                RetireMessage message = (RetireMessage) msg;
                if(message.command != null) { //The replica present in command is retiring
                    versionVector.remove(message.command.acceptStamp.replica.name);
                } else { //Remove myself
                    //ToDo: SEND ALL YOUR WRITE LOG TO ONE OF REPLICA, THEN BREAK ON ACK
                    message.command = new Command(new AcceptStamp(clock, me));
                }
                addToLog(message);
            }else {
                logger.log(Level.SEVERE, "Bayou.Replica: unknown msg type");
            }
        }
    }

    private boolean my_first_request_name_response(BayouCommandMessage message) {
        if(message instanceof RequestNameMessage) {
            RequestNameMessage requestNameMessage = (RequestNameMessage)message;
            if(requestNameMessage.my_original_name.equals(me.name) && requestNameMessage.command != null &&
                    !versionVector.containsKey(requestNameMessage.command.acceptStamp))
                return true;
        }
        return false;
    }

    private void addToLog(BayouCommandMessage msg) {
        writeLog.add(msg);
        //ToDo: POP BACK ALL ITEMS FROM STACK TO WRITE LOG
        BayouMessage message = new BayouMessage(me, msg);
        sendMessage(myGossiper, message);
        clock++;
    }

    private void start_gossip_thread() {
        Gossip Gossiper = new Gossip(this, new ProcessId("Gossiper:"+me));
        myGossiper = Gossiper.me;
    }

    private void assign_given_name(RequestNameMessage requestNameMessage) {
        me.name = requestNameMessage.command.acceptStamp.toString();
        env.dbProcs.put(me,this);
    }


    private void giveMeAName() {
        sendMessage((ProcessId)env.dbProcs.keySet().toArray()[0], new BayouMessage(me, new RequestNameMessage(me)));
//        while (true) {
//            BayouMessage msg = getNextMessage();
//            if (msg instanceof NameAssignedMessage) {
//                NameAssignedMessage nameMessage = (NameAssignedMessage) msg;
//                env.dbProcs.remove(me);
//                me.name = nameMessage.name;
//                env.dbProcs.put(me,this);
//                break;
//            } else {
//                logger.log(Level.SEVERE, "Bayou.Replica: unknown msg type");
//            }
//        }
    }

}
