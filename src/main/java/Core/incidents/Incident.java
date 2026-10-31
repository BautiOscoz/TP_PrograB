package Core.incidents;

import Core.domain.Player;
import Core.domain.Team;
import Core.domain.ParticipationTimeline;
import java.util.List;

import java.io.Serial;
import java.io.Serializable;

public abstract class Incident implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private final int minute;

    public Incident(int minute) {
        if (minute < 0) throw new IllegalArgumentException("Incident minute cannot be negative.");
        this.minute = minute;
    }

    public int getMinute() { return minute; }

    public abstract String getDescription();

    public void applyToParticipation(ParticipationTimeline timeline) {
        // Goals, cards without expulsion and shootout penalties do not change participation.
    }

    /** Replays this event on a temporary lineup; never changes the saved lineup or suspensions. */
    public void applyToLineup(Team team, List<Player> playersOnField) {
        // Most incidents do not change which players are on the field.
    }

    /** Player whose team is identified when displaying this event. */
    public Player getParticipant() { return getAffectedPlayer(); }

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
    public boolean isShootoutPenalty() { return false; }
    public Player getPenaltyTaker() { return null; }
    public boolean isPenaltyScored() { return false; }
}
