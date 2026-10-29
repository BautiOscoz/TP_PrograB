package Core.Run;

import Core.persistance.ChampionshipRepository;
import Core.report.PlayerAttributeFormatter;
import java.io.*;
import java.nio.file.*;
import java.util.*;

public class ReportAndSaveTest {
    public static void main(String[] args) throws Exception {
        Championship tournament = new Championship("torneo.json", 2026L);
        for (var team : tournament.getTeams()) {
            for (var player : team.getPlayers()) {
                for (var values : List.of(player.getCharacteristics(), player.getStatistics())) {
                    var before = new HashMap<>(values);
                    String formatted = PlayerAttributeFormatter.format(values);
                    check(formatted.lines().count() == values.size(), "Every imported field is displayed");
                    for (String key : values.keySet()) {
                        check(!formatted.contains("  " + key + ": "), "English label missing: " + key);
                    }
                    check(before.equals(values), "Presentation must not change original data");
                }
            }
        }
        check(PlayerAttributeFormatter.format(Map.of("goles", 17, "penalesAtajados", 2))
                .equals("  Goals: 17\n  Penalties saved: 2"), "Labels and numeric values preserved");
        check(PlayerAttributeFormatter.format(Map.of()).contains("No data"), "Empty map handled");
        check(PlayerAttributeFormatter.format(Map.of("extraField", 7)).contains("extraField: 7"), "Additional fields preserved");

        Path folder = Files.createTempDirectory("championship-save-test-");
        Path file = folder.resolve("test.dat");
        try {
            ChampionshipRepository repository = new ChampionshipRepository(file);
            repository.save(tournament);
            byte[] original = Files.readAllBytes(file);
            boolean failed = false;
            try { repository.save(new BrokenChampionship()); }
            catch (IllegalStateException e) { failed = e.getCause() instanceof IOException; }
            check(failed, "Serialization failure must propagate");
            check(Arrays.equals(original, Files.readAllBytes(file)), "Failed save must preserve old file");
            check(repository.load() != null, "Old tournament still loads");
            try (var files = Files.list(folder)) { check(files.count() == 1, "Temporary file cleaned up"); }
            failed = false;
            try { new ChampionshipRepository(folder.resolve("missing/test.dat")).save(tournament); }
            catch (IllegalStateException e) { failed = true; }
            check(failed, "Unavailable save location must report an error");
            tournament.simulateNextMatchday();
            repository.save(tournament);
            check(repository.load().getPlayedMatches().size() == 8, "Existing save can be replaced and reloaded");
        } finally {
            Files.deleteIfExists(file);
            Files.deleteIfExists(folder);
        }
        System.out.println("Report translations and safe save checks passed.");
    }

    private static final class BrokenChampionship extends Championship {
        BrokenChampionship() throws IOException { super("torneo.json", 2026L); }
        @Serial private void writeObject(ObjectOutputStream out) throws IOException {
            throw new IOException("Simulated serialization failure");
        }
    }
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
