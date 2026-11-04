package Core.classification;

import Core.domain.Match;
import Core.domain.Player;
import Core.incidents.Incident;
import Core.domain.Team;
import Core.domain.TeamStanding;
import Core.domain.TournamentZone;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class StandingsCalculator {

    private static final Comparator<TeamStanding> SPORTING_ORDER =
            Comparator.comparingInt(TeamStanding::getPoints)
                    .reversed()
                    .thenComparing(
                            Comparator.comparingInt(
                                    TeamStanding::getGoalDifference
                            ).reversed()
                    )
                    .thenComparing(
                            Comparator.comparingInt(
                                    TeamStanding::getGoalsFor
                            ).reversed()
                    );

    public List<TeamStanding> calculate(
            TournamentZone zone,
            List<Match> matches
    ) {
        // Only played group-stage matches in this group count.
        List<Match> playedMatches = matches.stream()
                .filter(Match::isGroupStage)
                .filter(Match::isPlayed)
                .filter(match ->
                        zone.getTeams().contains(match.getHomeTeam())
                                && zone.getTeams().contains(
                                match.getAwayTeam()
                        )
                )
                .toList();

        Map<Team, TeamStanding> standings =
                buildTable(zone.getTeams(), playedMatches);

        List<TeamStanding> table =
                new ArrayList<>(standings.values());

        // First: points, goal difference and goals scored.
        table.sort(SPORTING_ORDER);

        // Find groups of teams tied on all three criteria.
        int start = 0;

        while (start < table.size()) {
            int end = start + 1;

            while (
                    end < table.size()
                            && SPORTING_ORDER.compare(
                            table.get(start),
                            table.get(end)
                    ) == 0
            ) {
                end++;
            }

            if (end - start > 1) {
                resolveTie(
                        table.subList(start, end),
                        playedMatches
                );
            }

            start = end;
        }

        return List.copyOf(table);
    }

    private Map<Team, TeamStanding> buildTable(
            List<Team> teams,
            List<Match> matches
    ) {
        Map<Team, TeamStanding> standings =
                new LinkedHashMap<>();

        for (Team team : teams) {
            standings.put(team, new TeamStanding(team));
        }

        for (Match match : matches) {
            TeamStanding homeStanding =
                    standings.get(match.getHomeTeam());

            TeamStanding awayStanding =
                    standings.get(match.getAwayTeam());

            // Both teams must belong to the standings being calculated.
            if (homeStanding != null && awayStanding != null) {
                homeStanding.registerMatch(
                        match.getHomeGoals(),
                        match.getAwayGoals()
                );

                awayStanding.registerMatch(
                        match.getAwayGoals(),
                        match.getHomeGoals()
                );
            }
        }

        return standings;
    }

    private void resolveTie(
            List<TeamStanding> tiedTeams,
            List<Match> playedMatches
    ) {
        List<Team> teams = tiedTeams.stream()
                .map(TeamStanding::getTeam)
                .toList();

        // Standings based only on matches between the tied teams.
        Map<Team, TeamStanding> headToHead =
                buildTable(teams, playedMatches);
        Map<Team, Integer> fairPlay = calculateFairPlay(teams, playedMatches);

        tiedTeams.sort(
                Comparator.comparing(
                                (TeamStanding standing) ->
                                        headToHead.get(standing.getTeam()),
                                SPORTING_ORDER
                        )
                        .thenComparingInt(standing -> fairPlay.get(standing.getTeam()))
                        .thenComparingInt(
                                standing -> standing.getTeam().getRanking()
                        )
        );
    }

    private Map<Team, Integer> calculateFairPlay(List<Team> teams, List<Match> playedMatches) {
        Map<Player, Team> playerTeams = new LinkedHashMap<>();
        Map<Team, Integer> points = new LinkedHashMap<>();
        for (Team team : teams) {
            points.put(team, 0);
            for (Player player : team.getPlayers()) {
                playerTeams.put(player, team);
            }
        }
        // Count all played group matches, not just the head-to-head matches.
        for (Match match : playedMatches) {
            for (Incident incident : match.getIncidents()) {
                Team team = playerTeams.get(incident.getAffectedPlayer());
                if (team != null) {
                    points.merge(team, incident.getFairPlayPoints(), Integer::sum);
                }
            }
        }
        return points;
    }
}
