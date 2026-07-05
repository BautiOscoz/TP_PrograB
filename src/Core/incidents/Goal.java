package Core.incidents;

import Core.domain.Player;

public class Goal extends Incident {
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
    public Player getGoalkeeper() { return goalkeeper; }
    public boolean isPenalty() { return isPenalty; }
    public boolean isOwnGoal() { return isOwnGoal; }
}