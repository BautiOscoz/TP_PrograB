package Core.report;

import java.util.Comparator;
import java.util.Map;
import java.util.stream.Collectors;

/** English labels for the attributes provided by the original tournament data. */
public final class PlayerAttributeFormatter {
    private static final Map<String, String> LABELS = Map.ofEntries(
            Map.entry("achique", "Closing down"),
            Map.entry("cabezazo", "Heading"),
            Map.entry("capacidadQuite", "Tackling"),
            Map.entry("definicion", "Finishing"),
            Map.entry("habilidad", "Skill"),
            Map.entry("juegoAereo", "Aerial ability"),
            Map.entry("juegoPies", "Footwork"),
            Map.entry("potenciaDisparo", "Shot power"),
            Map.entry("reflejos", "Reflexes"),
            Map.entry("resistenciaFisica", "Stamina"),
            Map.entry("seguridadManos", "Handling"),
            Map.entry("ubicacion", "Positioning"),
            Map.entry("velocidad", "Speed"),
            Map.entry("visionDeJuego", "Vision"),
            Map.entry("expulsiones", "Red cards"),
            Map.entry("goles", "Goals"),
            Map.entry("golesRecibidos", "Goals conceded"),
            Map.entry("partidosJugados", "Matches played"),
            Map.entry("pasesGol", "Assists"),
            Map.entry("penales", "Penalties"),
            Map.entry("penalesAtajados", "Penalties saved"),
            Map.entry("penalesConvertidos", "Penalties scored"),
            Map.entry("penalesRecibidos", "Penalties faced")
    );

    private PlayerAttributeFormatter() { }

    public static String format(Map<String, Integer> values) {
        if (values == null || values.isEmpty()) return "  No data available.";
        return values.entrySet().stream()
                .sorted(Comparator.comparing(entry -> label(entry.getKey())))
                .map(entry -> "  " + label(entry.getKey()) + ": " + entry.getValue())
                .collect(Collectors.joining("\n"));
    }

    private static String label(String key) {
        // Preserve additional source fields instead of silently dropping them.
        return LABELS.getOrDefault(key, key);
    }
}
