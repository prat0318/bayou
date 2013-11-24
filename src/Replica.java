import java.util.ArrayList;
import java.util.List;
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
    List<Command> writeLog = new ArrayList<Command>();
    int csn = -1;
    boolean primary;

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
        logger.log(messageLevel, "Here I am: " + me);
        while (!stop_request()) {
            BayouMessage msg = getNextMessage();

            if (msg instanceof RequestMessage) {
                RequestCommand c = ((RequestMessage) msg).requestCommand;
                c.acceptClock = clock;
                clock++;
                c.replica = this.me;
                writeLog.add(c);
                playList.action(c);
                logger.log(messageLevel, "PERFORMED " + c + "OUTPUT :" + c.response);
                sendMessage(c.client, new ResponseMessage(me, c));
            } else if (msg instanceof GetNameMessage) {
                GetNameMessage message = (GetNameMessage) msg;
                AssignName command = new AssignName(message.src);
                command.response = me.name + "." + clock;
                command.acceptClock = clock;
                command.replica = this.me;
                clock++;
                writeLog.add(command);
                sendMessage(message.src, new GiveNameMessage(me, command.response));
            } else if (msg instanceof RetireMessage) {
                RetireMessage message = (RetireMessage) msg;
                RetireCommand command = new RetireCommand(message.src);
                command.response = me.name + "." + clock;
                command.acceptClock = clock;
                command.replica = this.me;
                clock++;
                writeLog.add(command);
            }else {
                logger.log(Level.SEVERE, "Bayou.Replica: unknown msg type");
            }
        }
    }

    private void giveMeAName() {
        sendMessage((ProcessId)env.dbProcs.keySet().toArray()[0], new GetNameMessage(me));
        while (true) {
            BayouMessage msg = getNextMessage();
            if (msg instanceof GiveNameMessage) {
                GiveNameMessage nameMessage = (GiveNameMessage) msg;
                me.name = nameMessage.name;
                break;
            } else {
                logger.log(Level.SEVERE, "Bayou.Replica: unknown msg type");
            }
        }
    }

}
