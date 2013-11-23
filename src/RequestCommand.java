/**
 * Created with IntelliJ IDEA.
 * User: bansal
 * Date: 22/11/13
 * Time: 8:52 PM
 * To change this template use File | Settings | File Templates.
 */
public class RequestCommand extends Command{
    RequestType type;
    String args;

    RequestCommand(ProcessId client, String requestString) {
        super(client);
        String[] splitRequest = requestString.split(Env.TX_MSG_SEPARATOR,2);
        this.args = splitRequest[1];
        this.type = RequestType.valueOf(splitRequest[0]);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RequestCommand)) return false;

        RequestCommand requestCommand = (RequestCommand) o;

        if (!args.equals(requestCommand.args)) return false;
        if (type != requestCommand.type) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = type.hashCode();
        result = 31 * result + args.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "RequestCommand{" +
                "type=" + type +
                ", args='" + args + "\', " +
                super.toString() +
                '}';
    }

    public enum RequestType {
        ADD("Add"),
        DELETE("Delete"),
        EDIT("Edit"),
        SHOW("Show"),
        ;

        public String type;

        RequestType(String type) {
            this.type = type;
        }
    }
}
