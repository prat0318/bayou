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

    public void sendAllWriteLogTo(ProcessId replica) {
        Iterator<BayouCommandMessage> i = this.replica.writeLog.iterator();
        while(i.hasNext())
            sendMessage(replica, new BayouMessage(me, i.next()));
    }

    @Override
    void body() {
        logger.log(messageLevel, "Here I am: " + me);

        while (!stop_request() && !replica.stop_request()) {
            BayouMessage rawMsg = getNextMessage();
//            BayouCommandMessage msg = rawMsg.bayouCommandMessage;

            //Shuffle all - exclude myself
            Set<ProcessId> keys = new HashSet<ProcessId>(replica.versionVector.keySet());
            keys.remove(replica.me);
            List<ProcessId> shuffle = new ArrayList<ProcessId>(keys);
            Collections.shuffle(shuffle);
            int probRange = 1;
            for(ProcessId replica : shuffle) {
                if(new Random().nextInt(probRange) == 0) {
                    sendAllWriteLogTo(replica);
//                    sendMessage(replica, new BayouMessage(me, msg));
                    //Commenting below will result in probability 1
//                    probRange *= 2;
                }
            }
        }

    }
}
