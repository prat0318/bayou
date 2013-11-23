/**
 * Created with IntelliJ IDEA.
 * User: bansal
 * Date: 22/11/13
 * Time: 8:41 PM
 * To change this template use File | Settings | File Templates.
 */
public class ReplicaCommand {

    int csn;
    int acceptClock;
    ProcessId replica;
    ProcessId client;
    Request request;
    String response;

    public ReplicaCommand(ProcessId client, Request request) {
        this.client = client;
        this.csn = -1;
        this.request = request;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ReplicaCommand)) return false;

        ReplicaCommand that = (ReplicaCommand) o;

        if (acceptClock != that.acceptClock) return false;
        if (csn != that.csn) return false;
        if (!client.equals(that.client)) return false;
        if (replica != null ? !replica.equals(that.replica) : that.replica != null) return false;
        if (!request.equals(that.request)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = csn;
        result = 31 * result + acceptClock;
        result = 31 * result + (replica != null ? replica.hashCode() : 0);
        result = 31 * result + client.hashCode();
        result = 31 * result + request.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "ReplicaCommand{" +
                "csn=" + csn +
                ", acceptClock=" + acceptClock +
                ", replica=" + replica +
                ", client=" + client +
                ", request=" + request +
                '}';
    }
}
