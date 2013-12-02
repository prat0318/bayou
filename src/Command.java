/**
 * Created with IntelliJ IDEA.
 * User: bansal
 * Date: 22/11/13
 * Time: 8:41 PM
 * To change this template use File | Settings | File Templates.
 */
public class Command {

    AcceptStamp acceptStamp;
    public int csn = Integer.MAX_VALUE;
    public boolean notShowCsn = false;

    public Command() {
    }

    public Command(AcceptStamp acceptStamp) {
        this.acceptStamp = acceptStamp;
    }

    public int compare(Command command) {
        if(csn != command.csn) return Integer.compare(csn, command.csn);
        if(acceptStamp == null || command.acceptStamp == null) return -1;
        return acceptStamp.compare(command.acceptStamp);
    }

    public void updateAcceptStamp(int acceptClock, ProcessId replica){
        this.acceptStamp = new AcceptStamp(acceptClock, replica);
    }

    @Override
    public String toString() {
        return "Command {" +
                " acceptStamp=" + acceptStamp +
                ", csn=" + (notShowCsn ? Integer.MAX_VALUE : csn) +
                '}';
    }
}
