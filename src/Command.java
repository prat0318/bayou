/**
 * Created with IntelliJ IDEA.
 * User: bansal
 * Date: 22/11/13
 * Time: 8:41 PM
 * To change this template use File | Settings | File Templates.
 */
public class Command {

    int acceptClock;
    ProcessId replica;
    ProcessId client;
    String response;

    public Command(ProcessId client) {
        this.client = client;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Command)) return false;

        Command command = (Command) o;

        if (acceptClock != command.acceptClock) return false;
        if (!client.equals(command.client)) return false;
        if (replica != null ? !replica.equals(command.replica) : command.replica != null) return false;
        if (response != null ? !response.equals(command.response) : command.response != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = acceptClock;
        result = 31 * result + (replica != null ? replica.hashCode() : 0);
        result = 31 * result + client.hashCode();
        result = 31 * result + (response != null ? response.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Command{" +
                "acceptClock=" + acceptClock +
                ", replica=" + replica +
                ", client=" + client +
                ", response=" + response +
                '}';
    }
}
