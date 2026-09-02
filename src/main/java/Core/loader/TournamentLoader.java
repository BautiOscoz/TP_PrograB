package Core.loader;

import Core.domain.Coach;
import Core.domain.Country;
import Core.domain.Player;
import Core.domain.Referee;
import Core.domain.Team;
import Core.dto.CoachDto;
import Core.dto.CountryDto;
import Core.dto.PersonDto;
import Core.dto.PlayerDto;
import Core.dto.RefereeDto;
import Core.dto.TeamDto;
import Core.dto.TournamentRootDto;
import Core.enums.Position;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TournamentLoader {
    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(CountryDto.class,
                    (JsonDeserializer<CountryDto>) (json, type, context) -> new CountryDto(json.getAsString()))
            .create();
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public static TournamentData load(String filePath) throws IOException {
        String json = Files.readString(Path.of(filePath));
        TournamentRootDto root = GSON.fromJson(json, TournamentRootDto.class);

        if (root == null || root.tournament == null) {
            throw new IOException("Invalid tournament file: missing root 'torneo' node.");
        }

        Map<String, Country> countriesByName = new HashMap<>();
        List<Team> teams = mapTeams(root, countriesByName);
        List<Referee> referees = mapReferees(root, countriesByName);
        TournamentData data = new TournamentData(teams, referees, new ArrayList<>(countriesByName.values()));
        TournamentValidator.requireValid(data);
        return data;
    }

    private static List<Team> mapTeams(TournamentRootDto root, Map<String, Country> countriesByName) {
        List<Team> teams = new ArrayList<>();

        if (root.tournament.teams == null || root.tournament.teams.team == null) {
            return teams;
        }

        int teamId = 1;
        int personId = 1;
        for (TeamDto teamDto : root.tournament.teams.team) {
            Coach coach = mapCoach(teamDto.squad != null ? teamDto.squad.coach : null,
                    personId++, countriesByName);
            Team team = new Team(teamId++, teamDto.name,
                    countryFor(teamDto.country, countriesByName), teamDto.ranking, coach);

            if (teamDto.squad != null
                    && teamDto.squad.players != null
                    && teamDto.squad.players.player != null) {
                int shirtNumber = 1;
                for (PlayerDto playerDto : teamDto.squad.players.player) {
                    team.addPlayer(mapPlayer(playerDto, personId++, shirtNumber++));
                }
            }

            teams.add(team);
        }

        return teams;
    }

    private static List<Referee> mapReferees(TournamentRootDto root, Map<String, Country> countriesByName) {
        List<Referee> referees = new ArrayList<>();

        if (root.tournament.referees == null || root.tournament.referees.referee == null) {
            return referees;
        }

        int personId = 10_000;
        for (RefereeDto refereeDto : root.tournament.referees.referee) {
            referees.add(mapReferee(refereeDto, personId++, countriesByName));
        }

        return referees;
    }

    private static Coach mapCoach(CoachDto coachDto, int id, Map<String, Country> countriesByName) {
        if (coachDto == null || coachDto.person == null) {
            throw new IllegalArgumentException("Each team must have a coach with personal data.");
        }

        PersonName personName = splitFullName(coachDto.person.fullName);
        return new Coach(
                id,
                personName.firstName(),
                personName.lastName(),
                coachDto.person.documentType,
                coachDto.person.documentNumber,
                parseDate(coachDto.person.birthDate),
                countryFor(coachDto.country, countriesByName),
                coachDto.titlesWon
        );
    }

    private static Referee mapReferee(RefereeDto refereeDto, int id, Map<String, Country> countriesByName) {
        PersonName personName = splitFullName(refereeDto.person.fullName);
        return new Referee(
                id,
                personName.firstName(),
                personName.lastName(),
                refereeDto.person.documentType,
                refereeDto.person.documentNumber,
                parseDate(refereeDto.person.birthDate),
                countryFor(refereeDto.country, countriesByName),
                refereeDto.refereeYears
        );
    }

    private static Player mapPlayer(PlayerDto playerDto, int id, int shirtNumber) {
        PersonDto person = playerDto.person;
        PersonName personName = splitFullName(person.fullName);

        Player player = new Player(
                id,
                personName.firstName(),
                personName.lastName(),
                person.documentType,
                person.documentNumber,
                parseDate(person.birthDate),
                shirtNumber,
                mapPosition(playerDto.position)
        );

        player.setCharacteristics(copyMap(playerDto.characteristics));
        player.setStatistics(copyMap(playerDto.statistics));
        return player;
    }

    private static Position mapPosition(String position) {
        return switch (position.toLowerCase()) {
            case "arquero" -> Position.GOALKEEPER;
            case "defensor" -> Position.DEFENDER;
            case "mediocampista" -> Position.MIDFIELDER;
            case "delantero" -> Position.FORWARD;
            default -> throw new IllegalArgumentException("Unknown player position: " + position);
        };
    }

    private static LocalDate parseDate(String date) {
        return LocalDate.parse(date, DATE_FORMAT);
    }

    private static Map<String, Integer> copyMap(Map<String, Integer> source) {
        if (source == null) {
            return new HashMap<>();
        }
        return new HashMap<>(source);
    }

    private static Country countryFor(CountryDto countryDto, Map<String, Country> countriesByName) {
        if (countryDto == null || countryDto.name == null || countryDto.name.isBlank()) {
            throw new IllegalArgumentException("A country name is required.");
        }
        String name = countryDto.name.trim();
        String key = name.toLowerCase(Locale.ROOT);
        return countriesByName.computeIfAbsent(key, ignored -> new Country(name));
    }

    private static PersonName splitFullName(String fullName) {
        String trimmed = fullName.trim();
        int separatorIndex = trimmed.indexOf(' ');

        if (separatorIndex == -1) {
            return new PersonName(trimmed, "");
        }

        return new PersonName(
                trimmed.substring(separatorIndex + 1).trim(),
                trimmed.substring(0, separatorIndex).trim()
        );
    }

    private record PersonName(String firstName, String lastName) {
    }
}
