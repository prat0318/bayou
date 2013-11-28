import java.util.*;
import java.util.logging.Level;

/**
 * Created with IntelliJ IDEA.
 * User: bansal
 */
public class Replica extends Process {

//    int clock = 0;
    public PlayList playList;

    Set<BayouCommandMessage> writeLog = new TreeSet<BayouCommandMessage>(new Comparator<BayouCommandMessage>(){
        public int compare(BayouCommandMessage a, BayouCommandMessage b){
//            if(a.equals(b)) return 0;
            return a.compare(b);
        }
    });

    int csn = 0;
    boolean primary;
    ProcessId myGossiper;
    Map<ProcessId, Integer> versionVector = new HashMap<ProcessId, Integer>();

    public Replica(Env env, ProcessId me, boolean primary) {
        this.env = env;
        this.me = me;
        playList = new PlayList();
        this.primary = primary;
        setLogger();
        loadProp();
        env.addProc(me, this);
    }

    public int getPositionInWriteLog(BayouCommandMessage b) {
        Iterator<BayouCommandMessage> i = writeLog.iterator();
        int index = 1;
        while(i.hasNext()) {
            if(i.next().equals(b)) return index;
            index++;
        }
        return 0;
    }

    public boolean isItTheBiggest(BayouCommandMessage b) {
        if(isSentFromClient(b)) return true;
        Iterator<BayouCommandMessage> i = writeLog.iterator();
        while(i.hasNext()) if(i.next().compare(b) > 0) return false;
        return true;
    }

    public void body() {
        if (!primary)
            giveMeAName();
        else {
            versionVector.put(me, 1);
            start_gossip_thread();
        }
        logger.log(messageLevel, "Here I am: " + me);

        while (!stop_request()) {
            BayouMessage rawMsg = getNextMessage();
            BayouCommandMessage msg = rawMsg.bayouCommandMessage;
            //DROP A MESSAGE IF COMMAND IS PRESENT AND IS AHEAD OF YOUR VERSION VECTOR
            if(checkForCSN(msg) && !isSentFromClient(msg)) {
                logger.log(messageLevel, "Ignoring message, Already have " + msg);
                continue;
            }
//            if (msg.command != null && msg.command.acceptStamp != null) {
//                //ToDo: EXTRACT THE CSN OUT OF COMMAND
//
//                Integer currentClock = versionVector.get(msg.command.acceptStamp.replica);
//                if (!my_first_request_name_response(msg))
//                    if ((currentClock != null && currentClock < msg.command.acceptStamp.acceptClock) ||
//                            (currentClock == null && msg.command.acceptStamp.acceptClock != 1)) {
//                        logger.log(messageLevel, "Dropping message, currClock=" + currentClock + " " + msg.command);
//                        continue;
//                    }
//                //ToDo: [NOT DOING RIGHT NOW] IF CANNOT BE ADDED AT THE END OF
//                //ToDo:  WRITE_LOG, POP ALL WRITE_LOGS AND
//                //ToDo: AND STORE IN SOME STACK - HANDLE ROLLBACK OF STATES
//            }
            if(isItTheBiggest(msg))
                takeActionOnMessage(rawMsg);
            else {               //Recreate the playlist
                playList.clear();
                takeActionOnPreviousMessages(rawMsg);
                takeActionOnMessage(rawMsg);
                takeActionOnNextMessages(rawMsg);
            }
        }
    }

    private void takeActionOnNextMessages(BayouMessage rawMsg) {
        Iterator<BayouCommandMessage> i = writeLog.iterator();
        while(i.hasNext()) {
            BayouCommandMessage msg = i.next();
            if(msg.compare(rawMsg.bayouCommandMessage) <= 0) continue;
            if(msg instanceof RequestMessage) takeActionOnMessage(new BayouMessage(me, msg));
        }
    }

    private void takeActionOnPreviousMessages(BayouMessage rawMsg) {
        Iterator<BayouCommandMessage> i = writeLog.iterator();
        while(i.hasNext()) {
            BayouCommandMessage msg = i.next();
            if(msg.compare(rawMsg.bayouCommandMessage) >= 0) continue;
            if(msg instanceof RequestMessage) takeActionOnMessage(new BayouMessage(me, msg));
        }
    }
    private void takeActionOnMessage(BayouMessage rawMsg) {
        BayouCommandMessage msg = rawMsg.bayouCommandMessage;
        boolean sentFromClient = isSentFromClient(msg);

        if (msg instanceof RequestMessage) {
            RequestCommand c = (RequestCommand) ((RequestMessage) msg).command;
            if(sentFromClient)
                c.updateAcceptStamp(versionVector.get(me), me);
            playList.action(c);
            logger.log(messageLevel, "PERFORMED " + c + "OUTPUT :" + c.response);
            if(sentFromClient) //if (rawMsg.src.equals(c.client))  //Only if I was the first replica then reply
                sendMessage(c.client, new BayouMessage(me, new ResponseMessage(c)));
            addToLog(msg);
        } else if (msg instanceof RequestNameMessage) {
            RequestNameMessage message = (RequestNameMessage) msg;
            if (message.command != null) {
                if (message.my_original_id.equals(me)) {
                    assign_given_name(message);
                    start_gossip_thread();
                }
                //ToDo: HANDLE RETIRED MESSAGE
                if (versionVector.get(message.my_original_id) == null)
                    versionVector.put(message.my_original_id, message.command.acceptStamp.acceptClock);
            } else {
                //Command null means sent first to you
                message.command = new Command();
                message.command.updateAcceptStamp(versionVector.get(me), me);
                versionVector.put(message.my_original_id, versionVector.get(me));
                sendMessage(rawMsg.src, new BayouMessage(me, message));
                //ToDo: SEND ALL YOUR WRITE LOGS TO THIS NEW REPLICA
            }
            addToLog(message);
        } else if (msg instanceof RetireMessage) {
            RetireMessage message = (RetireMessage) msg;
            if (message.command != null) { //The replica present in command is retiring
                versionVector.remove(message.command.acceptStamp.replica);
            } else { //Remove myself
                //ToDo: SEND ALL YOUR WRITE LOG TO ONE OF REPLICA, THEN BREAK ON ACK
                message.command = new Command(new AcceptStamp(versionVector.get(me), me));
            }
            addToLog(message);
        } else if (msg instanceof RequestSessionMessage) {
          RequestSessionMessage message = (RequestSessionMessage) msg;
            if(message.lastUpdatedStamp == null){
                //CHECK my vector clock contains the required accept Stamp
                sendMessage(rawMsg.src,new BayouMessage(me, new SessionReplyMessage(true)));
            }else {
                if(versionVector.containsKey(message.lastUpdatedStamp.replica) &&
                        (versionVector.get(message.lastUpdatedStamp.replica) >= (message.lastUpdatedStamp.acceptClock) ))
                sendMessage(rawMsg.src,new BayouMessage(me, new SessionReplyMessage(true)));
                else
                    sendMessage(rawMsg.src,new BayouMessage(me, new SessionReplyMessage(false)));
            }
        } else {
            logger.log(Level.SEVERE, "Bayou.Replica: unknown msg type");
        }
    }

