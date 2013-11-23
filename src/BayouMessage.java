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
    Request request;

    public RequestMessage(ProcessId src, Request request) {
        this.src = src;
        this.src_name = src.name;
        this.request = request;
    }

    @Override
    public String toString() {
        return "RequestMessage{" +
                "request=" + request +
                '}';
    }
}

class ResponseMessage extends BayouMessage {
    Request request;

    public ResponseMessage(ProcessId src, Request request) {
        this.src = src;
        this.src_name = src.name;
        this.request = request;
    }

    @Override
    public String toString() {
        return "Command: " + request + " done.";
    }
}

