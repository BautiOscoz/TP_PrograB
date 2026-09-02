package Core.simulation;

import Core.domain.Match;
import Core.domain.Player;
import Core.domain.Team;
import Core.enums.Position;
import Core.incidents.Goal;
import Core.incidents.RedCard;
import Core.incidents.Substitution;
import Core.incidents.YellowCard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

public class MatchSimulator {
    private final Random random;

    public MatchSimulator(Random random) {
        this.random = random;
    }

    public void simulate(Match match) {
        if (match.isPlayed()) throw new IllegalStateException("The match has already been played.");
        List<Player> homeLineup = selectLineup(match.getHomeTeam());
        List<Player> awayLineup = selectLineup(match.getAwayTeam());
        match.setInitialLineups(homeLineup, awayLineup);

        double homeStrength = match.getHomeTeam().getCompetitiveStrength() + 3.0;
        double awayStrength = match.getAwayTeam().getCompetitiveStrength();
        double surprise = random.nextGaussian() * 12.0;
        int homeGoals = sampleGoals(1.25 + (homeStrength - awayStrength + surprise) / 35.0);
        int awayGoals = sampleGoals(1.10 + (awayStrength - homeStrength - surprise) / 35.0);
        match.setHomeGoals(homeGoals);
        match.setAwayGoals(awayGoals);
        addGoals(match, homeGoals, homeLineup, awayLineup);
        addGoals(match, awayGoals, awayLineup, homeLineup);
        addDisciplinaryIncidents(match, homeLineup);
        addDisciplinaryIncidents(match, awayLineup);
        addSubstitutions(match, match.getHomeTeam(), homeLineup);
        addSubstitutions(match, match.getAwayTeam(), awayLineup);
        match.setPlayed(true);
    }

    private List<Player> selectLineup(Team team) {
        List<Player> available = team.getPlayers().stream()
                .filter(player -> !player.serveSuspensionIfNeeded())
                .sorted(Comparator.comparingDouble(Player::getAverageRating).reversed()).toList();
        List<Player> lineup = new ArrayList<>();
        addByPosition(available, lineup, Position.GOALKEEPER, 1);
        addByPosition(available, lineup, Position.DEFENDER, 4);
        addByPosition(available, lineup, Position.MIDFIELDER, 3);
        addByPosition(available, lineup, Position.FORWARD, 3);
        if (lineup.size() != 11) {
            throw new IllegalStateException(team.getName() + " does not have enough available players for a 4-3-3 lineup.");
        }
        return lineup;
    }

    private void addByPosition(List<Player> available, List<Player> lineup, Position position, int amount) {
        available.stream().filter(player -> player.getPosition() == position).limit(amount).forEach(lineup::add);
    }

    private int sampleGoals(double rawExpectedGoals) {
        double expectedGoals = Math.max(0.25, Math.min(3.5, rawExpectedGoals));
        double limit = Math.exp(-expectedGoals);
        double product = 1.0;
        int goals = 0;
        do {
            goals++;
            product *= random.nextDouble();
        } while (product > limit && goals < 7);
        return goals - 1;
    }

    private void addGoals(Match match, int amount, List<Player> attackers, List<Player> defenders) {
        Player goalkeeper = defenders.stream().filter(p -> p.getPosition() == Position.GOALKEEPER).findFirst().orElseThrow();
        List<Player> scorers = attackers.stream().filter(p -> p.getPosition() != Position.GOALKEEPER).toList();
        for (int i = 0; i < amount; i++) {
            boolean penalty = random.nextDouble() < 0.12;
            boolean ownGoal = !penalty && random.nextDouble() < 0.03;
            Player scorer = ownGoal
                    ? defenders.stream().filter(p -> p.getPosition() != Position.GOALKEEPER).toList()
                            .get(random.nextInt(10))
                    : scorers.get(random.nextInt(scorers.size()));
            match.addIncident(new Goal(1 + random.nextInt(90), scorer, goalkeeper, penalty, ownGoal));
        }
    }

    private void addDisciplinaryIncidents(Match match, List<Player> lineup) {
        int yellowCards = random.nextInt(4);
        for (int i = 0; i < yellowCards; i++) {
            match.addIncident(new YellowCard(1 + random.nextInt(90), lineup.get(random.nextInt(lineup.size()))));
        }
        if (random.nextDouble() < 0.10) {
            Player expelled = lineup.get(random.nextInt(lineup.size()));
            expelled.setSuspended(true);
            match.addIncident(new RedCard(1 + random.nextInt(90), expelled));
        }
    }

    private void addSubstitutions(Match match, Team team, List<Player> lineup) {
        List<Player> bench = new ArrayList<>(team.getPlayers().stream()
                .filter(player -> player.getPosition() != Position.GOALKEEPER).toList());
        bench.removeAll(lineup);
        Collections.shuffle(bench, random);
        List<Player> candidatesToLeave = new ArrayList<>(lineup.stream()
                .filter(player -> player.getPosition() != Position.GOALKEEPER).toList());
        Collections.shuffle(candidatesToLeave, random);

        int substitutions = 0;
        for (Player playerIn : bench) {
            Player playerOut = candidatesToLeave.stream()
                    .filter(candidate -> candidate.getPosition() == playerIn.getPosition())
                    .findFirst().orElse(null);
            if (playerOut != null) {
                match.addIncident(new Substitution(55 + random.nextInt(36), playerIn, playerOut));
                candidatesToLeave.remove(playerOut);
                substitutions++;
                if (substitutions == 3) break;
            }
        }
    }
}