    private boolean isSentFromClient(BayouCommandMessage msg) {
        return ((msg.command == null) || (msg.command.acceptStamp == null));
    }

    private boolean my_first_request_name_response(BayouCommandMessage message) {
        if (message instanceof RequestNameMessage) {
            RequestNameMessage requestNameMessage = (RequestNameMessage) message;
            if (requestNameMessage.my_original_id.equals(me) && requestNameMessage.command != null &&
                    !versionVector.containsKey(requestNameMessage.command.acceptStamp.replica))
                return true;
        }
        return false;
    }

    private int min_version_vector() {
        int min = Integer.MAX_VALUE;
        for(Integer i: versionVector.values())
            if(min > i) min = i;
        logger.log(messageLevel, "current VV: "+versionVector);
        return min;
    }

    private int max_version_vector() {
        int max = Integer.MIN_VALUE;
        for(Integer i: versionVector.values())
            if(max < i) max = i;
        return max;
    }

    private void addToLog(BayouCommandMessage msg) {
        writeLog.add(msg);

        //Assuming accept-clock will always be there in Command
        //Assuming that version vector has entry for this replica
        if(msg.command.acceptStamp.replica.equals(me))
            versionVector.put(me, 1 + versionVector.get(me));
        else
            versionVector.put(msg.command.acceptStamp.replica, msg.command.acceptStamp.acceptClock);
        //JUMP YOURSELF TO MAX VERSION VECTOR
        versionVector.put(me, max_version_vector());

        if(primary) {
            //assign csns
            Iterator<BayouCommandMessage> i = writeLog.iterator();
            int index = 1; int min = min_version_vector();
            while(i.hasNext()) {
                BayouCommandMessage singleMsg = i.next();
//                logger.log(messageLevel, min + " : " + singleMsg.command.csn + "::" + singleMsg.command.acceptStamp.acceptClock);
                if(singleMsg.command.csn == 0 && singleMsg.command.acceptStamp.acceptClock < min) {
                    singleMsg.command.csn = index;
                    logger.log(messageLevel, "MESSAGES STABLE TILL CSN:"+singleMsg.command.csn+" A# < "+min+" IN "+writeLog);
                }
                index++;
            }
        } else {
//            logger.log(messageLevel, "CSN :"+msg.command.csn+ "Position :"+getPositionInWriteLog(msg));
            if(msg.command.csn == getPositionInWriteLog(msg))
                logger.log(messageLevel, "MESSAGES STABLE TILL CSN:"+msg.command.csn+" WITH "+msg);
        }

        sendMessage(myGossiper, new BayouMessage(me, msg));
    }

    private boolean checkForCSN(BayouCommandMessage msg) {
        if(writeLog.contains(msg)){
            if(msg.command.csn == getWriteLogMsg(msg).command.csn)
                return true;
            if(msg.command.csn == getPositionInWriteLog(msg))
                logger.log(messageLevel, "MESSAGES STABLE TILL CSN:"+msg.command.csn+" WITH "+msg);
            writeLog.add(msg);
            return true;
        }
        return false;
    }

    private BayouCommandMessage getWriteLogMsg(BayouCommandMessage msg) {
        Iterator<BayouCommandMessage> i = writeLog.iterator();
        while(i.hasNext()) {
            BayouCommandMessage b = i.next();
            if(b.equals(msg)) return b;
        }
        return null;
    }

    private void start_gossip_thread() {
        Gossip Gossiper = new Gossip(this, new ProcessId("Gossiper:" + me));
        myGossiper = Gossiper.me;
    }

    private void assign_given_name(RequestNameMessage requestNameMessage) {
        me.name = requestNameMessage.command.acceptStamp.toString();
        env.dbProcs.put(me, this);
        env.procs.put(me, this);
    }


    private void giveMeAName() {
        sendMessage((ProcessId) env.dbProcs.keySet().toArray()[0], new BayouMessage(me, new RequestNameMessage(me)));
    }

}
