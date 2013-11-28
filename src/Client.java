import java.util.Properties;

public class Client extends Process {
    AcceptStamp lastAcceptStamp = null;
    ProcessId currentDb = null;
    boolean sessionEstablished = false;

    public Client(Env env, ProcessId me, ProcessId currentDb) {
        this.env = env;
        this.me = me;
        setLogger();
        loadProp();
        env.addProc(me, this);
        if (checkDbCanBeConnectedTo(currentDb))
            this.currentDb = currentDb;
        if (this.currentDb == null) {
            for (int i = 0; i < env.dbProcs.size(); i++) {
                if (checkDbCanBeConnectedTo((ProcessId) env.dbProcs.keySet().toArray()[i])) {
                    this.currentDb = (ProcessId) env.dbProcs.keySet().toArray()[i];
                    break;
                }
            }
            System.out.println("ALL the DB's are disconnected....");
            logger.log(messageLevel, "ALL the DB's are disconnected....");
            //TODO: System.exit in this case as the whole set of replicas is disconnected the client cannot do anything
        }
        establishSession();
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

        while (!stop_request()) {
            BayouMessage rawMsg = getNextMessage();
            BayouCommandMessage msg = rawMsg.bayouCommandMessage;

            if (msg instanceof SessionReplyMessage) {
                sessionEstablished = true;
            } else if (msg instanceof RequestMessage) {
                if (checkDbCanBeConnectedTo(currentDb))
                    sendMessage(currentDb, new BayouMessage(me, msg));
            } else if (msg instanceof ResponseMessage) {
                ResponseMessage message = (ResponseMessage) msg;
                lastAcceptStamp = message.command.acceptStamp;
                updateProperty(me.name + "lastAcceptStamp", lastAcceptStamp.toString());
                System.out.println(msg);
            }
        }
    }

    private boolean checkDbCanBeConnectedTo(ProcessId p) {
        if (!env.dbProcs.containsKey(p) || env.dbProcs.get(p).disconnect || disconnectFrom.contains(p)) {
            return false;
        }
        return true;
    }

    private ProcessId establishSession() {
        sendMessage(currentDb, new BayouMessage(me, new RequestSessionMessage(new Command(lastAcceptStamp))));
        while (!stop_request()) {
            BayouMessage rawMsg = getNextMessage();
            BayouCommandMessage msg = rawMsg.bayouCommandMessage;

            if (msg instanceof SessionReplyMessage) {
                sessionEstablished = true;
                break;
            }
        }
        return currentDb;
    }
}