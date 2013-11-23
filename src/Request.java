/**
 * Created with IntelliJ IDEA.
 * User: bansal
 * Date: 22/11/13
 * Time: 8:52 PM
 * To change this template use File | Settings | File Templates.
 */
public class Request extends Command{
    RequestType type;
    String args;

    Request(ProcessId client,String requestString) {
        super(client);
        String[] splitRequest = requestString.split("#",2);
        this.args = splitRequest[1];
        this.type = RequestType.valueOf(splitRequest[0]);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Request)) return false;

        Request request = (Request) o;

        if (!args.equals(request.args)) return false;
        if (type != request.type) return false;

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
        return "Request{" +
                "type=" + type +
                ", args='" + args + '\'' +
                '}';
    }

    public enum RequestType {
        ADD("Add"),
        DELETE("Delete"),
        EDIT("Edit");

        public String type;

        RequestType(String type) {
            this.type = type;
        }
    }
}
