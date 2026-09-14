package Core.incidents;

import Core.domain.Player;

import java.io.Serial;
import java.io.Serializable;

public class Goal extends Incident implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private Player scorer;
    private Player goalkeeper;
    private boolean isPenalty;
    private boolean isOwnGoal;

    public Goal(int minute, Player scorer, Player goalkeeper, boolean isPenalty, boolean isOwnGoal) {
        super(minute);
        this.scorer = scorer;
        this.goalkeeper = goalkeeper;
        this.isPenalty = isPenalty;
        this.isOwnGoal = isOwnGoal;
    }

    public Player getScorer() { return scorer; }
    public boolean isPenalty() { return isPenalty; }
    public boolean isOwnGoal() { return isOwnGoal; }

    @Override
    public String getDescription() {
        String type = isOwnGoal ? "OWN GOAL" : isPenalty ? "PENALTY GOAL" : "GOAL";
        return type + " - " + scorer.getName() + " " + scorer.getLastName();
    }

    @Override
    public Player getScoringPlayer() {
        return isOwnGoal ? null : scorer;
    }

    @Override
    public Player getGoalkeeper() {
        return goalkeeper;
    }

    @Override
    public boolean isMatchGoal() {
        return true;
    }

    @Override
    public boolean isPenaltyGoal() {
        return isPenalty;
    }
}
