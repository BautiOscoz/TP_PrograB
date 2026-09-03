package Core.domain;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;

public class FirstLegMatch extends Match implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    public FirstLegMatch(LocalDate matchDate, Team homeTeam, Team awayTeam, Referee referee,Lineup homeLineup, Lineup awayLineup) {
        super(matchDate, homeTeam, awayTeam, referee,homeLineup,awayLineup);
    }
}