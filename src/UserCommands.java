public enum UserCommands {
    START_CLIENT("START_CLIENT"),
    STOP_CLIENT("STOP_CLIENT <client_id>"),
    RESTART_CLIENT("RESTART_CLIENT <client_id>"),
    JOIN("JOIN"),
    RETIRE("RETIRE <db_id>"),
    OP("OP <client_id> [ADD|DELETE|EDIT|SHOW$]$<SongName>$[<SongURL>]"),
    SHOW_CLIENTS("Shows all live clients"),
    SHOW_DB("Shows all live servers"),
    CONNECT("CONNECT <db_name>"),
    CONNECT_THEM("CONNECT_THEM <db_name1> <db_name2"),
    DISCONNECT("DISCONNECT <db_name>"),
    CURR_STATE("CURR_STATE [<db_name>]"),
    NO_OP("NO_OP"),
    DISCONNECT_FROM("DISCONNECT_FROM <db_name> <db_name> "),
    PAUSE("PAUSE the system"),
    CONTINUE("CONTINUE the execution"),
    HELP("Shows this stuff again"),
    QUIT("QUIT"),
    ;

    private String description;

    UserCommands(String description) {
        this.description = description;
    }

    public String getDescription() {
        return this.description;
    }

}