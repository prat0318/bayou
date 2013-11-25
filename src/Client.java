import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class Client extends Process {
    AcceptStamp lastAcceptStamp = null;
    ProcessId currentDb = null;
    boolean sessionEstablished = false;

    public Client(Env env, ProcessId me) {
        this.env = env;
        this.me = me;
        setLogger();
        loadProp();
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
                }
            }
        }
        return prop;
    }


    @Override
    void body() {
        logger.log(messageLevel, "Here I am: " + me);

        while (!stop_request()) {
            BayouMessage rawMsg = getNextMessage();
            BayouCommandMessage msg = rawMsg.bayouCommandMessage;

            if (msg instanceof SessionReplyMessage) {
                sessionEstablished = true;
            } else if (msg instanceof RequestMessage) {
                if (checkCurrentDb() != null)
                    sendMessage(currentDb, new BayouMessage(me, msg));
            } else if (msg instanceof ResponseMessage) {
                ResponseMessage message = (ResponseMessage) msg;
                lastAcceptStamp = message.command.acceptStamp;
                updateProperty(me.name + "lastAcceptStamp", lastAcceptStamp.toString());
                System.out.println(msg);
            }
        }
    }

    private ProcessId checkCurrentDb() {
        if (lastAcceptStamp == null) {
            currentDb = (ProcessId) env.dbProcs.keySet().toArray()[0];
            sendMessage(currentDb, new BayouMessage(me, new RequestSessionMessage(new Command(lastAcceptStamp))));
            return currentDb;
        } else if (env.dbProcs.containsKey(lastAcceptStamp.replica)) {
            currentDb = env.dbProcs.get(lastAcceptStamp.replica).me;
            for (int i = 0; i < env.dbProcs.size(); i++) {
                if (env.dbProcs.get(currentDb).disconnect) {
                    currentDb = (ProcessId) env.dbProcs.keySet().toArray()[i];
                } else {
                    sendMessage(currentDb, new BayouMessage(me, new RequestSessionMessage(new Command(lastAcceptStamp))));
                    return currentDb;
                }
            }
            sessionEstablished = false;
            System.out.println("ALL the DB's are disconnected....");
            logger.log(messageLevel, "ALL the DB's are disconnected....");
            return null;
        }
        return currentDb;
    }
}