import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * Created with IntelliJ IDEA.
 * User: bansal
 * Date: 22/11/13
 * Time: 8:21 PM
 * To change this template use File | Settings | File Templates.
 */
public class Replica extends Process {

    int clock = 0;
    public PlayList playList;
    List<BayouMessage> writeLog = new ArrayList<BayouMessage>();
    int csn = -1;
    boolean primary;
    Map<String, Integer> versionVector = new HashMap<String, Integer>();

    public Replica(Env env, ProcessId me, boolean primary) {
        this.env = env;
        this.me = me;
        playList = new PlayList();
        this.primary = primary;
        setLogger();
        loadProp();
//        if(!primary){
//            giveMeAName();
//        }
        env.addProc(me, this);
    }


    public void body() {
        if(!primary)
            giveMeAName();
        logger.log(messageLevel, "Here I am: " + me);
        while (!stop_request()) {
            BayouMessage msg = getNextMessage();
            //DROP A MESSAGE IF COMMAND IS PRESENT AND IS AHEAD OF YOUR VERSION VECTOR
            if(msg.command != null) {
                int currentClock = versionVector.get(msg.command.acceptStamp.replica.name);
                if(currentClock < msg.command.acceptStamp.acceptClock)
                    logger.log(messageLevel, "Dropping message, currClock="+currentClock+" "+msg.command);
                    continue;
            }
            if (msg instanceof RequestMessage) {
                RequestCommand c = (RequestCommand)((RequestMessage) msg).command;
                c.updateAcceptStamp(clock, me);
                playList.action(c);
                logger.log(messageLevel, "PERFORMED " + c + "OUTPUT :" + c.response);
                sendMessage(c.client, new ResponseMessage(me, c));
                clock++;
                writeLog.add(msg);
            } else if (msg instanceof RequestNameMessage) {
                RequestNameMessage message = (RequestNameMessage) msg;
                if(message.command != null) {
                    AcceptStamp acceptStamp = message.command.acceptStamp;
                    if(versionVector.get(acceptStamp.replica.name) == null)
                        versionVector.put(acceptStamp.replica.name, acceptStamp.acceptClock);
                } else {                  //Command null means sent first to you
                    message.command = new Command();
                    message.command.updateAcceptStamp(clock, me);
                    versionVector.put(message.command.acceptStamp.toString(), clock);
                    sendMessage(message.src, new NameAssignedMessage(me, message.command.acceptStamp.toString()));
                    //ToDo: SEND ALL YOUR WRITE LOGS TO THIS NEW REPLICA
                }
                clock++;
                writeLog.add(message);
            } else if (msg instanceof RetireMessage) {
                RetireMessage message = (RetireMessage) msg;
                if(message.command != null) { //The replica present in command is retiring
                    versionVector.remove(message.command.acceptStamp.replica.name);
                } else { //Remove myself
                    //ToDo: SEND ALL YOUR WRITE LOG TO ONE OF REPLICA, THEN BREAK ON ACK
                    message.command = new Command(new AcceptStamp(clock, me));
                }
                clock++;
                writeLog.add(message);
            }else {
                logger.log(Level.SEVERE, "Bayou.Replica: unknown msg type");
            }
        }
    }


    private void giveMeAName() {
        sendMessage((ProcessId)env.dbProcs.keySet().toArray()[0], new RequestNameMessage(me));
        while (true) {
            BayouMessage msg = getNextMessage();
            if (msg instanceof NameAssignedMessage) {
                NameAssignedMessage nameMessage = (NameAssignedMessage) msg;
                System.out.println(env.dbProcs.get(me));
                me.name = nameMessage.name;
                System.out.println(env.dbProcs.get(me));
                //ToDo: Check what is happening there...
                //env.dbProcs.put(me,this);
                break;
            } else {
                logger.log(Level.SEVERE, "Bayou.Replica: unknown msg type");
            }
        }
    }

}
