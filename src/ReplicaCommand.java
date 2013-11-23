/**
 * Created with IntelliJ IDEA.
 * User: bansal
 * Date: 22/11/13
 * Time: 8:41 PM
 * To change this template use File | Settings | File Templates.
 */
public class ReplicaCommand {


}

enum CommandTypes {
    REQUEST("Request type"),
    NEWDB("Create a new Database"),
    RETIRE("Retire a database");

    private String description;

    CommandTypes(String description) {
        this.description = description;
    }

    public String getDescription() {
        return this.description;
    }
}

