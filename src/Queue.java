import java.util.*;

public class Queue<T> {
    LinkedList<T> ll = new LinkedList<T>();

    public synchronized void enqueue(T obj) {
        ll.add(obj);
        notify();
    }

    public synchronized T bdequeue() {
        while (ll.size() == 0) {
            try {
                wait();
            } catch (InterruptedException e) {
            }
        }
        return ll.removeFirst();
    }

    public synchronized T bdequeue(int timeout) {

        while (ll.size() == 0) {
            try {
                wait(timeout);
                if(ll.size() == 0)
                    return null;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return ll.removeFirst();
    }
}
