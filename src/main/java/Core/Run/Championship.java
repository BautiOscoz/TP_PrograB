package Core.Run;

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
    }

    public List<TeamStanding> getStandings(TournamentZone zone){
        Map<Team,TeamStanding> standings= new LinkedHashMap<>();
        zone.getTeams().forEach(team->standings.put(team,new TeamStanding(team)));
        matches.stream()
                .filter(Match::isGroupStage)
                .filter(Match::isPlayed)
                .filter(match ->
                        standings.containsKey(match.getHomeTeam())
                                && standings.containsKey(
                                match.getAwayTeam()
                        )
                )
                .forEach(match -> {
                    TeamStanding homeStanding =
                            standings.get(match.getHomeTeam());

                    TeamStanding awayStanding =
                            standings.get(match.getAwayTeam());

                    homeStanding.registerMatch(
                            match.getHomeGoals(),
                            match.getAwayGoals()
                    );

                    awayStanding.registerMatch(
                            match.getAwayGoals(),
                            match.getHomeGoals()
                    );
                });

        return standings.values().stream()
                .sorted(
                        Comparator
                                .comparingInt(
                                        TeamStanding::getPoints
                                )
                                .reversed()
                                .thenComparing(
                                        Comparator
                                                .comparingInt(
                                                        TeamStanding
                                                                ::getGoalDifference
                                                )
                                                .reversed()
                                )
                                .thenComparing(
                                        Comparator
                                                .comparingInt(
                                                        TeamStanding
                                                                ::getGoalsFor
                                                )
                                                .reversed()
                                )
                                .thenComparingInt(
                                        standing ->
                                                standing
                                                        .getTeam()
                                                        .getRanking()
                                )
                )
                .toList();
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
        return this;
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
        return determinePenaltyShootoutWinner(secondLeg);
    }

    private Team determinePenaltyShootoutWinner(
            Match match
    ) {
        Team homeTeam = match.getHomeTeam();
        Team awayTeam = match.getAwayTeam();

        List<Player> homePlayers =
                match.getHomeLineup().getPlayers();

        List<Player> awayPlayers =
                match.getAwayLineup().getPlayers();

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
        if (hasPendingGroupMatchdays()) {
            throw new IllegalStateException(
                    "Group stage must be completed first."
            );
        }

        TournamentZone zoneA = tournamentZones.get(0);
        TournamentZone zoneB = tournamentZones.get(1);
        TournamentZone zoneC = tournamentZones.get(2);
        TournamentZone zoneD = tournamentZones.get(3);

        List<Team> winners = new ArrayList<>();

        Team winner1 = simulateSeries(
                getFirst(zoneA),
                getSecond(zoneD),
                startDate
        );

        Team winner2 = simulateSeries(
                getFirst(zoneB),
                getSecond(zoneC),
                startDate
        );

        Team winner3 = simulateSeries(
                getFirst(zoneC),
                getSecond(zoneA),
                startDate
        );

        Team winner4 = simulateSeries(
                getFirst(zoneD),
                getSecond(zoneB),
                startDate
        );

        winners.add(winner1);
        winners.add(winner2);
        winners.add(winner3);
        winners.add(winner4);

        return winners;
    }

    public List<Team> simulateSemiFinals(
            List<Team> quarterFinalWinners,
            LocalDate startDate
    ) {
        if (quarterFinalWinners.size() != 4) {
            throw new IllegalArgumentException(
                    "Four quarter-final winners are required."
            );
        }

        List<Team> finalists = new ArrayList<>();

        Team semifinal1Winner = simulateSeries(
                quarterFinalWinners.get(0),
                quarterFinalWinners.get(1),
                startDate
        );

        Team semifinal2Winner = simulateSeries(
                quarterFinalWinners.get(2),
                quarterFinalWinners.get(3),
                startDate
        );

        finalists.add(semifinal1Winner);
        finalists.add(semifinal2Winner);

        return finalists;
    }

    public Team simulateFinal(
            List<Team> finalists,
            LocalDate finalDate
    ) {
        if (finalists.size() != 2) {
            throw new IllegalArgumentException(
                    "Two finalists are required."
            );
        }

        Team teamA = finalists.get(0);
        Team teamB = finalists.get(1);

        FinalMatch finalMatch = new FinalMatch(
                finalDate,
                teamA,
                teamB,
                chooseEligibleReferee(teamA, teamB)
        );

        matches.add(finalMatch);

        matchSimulator.simulate(finalMatch);

        return determineFinalWinner(finalMatch);
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
        return determinePenaltyShootoutWinner(finalMatch);
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

            championship.getMatches()
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

        System.out.println();
        System.out.println(
                "Estado actual de la fase de grupos guardado."
        );

        List<Team> quarterWinners =
                championship.simulateQuarterFinals(
                        LocalDate.of(2026, 10, 1)
                );

        System.out.println("=== GANADORES DE CUARTOS ===");
        for (Team team : quarterWinners) {
            System.out.println(team.getName());
        }

        List<Team> finalists =
                championship.simulateSemiFinals(
                        quarterWinners,
                        LocalDate.of(2026, 10, 15)
                );

        System.out.println("=== FINALISTAS ===");
        for (Team team : finalists) {
            System.out.println(team.getName());
        }

        Team champion =
                championship.simulateFinal(
                        finalists,
                        LocalDate.of(2026, 10, 30)
                );

        System.out.println("=== CAMPEÓN ===");
        System.out.println(champion.getName());

        System.out.println(
                "Cantidad total de partidos: "
                        + championship.getMatches().size()
        );

        repository.save(championship);
        scanner.close();
    }
}
