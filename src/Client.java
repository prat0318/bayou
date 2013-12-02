import java.util.Properties;
import java.util.logging.Level;

public class Client extends Process {
    AcceptStamp lastAcceptStamp = null;
    ProcessId currentDb = null;
    boolean sessionEstablished = false;

    public Client(Env env, ProcessId me, ProcessId currentDb) {
        this.env = env;
        this.me = me;
        setLogger();
        loadProp();
        if (checkDbCanBeConnectedTo(currentDb))
            this.currentDb = currentDb;
        if (this.currentDb == null) {
            setCurrentDb();
        }
        env.addProc(me, this);
    }

    Properties loadProp() {
        super.loadProp();
        if (prop.containsKey(me.name + "lastAcceptStamp")) {
            String acceptStamp = prop.getProperty(me.name + "lastAcceptStamp");
            String[] split = acceptStamp.split(AcceptStamp.SEPARATOR, 2);
            int acceptClock = Integer.parseInt(split[1]);
            for (ProcessId p : env.dbProcs.keySet()) {
                if (p.name.equals(split[0])) {
                    this.lastAcceptStamp = new AcceptStamp(acceptClock, p);
                    ProcessId processId = env.dbProcs.get(lastAcceptStamp.replica).me;
                    if (checkDbCanBeConnectedTo(processId)) {
                        this.currentDb = processId;
                    }
                }
            }
        }
        return prop;
    }


    @Override
    void body() {
        logger.log(messageLevel, "Here I am: " + me);
        establishSession();

        while (!stop_request()) {
            BayouMessage rawMsg = getNextMessage();
            BayouCommandMessage msg = rawMsg.bayouCommandMessage;

            if (msg instanceof SessionReplyMessage) {
                sessionEstablished = true;
            } else if (msg instanceof RequestMessage) {
                if (checkDbCanBeConnectedTo(currentDb))
                    sendMessage(currentDb, new BayouMessage(me, msg));
                else {
                    setCurrentDb();
                    establishSession();
                    sendMessage(currentDb, new BayouMessage(me, msg));
                }
            } else if (msg instanceof ResponseMessage) {
                ResponseMessage message = (ResponseMessage) msg;
                if (message.oP) {
                    lastAcceptStamp = message.command.acceptStamp;
                    updateProperty(me.name + "lastAcceptStamp", lastAcceptStamp.toString());
                }
                System.out.println(msg);
            }
        }
    }

    private void setCurrentDb() {
        int i;
        for (i = 0; i < env.dbProcs.size(); i++) {
            if (checkDbCanBeConnectedTo((ProcessId) env.dbProcs.keySet().toArray()[i])) {
                this.currentDb = (ProcessId) env.dbProcs.keySet().toArray()[i];
                break;
            }
        }
        if (i == env.dbProcs.size()) {
            System.out.println("CLose the client as aLL the DB's are disconnected....");
            logger.log(messageLevel, "CLose the client as aLL the DB's are disconnected....");
            System.exit(0);
        }
    }

    private ProcessId establishSession() {
        sendMessage(currentDb, new BayouMessage(me, new RequestSessionMessage(lastAcceptStamp)));
        while (!stop_request()) {
            BayouMessage rawMsg = getNextMessage();
            BayouCommandMessage msg = rawMsg.bayouCommandMessage;

            if (msg instanceof SessionReplyMessage) {
                SessionReplyMessage message = (SessionReplyMessage) msg;
                this.sessionEstablished = message.sessionGranted;
                logger.log(sessionEstablished ? Level.WARNING : Level.SEVERE, me + "'s session Status granted = " + this.sessionEstablished + " with " + rawMsg.src);
                break;
            }
        }
        return currentDb;
    }
}