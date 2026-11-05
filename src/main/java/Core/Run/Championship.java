package Core.Run;

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
import Core.simulation.GroupStageService;
import Core.simulation.KnockoutStageService;

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

    private transient Core.loader.VenueLoader venueLoader;
    private transient GroupStageService groupStage;
    private transient KnockoutStageService knockoutStage;

    private void initializeServices(Random random) {
        MatchSimulator simulator = new MatchSimulator(random);
        TournamentScheduler scheduler = new TournamentScheduler(random, referees);
        groupStage = new GroupStageService(matches, tournamentZones, simulator);
        knockoutStage = new KnockoutStageService(matches, scheduler, new KnockoutResolver(),
                new PenaltyShootoutSimulator(random, simulator), simulator, random);
        venueLoader = new Infrastructure.database.DatabaseVenueLoader();
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
        TournamentScheduler scheduler = new TournamentScheduler(random, referees);
        this.tournamentZones = scheduler.drawBalancedZones(teams);
        this.matches = scheduler.generateGroupMatches(tournamentZones, LocalDate.now());
        initializeServices(random);
        this.stage = TournamentStage.GROUP_STAGE;
        this.quarterFinalWinners = new ArrayList<>();
        this.finalists = new ArrayList<>();
    }

    public static boolean isRefereeEligible(Referee referee, Team home, Team away) {
        return TournamentScheduler.isRefereeEligible(referee, home, away);
    }

    public int getCurrentMatchday() { return groupStage.getCurrentMatchday(); }

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

    public void simulateNextMatchday(Consumer<Match> afterEachMatch) {
        if (!hasPendingGroupMatchdays()) return;
        groupStage.simulateNextMatchday(afterEachMatch);
        if (!hasPendingGroupMatchdays()) stage = TournamentStage.QUARTER_FINALS;
    }

    public List<TeamStanding> getStandings(TournamentZone zone) { return groupStage.getStandings(zone); }

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
        groupStage.restoreMissingMatchdays();
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

    public TournamentStage getStage() {
        return stage;
    }

    public Team getChampion() {
        return champion;
    }

    /** Resolves an already-played series without drawing new penalties. */
    public Team getRecordedSeriesWinner(FirstLegMatch firstLeg, SecondLegMatch secondLeg) {
        return knockoutStage.getRecordedSeriesWinner(firstLeg, secondLeg);
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
        if (stage != TournamentStage.QUARTER_FINALS || hasPendingGroupMatchdays()) {
            throw new IllegalStateException("Group stage must be completed first.");
        }
        if (knockoutStage.hasQuarterFinalFixtures()) return;
        knockoutStage.scheduleQuarterFinals(tournamentZones.stream().map(this::getStandings).toList(), startDate);
    }

    /** Plays exactly one pending knockout match and prepares the next round when needed. */
    public Match simulateNextKnockoutMatch() {
        if (stage == TournamentStage.GROUP_STAGE || hasPendingGroupMatchdays()) {
            throw new IllegalStateException("Complete the group stage first.");
        }
        if (stage == TournamentStage.FINISHED) {
            throw new IllegalStateException("The tournament has already finished.");
        }
        if (pendingPenaltyShootout != null) return simulatePendingPenaltyShootout();
        scheduleQuarterFinalsIfNeeded();
        Match next = knockoutStage.nextMatch();
        reloadVenuesFromDatabase();
        knockoutStage.simulateMatch(next, stadiums);
        if (knockoutStage.requiresPenaltyShootout(next)) pendingPenaltyShootout = next;
        else advanceKnockoutStageIfNeeded();
        return next;
    }

    private Match simulatePendingPenaltyShootout() {
        Match match = pendingPenaltyShootout;
        knockoutStage.simulatePenaltyShootout(match);
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
        return knockoutStage.getKnockoutDecisionCriterion(match, isPendingPenaltyShootout(match));
    }

    List<Stadium> getUnusedStadiums() { return knockoutStage.getUnusedStadiums(stadiums); }

    private void advanceKnockoutStageIfNeeded() {
        if (pendingPenaltyShootout != null) return;
        KnockoutStageService.Advancement advancement = knockoutStage.advanceRound(stage);
        if (stage == TournamentStage.QUARTER_FINALS && advancement.stage() == TournamentStage.SEMI_FINALS) {
            quarterFinalWinners.clear();
            quarterFinalWinners.addAll(advancement.winners());
        } else if (stage == TournamentStage.SEMI_FINALS && advancement.stage() == TournamentStage.FINAL) {
            finalists.clear();
            finalists.addAll(advancement.winners());
        }
        if (advancement.stage() == TournamentStage.FINISHED) champion = advancement.champion();
        stage = advancement.stage();
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

    public Team getRecordedFinalWinner(FinalMatch finalMatch) {
        return knockoutStage.getRecordedFinalWinner(finalMatch);
    }
    private void reloadVenuesFromDatabase() {
        Core.loader.VenueLoader.Venues venues = venueLoader.load(countries);
        loadVenues(venues.cities(), venues.stadiums());
    }

    /** Ensures quarter-finals start seven days after the last group match. */
    public void scheduleQuarterFinalsIfNeeded() {
        if (getStage() != TournamentStage.QUARTER_FINALS) {
            return;
        }

        scheduleQuarterFinals(groupStage.lastGroupDate().plusDays(7));
    }
}
