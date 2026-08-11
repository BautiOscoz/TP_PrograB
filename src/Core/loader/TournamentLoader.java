package Core.loader;

import Core.domain.Coach;
import Core.domain.Player;
import Core.domain.Referee;
import Core.domain.Team;
import Core.dto.ArbitroDto;
import Core.dto.DtDto;
import Core.dto.EquipoDto;
import Core.dto.JugadorDto;
import Core.dto.PersonaDto;
import Core.dto.TorneoRootDto;
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
        TorneoRootDto root = GSON.fromJson(json, TorneoRootDto.class);

        if (root == null || root.torneo == null) {
            throw new IOException("Invalid tournament file: missing root 'torneo' node.");
        }

        List<Team> teams = mapTeams(root);
        List<Referee> referees = mapReferees(root);
        return new TournamentData(teams, referees);
    }

    private static List<Team> mapTeams(TorneoRootDto root) {
        List<Team> teams = new ArrayList<>();

        if (root.torneo.equipos == null || root.torneo.equipos.equipo == null) {
            return teams;
        }

        int teamId = 1;
        for (EquipoDto equipoDto : root.torneo.equipos.equipo) {
            Coach coach = mapCoach(equipoDto.plantel != null ? equipoDto.plantel.dt : null);
            Team team = new Team(teamId++, equipoDto.nombre, equipoDto.pais, equipoDto.ranking, coach);

            if (equipoDto.plantel != null
                    && equipoDto.plantel.jugadores != null
                    && equipoDto.plantel.jugadores.jugador != null) {
                int shirtNumber = 1;
                for (JugadorDto jugadorDto : equipoDto.plantel.jugadores.jugador) {
                    team.addPlayer(mapPlayer(jugadorDto, shirtNumber++));
                }
            }

            teams.add(team);
        }

        return teams;
    }

    private static List<Referee> mapReferees(TorneoRootDto root) {
        List<Referee> referees = new ArrayList<>();

        if (root.torneo.arbitros == null || root.torneo.arbitros.arbitro == null) {
            return referees;
        }

        for (ArbitroDto arbitroDto : root.torneo.arbitros.arbitro) {
            referees.add(mapReferee(arbitroDto));
        }

        return referees;
    }

    private static Coach mapCoach(DtDto dtDto) {
        if (dtDto == null || dtDto.persona == null) {
            throw new IllegalArgumentException("Each team must have a coach with personal data.");
        }

        PersonName personName = splitFullName(dtDto.persona.nombre);
        return new Coach(
                dtDto.persona.nroDocumento,
                personName.firstName(),
                personName.lastName(),
                dtDto.persona.tipoDocumento,
                parseDate(dtDto.persona.fechaNacimiento),
                dtDto.pais,
                dtDto.titulosObtenidos
        );
    }

    private static Referee mapReferee(ArbitroDto arbitroDto) {
        PersonName personName = splitFullName(arbitroDto.persona.nombre);
        return new Referee(
                arbitroDto.persona.nroDocumento,
                personName.firstName(),
                personName.lastName(),
                arbitroDto.persona.tipoDocumento,
                parseDate(arbitroDto.persona.fechaNacimiento),
                arbitroDto.pais,
                arbitroDto.aniosReferato
        );
    }

    private static Player mapPlayer(JugadorDto jugadorDto, int shirtNumber) {
        PersonaDto persona = jugadorDto.persona;
        PersonName personName = splitFullName(persona.nombre);

        Player player = new Player(
                persona.nroDocumento,
                personName.firstName(),
                personName.lastName(),
                persona.tipoDocumento,
                parseDate(persona.fechaNacimiento),
                shirtNumber,
                mapPosition(jugadorDto.posicion)
        );

        player.setCharacteristics(copyMap(jugadorDto.caracteristicas));
        player.setStatistics(copyMap(jugadorDto.estadisticas));
        return player;
    }

    private static Position mapPosition(String posicion) {
        return switch (posicion.toLowerCase()) {
            case "arquero" -> Position.GOALKEEPER;
            case "defensor" -> Position.DEFENDER;
            case "mediocampista" -> Position.MIDFIELDER;
            case "delantero" -> Position.FORWARD;
            default -> throw new IllegalArgumentException("Unknown player position: " + posicion);
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
