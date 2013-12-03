public class BayouMessage {
    BayouCommandMessage bayouCommandMessage;
    ProcessId src;
    //String src_name;

    public BayouMessage() {
    }
//    public BayouMessage(BayouMessage bayouMessage, ProcessId new_src) {
//        this.bayouCommandMessage = bayouMessage.bayouCommandMessage;
//        updateSource(new_src);
//    }

    public BayouMessage(Command command) {
        this.bayouCommandMessage = new BayouCommandMessage(command);
    }

    public BayouMessage(ProcessId src, BayouCommandMessage bayouCommandMessage) {
        this.bayouCommandMessage = bayouCommandMessage;
        this.src = src;
        //this.src_name = src.name;
    }

    public void updateSource(ProcessId new_src) {
        this.src = new_src;
        //this.src_name = src.name;
    }

    @Override
    public String toString() {
        return "BayouMessage{" +
                "commandMsg=" + bayouCommandMessage +
                ", src=" + src +
                //", src_name='" + src_name + '\'' +
                '}';
    }
}

class BayouCommandMessage {
    Command command;

    public BayouCommandMessage() {
    }

    public BayouCommandMessage(Command command) {
        this.command = command;
    }

    public int compare(BayouCommandMessage bayouCommandMessage) {
        if (command == null || bayouCommandMessage.command == null) return -1;
        return command.compare(bayouCommandMessage.command);
    }

    @Override
    public String toString() {
        return "BayouCommandMessage{" +
                "command=" + command +
                '}';
    }
}

class RequestSessionMessage extends BayouCommandMessage {
    AcceptStamp lastUpdatedStamp;

    RequestSessionMessage(AcceptStamp lastUpdatedStamp) {
        this.lastUpdatedStamp = lastUpdatedStamp;
    }

//    RequestSessionMessage(Command cmd) {
//        this.command = cmd;
//    }

    @Override
    public String toString() {
        return "RequestSessionMessage{" +
                ", acceptStamp=" + lastUpdatedStamp +
                ", command= " + command +
                '}';
    }
}

class SessionReplyMessage extends BayouCommandMessage {
    boolean sessionGranted;

    public SessionReplyMessage(boolean sessionGranted) {
        this.sessionGranted = sessionGranted;
    }

    @Override
    public String toString() {
        return "SessionReplyMessage{" +
                "sessionGranted=" + sessionGranted +
                '}';
    }
}


class RequestNameMessage extends BayouCommandMessage {
    ProcessId my_original_id;

    @Override
    public String toString() {
        return "RequestNameMessage{" +
                "command=" + command +
                ", orig_name=" + my_original_id +
                '}';
    }

    RequestNameMessage(ProcessId src) {
        this.my_original_id = src;
    }
}

class NoOpMessage extends BayouCommandMessage {

}

class RetireMessage extends BayouCommandMessage {
    ProcessId nextPrimaryId = null;

    public RetireMessage(ProcessId p) {
        this.nextPrimaryId = p;
    }


    @Override
    public String toString() {
        return "RetireMessage{" +
                "command=" + command +
                '}';
    }
}

class RequestMessage extends BayouCommandMessage {

    public RequestMessage(RequestCommand requestCommand) {
        super(requestCommand);
    }

    @Override
    public String toString() {
        return "RequestMessage{" +
                "requestCommand=" + command +
                '}';
    }
}

class ResponseMessage extends BayouCommandMessage {

    boolean oP;
    public ResponseMessage(RequestCommand requestCommand, boolean oP) {
        super(requestCommand);
        this.oP = oP;
    }

    @Override
    public String toString() {
        return "Command: " + command + " done.";
    }
}

