public class BayouMessage {
    ProcessId src;
    String src_name;
}

class GetNameMessage extends BayouMessage {
    @Override
    public String toString() {
        return "GetNameMessage{" +
                "src=" + src +
                '}';
    }

    GetNameMessage(ProcessId src) {
        this.src = src;
        this.src_name = src.name;
    }
}

class GiveNameMessage extends BayouMessage {
    String name;

    @Override
    public String toString() {
        return "GetNameMessage{" +
                "src=" + src +
                "name=" + name +
                '}';
    }

    GiveNameMessage(ProcessId src, String name) {
        this.src = src;
        this.src_name = src.name;
        this.name = name;
    }
}

class RetierMessage extends BayouMessage {
    @Override
    public String toString() {
        return "RetierMessage{" +
                "src=" + src +
                '}';
    }

    RetierMessage(ProcessId src) {
        this.src = src;
        this.src_name = src.name;
    }
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

