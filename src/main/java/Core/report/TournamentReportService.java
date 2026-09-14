package Core.report;

import Core.Run.Championship;
import Core.domain.Match;
import Core.domain.Player;
import Core.domain.Referee;
import Core.domain.Team;
import Core.domain.TeamStanding;
import Core.incidents.Incident;

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

        for (Match match : championship.getPlayedMatches()) {
            for (Incident incident : match.getIncidents()) {
                Player scorer = incident.getScoringPlayer();
                if (scorer != null) {
                    TopScorerRow row = rows.computeIfAbsent(
                            scorer,
                            player -> new TopScorerRow(player, getTeam(championship, player), 0, 0)
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

        for (Team team : championship.getTeams()) {
            for (Player player : team.getPlayers()) {
                int matches = 0;
                int minutes = 0;

                for (Match match : championship.getPlayedMatches()) {
                    int playedMinutes = getPlayedMinutes(match, player);
                    if (playedMinutes > 0) {
                        matches++;
                        minutes += playedMinutes;
                    }
                }

                rows.add(new ParticipationRow(player, team, matches, minutes));
            }
        }

        return rows.stream()
                .sorted(Comparator.comparingInt(ParticipationRow::getMinutes).reversed()
                        .thenComparing(row -> row.getPlayer().getLastName()))
                .toList();
    }

    public List<FairPlayRow> getFairPlay(Championship championship) {
        List<FairPlayRow> rows = new ArrayList<>();

        for (Team team : championship.getTeams()) {
            int yellow = 0;
            int red = 0;
            int points = 0;

            for (Match match : championship.getPlayedMatches()) {
                for (Incident incident : match.getIncidents()) {
                    Player player = incident.getAffectedPlayer();
                    if (player != null && team.getPlayers().contains(player)) {
                        points += incident.getFairPlayPoints();
                        yellow += incident.isYellowCard() ? 1 : 0;
                        red += incident.isRedCard() ? 1 : 0;
                    }
                }
            }

            rows.add(new FairPlayRow(team, yellow, red, points));
        }

        return rows.stream()
                .sorted(Comparator.comparingInt(FairPlayRow::getPoints)
                        .thenComparing(row -> row.getTeam().getName()))
                .toList();
    }

    public List<TeamStatsRow> getTeamStats(Championship championship) {
        List<TeamStatsRow> rows = new ArrayList<>();

        for (Team team : championship.getTeams()) {
            TeamStanding standing = getStanding(championship, team);
            double averageAge = team.getPlayers().stream()
                    .mapToInt(player -> getAge(player.getBirthDate()))
                    .average()
                    .orElse(0);
            int coachAge = getAge(team.getCoach().getBirthDate());
            double effectiveness = standing.getPlayed() == 0
                    ? 0
                    : standing.getPoints() * 100.0 / (standing.getPlayed() * 3);

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

    public List<PlayerStatsRow> getPlayerStats(Championship championship) {
        List<PlayerStatsRow> rows = new ArrayList<>();
        List<ParticipationRow> participation = getParticipation(championship);

        for (Team team : championship.getTeams()) {
            for (Player player : team.getPlayers()) {
                ParticipationRow playerParticipation = participation.stream()
                        .filter(row -> row.getPlayer() == player)
                        .findFirst()
                        .orElse(new ParticipationRow(player, team, 0, 0));

                int goals = countGoals(championship, player);
                int goalkeeperGoalsAgainst = countGoalsAgainst(championship, player);

                rows.add(new PlayerStatsRow(
                        player,
                        team,
                        player.getPosition().name(),
                        playerParticipation.getMatches(),
                        playerParticipation.getMinutes(),
                        goals,
                        goalkeeperGoalsAgainst
                ));
            }
        }

        return rows.stream()
                .sorted(Comparator.comparing(row -> row.getPlayer().getLastName()))
                .toList();
    }

    public List<RefereeStatsRow> getRefereeStats(Championship championship) {
        List<RefereeStatsRow> rows = new ArrayList<>();

        for (Referee referee : championship.getReferees()) {
            long matches = championship.getPlayedMatches().stream()
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

    private Team getTeam(Championship championship, Player player) {
        return championship.getTeams().stream()
                .filter(team -> team.getPlayers().contains(player))
                .findFirst()
                .orElseThrow();
    }

    private TeamStanding getStanding(Championship championship, Team team) {
        return championship.getTournamentZones().stream()
                .map(championship::getStandings)
                .flatMap(List::stream)
                .filter(standing -> standing.getTeam() == team)
                .findFirst()
                .orElse(new TeamStanding(team));
    }

    private int getPlayedMinutes(Match match, Player player) {
        if (!match.isPlayed()) {
            return 0;
        }

        if (match.getHomeLineup().getPlayers().contains(player)
                || match.getAwayLineup().getPlayers().contains(player)) {
            for (Incident incident : match.getIncidents()) {
                Player playerOut = incident.getPlayerOut();
                if (playerOut == player) {
                    return incident.getMinute();
                }
                if (incident.isRedCard() && incident.getAffectedPlayer() == player) {
                    return incident.getMinute();
                }
            }
            return 90;
        }

        for (Incident incident : match.getIncidents()) {
            Player playerIn = incident.getPlayerIn();
            if (playerIn == player) {
                return 90 - incident.getMinute();
            }
        }

        return 0;
    }

    private int countGoals(Championship championship, Player player) {
        return (int) championship.getPlayedMatches().stream()
                .flatMap(match -> match.getIncidents().stream())
                .filter(incident -> incident.getScoringPlayer() == player)
                .count();
    }

    private int countGoalsAgainst(Championship championship, Player goalkeeper) {
        return (int) championship.getPlayedMatches().stream()
                .flatMap(match -> match.getIncidents().stream())
                .filter(incident -> incident.getGoalkeeper() == goalkeeper && incident.isMatchGoal())
                .count();
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
