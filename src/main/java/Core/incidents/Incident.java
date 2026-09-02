package Core.incidents;

import java.io.Serial;
import java.io.Serializable;

public abstract class Incident implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private int minute;

    public Incident(int minute) {
        this.minute = minute;
    }

    public int getMinute() { return minute; }
    public void setMinute(int minute) { this.minute = minute; }
}