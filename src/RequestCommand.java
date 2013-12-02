/**
 * Created with IntelliJ IDEA.
 * User: bansal
 * Date: 22/11/13
 * Time: 8:52 PM
 * To change this template use File | Settings | File Templates.
 */
public class RequestCommand extends Command{
    ProcessId client;
    OpType opType;
    String op;
    String response;
    boolean no_op = false;

    RequestCommand(AcceptStamp acceptStamp, ProcessId client, String requestString) {
        super(acceptStamp);
        String[] splitRequest = requestString.split(Env.TX_MSG_SEPARATOR,2);
        this.op = splitRequest[1];
        this.opType = OpType.valueOf(splitRequest[0]);
        this.client = client;
    }

    @Override
    public String toString() {
        return "RequestCommand{" +
                "type=" + opType +
                ", op='" + op + "\', " +
                ", client=" + client +
                ", RESPONSE=" + response +" "+
                super.toString() +
                '}';
    }

    public enum OpType {
        ADD("Add"),
        DELETE("Delete"),
        EDIT("Edit"),
        SHOW("Show"),
        ;

        public String type;

        OpType(String type) {
            this.type = type;
        }
    }
}
