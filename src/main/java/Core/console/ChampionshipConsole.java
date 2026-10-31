package Core.console;
import Core.Run.Championship;
import Core.domain.*;
import Core.enums.TournamentStage;
import Core.persistance.ChampionshipRepository;
import java.io.IOException;
import java.util.*;

/** Optional console application; the JavaFX entry point remains GUI.AppLauncher. */
public final class ChampionshipConsole {
    public static void main(String[] args) throws IOException {
        ChampionshipRepository repository = new ChampionshipRepository();
        Championship championship = null;

        Scanner scanner = new Scanner(System.in);
        if (repository.exists()) {
            System.out.println("==========================================");
            System.out.println("A saved tournament was found.");
            System.out.println("1. Continue the saved tournament");
            System.out.println("2. Start a new tournament (overwrites the previous save)");
            System.out.print("Select an option: ");

            int opcion = scanner.nextInt();
            scanner.nextLine();
            if (opcion == 1) {
                championship = repository.load();
            }
        }
        if (championship == null) {
            System.out.println("\nStarting a new tournament from torneo.json...");
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
                    "The group stage was already complete."
            );

            System.out.println(
                    "Saved results and standings:"
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

        System.out.println("=== CHAMPION ===");
        if (championship.getChampion() != null) {
            System.out.println(championship.getChampion().getName());
        } else {
            FinalMatch savedFinal = championship.getMatches().stream()
                    .map(Match::asFinal)
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(null);
            if (savedFinal != null) {
                System.out.println(championship.getRecordedFinalWinner(savedFinal).getName());
            }
        }

        System.out.println(
                "Total matches: "
                        + championship.getMatches().size()
        );

        repository.save(championship);
        scanner.close();
    }
}
