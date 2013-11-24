public class Gossip extends Process {
    Replica replica;


    public Gossip(Replica replica, ProcessId me) {
        this.replica = replica;
        this.env = replica.env;
        this.me = me;
        setLogger();
        loadProp();
        env.addProc(me, this);
    }

    @Override
    void body() {
        logger.log(messageLevel, "Here I am: " + me);

        while (!stop_request() && !replica.stop_request()) {
            BayouMessage rawMsg = getNextMessage();
            BayouCommandMessage msg = rawMsg.bayouCommandMessage;


        }

    }
}
