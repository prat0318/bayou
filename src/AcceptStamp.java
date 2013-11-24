public class AcceptStamp {
    static final String SEPARATOR = "#";
    public int acceptClock;
    public ProcessId replica;

    AcceptStamp(int acceptClock, ProcessId replica) {
        this.acceptClock = acceptClock;
        this.replica = replica;
    }

    @Override
    public String toString() {
        return replica + SEPARATOR + acceptClock;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AcceptStamp that = (AcceptStamp) o;

        if (acceptClock != that.acceptClock) return false;
        if (replica != null ? !replica.equals(that.replica) : that.replica != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = acceptClock;
        result = 31 * result + (replica != null ? replica.hashCode() : 0);
        return result;
    }
}

