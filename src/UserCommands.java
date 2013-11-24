public enum UserCommands {
    START_CLIENT("START_CLIENT"),
    STOP_CLIENT("STOP_CLIENT <client_id>"),
    JOIN("JOIN"),
    LEAVE("LEAVE <db_id>"),
    OP("OP <client_id> [ADD|DELETE|EDIT|SHOW$]$<SongName>$[<SongURL>]"),
    SHOW_CLIENTS("Shows all live clients"),
    SHOW_DB("Shows all live servers"),
    CONNECT("CONNECT <db_name>"),
    DISCONNECT("DISCONNECT <db_name>"),
    PAUSE("PAUSE the system"),
    CONTINUE("CONTINUE the execution"),
    HELP("Shows this stuff again"),
    ;

    private String description;

    UserCommands(String description) {
        this.description = description;
    }

    public String getDescription() {
        return this.description;
    }

}