import java.io.*;
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
    private static boolean scriptMode = false;
    private static long delay_interval = 1000;


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
                if (p.contains("client")) {
                    prop.remove(p);
                }
            }
            prop.store(out, null);
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void delay() {
        try {
            Thread.sleep(delay_interval);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws Exception {
        Env e = new Env();
        e.resetProperties();
        e.run(args);

        try {
            BufferedReader br = new BufferedReader(new FileReader(new File("script")));
            String input;
            while ((input = br.readLine()) != null) {
                if(input.startsWith("#")) continue;
                System.out.println("SCRIPT CMD: " + input);
                e.operateOn(input);
                delay();
            }
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        }

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
                ProcessId connectToDB = null;
                if (arr.length > 1)
                    for (ProcessId p : dbProcs.keySet())
                        if (p.name.equals(arr[1])) connectToDB = p;
                Client _client = new Client(this, pid, connectToDB);
                System.out.println("Started new Client " + pid);
                break;
            case RESTART_CLIENT:
                String[] splitArr = arr[1].split(BODY_MSG_SEPERATOR, 2);
                pid = new ProcessId(splitArr[0]);
                connectToDB = null;
                if (splitArr.length > 1)
                    for (ProcessId p : dbProcs.keySet())
                        if (p.name.equals(splitArr[1])) connectToDB = p;
                Client _reclient = new Client(this, pid, connectToDB);
                System.out.println("Restarted the Client " + pid);
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
                System.out.println("Started new DB " + pid);
                break;
            case RETIRE:
                for (ProcessId p : dbProcs.keySet()) {
                    if (p.name.equals(arr[1])) {
                        procs.get(p).assign_stop_request = true;
                        sendMessage(p, new BayouMessage(this.pid, new RetireMessage(null)));
                        System.out.println("Sent Retire Message to " + p);
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
                        sendMessage(p, new BayouMessage(this.pid, new NoOpMessage()));
                        System.out.println("Set disconnect for " + p);
                        return;
                    }
                }
                System.out.println("Could not find such db...type SHOW_DB for live dbs");
                break;
            case CURR_STATE:
                if(arr.length > 1) {
                    for (ProcessId p : dbProcs.keySet())
                        if (p.name.equals(arr[1])) {
                            ((Replica)dbProcs.get(p)).printMyState();
                            return;
                        }
                    System.out.println("Could not find such db...type SHOW_DB for live dbs");
                } else {
                    for (ProcessId p : dbProcs.keySet()) ((Replica)dbProcs.get(p)).printMyState();
                }
                break;
            case DISCONNECT_FROM:
                ProcessId disconnect1 = null;
                ProcessId disconnect2 = null;
                String[] disArr = arr[1].split(BODY_MSG_SEPERATOR, 2);
                for (ProcessId p : procs.keySet()) {
                    if (p.name.equals(disArr[0])) {
                        disconnect1 = p;
                    } else if (p.name.equals(disArr[1])) {
                        disconnect2 = p;
                    }
                }
                if (disconnect1 != null && disconnect2 != null) {
                    procs.get(disconnect1).disconnectFrom.add(disconnect2);
                    procs.get(disconnect2).disconnectFrom.add(disconnect1);
                    System.out.println("Set disconnect between " + disconnect1 + " and " + disconnect2);
                    return;
                }

                System.out.println("Could not find such dbs...type SHOW_DB for live dbs");
                break;
            case CONNECT_THEM:
                ProcessId connect1 = null;
                ProcessId connect2 = null;
                String[] conArr = arr[1].split(BODY_MSG_SEPERATOR, 2);
                for (ProcessId p : procs.keySet()) {
                    if (p.name.equals(conArr[0])) {
                        connect1 = p;
                    } else if (p.name.equals(conArr[1])) {
                        connect2 = p;
                    }
                }
                if (connect1 != null && connect2 != null) {
                    procs.get(connect1).disconnectFrom.remove(connect2);
                    procs.get(connect2).disconnectFrom.remove(connect1);
                    System.out.println("Removed disconnect between " + connect1 + " and " + connect2);
                    return;
                }

                System.out.println("Could not find such dbs...type SHOW_DB for live dbs");
                break;
            case OP:
                String[] opArr = arr[1].split(BODY_MSG_SEPERATOR, 2);
                for (ProcessId p : clientProcs.keySet()) {
                    if (p.name.equals(opArr[0])) {
                        sendMessage(p, new BayouMessage(this.pid, new RequestMessage(new RequestCommand(null, p, opArr[1]))));
                        return;
                    }
                }
                System.out.println("Could not find such client...type SHOW_CLIENTS for live clients");
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
                //TODO: TO CHECK IF PAUSE AND CONTINUE DO NOT MEAN THAT THE SERVERS PAUSE BUT IT IS CLIENTS DO NOT GIVE ANY INPUT
                for (ProcessId p : dbProcs.keySet()) {
                    dbProcs.get(p).disconnect = true;
                    sendMessage(p, new BayouMessage(this.pid, new NoOpMessage()));
                    System.out.println("Set disconnect for " + p);
                }
                System.out.println("Disconnected all the db ");
                break;
            case NO_OP:
                for (ProcessId p : dbProcs.keySet())
                    sendMessage(p, new BayouMessage(this.pid, new NoOpMessage()));
                break;
            case HELP:
                for (UserCommands cc : UserCommands.values()) {
                    System.out.println(cc + " -- " + cc.getDescription());
                }
                break;
            case QUIT:
                System.exit(1);
        }

    }

}