package Core.simulation;

import Core.domain.*;
import Core.enums.TournamentStage;
import Core.classification.KnockoutResolver;
import Core.scheduling.TournamentScheduler;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/** Coordinates knockout fixtures and results; Championship retains the persisted stage. */
public final class KnockoutStageService {
    private final List<Match> matches;
    private final TournamentScheduler scheduler;
    private final KnockoutResolver knockoutResolver;
    private final PenaltyShootoutSimulator penaltySimulator;
    private final MatchSimulator matchSimulator;
    private final Random random;

    public KnockoutStageService(List<Match> matches, TournamentScheduler scheduler,
            KnockoutResolver resolver, PenaltyShootoutSimulator penalties,
            MatchSimulator simulator, Random random) {
        this.matches = matches;
        this.scheduler = scheduler;
        this.knockoutResolver = resolver;
        this.penaltySimulator = penalties;
        this.matchSimulator = simulator;
        this.random = random;
    }

    /** The next progress values; no duplicate mutable tournament state is retained. */
    public record Advancement(TournamentStage stage, List<Team> winners, Team champion) {
        public Advancement { winners = List.copyOf(winners); }
    }

    public void scheduleQuarterFinals(List<List<TeamStanding>> standings, LocalDate date) {
        if (!hasQuarterFinalFixtures()) matches.addAll(scheduler.quarterFinals(standings, date));
    }

    public boolean hasQuarterFinalFixtures() {
        return matches.stream().map(Match::asFirstLeg).anyMatch(Objects::nonNull);
    }

    public Match nextMatch() {
        return matches.stream().filter(match -> !match.isGroupStage() && !match.isPlayed())
                .findFirst().orElseThrow(() -> new IllegalStateException("There are no pending matches in this stage."));
    }

    public void simulateMatch(Match match, List<Stadium> stadiums) {
        List<Stadium> available = getUnusedStadiums(stadiums);
        if (available.isEmpty()) throw new IllegalStateException(
                "There are no unused stadiums left. Add another stadium before continuing.");
        match.setStadium(available.get(random.nextInt(available.size())));
        matchSimulator.simulate(match);
    }

    public void simulatePenaltyShootout(Match match) {
        matchSimulator.prepareAdministrativeShootout(match);
        SecondLegMatch second = match.asSecondLeg();
        if (second != null) {
            second.setSettledByPenalties(true);
            determinePenaltyShootoutWinner(second);
        } else {
            FinalMatch finalMatch = match.asFinal();
            if (finalMatch == null) throw new IllegalStateException(
                    "The pending match cannot be decided by a penalty shootout.");
            finalMatch.setSettledByPenalties(true);
            determinePenaltyShootoutWinner(finalMatch);
        }
    }

    public Advancement advanceRound(TournamentStage stage) {
        if (stage == TournamentStage.QUARTER_FINALS || stage == TournamentStage.SEMI_FINALS) {
            int offset = stage == TournamentStage.QUARTER_FINALS ? 0 : 4;
            int count = stage == TournamentStage.QUARTER_FINALS ? 4 : 2;
            List<FirstLegMatch> first = firstLegs();
            List<SecondLegMatch> second = secondLegs();
            if (first.size() == offset + count
                    && first.subList(offset, offset + count).stream().allMatch(Match::isPlayed)
                    && second.size() == offset) {
                List<FirstLegMatch> round = first.subList(offset, offset + count);
                matches.addAll(scheduler.secondLegs(round, latestDate(round).plusDays(7)));
            }
            if (second.size() == offset + count
                    && second.subList(offset, offset + count).stream().allMatch(Match::isPlayed)) {
                List<Team> winners = new ArrayList<>();
                for (int i = offset; i < offset + count; i++) {
                    winners.add(determineSeriesWinner(first.get(i), second.get(i)));
                }
                LocalDate date = latestDate(second.subList(offset, offset + count)).plusDays(7);
                if (stage == TournamentStage.QUARTER_FINALS) {
                    Team[][] pairings = {{winners.get(0), winners.get(1)}, {winners.get(2), winners.get(3)}};
                    matches.addAll(scheduler.firstLegs(pairings, date));
                    return new Advancement(TournamentStage.SEMI_FINALS, winners, null);
                }
                if (matches.stream().map(Match::asFinal).anyMatch(Objects::nonNull)) {
                    throw new IllegalStateException("Two finalists and one unscheduled final are required.");
                }
                matches.add(scheduler.finalMatch(winners, date));
                return new Advancement(TournamentStage.FINAL, winners, null);
            }
            return new Advancement(stage, List.of(), null);
        }
        FinalMatch finalMatch = matches.stream().map(Match::asFinal)
                .filter(Objects::nonNull).findFirst().orElseThrow();
        return finalMatch.isPlayed()
                ? new Advancement(TournamentStage.FINISHED, List.of(), determineFinalWinner(finalMatch))
                : new Advancement(stage, List.of(), null);
    }

    private int getSeriesPoints(
            FirstLegMatch firstLeg,
            SecondLegMatch secondLeg,
            Team team
    ) {
        return knockoutResolver.getSeriesPoints(firstLeg, secondLeg, team);
    }

    private int getWeightedGoalDifference(
            FirstLegMatch firstLeg,
            SecondLegMatch secondLeg,
            Team team
    ) {
        return knockoutResolver.getWeightedGoalDifference(firstLeg, secondLeg, team);
    }

