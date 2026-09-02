package Core.loader;

import Core.domain.Person;
import Core.domain.Player;
import Core.domain.Referee;
import Core.domain.Team;
import Core.enums.Position;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class TournamentValidator {
    private TournamentValidator() {
    }

    public static List<String> validate(TournamentData data) {
        List<String> errors = new ArrayList<>();
        if (data.getTeams().size() != 16) {
            errors.add("The tournament must contain exactly 16 teams.");
        }
        if (data.getReferees().isEmpty()) {
            errors.add("The tournament must contain referees.");
        }

        for (Team team : data.getTeams()) {
            validateTeam(team, errors);
        }
        return errors;
    }

    public static void requireValid(TournamentData data) {
        List<String> errors = validate(data);
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException("Invalid tournament data:\n- " + String.join("\n- ", errors));
        }
    }

    public static List<String> findDuplicateDocuments(TournamentData data) {
        Map<String, List<String>> owners = new HashMap<>();
        for (Team team : data.getTeams()) {
            register(owners, team.getCoach(), "coach of " + team.getName());
            for (Player player : team.getPlayers()) {
                register(owners, player, "player of " + team.getName());
            }
        }
        for (Referee referee : data.getReferees()) {
            register(owners, referee, "referee");
        }

        return owners.entrySet().stream()
                .filter(entry -> entry.getValue().size() > 1)
                .map(entry -> entry.getKey() + ": " + String.join(", ", entry.getValue()))
                .sorted()
                .toList();
    }

    private static void validateTeam(Team team, List<String> errors) {
        if (team.getPlayers().size() != 18) {
            errors.add(team.getName() + " must have exactly 18 players.");
        }

        Map<Position, Integer> expected = new EnumMap<>(Position.class);
        expected.put(Position.GOALKEEPER, 2);
        expected.put(Position.DEFENDER, 6);
        expected.put(Position.MIDFIELDER, 5);
        expected.put(Position.FORWARD, 5);

        for (Position position : Position.values()) {
            long actual = team.getPlayers().stream().filter(player -> player.getPosition() == position).count();
            if (actual != expected.get(position)) {
                errors.add(team.getName() + " must have " + expected.get(position) + " " + position + " players.");
            }
        }
    }

    private static void register(Map<String, List<String>> owners, Person person, String role) {
        String key = person.getDocumentType() + " " + person.getDocumentNumber();
        owners.computeIfAbsent(key, ignored -> new ArrayList<>())
                .add(person.getLastName() + ", " + person.getName() + " (" + role + ")");
    }
}
