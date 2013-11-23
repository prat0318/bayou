public enum UserCommands {
    START_CLIENT("START_CLIENT"),
    STOP_CLIENT("STOP_CLIENT <client_id>"),
    OP("OP <client_id>#[ADD|DEL|EDIT]#<SongName>[<SongURL>]"),
    SHOW_CLIENTS("Shows all live clients"),
    SHOW_DB("Shows all live servers"),
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