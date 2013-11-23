public class BayouMessage {
    ProcessId src;
    String src_name;
}

class RequestMessage extends BayouMessage {
    ReplicaCommand command;

    public RequestMessage(ProcessId src, ReplicaCommand command) {
        this.src = src;
        this.src_name = src.name;
        this.command = command;
    }

    @Override
    public String toString() {
        return "RequestMessage{" +
                "command=" + command +
                '}';
    }
}

class ResponseMessage extends BayouMessage {
    ReplicaCommand command;
    long createdAt;

    public ResponseMessage(ProcessId src, ReplicaCommand command) {
        this.src = src;
        this.src_name = src.name;
        this.command = command;
        this.createdAt = System.currentTimeMillis();
    }

    @Override
    public String toString() {
        return "Command: " + command + " done. Executed at T=" + createdAt;
    }
}

