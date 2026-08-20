package Core.incidents;

public abstract class Incident {
    private int minute;

    public Incident(int minute) {
        this.minute = minute;
    }

    public int getMinute() { return minute; }
    public void setMinute(int minute) { this.minute = minute; }
}