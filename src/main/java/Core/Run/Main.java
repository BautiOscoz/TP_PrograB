package Core.Run;

import Core.domain.Coach;
import Core.domain.Person;
import Core.domain.Player;
import Core.domain.Referee;
import Core.domain.Team;
import Core.loader.TournamentData;
import Core.loader.TournamentLoader;

import java.util.Map;

public class Main {

     public static void main(String[] args) {
        try {
            String jsonPath = args.length > 0 ? args[0] : "torneo.json";
            TournamentData tournament = TournamentLoader.load(jsonPath);

            System.out.println("=== TOURNAMENT DATA ===");
            System.out.println("Teams loaded: " + tournament.getTeams().size());
            System.out.println("Referees loaded: " + tournament.getReferees().size());
            System.out.println();

            //printTeams(tournament);
            //printReferees(tournament);
        } catch (Exception exception) {
            System.err.println("Could not load tournament data: " + exception.getMessage());
            exception.printStackTrace();
        }
    }

    private static void printTeams(TournamentData tournament) {
        System.out.println("=== TEAMS ===");

        for (Team team : tournament.getTeams()) {
            System.out.println("----------------------------------------");
            System.out.println("Team ID: " + team.getId());
            System.out.println("Name: " + team.getName());
            System.out.println("Country: " + team.getCountry());
            System.out.println("Ranking: " + team.getRanking());
            System.out.println();

            System.out.println("Coach:");
            printCoach(team.getCoach());
            System.out.println();

            System.out.println("Players (" + team.getPlayers().size() + "):");
            int playerIndex = 1;
            for (Player player : team.getPlayers()) {
                System.out.println("  [" + playerIndex++ + "]");
                printPlayer(player);
                System.out.println();
            }
        }
    }

    private static void printReferees(TournamentData tournament) {
        System.out.println("=== REFEREES ===");

        int refereeIndex = 1;
        for (Referee referee : tournament.getReferees()) {
            System.out.println("----------------------------------------");
            System.out.println("Referee #" + refereeIndex++);
            printPerson(referee);
            System.out.println("Nationality: " + referee.getNationality());
            System.out.println("Referee years: " + referee.getRefereeYears());
            System.out.println();
        }
    }

    private static void printCoach(Coach coach) {
        printPerson(coach);
        System.out.println("  Nationality: " + coach.getNationality());
        System.out.println("  Titles won: " + coach.getTitlesWon());
    }

    private static void printPlayer(Player player) {
        printPerson(player, "  ");
        System.out.println("  Shirt number: " + player.getShirtNumber());
        System.out.println("  Position: " + player.getPosition());
        System.out.println("  Suspended: " + player.isSuspended());
        System.out.println("  Average rating: " + String.format("%.2f", player.getAverageRating()));
        printMap("  Characteristics", player.getCharacteristics());
        printMap("  Statistics", player.getStatistics());
    }

    private static void printPerson(Person person) {
        printPerson(person, "");
    }

    private static void printPerson(Person person, String prefix) {
        System.out.println(prefix + "ID: " + person.getId());
        System.out.println(prefix + "First name: " + person.getName());
        System.out.println(prefix + "Last name: " + person.getLastName());
        System.out.println(prefix + "Document type: " + person.getDocumentType());
        System.out.println(prefix + "Birth date: " + person.getBirthDate());
    }

    private static void printMap(String label, Map<String, Integer> values) {
        System.out.println("  " + label + ":");
        if (values == null || values.isEmpty()) {
            System.out.println("    (empty)");
            return;
        }

        for (Map.Entry<String, Integer> entry : values.entrySet()) {
            System.out.println("    " + entry.getKey() + ": " + entry.getValue());
        }
    }
}
