package Core.classification;

import Core.domain.Match;
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
        // Solo cuentan los partidos de grupos jugados en esta zona.
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

        // Primero: puntos, diferencia de gol y goles a favor.
        table.sort(SPORTING_ORDER);

        // Buscamos bloques de equipos igualados en esos tres criterios.
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

            // Ambos equipos deben pertenecer a la tabla que calculamos.
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

        // Tabla que cuenta únicamente los partidos entre los empatados.
        Map<Team, TeamStanding> headToHead =
                buildTable(teams, playedMatches);

        tiedTeams.sort(
                Comparator.comparing(
                                (TeamStanding standing) ->
                                        headToHead.get(standing.getTeam()),
                                SPORTING_ORDER
                        )
                        .thenComparingInt(
                                standing -> standing.getTeam().getRanking()
                        )
        );
    }
}