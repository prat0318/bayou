import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class Client extends Process {
    List<BayouMessage> queue = new ArrayList<BayouMessage>();
    AcceptStamp lastAcceptStamp = null;
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

    Properties loadProp() {
        super.loadProp();
        if (prop.containsKey(me.name + "lastAcceptStamp")) {
            String acceptStamp = prop.getProperty(me.name + "lastAcceptStamp");
            String[] split = acceptStamp.split(AcceptStamp.SEPARATOR, 2);
            int acceptClock = Integer.parseInt(split[0]);
            for (ProcessId p : env.dbProcs.keySet()) {
                if (p.name.equals(split[1])) {
                    this.lastAcceptStamp = new AcceptStamp(acceptClock, p);
                    ;
                }
            }
        }else {
            this.lastAcceptStamp = new AcceptStamp(0,(ProcessId)env.dbProcs.keySet().toArray()[0]);
        }
        return prop;
    }


    @Override
    void body() {
        logger.log(messageLevel, "Here I am: " + me);
        while (!stop_request()) {
            BayouMessage rawMsg = getNextMessage();
            BayouCommandMessage msg = rawMsg.bayouCommandMessage;

            if (msg instanceof RequestMessage) {
                checkCurrentDb();
                sendMessage(currentDb, new BayouMessage(me, msg));
            } else if (msg instanceof ResponseMessage) {
                ResponseMessage message = (ResponseMessage) msg;
                lastAcceptStamp = message.command.acceptStamp;
                updateProperty(me.name + "lastAcceptStamp", lastAcceptStamp.toString());
                System.out.println(msg);
            }
        }
    }

    private void checkCurrentDb() {
        if (!env.dbProcs.containsKey(currentDb))
            currentDb = env.dbProcs.get(lastAcceptStamp.replica).me;
        //currentDb = (ProcessId) env.dbProcs.keySet().toArray()[0];
    }
}