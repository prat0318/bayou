import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class Env {
    Map<ProcessId, Process> clientProcs = new HashMap<ProcessId, Process>();
    Map<ProcessId, Process> dbProcs = new HashMap<ProcessId, Process>();
    Map<ProcessId, Process> procs = new HashMap<ProcessId, Process>();
    int maxClientNo = 0;
    static final String TX_MSG_SEPARATOR = "\\$";
    static final String CLIENT_MSG_SEPARATOR = ":";
    static final String BODY_MSG_SEPERATOR = " ";
    ProcessId pid = new ProcessId("Main");

    synchronized void sendMessage(ProcessId dst, BayouMessage msg) {
        Process p = procs.get(dst);
        if (p != null) {
            p.deliver(msg);
        }
    }

    synchronized void addProc(ProcessId pid, Process proc) {
        procs.put(pid, proc);
        proc.start();
    }

    synchronized void removeProc(ProcessId pid) {
        procs.remove(pid);
    }


    public static void main(String[] args) throws Exception {
        Env e = new Env();
        e.run(args);
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            System.out.print("$ Enter new ReplicaCommand KILL|SHOW|TX|HELP > ");
            String input = br.readLine();
            e.operateOn(input);
        }
    }


    void run(String[] args) {
        try {
            Properties prop = new Properties();
            prop.load(new FileInputStream("config.properties"));
        } catch (Exception e) {
            System.out.println("Error while reading the properties file for the Operation");
       }
    }

    private void operateOn(String input) {
        String[] arr = input.split(BODY_MSG_SEPERATOR,2);
        String inputCommand = arr[0];
        String commandBody = arr[1];
        UserCommands c = null;
        try {
            c = UserCommands.valueOf(inputCommand);
        } catch (IllegalArgumentException e) {
            if(!("".equals(inputCommand)))
                System.err.println("Unknown Command! "+inputCommand);
            return;
        }
        switch (c) {
            case START_CLIENT:
                ProcessId pid = new ProcessId("client_"+maxClientNo++);
                Client c = new Client();
                break;
            case STOP_CLIENT:
                for(ProcessId p : procs.keySet()){
                    procs.get(p).assign_stop_request = true;
                    System.out.println("Scheduled Kill for " + p);
                    return;
                }
                System.out.println("Could not find such process...type SHOW for live clients");
                break;
            case SHOW_DB:
                for (ProcessId p : dbProcs.keySet()) {
                    System.out.print(p + " | ");
                }
                System.out.println();
                break;
            case SHOW_C:
                for (ProcessId p : clientProcs.keySet()) {
                    System.out.print(p + " | ");
                }
                System.out.println();
                break;
            case HELP:
                for (UserCommands cc : UserCommands.values()) {
                    System.out.println(cc + " -- " + cc.getDescription());
                }
                break;
        }

    }
}