import java.util.*;
import java.util.logging.Level;

/**
 * Created with IntelliJ IDEA.
 * User: bansal
 */
public class Replica extends Process {

    public PlayList playList;

    Set<BayouCommandMessage> writeLog = new TreeSet<BayouCommandMessage>(new Comparator<BayouCommandMessage>() {
        public int compare(BayouCommandMessage a, BayouCommandMessage b) {
            return a.compare(b);
        }
    });

    int maxCsn = Integer.MAX_VALUE;
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
        while (i.hasNext()) {
            if (i.next().equals(b)) return index;
            index++;
        }
        return 0;
    }

    public boolean isItTheBiggest(BayouCommandMessage b) {
        if (isSentFromClient(b)) return true;
        Iterator<BayouCommandMessage> i = writeLog.iterator();
        while (i.hasNext()) if (i.next().compare(b) > 0) return false;
        return true;
    }

    public void body() {
        if (!primary) {
            if (!giveMeAName()){
              return;    //Could not find any sever which is alive so killing itself..
            }
        } else {
            versionVector.put(me, 1);
            start_gossip_thread();
        }
        logger.log(messageLevel, "Here I am: " + me);

        while (!stop_request()) {
            BayouMessage rawMsg = getNextMessage();
            BayouCommandMessage msg = rawMsg.bayouCommandMessage;
            //DROP A MESSAGE IF COMMAND IS PRESENT AND IS AHEAD OF YOUR VERSION VECTOR
            if (!isSentFromClient(msg) && checkForCSN(msg)) {
                logger.log(messageLevel, "Ignoring message, Already have " + msg);
                continue;
            }
            if (isItTheBiggest(msg)) {
                if (!takeActionOnMessage(rawMsg)) {
                    printMyState();
                    break;
                }
            } else {               //Recreate the playlist
                playList.clear();
                takeActionOnPreviousMessages(rawMsg);
                takeActionOnMessage(rawMsg);
                takeActionOnNextMessages(rawMsg);
            }
        }
    }

    public void printMyState() {
        String log = "";
        Iterator<BayouCommandMessage> i = writeLog.iterator();
        while (i.hasNext()) {
            log = log + i.next().toString() + "\n";
        }
        logger.log(messageLevel, "\n*****************************************\n" +
                "LATEST COMMIT SEQ. NO. : " + maxCsn + "\n" +
                "CURRENT VERSION VECTOR : " + versionVector + "\n" +
                "MESSAGES in Write LOG  : " + log + "\n" +
                "CURRENT PLAYLIST       : " + playList.show() + "\n" +
                "*****************************************");
    }

    private void takeActionOnNextMessages(BayouMessage rawMsg) {
        Iterator<BayouCommandMessage> i = writeLog.iterator();
        while (i.hasNext()) {
            BayouCommandMessage msg = i.next();
            if (msg.compare(rawMsg.bayouCommandMessage) <= 0) continue;
            if (msg instanceof RequestMessage) takeActionOnMessage(new BayouMessage(me, msg));
        }
    }

    private void takeActionOnPreviousMessages(BayouMessage rawMsg) {
        Iterator<BayouCommandMessage> i = writeLog.iterator();
        while (i.hasNext()) {
            BayouCommandMessage msg = i.next();
            if (msg.compare(rawMsg.bayouCommandMessage) >= 0) continue;
            if (msg instanceof RequestMessage) takeActionOnMessage(new BayouMessage(me, msg));
        }
    }

    /**
     * Returns False when the server is supposed to retire
     *
     * @param rawMsg
     * @return
     */
    private boolean takeActionOnMessage(BayouMessage rawMsg) {
        BayouCommandMessage msg = rawMsg.bayouCommandMessage;
        boolean sentFromClient = isSentFromClient(msg);

        if (msg instanceof RequestMessage) {
            RequestCommand c = (RequestCommand) ((RequestMessage) msg).command;
            if (sentFromClient)
                c.updateAcceptStamp(versionVector.get(me), me);
            boolean oP = playList.action(c);
            logger.log(messageLevel, "PERFORMED " + c + "OUTPUT :" + c.response);
            if (sentFromClient) //if (rawMsg.src.equals(c.client))  //Only if I was the first replica then reply
                sendMessage(c.client, new BayouMessage(me, new ResponseMessage(c, oP)));
            if (oP) {
                addToLog(msg);
            }
        } else if (msg instanceof NoOpMessage) {
            logger.log(messageLevel, "Rcvd a NO-OP message from " + rawMsg.src);
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
                if (message.nextPrimaryId == me) {
                    this.primary = true;
                    logger.log(messageLevel, "Promoting to primary");
                }
                versionVector.remove(message.command.acceptStamp.replica);
                logger.log(messageLevel, "Deleting process from Version Vector " + versionVector);
                addToLog(message);
            } else { //Remove myself
                //ToDo: SEND ALL YOUR WRITE LOG TO ONE OF REPLICA, THEN BREAK ON ACK
                Set<ProcessId> keys = new TreeSet<ProcessId>(versionVector.keySet());
                keys.remove(me);
                if (keys.size() == 0) {
                    System.out.println("Can retire without Entropy as no other sever is Alive");
                }
                for (ProcessId p : keys) {
                    if (checkDbCanBeConnectedTo(p) && !env.dbProcs.get(p).cannotRetire) {
                        Process entropyWithReplica = env.dbProcs.get(p);
                        isRetiring = true;
                        entropyWithReplica.cannotRetire = true;
                        Gossip gossiper = (Gossip) env.procs.get(myGossiper);
                        message.command = new Command(new AcceptStamp(versionVector.get(me), me));
                        if (primary) {
                            message.nextPrimaryId = p;
                        }

                        writeLog.add(message);
                        gossiper.sendAllWriteLogTo(p);
                        logger.log(Level.WARNING, me + " RETIRING AFTER SENDING LOG's to " + p);
                        entropyWithReplica.cannotRetire = false;
                        return false;
                    }
                }
                System.out.println("Can not retire without Entropy as connection with other Alive severs is not possible");
                return true;
            }
        } else if (msg instanceof RequestSessionMessage) {
            RequestSessionMessage message = (RequestSessionMessage) msg;
            if (message.lastUpdatedStamp == null) {
                //CHECK my vector clock contains the required accept Stamp
                sendMessage(rawMsg.src, new BayouMessage(me, new SessionReplyMessage(true)));
            } else {
                if (versionVector.containsKey(message.lastUpdatedStamp.replica) &&
                        (versionVector.get(message.lastUpdatedStamp.replica) >= (message.lastUpdatedStamp.acceptClock)))
                    sendMessage(rawMsg.src, new BayouMessage(me, new SessionReplyMessage(true)));
                else
                    sendMessage(rawMsg.src, new BayouMessage(me, new SessionReplyMessage(false)));
            }
        } else {
            logger.log(Level.SEVERE, "Bayou.Replica: unknown msg type");
        }
        return true;
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
        for (Integer i : versionVector.values())
            if (min > i) min = i;
        return min;
    }

    private int max_version_vector() {
        int max = Integer.MIN_VALUE;
        for (Integer i : versionVector.values())
            if (max < i) max = i;
        return max;
    }

    private void addToLog(BayouCommandMessage msg) {
        if (writeLog.contains(msg)) return;
        writeLog.add(msg);

        //Assuming accept-clock will always be there in Command
        //Assuming that version vector has entry for this replica
//        if (msg.command.acceptStamp.replica.equals(me))
//            versionVector.put(me, 1 + msg.command.acceptStamp.acceptClock);
//        else
        //Only add to your version vector if msg not type of RetireMessage
        if (!(msg instanceof RetireMessage)) {
            versionVector.put(msg.command.acceptStamp.replica, 1 + msg.command.acceptStamp.acceptClock);
        }

        //JUMP YOURSELF TO MAX VERSION VECTOR
        versionVector.put(me, max_version_vector());

        if (primary) {
            //assign csns
            Iterator<BayouCommandMessage> i = writeLog.iterator();
            int index = 1;
//            int min = min_version_vector();
            while (i.hasNext()) {
                BayouCommandMessage singleMsg = i.next();
//                logger.log(messageLevel, min + " : " + singleMsg.command.csn + "::" + singleMsg.command.acceptStamp.acceptClock);
                if (isCsnUnassigned(singleMsg)) {
                    singleMsg.command.csn = index;
                    updateMaxCsn(singleMsg);
//                    logger.log(messageLevel, "MESSAGES STABLE TILL CSN:" + singleMsg.command.csn + " A# < " + min + " IN " + writeLog);
                }
                index++;
            }
        } else {
//            logger.log(messageLevel, "CSN :"+msg.command.csn+ "Position :"+getPositionInWriteLog(msg));
//            if (msg.command.csn == getPositionInWriteLog(msg)) {
//                logger.log(messageLevel, "MESSAGES STABLE TILL CSN:" + msg.command.csn + " IN " + writeLog);
            updateMaxCsn(msg);
//            }
        }

        sendMessage(myGossiper, new BayouMessage(me, msg));
    }

    private boolean isCsnUnassigned(int csn) {
        return (csn == Integer.MAX_VALUE);
    }

    private boolean isCsnUnassigned(BayouCommandMessage b) {
        return (b.command.csn == Integer.MAX_VALUE);
    }

    private void updateMaxCsn(BayouCommandMessage singleMsg) {
        if (isCsnUnassigned(singleMsg)) return;
        if (isCsnUnassigned(maxCsn) || maxCsn < singleMsg.command.csn) {
            maxCsn = singleMsg.command.csn;
            logger.log(messageLevel, "MESSAGES STABLE TILL CSN:" + maxCsn + " WITH " + singleMsg);
        }
    }

    private boolean checkForCSN(BayouCommandMessage msg) {

        if (writeLog.contains(msg)) {
            if (primary) return true;
            updateMaxCsn(msg);
//            if (msg.command.csn == getWriteLogMsg(msg).command.csn) {
//                if(!(isCsnUnassigned(msg)) && msg.command.csn > maxCsn) {
//                    logger.log(messageLevel, "MESSAGES STABLE TILL CSN:" + msg.command.csn + " IN " + writeLog);
//                    maxCsn = msg.command.csn;
//                }
//                return true;
//            }
//            if (msg.command.csn == getPositionInWriteLog(msg)) {
//                logger.log(messageLevel, "MESSAGES STABLE TILL CSN:" + msg.command.csn + " IN " + writeLog);
//                updateMaxCsn(msg);
//            }
            writeLog.add(msg);
            return true;
        }
        return false;
    }

    private BayouCommandMessage getWriteLogMsg(BayouCommandMessage msg) {
        Iterator<BayouCommandMessage> i = writeLog.iterator();
        while (i.hasNext()) {
            BayouCommandMessage b = i.next();
            if (b.equals(msg)) return b;
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


    private boolean giveMeAName() {
        ProcessId aReplica = getMeCurrentDb();
        if (aReplica == null) {
            System.out.println("Killing the Replica as ALL the DB's are disconnected or are Retiring....");
            logger.log(messageLevel, "Killing the Replica as ALL the DB's are disconnected or are Retiring....");
            return false ;
        }
        sendMessage(aReplica, new BayouMessage(me, new RequestNameMessage(me)));
        return true;
    }

}
