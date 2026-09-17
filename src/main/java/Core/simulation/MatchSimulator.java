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
import java.util.stream.Collectors;

public class MatchSimulator {
    private enum PlannedIncidentType {
        YELLOW_CARD,
        RED_CARD,
        SUBSTITUTION
    }

    private record PlannedIncident(int minute, PlannedIncidentType type) {}

    private final Random random;

    public MatchSimulator(Random random) {
        this.random = random;
    }


    private FormationType chooseRandomFormation(Team team) {
        List<FormationType> validFormations = Arrays.stream(FormationType.values())
                .filter(formation -> Lineup.canUseFormation(team, formation))
                .toList();

        if (validFormations.isEmpty()) {
            throw new IllegalStateException(
                    team.getName() + " does not have enough players for any formation."
            );
        }

        return validFormations.get(random.nextInt(validFormations.size()));
    }

    private Set<Player> getSuspendedPlayers(Team team) {
        return team.getPlayers().stream()
                .filter(Player::isSuspended)
                .collect(Collectors.toSet());
    }

    public void simulate(Match match) {

        if (match.isPlayed()) throw new IllegalStateException("The match has already been played.");

        Set<Player> unavailableHomePlayers = getSuspendedPlayers(match.getHomeTeam());
        Set<Player> unavailableAwayPlayers = getSuspendedPlayers(match.getAwayTeam());
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

        addTeamIncidents(match, match.getHomeTeam(), homeLineup, unavailableHomePlayers);
        addTeamIncidents(match, match.getAwayTeam(), awayLineup, unavailableAwayPlayers);
        addGoals(match, homeGoals, match.getHomeTeam(), match.getAwayTeam());
        addGoals(match, awayGoals, match.getAwayTeam(), match.getHomeTeam());

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

    private void addGoals(Match match, int amount, Team attackingTeam, Team defendingTeam) {
        List<Integer> goalMinutes = new ArrayList<>();
        for (int i = 0; i < amount; i++) {
            goalMinutes.add(1 + random.nextInt(90));
        }
        Collections.sort(goalMinutes);

        for (int minute : goalMinutes) {
            List<Player> attackers = match.getPlayersOnField(attackingTeam, minute);
            List<Player> defenders = match.getPlayersOnField(defendingTeam, minute);
            List<Player> attackingOutfieldPlayers = outfieldPlayers(attackers);
            List<Player> defendingOutfieldPlayers = outfieldPlayers(defenders);

            boolean penalty = random.nextDouble() < 0.12;
            boolean ownGoal = !penalty && random.nextDouble() < 0.03;

            Player scorer = ownGoal
                    ? randomPlayer(defendingOutfieldPlayers.isEmpty() ? defenders : defendingOutfieldPlayers)
                    : randomPlayer(attackingOutfieldPlayers.isEmpty() ? attackers : attackingOutfieldPlayers);

            Player goalkeeper = defenders.stream()
                    .filter(player -> player.getPosition() == Position.GOALKEEPER)
                    .findFirst()
                    .orElseGet(() -> randomPlayer(defenders));

            match.addIncident(new Goal(minute, scorer, goalkeeper, penalty, ownGoal));
        }
    }

    private List<Player> outfieldPlayers(List<Player> players) {
        return players.stream()
                .filter(player -> player.getPosition() != Position.GOALKEEPER)
                .toList();
    }

    private Player randomPlayer(List<Player> players) {
        if (players.isEmpty()) {
            throw new IllegalStateException("There are no eligible players for this incident.");
        }
        return players.get(random.nextInt(players.size()));
    }

    private void addTeamIncidents(
            Match match,
            Team team,
            Lineup lineup,
            Set<Player> unavailablePlayers
    ) {
        List<Player> bench = new ArrayList<>(
                team.getPlayers().stream()
                        .filter(player -> player.getPosition() != Position.GOALKEEPER)
                        .filter(player -> !unavailablePlayers.contains(player))
                        .toList()
        );
        bench.removeAll(lineup.getPlayers());
        Collections.shuffle(bench, random);

        List<PlannedIncident> plans = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            plans.add(new PlannedIncident(
                    55 + random.nextInt(36),
                    PlannedIncidentType.SUBSTITUTION
            ));
        }
        for (int i = 0; i < random.nextInt(4); i++) {
            plans.add(new PlannedIncident(
                    1 + random.nextInt(90),
                    PlannedIncidentType.YELLOW_CARD
            ));
        }
        if (random.nextDouble() < 0.10) {
            plans.add(new PlannedIncident(
                    1 + random.nextInt(90),
                    PlannedIncidentType.RED_CARD
            ));
        }

        plans.sort(
                Comparator.comparingInt(PlannedIncident::minute)
                        .thenComparing(PlannedIncident::type)
        );

        Map<Player, Integer> yellowCardCounts = new HashMap<>();
        for (PlannedIncident plan : plans) {
            List<Player> playersOnField = new ArrayList<>(
                    match.getPlayersOnField(team, plan.minute())
            );

            switch (plan.type()) {
                case YELLOW_CARD -> addYellowCard(
                        match,
                        playersOnField,
                        yellowCardCounts,
                        plan.minute()
                );
                case RED_CARD -> addRedCard(match, playersOnField, plan.minute());
                case SUBSTITUTION -> addSubstitution(
                        match,
                        playersOnField,
                        bench,
                        plan.minute()
                );
            }
        }
    }

    private void addYellowCard(
            Match match,
            List<Player> playersOnField,
            Map<Player, Integer> yellowCardCounts,
            int minute
    ) {
        if (playersOnField.isEmpty()) return;

        Player cautionedPlayer = randomPlayer(playersOnField);
        match.addIncident(new YellowCard(minute, cautionedPlayer));

        int currentCount = yellowCardCounts.getOrDefault(cautionedPlayer, 0) + 1;
        yellowCardCounts.put(cautionedPlayer, currentCount);
        if (currentCount == 2) {
            cautionedPlayer.setSuspended(true);
            match.addIncident(new RedCard(minute, cautionedPlayer));
        }
    }

    private void addRedCard(Match match, List<Player> playersOnField, int minute) {
        if (playersOnField.isEmpty()) return;

        Player expelledPlayer = randomPlayer(playersOnField);
        expelledPlayer.setSuspended(true);
        match.addIncident(new RedCard(minute, expelledPlayer));
    }

    private void addSubstitution(
            Match match,
            List<Player> playersOnField,
            List<Player> bench,
            int minute
    ) {
        List<Player> candidatesToLeave = new ArrayList<>(outfieldPlayers(playersOnField));
        Collections.shuffle(candidatesToLeave, random);

        for (Player playerIn : bench) {
            Player playerOut = candidatesToLeave.stream()
                    .filter(candidate -> candidate.getPosition() == playerIn.getPosition())
                    .findFirst()
                    .orElse(null);

            if (playerOut != null) {
                match.addIncident(new Substitution(minute, playerIn, playerOut));
                bench.remove(playerIn);
                return;
            }
        }
    }
}
