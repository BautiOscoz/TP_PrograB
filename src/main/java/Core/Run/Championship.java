package Core.Run;

import Core.console.MatchConsoleReporter;
import Core.domain.Country;
import Core.domain.GroupMatch;
import Core.domain.Match;
import Core.domain.Referee;
import Core.domain.Team;
import Core.domain.TournamentZone;
import Core.loader.TournamentData;
import Core.loader.TournamentLoader;
import Core.persistance.ChampionshipRepository;
import Core.simulation.MatchSimulator;

import java.io.IOException;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Consumer;

import Core.domain.City;
import Core.domain.Stadium;

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

    private List<Match> generateGroupMatches(LocalDate startDate) {
        List<Match> generated = new ArrayList<>();
        int dayOffset = 0;
        for (TournamentZone zone : tournamentZones) {
            List<Team> zoneTeams = zone.getTeams();
            for (int homeIndex = 0; homeIndex < zoneTeams.size(); homeIndex++) {
                for (int awayIndex = homeIndex + 1; awayIndex < zoneTeams.size(); awayIndex++) {
                    Team home = zoneTeams.get(homeIndex);
                    Team away = zoneTeams.get(awayIndex);
                    generated.add(new GroupMatch(startDate.plusDays(dayOffset++), home, away,
                            chooseEligibleReferee(home, away), null, null));
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

    public void simulateGroupStage() {
        simulateGroupStage(match -> { });
    }

    public void simulateGroupStage(Consumer<Match> afterEachMatch) {
        for (Match match : matches) {
            if (!match.isPlayed()) {
                matchSimulator.simulate(match);
                afterEachMatch.accept(match);
            }
        }
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
    public List<Team> getTeams() { return Collections.unmodifiableList(teams); }
    public List<Referee> getReferees() { return Collections.unmodifiableList(referees); }
    public List<Country> getCountries() { return Collections.unmodifiableList(countries); }
    public List<City> getCities() {return Collections.unmodifiableList(cities);}
    public List<Stadium> getStadiums() {return Collections.unmodifiableList(stadiums);}

    @Serial
    private Object readResolve() {
        this.random = new Random();
        this.matchSimulator = new MatchSimulator(this.random);
        return this;
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
            if (opcion == 1) {
                championship = repository.load();
            }
        }
        if (championship == null) {
            System.out.println("\nInicializando un nuevo torneo desde torneo.json...");
            championship = new Championship("torneo.json");
        }
        MatchConsoleReporter reporter = new MatchConsoleReporter();
        long pendingMatches = championship.getMatches().stream()
                .filter(match -> !match.isPlayed())
                .count();

        if (pendingMatches > 0) {
            System.out.println("\nSimulando " + pendingMatches + " partidos pendientes...");
            championship.simulateGroupStage(reporter::printMatch);
        } else {
            System.out.println("\nLa fase de grupos ya estaba completa. Resultados guardados:");
            championship.getMatches().forEach(reporter::printMatch);
        }

        System.out.println("\nEstado actual de la Fase de Grupos completado.");
        repository.save(championship);
        scanner.close();
    }
}
