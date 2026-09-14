package Core.incidents;

import Core.domain.Player;

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

    public abstract String getDescription();

    public Player getScoringPlayer() { return null; }
    public Player getGoalkeeper() { return null; }
    public boolean isMatchGoal() { return false; }
    public boolean isPenaltyGoal() { return false; }
    public Player getAffectedPlayer() { return null; }
    public int getFairPlayPoints() { return 0; }
    public boolean isYellowCard() { return false; }
    public boolean isRedCard() { return false; }
    public Player getPlayerIn() { return null; }
    public Player getPlayerOut() { return null; }
}
