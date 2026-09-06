package Core.domain;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;

public class SecondLegMatch extends Match implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private int firstLegHomeGoals;
    private int firstLegAwayGoals;
    private boolean settledByPenalties;

    public SecondLegMatch(LocalDate matchDate, Team homeTeam, Team awayTeam, Referee referee, int firstLegHomeGoals, int firstLegAwayGoals) {
        super(matchDate, homeTeam, awayTeam, referee);
        this.firstLegHomeGoals = firstLegHomeGoals;
        this.firstLegAwayGoals = firstLegAwayGoals;
        this.settledByPenalties = false;
    }

    public int getFirstLegHomeGoals() { return firstLegHomeGoals; }
    public int getFirstLegAwayGoals() { return firstLegAwayGoals; }
    public boolean isSettledByPenalties() { return settledByPenalties; }
    public void setSettledByPenalties(boolean settledByPenalties) { this.settledByPenalties = settledByPenalties; }
}