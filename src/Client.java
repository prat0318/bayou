import java.util.ArrayList;
import java.util.List;

public class Client extends Process {
    List<BayouMessage> queue = new ArrayList<BayouMessage>();
    ProcessId currentDb = null;
//    int clientTimeout;

    public Client(Env env, ProcessId me) {
        this.env = env;
        this.me = me;
        setLogger();
        loadProp();
//        clientTimeout = Integer.parseInt(prop.getProperty("clientTimeout"));
        env.addProc(me, this);
        checkCurrentDb();
    }

    @Override
    void body() {
        logger.log(messageLevel, "Here I am: " + me);
        while (!stop_request()) {
            BayouMessage msg = getNextMessage();

            if (msg instanceof RequestMessage) {
                checkCurrentDb();
                sendMessage(currentDb, msg);
            } else if (msg instanceof ResponseMessage) {
                System.out.println(msg);
            }
        }
    }

    private void checkCurrentDb() {
        if (!env.dbProcs.containsKey(currentDb))
            currentDb = (ProcessId) env.dbProcs.keySet().toArray()[0];
    }
}