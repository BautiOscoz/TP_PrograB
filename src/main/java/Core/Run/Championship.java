package Core.Run;

import Core.classification.StandingsCalculator;
import Core.domain.*;
import Core.loader.TournamentData;
import Core.loader.TournamentLoader;
import Core.simulation.MatchSimulator;
import Core.simulation.PenaltyShootoutSimulator;
import Core.classification.KnockoutResolver;
import Core.scheduling.TournamentScheduler;

import java.io.IOException;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import Core.enums.TournamentStage;

import java.io.Serial;
public class Championship implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private final List<TournamentZone> tournamentZones;
    private final List<Match> matches;
    private final List<Team> teams;
    private final List<Referee> referees;
    private final List<Country> countries;
    private final List<City> cities;
    private final List<Stadium> stadiums;
    private transient Random random;
    private transient MatchSimulator matchSimulator;
    private transient TournamentScheduler scheduler;
    private transient KnockoutResolver knockoutResolver;
    private transient PenaltyShootoutSimulator penaltySimulator;
    private transient Core.loader.VenueLoader venueLoader;

    private void initializeServices(Random random) {
        this.random = random;
        this.matchSimulator = new MatchSimulator(random);
        this.scheduler = new TournamentScheduler(random, referees);
        this.knockoutResolver = new KnockoutResolver();
        this.penaltySimulator = new PenaltyShootoutSimulator(random, matchSimulator);
        this.venueLoader = new Infrastructure.database.DatabaseVenueLoader();
    }

    /** Allows alternative venue sources without changing tournament rules. */
    public void setVenueLoader(Core.loader.VenueLoader venueLoader) {
        this.venueLoader = Objects.requireNonNull(venueLoader);
    }
    private TournamentStage stage;
    private List<Team> quarterFinalWinners;
    private List<Team> finalists;
    private Team champion;
    private Match pendingPenaltyShootout;
    public Championship(String jsonPath) throws IOException {
        this(jsonPath, new Random());
    }

    public Championship(String jsonPath, long seed) throws IOException {
        this(jsonPath, new Random(seed));
    }

    private Championship(String jsonPath, Random random) throws IOException {
        TournamentData tournament = TournamentLoader.load(jsonPath);
        this.teams = new ArrayList<>(tournament.getTeams());
        this.referees = new ArrayList<>(tournament.getReferees());
        this.countries = new ArrayList<>(tournament.getCountries());
        this.cities = new ArrayList<>();
        this.stadiums = new ArrayList<>();
        initializeServices(random);
        this.tournamentZones = scheduler.drawBalancedZones(teams);
        this.matches = scheduler.generateGroupMatches(tournamentZones, LocalDate.now());
        this.stage = TournamentStage.GROUP_STAGE;
        this.quarterFinalWinners = new ArrayList<>();
        this.finalists = new ArrayList<>();
    }

    public static boolean isRefereeEligible(Referee referee, Team home, Team away) {
        return TournamentScheduler.isRefereeEligible(referee, home, away);
    }

    public int getCurrentMatchday() {
        return matches.stream()
                .filter(Match::isGroupStage)
                .filter(match -> !match.isPlayed())
                .mapToInt(Match::getMatchday)
                .min()
                .orElse(0);
    }

    public boolean hasPendingGroupMatchdays() {
        return getCurrentMatchday() != 0;
    }

    public void simulateGroupStage() {
        simulateGroupStage(match -> { });
    }

    public void simulateGroupStage(
            Consumer<Match> afterEachMatch
    ) {
        while (hasPendingGroupMatchdays()) {
            simulateNextMatchday(afterEachMatch);
        }
    }

    public void simulateNextMatchday() {
        simulateNextMatchday(match -> { });
    }

    public void simulateNextMatchday(
            Consumer<Match> afterEachMatch
    ) {
        int matchday = getCurrentMatchday();
        if (matchday == 0) {
            return;
        }
        matches.stream()
                .filter(Match::isGroupStage)
                .filter(match ->
                        match.getMatchday() == matchday
                )
                .filter(match -> !match.isPlayed())
                .forEach(match -> {
                    matchSimulator.simulate(match);
                    afterEachMatch.accept(match);
                });
        if (!hasPendingGroupMatchdays()) {
            stage = TournamentStage.QUARTER_FINALS;
        }
    }

    public List<TeamStanding> getStandings(TournamentZone zone) {
        StandingsCalculator calculator = new StandingsCalculator();
        return calculator.calculate(zone, matches);
    }

    public void loadVenues(List<City> cities, List<Stadium> stadiums) {
        if (cities == null || stadiums == null) {
            throw new IllegalArgumentException(
                    "Cities and stadiums cannot be null."
            );
        }
        for (Stadium stadium : stadiums) {
            boolean cityExists =
                    cities.stream()
                            .anyMatch(city ->
                                    city.getId()
                                            == stadium
                                            .getCity()
                                            .getId()
                            );
            if (!cityExists) {
                throw new IllegalArgumentException(
                        "Stadium "
                                + stadium.getName()
                                + " references an unknown city."
                );
            }
        }
        this.cities.clear();
        this.cities.addAll(cities);
        this.stadiums.clear();
        this.stadiums.addAll(stadiums);
    }

    private void addKnockoutFixtures(List<? extends Match> fixtures) {
        matches.addAll(fixtures);
    }

    public List<TournamentZone> getTournamentZones() { return Collections.unmodifiableList(tournamentZones); }
    public List<Match> getMatches() { return Collections.unmodifiableList(matches); }

    public List<Match> getPlayedMatches() {
        return matches.stream().filter(Match::isPlayed).toList();
    }
    public List<Team> getTeams() { return Collections.unmodifiableList(teams); }
    public List<Referee> getReferees() { return Collections.unmodifiableList(referees); }
    public List<Country> getCountries() { return Collections.unmodifiableList(countries); }
    public List<City> getCities() {return Collections.unmodifiableList(cities);}
    public List<Stadium> getStadiums() {return Collections.unmodifiableList(stadiums);}

    @Serial
    private Object readResolve() {
        initializeServices(new Random());
        restoreMissingMatchdays();
        TournamentStage observedStage = inferStageFromPlayedMatches();
        if (pendingPenaltyShootout == null
                && (stage == null
                || observedStage.ordinal() > stage.ordinal())) {

            stage = observedStage;
        }
        if (quarterFinalWinners == null) {
            quarterFinalWinners = new ArrayList<>();
        }
        if (finalists == null) {
            finalists = new ArrayList<>();
        }
        return this;
    }
    private TournamentStage inferStageFromPlayedMatches() {
        if (hasPendingGroupMatchdays()) {
            return TournamentStage.GROUP_STAGE;
        }

        long knockoutPlayed = matches.stream()
                .filter(match -> !match.isGroupStage() && match.isPlayed())
                .count();

        if (knockoutPlayed >= 13) return TournamentStage.FINISHED;
        if (knockoutPlayed >= 12) return TournamentStage.FINAL;
        if (knockoutPlayed >= 8) return TournamentStage.SEMI_FINALS;
        return TournamentStage.QUARTER_FINALS;
    }

    private void restoreMissingMatchdays() {
        matches.stream()
                .filter(Match::isGroupStage)
                .filter(match ->
                        match.getMatchday() == 0
                )
                .forEach(match ->
                        match.setMatchday(
                                findMatchday(match)
                        )
                );
    }

    private int findMatchday(Match match) {
        TournamentZone zone =
                tournamentZones.stream()
                        .filter(candidate ->
                                candidate
                                        .getTeams()
                                        .contains(
                                                match.getHomeTeam()
                                        )
                                        &&
                                        candidate
                                                .getTeams()
                                                .contains(
                                                        match.getAwayTeam()
                                                )
                        )
                        .findFirst()
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "Could not find the zone "
                                                + "for a saved match."
                                )
                        );

        int homeIndex =
                zone.getTeams().indexOf(
                        match.getHomeTeam()
                );

        int awayIndex =
                zone.getTeams().indexOf(
                        match.getAwayTeam()
                );

        if (
                isPair(homeIndex, awayIndex, 0, 3)
                        || isPair(
                        homeIndex,
                        awayIndex,
                        1,
                        2
                )
        ) {
            return 1;
        }

        if (
                isPair(homeIndex, awayIndex, 3, 2)
                        || isPair(
                        homeIndex,
                        awayIndex,
                        0,
                        1
                )
        ) {
            return 2;
        }

        return 3;
    }

    private boolean isPair(
            int first,
            int second,
            int expectedFirst,
            int expectedSecond
    ) {
        return (
                first == expectedFirst
                        && second == expectedSecond
        ) || (
                first == expectedSecond
                        && second == expectedFirst
        );
    }

    private int getSeriesPoints(
            FirstLegMatch firstLeg,
            SecondLegMatch secondLeg,
            Team team
    ) {
        return knockoutResolver.getSeriesPoints(firstLeg, secondLeg, team);
    }

    public TournamentStage getStage() {
        return stage;
    }

    public Team getChampion() {
        return champion;
    }

    private int getWeightedGoalDifference(
            FirstLegMatch firstLeg,
            SecondLegMatch secondLeg,
            Team team
    ) {
        return knockoutResolver.getWeightedGoalDifference(firstLeg, secondLeg, team);
    }

    private Team determineSeriesWinner(FirstLegMatch firstLeg, SecondLegMatch secondLeg) {
        Optional<Team> winner = knockoutResolver.seriesWinnerBeforePenalties(firstLeg, secondLeg);
        if (winner.isPresent()) return winner.get();
        secondLeg.setSettledByPenalties(true);
        return hasRecordedShootout(secondLeg) ? recordedPenaltyWinner(secondLeg)
                : determinePenaltyShootoutWinner(secondLeg);
    }

    /** Resolves an already-played series without drawing new penalties. */
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

    public List<Team> simulateQuarterFinals(
            LocalDate startDate
    ) {
        if (stage != TournamentStage.QUARTER_FINALS) {
            throw new IllegalStateException("Quarter-finals are not the current stage.");
        }
        scheduleQuarterFinals(startDate);
        while (stage == TournamentStage.QUARTER_FINALS) {
            simulateNextKnockoutMatch();
        }
        return List.copyOf(quarterFinalWinners);
    }

    public void scheduleQuarterFinals(LocalDate startDate) {
        if (stage != TournamentStage.QUARTER_FINALS
                || hasPendingGroupMatchdays()) {
            throw new IllegalStateException(
                    "Group stage must be completed first."
            );
        }

        // Avoid recreating fixtures when loading the tournament.
        boolean alreadyScheduled = matches.stream()
                .map(Match::asFirstLeg)
                .anyMatch(Objects::nonNull);
        if (alreadyScheduled) {
            return;
        }

        addKnockoutFixtures(scheduler.quarterFinals(
                tournamentZones.stream().map(this::getStandings).toList(), startDate));
    }

    /** Plays exactly one pending knockout match and prepares the next round when needed. */
    public Match simulateNextKnockoutMatch() {
        if (stage == TournamentStage.GROUP_STAGE
                || hasPendingGroupMatchdays()) {

            throw new IllegalStateException(
                    "Complete the group stage first."
            );
        }

        if (stage == TournamentStage.FINISHED) {
            throw new IllegalStateException(
                    "The tournament has already finished."
            );
        }

        // The shootout is the second step of knockout simulation.
        // Process it before looking for another match or querying available stadiums.
        if (pendingPenaltyShootout != null) {
            return simulatePendingPenaltyShootout();
        }

        if (stage == TournamentStage.QUARTER_FINALS
                && firstLegs().isEmpty()) {

            scheduleQuarterFinals(
                    lastGroupDate().plusDays(7)
            );
        }

        Match next = matches.stream()
                .filter(match ->
                        !match.isGroupStage() && !match.isPlayed()
                )
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "There are no pending matches in this stage."
                ));

        // Query available stadiums in PostgreSQL before each match.
        reloadVenuesFromDatabase();

        List<Stadium> availableStadiums = getUnusedStadiums();

        if (availableStadiums.isEmpty()) {
            throw new IllegalStateException(
                    "There are no unused stadiums left. "
                            + "Add another stadium before continuing."
            );
        }

        Stadium selectedStadium =
                availableStadiums.get(random.nextInt(availableStadiums.size()));

        next.setStadium(selectedStadium);

        matchSimulator.simulate(next);
        if (requiresPenaltyShootout(next)) {
            pendingPenaltyShootout = next;
        } else {
            advanceKnockoutStageIfNeeded();
        }
        return next;
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

    private boolean requiresPenaltyShootout(Match playedMatch) {
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

    private Match simulatePendingPenaltyShootout() {
        Match match = pendingPenaltyShootout;
        matchSimulator.prepareAdministrativeShootout(match);

        SecondLegMatch secondLeg = match.asSecondLeg();
        if (secondLeg != null) {
            secondLeg.setSettledByPenalties(true);
            determinePenaltyShootoutWinner(secondLeg);
        } else {
            FinalMatch finalMatch = match.asFinal();
            if (finalMatch == null) {
                throw new IllegalStateException(
                        "The pending match cannot be decided by a penalty shootout."
                );
            }
            finalMatch.setSettledByPenalties(true);
            determinePenaltyShootoutWinner(finalMatch);
        }

        pendingPenaltyShootout = null;
        advanceKnockoutStageIfNeeded();
        return match;
    }

    public boolean hasPendingPenaltyShootout() {
        return pendingPenaltyShootout != null;
    }

    public boolean isPendingPenaltyShootout(Match match) {
        return pendingPenaltyShootout == match;
    }

    public Match getPendingPenaltyShootout() {
        return pendingPenaltyShootout;
    }

    public String getKnockoutDecisionCriterion(Match match) {
        if (!match.isPlayed() && match.getPostponements() > 0) {
            return "Postponed: both teams lacked players. Previous suspensions served. New date: " + match.getMatchDate();
        }
        if (isPendingPenaltyShootout(match)) {
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

    List<Stadium> getUnusedStadiums() {
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

    private LocalDate lastGroupDate() {
        return matches.stream()
                .filter(Match::isGroupStage)
                .map(Match::getMatchDate)
                .max(LocalDate::compareTo)
                .orElseThrow();
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

    private void advanceKnockoutStageIfNeeded() {
        if (pendingPenaltyShootout != null) {
            return;
        }
        if (stage == TournamentStage.QUARTER_FINALS) {
            List<FirstLegMatch> first = firstLegs();
            List<SecondLegMatch> second = secondLegs();
            if (first.size() == 4 && first.stream().allMatch(Match::isPlayed)
                    && second.isEmpty()) {
                scheduleSecondLegs(first, latestDate(first).plusDays(7));
            }
            if (second.size() == 4 && second.stream().allMatch(Match::isPlayed)) {
                quarterFinalWinners.clear();
                for (int i = 0; i < 4; i++) {
                    quarterFinalWinners.add(determineSeriesWinner(first.get(i), second.get(i)));
                }
                stage = TournamentStage.SEMI_FINALS;
                scheduleSemiFinals(latestDate(second).plusDays(7));
            }
            return;
        }

        if (stage == TournamentStage.SEMI_FINALS) {
            List<FirstLegMatch> first = firstLegs();
            List<SecondLegMatch> second = secondLegs();
            if (first.size() == 6 && first.subList(4, 6).stream().allMatch(Match::isPlayed)
                    && second.size() == 4) {
                scheduleSecondLegs(first.subList(4, 6), latestDate(first.subList(4, 6)).plusDays(7));
            }
            if (second.size() == 6 && second.subList(4, 6).stream().allMatch(Match::isPlayed)) {
                finalists.clear();
                for (int i = 0; i < 2; i++) {
                    finalists.add(determineSeriesWinner(first.get(i + 4), second.get(i + 4)));
                }
                stage = TournamentStage.FINAL;
                scheduleFinal(latestDate(second.subList(4, 6)).plusDays(7));
            }
            return;
        }

        FinalMatch finalMatch = matches.stream()
                .map(Match::asFinal)
                .filter(Objects::nonNull)
                .findFirst()
                .orElseThrow();
        if (finalMatch.isPlayed()) {
            champion = determineFinalWinner(finalMatch);
            stage = TournamentStage.FINISHED;
        }
    }

    private void scheduleSecondLegs(List<FirstLegMatch> firstLegs, LocalDate date) {
        addKnockoutFixtures(scheduler.secondLegs(firstLegs, date));
    }

    private LocalDate latestDate(List<? extends Match> fixtures) {
        return fixtures.stream().map(Match::getMatchDate).max(LocalDate::compareTo).orElseThrow();
    }

    private void scheduleSemiFinals(LocalDate date) {
        if (quarterFinalWinners.size() != 4 || firstLegs().size() != 4) {
            throw new IllegalStateException("Four decided quarter-finals are required.");
        }
        Team[][] pairings = {
                {quarterFinalWinners.get(0), quarterFinalWinners.get(1)},
                {quarterFinalWinners.get(2), quarterFinalWinners.get(3)}
        };
        addKnockoutFixtures(scheduler.firstLegs(pairings, date));
    }

    private void scheduleFinal(LocalDate date) {
        if (finalists.size() != 2 || matches.stream()
                .map(Match::asFinal)
                .anyMatch(Objects::nonNull)) {
            throw new IllegalStateException("Two finalists and one unscheduled final are required.");
        }
        addKnockoutFixtures(List.of(scheduler.finalMatch(finalists, date)));
    }

    public List<Team> simulateSemiFinals(
            List<Team> quarterFinalWinners,
            LocalDate startDate
    ) {
        if (stage != TournamentStage.SEMI_FINALS
                || !this.quarterFinalWinners.equals(quarterFinalWinners)) {
            throw new IllegalStateException("Decided quarter-finals are required first.");
        }
        while (stage == TournamentStage.SEMI_FINALS) {
            simulateNextKnockoutMatch();
        }
        return List.copyOf(finalists);
    }

    public Team simulateFinal(
            List<Team> finalists,
            LocalDate finalDate
    ) {
        if (stage != TournamentStage.FINAL || !this.finalists.equals(finalists)) {
            throw new IllegalStateException("Decided semi-finals are required first.");
        }
        while (stage == TournamentStage.FINAL) {
            simulateNextKnockoutMatch();
        }
        return champion;
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
    private void reloadVenuesFromDatabase() {
        Core.loader.VenueLoader.Venues venues = venueLoader.load(countries);
        loadVenues(venues.cities(), venues.stadiums());
    }

}
