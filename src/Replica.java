import com.sun.corba.se.impl.protocol.giopmsgheaders.RequestMessage;

import java.util.HashMap;
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

    public PlayList playList;
    Map<Integer, ReplicaCommand> proposals = new HashMap<Integer, ReplicaCommand>();

    public Replica(Env env, ProcessId me, ProcessId[] leaders) {
        getName();
        this.env = env;
        this.me = me;
        setLogger();
        loadProp();
        env.addProc(me, this);
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
