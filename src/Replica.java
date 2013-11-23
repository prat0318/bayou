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

    public PlayList playList;
    List<ReplicaCommand> writeLog = new ArrayList<ReplicaCommand>();

    public Replica(Env env, ProcessId me, ProcessId[] leaders) {
        getName();
        this.env = env;
        this.me = me;
        setLogger();
        loadProp();
        env.addProc(me, this);
    }

    boolean action(ReplicaCommand c) {
        String[] args = c.request.args.split(Env.TX_MSG_SEPARATOR);
        String output ="";
        try {

            switch (c.request.type) {
                case ADD:
                    playList.add(args[0],args[1]);
                    break;
                case DELETE:
                    playList.delete(args[0]);
                    break;
                case EDIT:
                    playList.edit(args[0],args[1]);
                    break;
                default:
                    output = "INVALID OPERATION TYPE";
                    break;
            }
            logger.log(messageLevel, "PERFORMED OUTPUT :" + output);
            sendMessage(c.client, new ResponseMessage(me, c));
        } catch (Exception e) {
            e.printStackTrace();
            logger.log(Level.SEVERE, "Error in the input msg ");
            sendMessage(c.client, new ResponseMessage(me, c));
        }
        return true;
    }


    public void body() {
        logger.log(messageLevel, "Here I am: " + me);
        while (!stop_request()) {
            BayouMessage msg = getNextMessage();

            if (msg instanceof RequestMessage) {

            } else {
                logger.log(Level.SEVERE, "Bayou.Replica: unknown msg type");
            }
        }
    }

}
