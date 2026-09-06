package Core.console;

import Core.domain.TeamStanding;
import Core.domain.TournamentZone;

import java.util.List;

public class StandingsConsoleReporter {
    public void printZone(TournamentZone zone, List<TeamStanding> standings) {
        System.out.println();
        System.out.println("TABLE - " + zone.getName());
        System.out.println("POS TEAM                         PTS PJ PG PE PP GF GC  DG");

        int position = 1;
        for (TeamStanding standing : standings) {
            System.out.printf(
                    "%-3d %-28s %3d %2d %2d %2d %2d %2d %2d %+3d%n",
                    position++,
                    standing.getTeam().getName(),
                    standing.getPoints(),
                    standing.getPlayed(),
                    standing.getWon(),
                    standing.getDrawn(),
                    standing.getLost(),
                    standing.getGoalsFor(),
                    standing.getGoalsAgainst(),
                    standing.getGoalDifference()
            );
        }
    }
}
