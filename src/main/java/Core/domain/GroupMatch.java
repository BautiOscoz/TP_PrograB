package Core.domain;

import java.time.LocalDate;

public class GroupMatch extends Match {
    public GroupMatch(LocalDate matchDate, Team homeTeam, Team awayTeam, Referee referee) {
        super(matchDate, homeTeam, awayTeam, referee);
    }
}