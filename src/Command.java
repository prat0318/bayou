/**
 * Created with IntelliJ IDEA.
 * User: bansal
 * Date: 22/11/13
 * Time: 8:41 PM
 * To change this template use File | Settings | File Templates.
 */
public class Command {

    AcceptStamp acceptStamp;

    public Command() {
    }

    public Command(AcceptStamp acceptStamp) {
        this.acceptStamp = acceptStamp;
    }

    public void updateAcceptStamp(int acceptClock, ProcessId replica){
        this.acceptStamp = new AcceptStamp(acceptClock, replica);
    }
    @Override
    public String toString() {
        return "Command {" +
                "acceptStamp=" + acceptStamp +
                '}';
    }
}
