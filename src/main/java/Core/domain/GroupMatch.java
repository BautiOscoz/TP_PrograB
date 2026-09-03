package Core.domain;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;

public class GroupMatch extends Match implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    public GroupMatch(LocalDate matchDate, Team homeTeam, Team awayTeam, Referee referee,Lineup homeLineup, Lineup awayLineup) {
        super(matchDate, homeTeam, awayTeam, referee,homeLineup,awayLineup);
    }
}