    public Team determineSeriesWinner(FirstLegMatch firstLeg, SecondLegMatch secondLeg) {
        Optional<Team> winner = knockoutResolver.seriesWinnerBeforePenalties(firstLeg, secondLeg);
        if (winner.isPresent()) return winner.get();
        secondLeg.setSettledByPenalties(true);
        return hasRecordedShootout(secondLeg) ? recordedPenaltyWinner(secondLeg)
                : determinePenaltyShootoutWinner(secondLeg);
    }

    public Team getRecordedSeriesWinner(FirstLegMatch firstLeg, SecondLegMatch secondLeg) {
        return knockoutResolver.getRecordedSeriesWinner(firstLeg, secondLeg);
    }

    private boolean hasRecordedShootout(Match match) {
        return knockoutResolver.hasRecordedShootout(match);
    }

    private Team recordedPenaltyWinner(Match match) {
        return knockoutResolver.recordedPenaltyWinner(match);
    }

    private Team determinePenaltyShootoutWinner(Match match) {
        return penaltySimulator.simulate(match);
    }

    private FirstLegMatch findFirstLeg(SecondLegMatch secondLeg) {
        return firstLegs().stream()
                .filter(first -> first.getHomeTeam() == secondLeg.getAwayTeam()
                        && first.getAwayTeam() == secondLeg.getHomeTeam())
                .filter(Match::isPlayed)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "The first leg of this series was not found."
                ));
    }

    public boolean requiresPenaltyShootout(Match playedMatch) {
        SecondLegMatch secondLeg = playedMatch.asSecondLeg();
        if (secondLeg != null) {
            FirstLegMatch firstLeg = findFirstLeg(secondLeg);
            Team teamA = firstLeg.getHomeTeam();
            Team teamB = firstLeg.getAwayTeam();
            return getSeriesPoints(firstLeg, secondLeg, teamA)
                    == getSeriesPoints(firstLeg, secondLeg, teamB)
                    && getWeightedGoalDifference(firstLeg, secondLeg, teamA)
                    == getWeightedGoalDifference(firstLeg, secondLeg, teamB);
        }

        FinalMatch finalMatch = playedMatch.asFinal();
        return finalMatch != null
                && finalMatch.getHomeGoals() == finalMatch.getAwayGoals();
    }

    private List<FirstLegMatch> firstLegs() {
        return matches.stream()
                .map(Match::asFirstLeg)
                .filter(Objects::nonNull)
                .toList();
    }

    private List<SecondLegMatch> secondLegs() {
        return matches.stream()
                .map(Match::asSecondLeg)
                .filter(Objects::nonNull)
                .toList();
    }

    private LocalDate latestDate(List<? extends Match> fixtures) {
        return fixtures.stream().map(Match::getMatchDate).max(LocalDate::compareTo).orElseThrow();
    }

    private Team determineFinalWinner(
            FinalMatch finalMatch
    ) {
        int homeGoals = finalMatch.getHomeGoals();
        int awayGoals = finalMatch.getAwayGoals();

        if (homeGoals > awayGoals) {
            return finalMatch.getHomeTeam();
        }

        if (awayGoals > homeGoals) {
            return finalMatch.getAwayTeam();
        }

        // Resolve a draw with a penalty shootout
        finalMatch.setSettledByPenalties(true);
        if (hasRecordedShootout(finalMatch)) {
            return recordedPenaltyWinner(finalMatch);
        }
        return determinePenaltyShootoutWinner(finalMatch);
    }

    public Team getRecordedFinalWinner(FinalMatch finalMatch) {
        return knockoutResolver.getRecordedFinalWinner(finalMatch);
    }

    public String getKnockoutDecisionCriterion(Match match, boolean penaltiesPending) {
        if (!match.isPlayed() && match.getPostponements() > 0) {
            return "Postponed: both teams lacked players. Previous suspensions served. New date: " + match.getMatchDate();
        }
        if (penaltiesPending) {
            return "Tie: penalty shootout pending";
        }
        if (match.isSettledByPenalties()) {
            return "Winner decided by penalty shootout";
        }

        SecondLegMatch secondLeg = match.asSecondLeg();
        if (secondLeg != null) {
            FirstLegMatch firstLeg = findFirstLeg(secondLeg);
            Team teamA = firstLeg.getHomeTeam();
            Team teamB = firstLeg.getAwayTeam();
            if (getSeriesPoints(firstLeg, secondLeg, teamA)
                    != getSeriesPoints(firstLeg, secondLeg, teamB)) {
                return "Winner decided by series points";
            }
            return "Winner decided by goal difference (away goals count double)";
        }

        if (match.asFinal() != null) {
            if (match.isForfeit()) return match.getAdministrativeReason();
            return "Winner decided by the 90-minute result";
        }
        return "First leg: the series continues";
    }

    public List<Stadium> getUnusedStadiums(List<Stadium> stadiums) {
        Set<Long> usedStadiumIds = matches.stream()
                .filter(match -> !match.isGroupStage())
                .filter(Match::isPlayed)
                .map(Match::getStadium)
                .filter(Objects::nonNull)
                .map(Stadium::getId)
                .collect(Collectors.toSet());

        return stadiums.stream()
                .filter(stadium -> !usedStadiumIds.contains(stadium.getId()))
                .toList();
    }
}
