package Core.domain;

import java.time.LocalDate;

public class FinalMatch extends Match {
    private boolean settledByPenalties;

    public FinalMatch(LocalDate matchDate, Team homeTeam, Team awayTeam, Referee referee) {
        super(matchDate, homeTeam, awayTeam, referee);
        this.settledByPenalties = false;
    }

    public boolean isSettledByPenalties() { return settledByPenalties; }
    public void setSettledByPenalties(boolean settledByPenalties) { this.settledByPenalties = settledByPenalties; }
}