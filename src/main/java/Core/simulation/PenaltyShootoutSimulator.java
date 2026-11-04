package Core.simulation;
import Core.domain.*;
import Core.incidents.PenaltyShootout;
import java.util.*;

public final class PenaltyShootoutSimulator {
    private final Random random;
    private final MatchSimulator matchSimulator;
    public PenaltyShootoutSimulator(Random random, MatchSimulator matchSimulator) {
        this.random = random;
        this.matchSimulator = matchSimulator;
    }
    public Team simulate(
            Match match
    ) {
        matchSimulator.prepareAdministrativeShootout(match);
        Team homeTeam = match.getHomeTeam();
        Team awayTeam = match.getAwayTeam();

        List<Player> homePlayers =
                match.getPlayersOnField(homeTeam, 90);

        List<Player> awayPlayers =
                match.getPlayersOnField(awayTeam, 90);

        int homeScore = 0;
        int awayScore = 0;


        // First five penalty kicks
        for (int i = 0; i < 5; i++) {

            if (simulatePenalty(
                    match,
                    homePlayers.get(i)
            )) {
                homeScore++;
            }

            if (simulatePenalty(
                    match,
                    awayPlayers.get(i)
            )) {
                awayScore++;
            }
        }

        if (homeScore > awayScore) {
            return homeTeam;
        }

        if (awayScore > homeScore) {
            return awayTeam;
        }

        // Sudden death
        int kickerIndex = 5;

        while (true) {

            boolean homeScored =
                    simulatePenalty(
                            match,
                            homePlayers.get(
                                    kickerIndex
                                            % homePlayers.size()
                            )
                    );

            boolean awayScored =
                    simulatePenalty(
                            match,
                            awayPlayers.get(
                                    kickerIndex
                                            % awayPlayers.size()
                            )
                    );

            if (homeScored && !awayScored) {
                return homeTeam;
            }

            if (!homeScored && awayScored) {
                return awayTeam;
            }

            kickerIndex++;
        }
    }
    private boolean simulatePenalty(
            Match match,
            Player player
    ) {
        boolean scored =
                random.nextDouble() < 0.75;

        match.addIncident(
                new PenaltyShootout(
                        90,
                        player,
                        scored
                )
        );

        return scored;
    }
}
