package Core.simulation;

import Core.classification.StandingsCalculator;
import Core.domain.*;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Consumer;

/** Coordinates group matchdays and standings without owning tournament progress. */
public final class GroupStageService {
    private final List<Match> matches;
    private final List<TournamentZone> zones;
    private final MatchSimulator simulator;
    private final StandingsCalculator standings = new StandingsCalculator();

    public GroupStageService(List<Match> matches, List<TournamentZone> zones, MatchSimulator simulator) {
        this.matches = List.copyOf(matches);
        this.zones = List.copyOf(zones);
        this.simulator = simulator;
    }
    public int getCurrentMatchday() {
        return matches.stream()
                .filter(Match::isGroupStage)
                .filter(match -> !match.isPlayed())
                .mapToInt(Match::getMatchday)
                .min()
                .orElse(0);
    }
    public void simulateNextMatchday(Consumer<Match> afterEachMatch) {
        int matchday = getCurrentMatchday();
        if (matchday == 0) return;
        matches.stream().filter(Match::isGroupStage)
                .filter(match -> match.getMatchday() == matchday && !match.isPlayed())
                .forEach(match -> {
                    simulator.simulate(match);
                    afterEachMatch.accept(match);
                });
    }

    public List<TeamStanding> getStandings(TournamentZone zone) {
        return standings.calculate(zone, matches);
    }

    /** Restores matchdays from saves created before matchday tracking was introduced. */
    public void restoreMissingMatchdays() {
        matches.stream().filter(Match::isGroupStage)
                .filter(match -> match.getMatchday() == 0)
                .forEach(match -> match.setMatchday(findMatchday(match)));
    }

    private int findMatchday(Match match) {
        TournamentZone zone = zones.stream()
                .filter(candidate -> candidate.getTeams().contains(match.getHomeTeam())
                        && candidate.getTeams().contains(match.getAwayTeam()))
                .findFirst().orElseThrow(() -> new IllegalStateException("Could not find the zone for a saved match."));
        int home = zone.getTeams().indexOf(match.getHomeTeam());
        int away = zone.getTeams().indexOf(match.getAwayTeam());
        if (isPair(home, away, 0, 3) || isPair(home, away, 1, 2)) return 1;
        if (isPair(home, away, 3, 2) || isPair(home, away, 0, 1)) return 2;
        return 3;
    }

    private boolean isPair(int home, int away, int first, int second) {
        return (home == first && away == second) || (home == second && away == first);
    }
    public LocalDate lastGroupDate() {
        return matches.stream()
                .filter(Match::isGroupStage)
                .map(Match::getMatchDate)
                .max(LocalDate::compareTo)
                .orElseThrow();
    }
}
