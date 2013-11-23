/**
 * Created with IntelliJ IDEA.
 * User: bansal
 * Date: 22/11/13
 * Time: 8:41 PM
 * To change this template use File | Settings | File Templates.
 */
public class ReplicaCommand {
     ;
    int csn;
    ProcessId client;
    Request request;

    public ReplicaCommand(ProcessId client, int req_id, Request request) {
        this.client = client;
        this.csn = -1;
        this.request = request;
    }

}
