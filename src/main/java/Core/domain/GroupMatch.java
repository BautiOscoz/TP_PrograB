package Core.domain;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;

public class GroupMatch extends Match implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private int matchday;

    public GroupMatch(LocalDate matchDate, Team homeTeam, Team awayTeam, Referee referee, int matchday) {
        super(matchDate, homeTeam, awayTeam, referee);
        if (matchday < 1 || matchday > 3) {
            throw new IllegalArgumentException("A group-stage matchday must be between 1 and 3.");
        }
        this.matchday = matchday;
    }

    public int getMatchday() { return matchday; }
    public void setMatchday(int matchday) { this.matchday = matchday; }
}
