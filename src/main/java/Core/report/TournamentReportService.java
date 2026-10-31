package Core.report;

import Core.Run.Championship;
import Core.domain.Match;
import Core.domain.ParticipationTimeline;
import Core.domain.Player;
import Core.domain.Referee;
import Core.domain.Team;
import Core.domain.TeamStanding;
import Core.incidents.Incident;
import Core.enums.Position;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TournamentReportService {

    public List<TopScorerRow> getTopScorers(Championship championship) {
        Map<Player, TopScorerRow> rows = new HashMap<>();
        Map<Player, Team> teams = indexTeams(championship);

        for (Match match : championship.getPlayedMatches()) {
            for (Incident incident : match.getIncidents()) {
                Player scorer = incident.getScoringPlayer();
                if (scorer != null) {
                    TopScorerRow row = rows.computeIfAbsent(
                            scorer,
                            player -> new TopScorerRow(player, teams.get(player), 0, 0)
                    );
                    row.addGoal(incident.isPenaltyGoal());
                }
            }
        }

        return rows.values().stream()
                .sorted(Comparator.comparingInt(TopScorerRow::getGoals).reversed()
                        .thenComparing(row -> row.getPlayer().getLastName()))
                .toList();
    }

    public List<ParticipationRow> getParticipation(Championship championship) {
        List<ParticipationRow> rows = new ArrayList<>();
        Map<Player, PlayerTotals> totals = collectPlayerTotals(championship);
        for (Team team : championship.getTeams()) {
            for (Player player : team.getPlayers()) {
                PlayerTotals total = totals.get(player);
                rows.add(new ParticipationRow(player, team, total.matches, total.minutes));
            }
        }

        return rows.stream()
                .sorted(
                        Comparator.comparingInt(ParticipationRow::getMinutes)
                                .reversed()
                                .thenComparing(row ->
                                        row.getPlayer().getLastName())
                )
                .toList();
    }

    /** Local to one report request: never cached or serialized. */
    private static class PlayerTotals {
        int matches;
        int minutes;
        int goals;
        int goalsAgainst;
    }

    private Map<Player, PlayerTotals> collectPlayerTotals(Championship championship) {
        Map<Player, PlayerTotals> totals = new HashMap<>();
        championship.getTeams().forEach(team -> team.getPlayers()
                .forEach(player -> totals.put(player, new PlayerTotals())));
        for (Match match : championship.getPlayedMatches()) {
            boolean countMinutes = !match.isForfeit() && match.getHomeLineup() != null
                    && match.getAwayLineup() != null;
            ParticipationTimeline timeline = new ParticipationTimeline(
                    countMinutes ? match.getHomeLineup().getPlayers() : List.of(),
                    countMinutes ? match.getAwayLineup().getPlayers() : List.of());
            for (Incident incident : match.getIncidents()) {
                PlayerTotals scorer = totals.get(incident.getScoringPlayer());
                if (scorer != null) scorer.goals++;
                PlayerTotals keeper = totals.get(incident.getGoalkeeper());
                if (incident.isMatchGoal() && keeper != null) keeper.goalsAgainst++;
                if (countMinutes) incident.applyToParticipation(timeline);
            }
            timeline.minutesPlayed().forEach((player, minutes) -> {
                PlayerTotals total = totals.get(player);
                if (total != null) {
                    total.matches++;
                    total.minutes += minutes;
                }
            });
        }
        return totals;
    }

    public List<FairPlayRow> getFairPlay(Championship championship) {
        List<FairPlayRow> rows = new ArrayList<>();

        Map<Player, Team> teams = indexTeams(championship);
        Map<Team, int[]> counts = new HashMap<>();
        championship.getTeams().forEach(team -> counts.put(team, new int[3]));
        for (Match match : championship.getPlayedMatches()) {
            for (Incident incident : match.getIncidents()) {
                int[] count = counts.get(teams.get(incident.getAffectedPlayer()));
                if (count != null) {
                    count[0] += incident.isYellowCard() ? 1 : 0;
                    count[1] += incident.isRedCard() ? 1 : 0;
                    count[2] += incident.getFairPlayPoints();
                }
            }
        }
        for (Team team : championship.getTeams()) {
            int[] count = counts.get(team);
            rows.add(new FairPlayRow(team, count[0], count[1], count[2]));
        }

        return rows.stream()
                .sorted(Comparator.comparingInt(FairPlayRow::getPoints)
                        .thenComparing(row -> row.getTeam().getName()))
                .toList();
    }

    public List<TeamStatsRow> getTeamStats(Championship championship) {
        List<TeamStatsRow> rows = new ArrayList<>();

        for (Team team : championship.getTeams()) {
            TeamStanding standing = getOverallStanding(championship, team);

            double averageAge = team.getPlayers().stream()
                    .mapToInt(player -> getAge(player.getBirthDate()))
                    .average()
                    .orElse(0);

            int coachAge = getAge(team.getCoach().getBirthDate());

            double effectiveness = 0;

            if (standing.getPlayed() > 0) {
                effectiveness = standing.getPoints() * 100.0
                        / (standing.getPlayed() * 3.0);
            }

            rows.add(new TeamStatsRow(
                    team,
                    averageAge,
                    getFullName(team.getCoach()),
                    coachAge,
                    team.getCoach().getNationality().getName(),
                    standing.getGoalsFor(),
                    standing.getGoalsAgainst(),
                    effectiveness
            ));
        }

        return rows.stream()
                .sorted(Comparator.comparing(row -> row.getTeam().getName()))
                .toList();
    }

    public List<PlayerStatsRow> getPlayerStats(
            Championship championship
    ) {
        return getPlayerStats(championship, null);
    }

    // position == null significa "Todos".
    public List<PlayerStatsRow> getPlayerStats(
            Championship championship,
            Position position
    ) {
        List<PlayerStatsRow> rows = new ArrayList<>();
        Map<Player, PlayerTotals> totals = collectPlayerTotals(championship);

        for (Team team : championship.getTeams()) {
            for (Player player : team.getPlayers()) {

                if (position == null || player.getPosition() == position) {

                    PlayerTotals total = totals.get(player);

                    rows.add(new PlayerStatsRow(
                            player,
                            team,
                            player.getPosition().name(),
                            total.matches,
                            total.minutes,
                            total.goals,
                            total.goalsAgainst
                    ));
                }
            }
        }

        return rows.stream()
                .sorted(Comparator.comparing(row ->
                        row.getPlayer().getLastName()))
                .toList();
    }

    public List<RefereeStatsRow> getRefereeStats(Championship championship) {
        List<RefereeStatsRow> rows = new ArrayList<>();

        for (Referee referee : championship.getReferees()) {
            long matches = championship.getPlayedMatches().stream()
                    .filter(match -> !match.isForfeit())
                    .filter(match -> match.getReferee().equals(referee))
                    .count();
            rows.add(new RefereeStatsRow(
                    referee,
                    (int) matches,
                    referee.getRefereeYears()
            ));
        }

        return rows.stream()
                .sorted(Comparator.comparingInt(RefereeStatsRow::getMatches).reversed()
                        .thenComparing(row -> row.getReferee().getLastName()))
                .toList();
    }

    private Map<Player, Team> indexTeams(Championship championship) {
        Map<Player, Team> teams = new HashMap<>();
        championship.getTeams().forEach(team -> team.getPlayers()
                .forEach(player -> teams.putIfAbsent(player, team)));
        return teams;
    }

    private TeamStanding getOverallStanding(
            Championship championship,
            Team team
    ) {
        TeamStanding standing = new TeamStanding(team);

        for (Match match : championship.getPlayedMatches()) {
            if (match.getHomeTeam() == team) {
                standing.registerMatch(
                        match.getHomeGoals(),
                        match.getAwayGoals()
                );
            } else if (match.getAwayTeam() == team) {
                standing.registerMatch(
                        match.getAwayGoals(),
                        match.getHomeGoals()
                );
            }
        }

        return standing;
    }


    public double getAverageRefereeExperience(
            Championship championship
    ) {
        return championship.getReferees().stream()
                .mapToInt(Referee::getRefereeYears)
                .average()
                .orElse(0.0);
    }
    private int getAge(LocalDate birthDate) {
        return Period.between(birthDate, LocalDate.now()).getYears();
    }

    private String getFullName(Core.domain.Person person) {
        return person.getName() + " " + person.getLastName();
    }

    public static class TopScorerRow {
        private final Player player;
        private final Team team;
        private int goals;
        private int penaltyGoals;

        public TopScorerRow(Player player, Team team, int goals, int penaltyGoals) {
            this.player = player;
            this.team = team;
            this.goals = goals;
            this.penaltyGoals = penaltyGoals;
        }

        public void addGoal(boolean penalty) {
            goals++;
            if (penalty) penaltyGoals++;
        }

        public Player getPlayer() { return player; }
        public Team getTeam() { return team; }
        public int getGoals() { return goals; }
        public int getPenaltyGoals() { return penaltyGoals; }
    }

    public static class ParticipationRow {
        private final Player player;
        private final Team team;
        private final int matches;
        private final int minutes;

        public ParticipationRow(Player player, Team team, int matches, int minutes) {
            this.player = player;
            this.team = team;
            this.matches = matches;
            this.minutes = minutes;
        }

        public Player getPlayer() { return player; }
        public Team getTeam() { return team; }
        public int getMatches() { return matches; }
        public int getMinutes() { return minutes; }
    }

    public static class FairPlayRow {
        private final Team team;
        private final int yellow;
        private final int red;
        private final int points;

        public FairPlayRow(Team team, int yellow, int red, int points) {
            this.team = team;
            this.yellow = yellow;
            this.red = red;
            this.points = points;
        }

        public Team getTeam() { return team; }
        public int getYellow() { return yellow; }
        public int getRed() { return red; }
        public int getPoints() { return points; }
    }

    public static class TeamStatsRow {
        private final Team team;
        private final double averageAge;
        private final String coach;
        private final int coachAge;
        private final String coachNationality;
        private final int goalsFor;
        private final int goalsAgainst;
        private final double effectiveness;

        public TeamStatsRow(Team team, double averageAge, String coach, int coachAge,
                            String coachNationality, int goalsFor, int goalsAgainst,
                            double effectiveness) {
            this.team = team;
            this.averageAge = averageAge;
            this.coach = coach;
            this.coachAge = coachAge;
            this.coachNationality = coachNationality;
            this.goalsFor = goalsFor;
            this.goalsAgainst = goalsAgainst;
            this.effectiveness = effectiveness;
        }

        public Team getTeam() { return team; }
        public double getAverageAge() { return averageAge; }
        public String getCoach() { return coach; }
        public int getCoachAge() { return coachAge; }
        public String getCoachNationality() { return coachNationality; }
        public int getGoalsFor() { return goalsFor; }
        public int getGoalsAgainst() { return goalsAgainst; }
        public double getEffectiveness() { return effectiveness; }
    }

    public static class PlayerStatsRow {
        private final Player player;
        private final Team team;
        private final String position;
        private final int matches;
        private final int minutes;
        private final int goals;
        private final int goalsAgainst;

        public PlayerStatsRow(Player player, Team team, String position, int matches,
                              int minutes, int goals, int goalsAgainst) {
            this.player = player;
            this.team = team;
            this.position = position;
            this.matches = matches;
            this.minutes = minutes;
            this.goals = goals;
            this.goalsAgainst = goalsAgainst;
        }

        public Player getPlayer() { return player; }
        public Team getTeam() { return team; }
        public String getPosition() { return position; }
        public int getMatches() { return matches; }
        public int getMinutes() { return minutes; }
        public int getGoals() { return goals; }
        public int getGoalsAgainst() { return goalsAgainst; }
        public double getAverageGoalsAgainst() {
            return matches == 0
                    ? 0.0
                    : (double) goalsAgainst / matches;
        }
    }

    public static class RefereeStatsRow {
        private final Referee referee;
        private final int matches;
        private final int years;

        public RefereeStatsRow(Referee referee, int matches, int years) {
            this.referee = referee;
            this.matches = matches;
            this.years = years;
        }

        public Referee getReferee() { return referee; }
        public int getMatches() { return matches; }
        public int getYears() { return years; }
    }
}
