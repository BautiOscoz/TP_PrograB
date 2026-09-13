package Core.simulation;

import Core.domain.Lineup;
import Core.domain.Match;
import Core.domain.Player;
import Core.domain.Team;
import Core.enums.FormationType;
import Core.enums.Position;
import Core.incidents.Goal;
import Core.incidents.RedCard;
import Core.incidents.Substitution;
import Core.incidents.YellowCard;

import java.util.*;

public class MatchSimulator {
    private final Random random;

    public MatchSimulator(Random random) {
        this.random = random;
    }


    private FormationType chooseRandomFormation(Team team){
        List<FormationType> validFormation= Arrays.stream(FormationType.values())
                .filter(formation-> Lineup.canUseFormation(team,formation)).toList();

        if(validFormation.isEmpty())
            throw new IllegalStateException(team.getName()+"Doesnt have enougth player for any formation");

        int randomPosition= random.nextInt(validFormation.size());
        return validFormation.get(randomPosition);

    }

    public void simulate(Match match) {

        if (match.isPlayed()) throw new IllegalStateException("The match has already been played.");

        FormationType homeTactics = chooseRandomFormation(match.getHomeTeam());
        FormationType awayTactics = chooseRandomFormation(match.getAwayTeam());

        Lineup homeLineup = new Lineup(match.getHomeTeam(), homeTactics);
        Lineup awayLineup = new Lineup(match.getAwayTeam(), awayTactics);

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

    private void addGoals(Match match, int amount, Lineup attackers, Lineup defenders) {
        Player goalkeeper = defenders.getGoalkeeper();
        List<Player> outfieldScorers = attackers.getOutfieldPlayers();

        for (int i = 0; i < amount; i++) {
            boolean penalty = random.nextDouble() < 0.12;
            boolean ownGoal = !penalty && random.nextDouble() < 0.03;

            Player scorer = ownGoal
                    ? defenders.getOutfieldPlayers().get(random.nextInt(defenders.getOutfieldPlayers().size()))
                    : outfieldScorers.get(random.nextInt(outfieldScorers.size()));

            match.addIncident(new Goal(1 + random.nextInt(90), scorer, goalkeeper, penalty, ownGoal));
        }
    }

    private void addDisciplinaryIncidents(Match match, Lineup lineup) {
        int yellowCards = random.nextInt(4);
        Map<Player, Integer> yellowCardCounts = new HashMap<>();

        for (int i = 0; i < yellowCards; i++) {
            Player cautionedPlayer = lineup.getRandomPlayer(random);
            int minute = 1 + random.nextInt(90);
            match.addIncident(new YellowCard(minute, cautionedPlayer));

            int currentCount = yellowCardCounts.getOrDefault(cautionedPlayer, 0) + 1;
            yellowCardCounts.put(cautionedPlayer, currentCount);

            if (currentCount == 2) {
                cautionedPlayer.setSuspended(true);
                match.addIncident(new RedCard(Math.min(90, minute + 1), cautionedPlayer));
            }
        }

        if (random.nextDouble() < 0.10) {
            Player expelled = lineup.getRandomPlayer(random);
            if (!expelled.isSuspended()) {
                expelled.setSuspended(true);
                match.addIncident(new RedCard(1 + random.nextInt(90), expelled));
            }
        }
    }

    private void addSubstitutions(Match match, Team team, Lineup lineup) {
        List<Player> bench = new ArrayList<>(team.getPlayers().stream()
                .filter(player -> player.getPosition() != Position.GOALKEEPER).toList());
        bench.removeAll(lineup.getPlayers());
        Collections.shuffle(bench, random);

        List<Player> candidatesToLeave = new ArrayList<>(lineup.getOutfieldPlayers());
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