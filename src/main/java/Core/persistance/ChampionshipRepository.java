package Core.persistance;

import Core.Run.Championship;

import java.io.*;
import java.nio.file.*;
import java.util.Objects;

public class ChampionshipRepository {
    private final Path filePath;

    public ChampionshipRepository() {
        this(Path.of("campeonato.dat"));
    }

    public ChampionshipRepository(Path filePath) {
        this.filePath = Objects.requireNonNull(filePath).toAbsolutePath();
    }

    public void save(Championship championship) {
        Objects.requireNonNull(championship, "A tournament is required.");
        Path temporary = null;
        try {
            temporary = Files.createTempFile(filePath.getParent(), "championship-", ".tmp");
            try (ObjectOutputStream oos = new ObjectOutputStream(Files.newOutputStream(temporary))) {
                oos.writeObject(championship);
            }
            // Replace the previous save only after serialization has finished successfully.
            Files.move(temporary, filePath, StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
            System.out.println("Tournament saved successfully to " + filePath);
        } catch (IOException e) {
            throw new IllegalStateException("Could not save the tournament to " + filePath
                    + ". Check write permissions, available disk space and whether the file is in use.", e);
        } finally {
            if (temporary != null) {
                try { Files.deleteIfExists(temporary); }
                catch (IOException ignored) { /* Do not hide the original save error. */ }
            }
        }
    }

    public Championship load() {
        File file = filePath.toFile();
        if (!file.exists()) {
            return null; // No previous saved tournament exists
        }

        try (ObjectInputStream ois = new ObjectInputStream(Files.newInputStream(filePath))) {
            Championship championship = (Championship) ois.readObject();
            System.out.println("Tournament loaded from " + filePath);
            return championship;
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Could not load the tournament: " + e.getMessage());
            return null;
        }
    }
    public boolean exists (){
        return Files.exists(filePath);
    }
}
