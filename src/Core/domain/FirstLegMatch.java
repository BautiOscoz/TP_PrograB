package Core.domain;

import java.time.LocalDate;

public class FirstLegMatch extends Match {
    public FirstLegMatch(LocalDate matchDate, Team homeTeam, Team awayTeam, Referee referee) {
        super(matchDate, homeTeam, awayTeam, referee);
    }
}