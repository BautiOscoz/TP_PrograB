package Core.Run;

import Core.classification.StandingsCalculator;
import Core.console.MatchConsoleReporter;
import Core.domain.*;
import Core.incidents.PenaltyShootout;
import Core.loader.TournamentData;
import Core.loader.TournamentLoader;
import Core.persistance.ChampionshipRepository;
import Core.simulation.MatchSimulator;

import java.io.IOException;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Consumer;

import Core.console.MatchConsoleReporter;
import Core.console.StandingsConsoleReporter;

import Core.enums.TournamentStage;
import Infrastructure.database.CityRepository;
import Infrastructure.database.StadiumRepository;

import java.sql.SQLException;
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
    private TournamentStage stage;
    private List<Team> quarterFinalWinners;
    private List<Team> finalists;
    private Team champion;
    private Match pendingKnockoutMatch;
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
        this.random = random;
        this.matchSimulator = new MatchSimulator(random);
        this.tournamentZones = drawBalancedZones();
        this.matches = generateGroupMatches(LocalDate.now());
        this.stage = TournamentStage.GROUP_STAGE;
        this.quarterFinalWinners = new ArrayList<>();
        this.finalists = new ArrayList<>();
    }

    private List<TournamentZone> drawBalancedZones() {
        List<Team> rankedTeams = new ArrayList<>(teams);
        rankedTeams.sort(Comparator.comparingInt(Team::getRanking));
        List<TournamentZone> zones = new ArrayList<>();
        for (int i = 0; i < 4; i++) zones.add(new TournamentZone("Zone " + (char) ('A' + i)));

        for (int potIndex = 0; potIndex < 4; potIndex++) {
            List<Team> pot = new ArrayList<>(rankedTeams.subList(potIndex * 4, potIndex * 4 + 4));
            Collections.shuffle(pot, random);
            for (int zoneIndex = 0; zoneIndex < 4; zoneIndex++) {
                zones.get(zoneIndex).addTeam(pot.get(zoneIndex));
            }
        }
        return zones;
    }

    private List<Match> generateGroupMatches(
            LocalDate startDate
    ) {
        List<Match> generated = new ArrayList<>();

        int[][][] schedule = {
                {
                        {0, 3},
                        {1, 2}
                },
                {
                        {3, 2},
                        {0, 1}
                },
                {
                        {1, 3},
                        {2, 0}
                }
        };

        for (TournamentZone zone : tournamentZones) {
            List<Team> zoneTeams = zone.getTeams();

            for (
                    int matchdayIndex = 0;
                    matchdayIndex < schedule.length;
                    matchdayIndex++
            ) {
                for (int[] pairing : schedule[matchdayIndex]) {
                    Team home =
                            zoneTeams.get(pairing[0]);

                    Team away =
                            zoneTeams.get(pairing[1]);

                    generated.add(
                            new GroupMatch(
                                    startDate.plusDays(matchdayIndex),
                                    home,
                                    away,
                                    chooseEligibleReferee(home, away),
                                    matchdayIndex + 1
                            )
                    );
                }
            }
        }
        return generated;
    }

    private Referee chooseEligibleReferee(Team home, Team away) {
        List<Referee> eligible = referees.stream()
                .filter(referee -> isRefereeEligible(referee, home, away)).toList();
        if (eligible.isEmpty()) {
            throw new IllegalStateException("No eligible referee for " + home.getName() + " vs " + away.getName());
        }
        return eligible.get(random.nextInt(eligible.size()));
    }

    public static boolean isRefereeEligible(Referee referee, Team home, Team away) {
        if (home.getCountry().equals(away.getCountry())) return true;
        return !referee.getNationality().equals(home.getCountry())
                && !referee.getNationality().equals(away.getCountry());
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
        this.random = new Random();
        this.matchSimulator = new MatchSimulator(this.random);
        restoreMissingMatchdays();
        TournamentStage observedStage = inferStageFromPlayedMatches();
        if (pendingKnockoutMatch == null
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

    private Team simulateSeries(
            Team firstTeam,
            Team secondTeam,
            LocalDate firstLegDate
    ) {
        FirstLegMatch firstLeg = new FirstLegMatch(
                firstLegDate,
                firstTeam,
                secondTeam,
                chooseEligibleReferee(firstTeam, secondTeam)
        );

        matches.add(firstLeg);
        matchSimulator.simulate(firstLeg);

        SecondLegMatch secondLeg = new SecondLegMatch(
                firstLegDate.plusDays(7),
                secondTeam,
                firstTeam,
                chooseEligibleReferee(secondTeam, firstTeam),
                firstLeg.getHomeGoals(),
                firstLeg.getAwayGoals()
        );

        matches.add(secondLeg);
        matchSimulator.simulate(secondLeg);

        return determineSeriesWinner(firstLeg, secondLeg);
    }

    private int getMatchPoints(
            int goalsFor,
            int goalsAgainst
    ) {
        if (goalsFor > goalsAgainst) {
            return 3;
        }

        if (goalsFor == goalsAgainst) {
            return 1;
        }

        return 0;
    }


    private int getSeriesPoints(
            FirstLegMatch firstLeg,
            SecondLegMatch secondLeg,
            Team team
    ) {
        if (team.equals(firstLeg.getHomeTeam())) {

            int firstLegPoints = getMatchPoints(
                    firstLeg.getHomeGoals(),
                    firstLeg.getAwayGoals()
            );

            int secondLegPoints = getMatchPoints(
                    secondLeg.getAwayGoals(),
                    secondLeg.getHomeGoals()
            );

            return firstLegPoints + secondLegPoints;
        }

        if (team.equals(firstLeg.getAwayTeam())) {

            int firstLegPoints = getMatchPoints(
                    firstLeg.getAwayGoals(),
                    firstLeg.getHomeGoals()
            );

            int secondLegPoints = getMatchPoints(
                    secondLeg.getHomeGoals(),
                    secondLeg.getAwayGoals()
            );

            return firstLegPoints + secondLegPoints;
        }

        throw new IllegalArgumentException(
                "Team does not belong to this series."
        );
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
        Team teamA = firstLeg.getHomeTeam();
        Team teamB = firstLeg.getAwayTeam();

        if (team.equals(teamA)) {

            int goalsFor =
                    firstLeg.getHomeGoals()
                            + secondLeg.getAwayGoals() * 2;

            int goalsAgainst =
                    firstLeg.getAwayGoals() * 2
                            + secondLeg.getHomeGoals();

            return goalsFor - goalsAgainst;
        }

        if (team.equals(teamB)) {

            int goalsFor =
                    firstLeg.getAwayGoals() * 2
                            + secondLeg.getHomeGoals();

            int goalsAgainst =
                    firstLeg.getHomeGoals()
                            + secondLeg.getAwayGoals() * 2;

            return goalsFor - goalsAgainst;
        }

        throw new IllegalArgumentException(
                "Team does not belong to this series."
        );
    }

    private Team determineSeriesWinner(
            FirstLegMatch firstLeg,
            SecondLegMatch secondLeg
    ) {
        Team teamA = firstLeg.getHomeTeam();
        Team teamB = firstLeg.getAwayTeam();

        int teamAPoints =
                getSeriesPoints(firstLeg, secondLeg, teamA);

        int teamBPoints =
                getSeriesPoints(firstLeg, secondLeg, teamB);

        // 1. Puntos
        if (teamAPoints > teamBPoints) {
            return teamA;
        }

        if (teamBPoints > teamAPoints) {
            return teamB;
        }

        // 2. Diferencia de gol con visitante doble
        int teamAGoalDifference =
                getWeightedGoalDifference(
                        firstLeg,
                        secondLeg,
                        teamA
                );

        int teamBGoalDifference =
                getWeightedGoalDifference(
                        firstLeg,
                        secondLeg,
                        teamB
                );

        if (teamAGoalDifference > teamBGoalDifference) {
            return teamA;
        }

        if (teamBGoalDifference > teamAGoalDifference) {
            return teamB;
        }

        // 3. Penales
        secondLeg.setSettledByPenalties(true);
        if (hasRecordedShootout(secondLeg)) {
            return recordedPenaltyWinner(secondLeg);
        }
        return determinePenaltyShootoutWinner(secondLeg);
    }

    /** Resolves an already-played series without drawing new penalties. */
    public Team getRecordedSeriesWinner(FirstLegMatch firstLeg, SecondLegMatch secondLeg) {
        if (!firstLeg.isPlayed() || !secondLeg.isPlayed()) {
            throw new IllegalStateException("Both legs must be played.");
        }
        Team teamA = firstLeg.getHomeTeam();
        Team teamB = firstLeg.getAwayTeam();
        int pointsA = getSeriesPoints(firstLeg, secondLeg, teamA);
        int pointsB = getSeriesPoints(firstLeg, secondLeg, teamB);
        if (pointsA != pointsB) {
            return pointsA > pointsB ? teamA : teamB;
        }
        int differenceA = getWeightedGoalDifference(firstLeg, secondLeg, teamA);
        int differenceB = getWeightedGoalDifference(firstLeg, secondLeg, teamB);
        if (differenceA != differenceB) {
            return differenceA > differenceB ? teamA : teamB;
        }
        return recordedPenaltyWinner(secondLeg);
    }

    private boolean hasRecordedShootout(Match match) {
        return match.getIncidents().stream().anyMatch(PenaltyShootout.class::isInstance);
    }

    private Team recordedPenaltyWinner(Match match) {
        int home = 0;
        int away = 0;
        for (var incident : match.getIncidents()) {
            if (!(incident instanceof PenaltyShootout penalty) || !penalty.isScored()) {
                continue;
            }
            if (match.getHomeTeam().getPlayers().contains(penalty.getPlayer())) {
                home++;
            } else if (match.getAwayTeam().getPlayers().contains(penalty.getPlayer())) {
                away++;
            }
        }
        if (!hasRecordedShootout(match) || home == away) {
            throw new IllegalStateException("No decided shootout was recorded.");
        }
        return home > away ? match.getHomeTeam() : match.getAwayTeam();
    }

    private Team determinePenaltyShootoutWinner(
            Match match
    ) {
        Team homeTeam = match.getHomeTeam();
        Team awayTeam = match.getAwayTeam();

        List<Player> homePlayers =
                match.getPlayersOnField(homeTeam, 90);

        List<Player> awayPlayers =
                match.getPlayersOnField(awayTeam, 90);

        int homeScore = 0;
        int awayScore = 0;


        // Cinco penales iniciales
        for (int i = 0; i < 5; i++) {

            if (simulatePenalty(
                    match,
                    homePlayers.get(i)
            )) {
                homeScore++;
            }

            if (simulatePenalty(
                    match,
                    awayPlayers.get(i)
            )) {
                awayScore++;
            }
        }

        if (homeScore > awayScore) {
            return homeTeam;
        }

        if (awayScore > homeScore) {
            return awayTeam;
        }

        // Muerte súbita
        int kickerIndex = 5;

        while (true) {

            boolean homeScored =
                    simulatePenalty(
                            match,
                            homePlayers.get(
                                    kickerIndex
                                            % homePlayers.size()
                            )
                    );

            boolean awayScored =
                    simulatePenalty(
                            match,
                            awayPlayers.get(
                                    kickerIndex
                                            % awayPlayers.size()
                            )
                    );

            if (homeScored && !awayScored) {
                return homeTeam;
            }

            if (!homeScored && awayScored) {
                return awayTeam;
            }

            kickerIndex++;
        }
    }

    private boolean simulatePenalty(
            Match match,
            Player player
    ) {
        boolean scored =
                random.nextDouble() < 0.75;

        match.addIncident(
                new PenaltyShootout(
                        90,
                        player,
                        scored
                )
        );

        return scored;
    }

    private Team getFirst(TournamentZone zone) {
        return getStandings(zone)
                .get(0)
                .getTeam();
    }

    private Team getSecond(TournamentZone zone) {
        return getStandings(zone)
                .get(1)
                .getTeam();
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

        // Evita volver a crear los cruces al cargar el torneo.
        boolean alreadyScheduled = matches.stream()
                .anyMatch(match -> match instanceof FirstLegMatch);
        if (alreadyScheduled) {
            return;
        }

        TournamentZone zoneA = tournamentZones.get(0);
        TournamentZone zoneB = tournamentZones.get(1);
        TournamentZone zoneC = tournamentZones.get(2);
        TournamentZone zoneD = tournamentZones.get(3);

        Team[][] pairings = {
                {getFirst(zoneA), getSecond(zoneD)},
                {getFirst(zoneB), getSecond(zoneC)},
                {getFirst(zoneC), getSecond(zoneA)},
                {getFirst(zoneD), getSecond(zoneB)}
        };

        List<FirstLegMatch> fixtures = new ArrayList<>();
        for (Team[] pairing : pairings) {
            fixtures.add(new FirstLegMatch(
                    startDate,
                    pairing[0],
                    pairing[1],
                    chooseEligibleReferee(pairing[0], pairing[1])
            ));
        }

        matches.addAll(fixtures);
    }

    /** Plays exactly one pending knockout match and prepares the next round when needed. */
    public Match simulateNextKnockoutMatch() {
        // Primero termina una baja pendiente, sin repetir el partido.
        if (pendingKnockoutMatch != null) {
            return completePendingKnockoutMatch();
        }

        if (stage == TournamentStage.GROUP_STAGE
                || hasPendingGroupMatchdays()) {

            throw new IllegalStateException(
                    "Primero tenés que terminar la fase de grupos."
            );
        }

        if (stage == TournamentStage.FINISHED) {
            throw new IllegalStateException(
                    "El torneo ya terminó."
            );
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
                        "No hay partidos pendientes para esta fase."
                ));

        // Consulta los disponibles en PostgreSQL antes de cada partido.
        reloadVenuesFromDatabase();

        if (stadiums.isEmpty()) {
            throw new IllegalStateException(
                    "No quedan estadios disponibles. "
                            + "Agregá uno antes de continuar."
            );
        }

        Stadium selectedStadium =
                stadiums.get(random.nextInt(stadiums.size()));

        next.setStadium(selectedStadium);

        matchSimulator.simulate(next);

// Si terminó una vuelta, resolvemos la serie antes de mostrarla.
        if (next instanceof SecondLegMatch secondLeg) {
            FirstLegMatch firstLeg = matches.stream()
                    .filter(FirstLegMatch.class::isInstance)
                    .map(FirstLegMatch.class::cast)
                    .filter(first ->
                            first.getHomeTeam() == secondLeg.getAwayTeam()
                                    && first.getAwayTeam() == secondLeg.getHomeTeam()
                    )
                    .filter(Match::isPlayed)
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException(
                            "The first leg of this series was not found."
                    ));

            determineSeriesWinner(firstLeg, secondLeg);
        }

        advanceKnockoutStageIfNeeded();
        return next;
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
                .filter(FirstLegMatch.class::isInstance)
                .map(FirstLegMatch.class::cast)
                .toList();
    }

    private List<SecondLegMatch> secondLegs() {
        return matches.stream()
                .filter(SecondLegMatch.class::isInstance)
                .map(SecondLegMatch.class::cast)
                .toList();
    }

    private void advanceKnockoutStageIfNeeded() {
        if (stage == TournamentStage.QUARTER_FINALS) {
            List<FirstLegMatch> first = firstLegs();
            List<SecondLegMatch> second = secondLegs();
            if (first.size() == 4 && first.stream().allMatch(Match::isPlayed)
                    && second.isEmpty()) {
                scheduleSecondLegs(first, first.get(0).getMatchDate().plusDays(7));
            }
            if (second.size() == 4 && second.stream().allMatch(Match::isPlayed)) {
                quarterFinalWinners.clear();
                for (int i = 0; i < 4; i++) {
                    quarterFinalWinners.add(determineSeriesWinner(first.get(i), second.get(i)));
                }
                stage = TournamentStage.SEMI_FINALS;
                scheduleSemiFinals(second.get(0).getMatchDate().plusDays(7));
            }
            return;
        }

        if (stage == TournamentStage.SEMI_FINALS) {
            List<FirstLegMatch> first = firstLegs();
            List<SecondLegMatch> second = secondLegs();
            if (first.size() == 6 && first.subList(4, 6).stream().allMatch(Match::isPlayed)
                    && second.size() == 4) {
                scheduleSecondLegs(first.subList(4, 6), first.get(4).getMatchDate().plusDays(7));
            }
            if (second.size() == 6 && second.subList(4, 6).stream().allMatch(Match::isPlayed)) {
                finalists.clear();
                for (int i = 0; i < 2; i++) {
                    finalists.add(determineSeriesWinner(first.get(i + 4), second.get(i + 4)));
                }
                stage = TournamentStage.FINAL;
                scheduleFinal(second.get(4).getMatchDate().plusDays(7));
            }
            return;
        }

        FinalMatch finalMatch = matches.stream()
                .filter(FinalMatch.class::isInstance)
                .map(FinalMatch.class::cast)
                .findFirst()
                .orElseThrow();
        if (finalMatch.isPlayed()) {
            champion = determineFinalWinner(finalMatch);
            stage = TournamentStage.FINISHED;
        }
    }

    private void scheduleSecondLegs(List<FirstLegMatch> firstLegs, LocalDate date) {
        List<SecondLegMatch> fixtures = new ArrayList<>();
        for (FirstLegMatch first : firstLegs) {
            Team home = first.getAwayTeam();
            Team away = first.getHomeTeam();
            fixtures.add(new SecondLegMatch(date, home, away,
                    chooseEligibleReferee(home, away),
                    first.getHomeGoals(), first.getAwayGoals()));
        }
        matches.addAll(fixtures);
    }

    private void scheduleSemiFinals(LocalDate date) {
        if (quarterFinalWinners.size() != 4 || firstLegs().size() != 4) {
            throw new IllegalStateException("Four decided quarter-finals are required.");
        }
        Team[][] pairings = {
                {quarterFinalWinners.get(0), quarterFinalWinners.get(1)},
                {quarterFinalWinners.get(2), quarterFinalWinners.get(3)}
        };
        List<FirstLegMatch> fixtures = new ArrayList<>();
        for (Team[] pairing : pairings) {
            fixtures.add(new FirstLegMatch(date, pairing[0], pairing[1],
                    chooseEligibleReferee(pairing[0], pairing[1])));
        }
        matches.addAll(fixtures);
    }

    private void scheduleFinal(LocalDate date) {
        if (finalists.size() != 2 || matches.stream().anyMatch(FinalMatch.class::isInstance)) {
            throw new IllegalStateException("Two finalists and one unscheduled final are required.");
        }
        Team home = finalists.get(0);
        Team away = finalists.get(1);
        matches.add(new FinalMatch(date, home, away, chooseEligibleReferee(home, away)));
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
        simulateNextKnockoutMatch();
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

        // Si empatan, definición por penales
        finalMatch.setSettledByPenalties(true);
        if (hasRecordedShootout(finalMatch)) {
            return recordedPenaltyWinner(finalMatch);
        }
        return determinePenaltyShootoutWinner(finalMatch);
    }

    public Team getRecordedFinalWinner(FinalMatch finalMatch) {
        if (!finalMatch.isPlayed()) {
            throw new IllegalStateException("The final has not been played.");
        }
        if (finalMatch.getHomeGoals() != finalMatch.getAwayGoals()) {
            return finalMatch.getHomeGoals() > finalMatch.getAwayGoals()
                    ? finalMatch.getHomeTeam() : finalMatch.getAwayTeam();
        }
        return recordedPenaltyWinner(finalMatch);
    }
    private void reloadVenuesFromDatabase() {
        try {
            CityRepository cityRepository = new CityRepository();
            StadiumRepository stadiumRepository = new StadiumRepository();

            List<City> loadedCities =
                    cityRepository.findAll(countries);

            List<Stadium> loadedStadiums =
                    stadiumRepository.findAll(loadedCities);

            loadVenues(loadedCities, loadedStadiums);

        } catch (SQLException e) {
            e.printStackTrace();

            throw new IllegalStateException(
                    "No se pudieron cargar los estadios.\n"
                            + "Detalle: " + e.getMessage()
                            + "\nCódigo SQL: " + e.getSQLState(),
                    e
            );
        }
    }

    private Match completePendingKnockoutMatch() {
        Match played = pendingKnockoutMatch;
        Stadium stadium = played.getStadium();

        try {
            StadiumRepository repository = new StadiumRepository();

            // Si ya no existe, la baja ya está cumplida.
            repository.deleteById(stadium.getId());

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "El partido ya se jugó, pero no se pudo eliminar "
                            + "el estadio de PostgreSQL. "
                            + "Volvé a presionar Simular para reintentar "
                            + "la baja sin repetir el partido.",
                    e
            );
        }

        // También deja de estar disponible dentro del campeonato.
        stadiums.removeIf(
                available -> available.getId() == stadium.getId()
        );

        advanceKnockoutStageIfNeeded();

        pendingKnockoutMatch = null;

        return played;
    }

    public static void main(String[] args) throws IOException {
        ChampionshipRepository repository = new ChampionshipRepository();
        Championship championship = null;

        Scanner scanner = new Scanner(System.in);
        if (repository.exists()) {
            System.out.println("==========================================");
            System.out.println("Se encontró una partida guardada previamente.");
            System.out.println("1. Continuar torneo guardado");
            System.out.println("2. Iniciar un nuevo torneo (se sobrescribirá el anterior)");
            System.out.print("Seleccione una opción: ");

            int opcion = scanner.nextInt();
            scanner.nextLine();
            if (opcion == 1) {
                championship = repository.load();
            }
        }
        if (championship == null) {
            System.out.println("\nInicializando un nuevo torneo desde torneo.json...");
            championship = new Championship("torneo.json");
        }
        MatchConsoleReporter matchReporter =
                new MatchConsoleReporter();

        StandingsConsoleReporter standingsReporter =
                new StandingsConsoleReporter();

        if (championship.hasPendingGroupMatchdays()) {

            while (championship.hasPendingGroupMatchdays()) {
                int matchday =
                        championship.getCurrentMatchday();

                System.out.println();
                System.out.println(
                        "================ MATCHDAY "
                                + matchday
                                + " ================"
                );

                championship.simulateNextMatchday(
                        matchReporter::printMatch
                );

                for (
                        TournamentZone zone
                        : championship.getTournamentZones()
                ) {
                    standingsReporter.printZone(
                            zone,
                            championship.getStandings(zone)
                    );
                }

                repository.save(championship);

                if (championship.hasPendingGroupMatchdays()) {
                    System.out.print(
                            "\nPress Enter to simulate "
                                    + "the next matchday..."
                    );

                    scanner.nextLine();
                }
            }

        } else {
            System.out.println();
            System.out.println(
                    "La fase de grupos ya estaba completa."
            );

            System.out.println(
                    "Resultados y posiciones guardadas:"
            );

            championship.getPlayedMatches()
                    .forEach(matchReporter::printMatch);

            for (
                    TournamentZone zone
                    : championship.getTournamentZones()
            ) {
                standingsReporter.printZone(
                        zone,
                        championship.getStandings(zone)
                );
            }
        }

        while (championship.getStage() != TournamentStage.FINISHED) {
            System.out.print("\nPress Enter to simulate the next knockout match...");
            scanner.nextLine();
            Match played = championship.simulateNextKnockoutMatch();
            matchReporter.printMatch(played);
            repository.save(championship);
        }

        System.out.println("=== CAMPEÓN ===");
        if (championship.getChampion() != null) {
            System.out.println(championship.getChampion().getName());
        } else {
            FinalMatch savedFinal = championship.getMatches().stream()
                    .filter(FinalMatch.class::isInstance)
                    .map(FinalMatch.class::cast)
                    .findFirst()
                    .orElse(null);
            if (savedFinal != null) {
                System.out.println(championship.getRecordedFinalWinner(savedFinal).getName());
            }
        }

        System.out.println(
                "Cantidad total de partidos: "
                        + championship.getMatches().size()
        );

        repository.save(championship);
        scanner.close();
    }
}
