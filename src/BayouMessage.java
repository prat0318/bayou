public class BayouMessage {
    Command command;
    ProcessId src;
    String src_name;

    public BayouMessage() {
    }

    public BayouMessage(Command command) {
        this.command = command;
    }

    public void updateSource(ProcessId new_src) {
        this.src = new_src;
        this.src_name = src.name;
    }
}

class RequestSessionMessage extends BayouMessage {

    @Override
    public String toString() {
        return "RequestSessionMessage{" +
                "src=" + src +
                '}';
    }

    RequestSessionMessage(ProcessId src) {
        this.src = src;
        this.src_name = src.name;
    }
}

class SessionReplyMessage extends BayouMessage {

    public SessionReplyMessage(ProcessId src, Command command) {
        super(command);
        this.src = src;
        this.src_name = src.name;
    }

    @Override
    public String toString() {
        return "SessionReplyMessage{" +
                "src=" + src +
                "command=" + command +
                '}';
    }
}


class RequestNameMessage extends BayouMessage {
    String my_original_name;
//    Command command;

    @Override
    public String toString() {
        return "RequestNameMessage{" +
                "src=" + src +
                "command=" + command +
                "orig_name="+ my_original_name +
                '}';
    }

    RequestNameMessage(ProcessId src) {
        this.src = src;
        this.src_name = src.name;
        this.my_original_name = src.name;
    }
}

//class NameAssignedMessage extends BayouMessage {
//    String name;
//
//    @Override
//    public String toString() {
//        return "RequestNameMessage{" +
//                "src=" + src +
//                "name=" + name +
//                '}';
//    }
//
//    NameAssignedMessage(ProcessId src, String name) {
//        this.src = src;
//        this.src_name = src.name;
//        this.name = name;
//    }
//}

class RetireMessage extends BayouMessage {
//    Command command;

    @Override
    public String toString() {
        return "RetireMessage{" +
                "src=" + src +
                "command=" + command +
                '}';
    }

    RetireMessage(ProcessId src) {
        this.src = src;
        this.src_name = src.name;
    }
}

class RequestMessage extends BayouMessage {
//    RequestCommand requestCommand;

    public RequestMessage(ProcessId src, RequestCommand requestCommand) {
        super(requestCommand);
        this.src = src;
        this.src_name = src.name;
//        this.requestCommand = requestCommand;
    }

    @Override
    public String toString() {
        return "RequestMessage{" +
                "requestCommand=" + command +
                '}';
    }
}

class ResponseMessage extends BayouMessage {
//    RequestCommand requestCommand;

    public ResponseMessage(ProcessId src, RequestCommand requestCommand) {
        super(requestCommand);
        this.src = src;
        this.src_name = src.name;
    }

    @Override
    public String toString() {
        return "Command: " + command + " done.";
    }
}

