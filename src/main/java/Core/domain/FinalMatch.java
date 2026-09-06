package Core.domain;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;

public class FinalMatch extends Match implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private boolean settledByPenalties;

    public FinalMatch(LocalDate matchDate, Team homeTeam, Team awayTeam, Referee referee) {
        super(matchDate, homeTeam, awayTeam, referee);
        this.settledByPenalties = false;
    }

    public boolean isSettledByPenalties() { return settledByPenalties; }
    public void setSettledByPenalties(boolean settledByPenalties) { this.settledByPenalties = settledByPenalties; }
}
