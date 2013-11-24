import java.util.*;

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

            //Shuffle all - exclude myself
            Set<ProcessId> keys = new HashSet<ProcessId>(replica.versionVector.keySet());
            keys.remove(replica.me);
            List<ProcessId> shuffle = new ArrayList<ProcessId>(keys);
            Collections.shuffle(shuffle);
            int probRange = 1;
            for(ProcessId replica : shuffle) {
                if(new Random().nextInt(probRange) == 0) {
                    sendMessage(replica, new BayouMessage(me, msg));
                    probRange *= 2;
                }
            }
        }

    }
}
