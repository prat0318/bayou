import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: bansal
 * Date: 22/11/13
 * Time: 8:27 PM
 * To change this template use File | Settings | File Templates.
 */

public class PlayList {

    private Map<String, String> songMap;

    public PlayList() {
        songMap = new HashMap<String, String>();
    }

    @Override
    public String toString() {
        String result = "";
        for (String s : songMap.keySet())
            result += "\n" + s + " " + songMap.get(s);
        return result;
    }

    public String add(String song, String url) {
        songMap.put(song, url);
        return "Added " + song;
    }

    public String edit(String song, String url) {
        if (!songMap.containsKey(song)) {
            return "ERROR: PlayList does not contain " + song;
        }

        songMap.put(song, url);
        return "Edited Song " + song;
    }

    public String delete(String song) {
        if (!songMap.containsKey(song)) {
            return "ERROR: PlayList does not contain " + song;
        }
        songMap.remove(song);
        return "Deleted Song " + song;
    }

    public boolean containsSong(String song) {
        return (songMap.get(song) != null);
    }

    public void clear() {
        songMap.clear();
    }

    public String show() {
        return songMap.toString();
    }


    void action(RequestCommand c) {
        String[] args = c.args.split(Env.TX_MSG_SEPARATOR);
        switch (c.type) {
            case ADD:
                c.response = add(args[0], args[1]);
                break;
            case DELETE:
                c.response = delete(args[0]);
                break;
            case EDIT:
                c.response = edit(args[0], args[1]);
                break;
            case SHOW:
                c.response = show();
                break;
            default:
                c.response = "INVALID OPERATION TYPE";
                break;
        }
    }
}

