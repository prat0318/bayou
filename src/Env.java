import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
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
        if (proc instanceof Client)
            clientProcs.put(pid, proc);
        //REPLICA IS REGISTERED ONLY AFTER ASSIGNED NAME
        else if (proc instanceof Replica && ((Replica) proc).primary)
            dbProcs.put(pid, proc);
        procs.put(pid, proc);
        proc.start();
    }

    synchronized void removeProc(ProcessId pid) {
        if (procs.get(pid) instanceof Client)
            clientProcs.remove(pid);
        else if (procs.get(pid) instanceof Replica)
            dbProcs.remove(pid);
        procs.remove(pid);
    }

    void resetProperties() {
        try {
            FileInputStream in = new FileInputStream("config.properties");
            Properties prop = new Properties();
            prop.load(in);
            in.close();

            FileOutputStream out = new FileOutputStream("config.properties");
            for (String p : prop.stringPropertyNames()) {
                if(p.contains("client")){
                    prop.remove(p);
                }
            }
            prop.store(out, null);
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static void main(String[] args) throws Exception {
        Env e = new Env();
        e.resetProperties();
        e.run(args);
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            System.out.print("$ Enter new Command (HELP) > ");
            String input = br.readLine();
            e.operateOn(input);
        }
    }


    void run(String[] args) {
        try {
            Properties prop = new Properties();
            prop.load(new FileInputStream("config.properties"));
            int initialDbCount = Integer.parseInt(prop.getProperty("initialDbCount"));
            for (int i = 0; i < initialDbCount; i++) {
                ProcessId pid = new ProcessId("db_" + i);
                boolean primaryFlag = i == 0;
                Replica _ = new Replica(this, pid, primaryFlag);
            }
        } catch (Exception e) {
            System.out.println("Error while reading the properties file for the Operation");
        }
    }

    private void operateOn(String input) {
        String[] arr = input.split(BODY_MSG_SEPERATOR, 2);
        String inputCommand = arr[0];
        UserCommands c = null;
        try {
            c = UserCommands.valueOf(inputCommand);
        } catch (IllegalArgumentException e) {
            if (!("".equals(inputCommand)))
                System.err.println("Unknown Command! " + inputCommand);
            return;
        }
        ProcessId pid;
        switch (c) {
            case START_CLIENT:
                pid = new ProcessId("client_" + maxClientNo++);
                Client _client = new Client(this, pid);
                System.out.println("Started new Client " + pid);
                break;
            case STOP_CLIENT:
                for (ProcessId p : clientProcs.keySet()) {
                    if (p.name.equals(arr[1])) {
                        procs.get(p).assign_stop_request = true;
                        System.out.println("Scheduled Kill for " + p);
                        return;
                    }
                }
                System.out.println("Could not find such client...type SHOW_CLIENTS for live clients");
                break;
            case JOIN:
                pid = new ProcessId("db_" + arr[1]);
                Replica _db = new Replica(this, pid, false);
                System.out.println("Started new DB " + pid );
                break;
            case LEAVE:
                for (ProcessId p : dbProcs.keySet()) {
                    if (p.name.equals(arr[1])) {
                        procs.get(p).assign_stop_request = true;
                        System.out.println("Scheduled Kill for " + p);
                        return;
                    }
                }
                System.out.println("Could not find such db...type SHOW_DB for live dbs");
                break;
            case SHOW_DB:
                for (ProcessId p : dbProcs.keySet()) {
                    System.out.print(p + " | ");
                }
                System.out.println();
                break;
            case SHOW_CLIENTS:
                for (ProcessId p : clientProcs.keySet()) {
                    System.out.print(p + " | ");
                }
                System.out.println();
                break;
            case CONNECT:
                for (ProcessId p : dbProcs.keySet()) {
                    if (p.name.equals(arr[1])) {
                        dbProcs.get(p).disconnect = false;
                        synchronized (dbProcs.get(p)) {
                            dbProcs.get(p).notifyAll();
                        }
                        System.out.println("Set connect for " + p);
                        return;
                    }
                }
                System.out.println("Could not find such db...type SHOW_DB for live dbs");
                break;
            case DISCONNECT:
                for (ProcessId p : dbProcs.keySet()) {
                    if (p.name.equals(arr[1])) {
                        dbProcs.get(p).disconnect = true;
                        System.out.println("Set disconnect for " + p);
                        return;
                    }
                }
                System.out.println("Could not find such db...type SHOW_DB for live dbs");
                break;
            case OP:
                String[] opArr = arr[1].split(BODY_MSG_SEPERATOR, 2);
                for (ProcessId p : clientProcs.keySet()) {
                    if (p.name.equals(opArr[0])) {
                        sendMessage(p, new BayouMessage(this.pid, new RequestMessage(new RequestCommand(null, p, opArr[1]))));
                        return;
                    }
                    System.out.println("Could not find such db...type SHOW_DB for live dbs");
                }
                break;
            case CONTINUE:
                for (ProcessId p : dbProcs.keySet()) {
                    dbProcs.get(p).disconnect = false;
                    synchronized (dbProcs.get(p)) {
                        dbProcs.get(p).notifyAll();
                    }
                    System.out.println("Set connect for " + p);
                }
                System.out.println("Connected all the db ");
                break;
            case PAUSE:
                for (ProcessId p : dbProcs.keySet()) {
                    System.out.println("to Set disconnect for " + p);
                    dbProcs.get(p).disconnect = true;
                    System.out.println("Set disconnect for " + p);
                }
                System.out.println("Disconnected all the db ");
                break;
            case HELP:
                for (UserCommands cc : UserCommands.values()) {
                    System.out.println(cc + " -- " + cc.getDescription());
                }
                break;
        }

    }

}