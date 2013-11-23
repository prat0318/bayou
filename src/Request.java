/**
 * Created with IntelliJ IDEA.
 * User: bansal
 * Date: 22/11/13
 * Time: 8:52 PM
 * To change this template use File | Settings | File Templates.
 */
public class Request {
    RequestType type;
    String args;

    Request(String type, String op) {
        this.args = op;
        this.type = RequestType.valueOf(type);
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
