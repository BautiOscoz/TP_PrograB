package Core.loader;

import Core.domain.Coach;
import Core.domain.Player;
import Core.domain.Referee;
import Core.domain.Team;
import Core.dto.CoachDto;
import Core.dto.PersonDto;
import Core.dto.PlayerDto;
import Core.dto.RefereeDto;
import Core.dto.TeamDto;
import Core.dto.TournamentRootDto;
import Core.enums.Position;
import com.google.gson.Gson;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TournamentLoader {
    private static final Gson GSON = new Gson();
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public static TournamentData load(String filePath) throws IOException {
        String json = Files.readString(Path.of(filePath));
        TournamentRootDto root = GSON.fromJson(json, TournamentRootDto.class);

        if (root == null || root.tournament == null) {
            throw new IOException("Invalid tournament file: missing root 'torneo' node.");
        }

        List<Team> teams = mapTeams(root);
        List<Referee> referees = mapReferees(root);
        return new TournamentData(teams, referees);
    }

    private static List<Team> mapTeams(TournamentRootDto root) {
        List<Team> teams = new ArrayList<>();

        if (root.tournament.teams == null || root.tournament.teams.team == null) {
            return teams;
        }

        int teamId = 1;
        for (TeamDto teamDto : root.tournament.teams.team) {
            Coach coach = mapCoach(teamDto.squad != null ? teamDto.squad.coach : null);
            Team team = new Team(teamId++, teamDto.name, teamDto.country, teamDto.ranking, coach);

            if (teamDto.squad != null
                    && teamDto.squad.players != null
                    && teamDto.squad.players.player != null) {
                int shirtNumber = 1;
                for (PlayerDto playerDto : teamDto.squad.players.player) {
                    team.addPlayer(mapPlayer(playerDto, shirtNumber++));
                }
            }

            teams.add(team);
        }

        return teams;
    }

    private static List<Referee> mapReferees(TournamentRootDto root) {
        List<Referee> referees = new ArrayList<>();

        if (root.tournament.referees == null || root.tournament.referees.referee == null) {
            return referees;
        }

        for (RefereeDto refereeDto : root.tournament.referees.referee) {
            referees.add(mapReferee(refereeDto));
        }

        return referees;
    }

    private static Coach mapCoach(CoachDto coachDto) {
        if (coachDto == null || coachDto.person == null) {
            throw new IllegalArgumentException("Each team must have a coach with personal data.");
        }

        PersonName personName = splitFullName(coachDto.person.fullName);
        return new Coach(
                coachDto.person.documentNumber,
                personName.firstName(),
                personName.lastName(),
                coachDto.person.documentType,
                parseDate(coachDto.person.birthDate),
                coachDto.country,
                coachDto.titlesWon
        );
    }

    private static Referee mapReferee(RefereeDto refereeDto) {
        PersonName personName = splitFullName(refereeDto.person.fullName);
        return new Referee(
                refereeDto.person.documentNumber,
                personName.firstName(),
                personName.lastName(),
                refereeDto.person.documentType,
                parseDate(refereeDto.person.birthDate),
                refereeDto.country,
                refereeDto.refereeYears
        );
    }

    private static Player mapPlayer(PlayerDto playerDto, int shirtNumber) {
        PersonDto person = playerDto.person;
        PersonName personName = splitFullName(person.fullName);

        Player player = new Player(
                person.documentNumber,
                personName.firstName(),
                personName.lastName(),
                person.documentType,
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
