import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.*;

public abstract class Process extends Thread {

    Logger logger;
    ProcessId me;
    Env env;
    Properties prop = new Properties();
    Queue<BayouMessage> inbox = new Queue<BayouMessage>();
    int delay;
    public boolean assign_stop_request = false;

    public Level messageLevel = Level.FINER;
    String my_name = "";

    public boolean stop_request(ProcessId whoGotKilled){
        try {
            Thread.sleep(this.delay);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if(assign_stop_request) {
            env.removeProc(whoGotKilled);
            logger.log(Level.SEVERE, whoGotKilled+" is getting killed. Bbye.");
        }
        return assign_stop_request;
    }

    public boolean stop_request() {
        return stop_request(me);
    }

    abstract void body();

    public void run() {
        body();
        env.removeProc(me);
    }

    Properties loadProp() {
        try {
            prop.load(new FileInputStream("config.properties"));
            if (prop.getProperty(me.name) != null) {
                delay = Integer.parseInt(prop.getProperty(me.name));
            } else {
                delay = Integer.parseInt(prop.getProperty("delay"));
            }
            messageLevel = "TRUE".equalsIgnoreCase(prop.getProperty("printMessages")) ? Level.CONFIG : Level.FINER;
        } catch (Exception e) {
            logger.log(Level.SEVERE, e.getMessage());
        }
        return prop;
    }

    BayouMessage getNextMessage() {
        return inbox.bdequeue();
    }

    BayouMessage getNextMessage(int timeout) {
        return inbox.bdequeue(timeout);
    }

    void sendMessage(ProcessId dst, BayouMessage msg) {
        this.logger.log(messageLevel, my_name + "SENT >>" + dst + ">> : " + msg);
        env.sendMessage(dst, msg);
    }

    void deliver(BayouMessage msg) {
        inbox.enqueue(msg);
        this.logger.log(messageLevel,my_name+ "RCVD <<" + msg.src_name + "<< : " + msg);
    }

    public void setLogger() {
        String loggerName = me.toString();
        logger = Logger.getLogger(loggerName);
        logger.setUseParentHandlers(false);
        logger.setLevel(Level.FINER);
        Handler consoleHandler = null;
        for (Handler handler : logger.getHandlers()) {
            if (handler instanceof ConsoleHandler) {
                consoleHandler = handler;
                break;
            }
        }
        if (consoleHandler == null) {
            //there was no console handler found, create a new one
            consoleHandler = new ConsoleHandler();
            logger.addHandler(consoleHandler);
        }
        consoleHandler.setLevel(Level.CONFIG);
        try {
            boolean clean = Boolean.parseBoolean(prop.getProperty("clean"));
            FileHandler fileHandler = new FileHandler("log/Log" + loggerName + ".log", !clean);
            fileHandler.setLevel(Level.FINER);
            logger.addHandler(fileHandler);
            SimpleFormatter formatter = new SimpleFormatter();
            fileHandler.setFormatter(formatter);

        } catch (SecurityException e) {
            logger.log(Level.SEVERE, e.getMessage());
        } catch (IOException e) {
            logger.log(Level.SEVERE, e.getMessage());
        }
    }

}
