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

class RetireMessage extends BayouMessage {
    @Override
    public String toString() {
        return "RetireMessage{" +
                "src=" + src +
                '}';
    }

    RetireMessage(ProcessId src) {
        this.src = src;
        this.src_name = src.name;
    }
}

class RequestMessage extends BayouMessage {
    RequestCommand requestCommand;

    public RequestMessage(ProcessId src, RequestCommand requestCommand) {
        this.src = src;
        this.src_name = src.name;
        this.requestCommand = requestCommand;
    }

    @Override
    public String toString() {
        return "RequestMessage{" +
                "requestCommand=" + requestCommand +
                '}';
    }
}

class ResponseMessage extends BayouMessage {
    RequestCommand requestCommand;

    public ResponseMessage(ProcessId src, RequestCommand requestCommand) {
        this.src = src;
        this.src_name = src.name;
        this.requestCommand = requestCommand;
    }

    @Override
    public String toString() {
        return "Command: " + requestCommand + " done.";
    }
}

