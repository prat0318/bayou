public class ProcessId implements Comparable {
    String name;

    public ProcessId(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !(o instanceof ProcessId)) return false;

        ProcessId processId = (ProcessId) o;

        if (name != null ? !name.equals(processId.name) : processId.name != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }

    public int compareTo(Object other) {
        return name.compareTo(((ProcessId) other).name);
    }

    public String toString() {
        return name;
    }
}