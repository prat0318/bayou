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
    String my_name;

    public Replica(Env env, ProcessId me, boolean primary) {
        this.env = env;
        this.me = me;
        playList = new PlayList();
        this.primary = primary;
        setLogger();
        loadProp();
        env.addProc(me, this);
    }

    void action(Request c) {
        String[] args = c.args.split(Env.TX_MSG_SEPARATOR);
        switch (c.type) {
            case ADD:
                c.response = playList.add(args[0], args[1]);
                break;
            case DELETE:
                c.response = playList.delete(args[0]);
                break;
            case EDIT:
                c.response = playList.edit(args[0], args[1]);
                break;
            case SHOW:
                c.response = playList.show();
                break;
            default:
                c.response = "INVALID OPERATION TYPE";
                break;
        }
        logger.log(messageLevel, "PERFORMED " + c + "OUTPUT :" + c.response);
    }


    public void body() {
        sendMessage(env.dbProcs.get("1").me, new GetNameMessage(me));
        while (my_name == null) {
            BayouMessage msg = getNextMessage();
            if (msg instanceof GiveNameMessage) {
                GiveNameMessage nameMessage = (GiveNameMessage) msg;
                my_name = nameMessage.name;
            } else {
                logger.log(Level.SEVERE, "Bayou.Replica: unknown msg type");
            }
        }
        logger.log(messageLevel, "Here I am: " + me);
        while (!stop_request()) {
            BayouMessage msg = getNextMessage();

            if (msg instanceof RequestMessage) {
                Request c = ((RequestMessage) msg).request;
                c.acceptClock = clock;
                clock++;
                c.replica = this.me;
                writeLog.add(c);
                action(c);
                sendMessage(c.client, new ResponseMessage(me, c));
            } else if (msg instanceof GetNameMessage) {
                GetNameMessage message = (GetNameMessage) msg;
                AssignName command = new AssignName(message.src);
                command.response = clock + this.my_name;
                command.acceptClock = clock;
                command.replica = this.me;
                clock++;
                writeLog.add(command);
                sendMessage(message.src, new GiveNameMessage(me, command.response));
            } else if (msg instanceof RetierMessage) {
                RetierMessage message = (RetierMessage) msg;
                Retier command = new Retier(message.src);
                command.response = clock + this.my_name;
                command.acceptClock = clock;
                command.replica = this.me;
                clock++;
                writeLog.add(command);
            }else {
                logger.log(Level.SEVERE, "Bayou.Replica: unknown msg type");
            }
        }
    }

}
