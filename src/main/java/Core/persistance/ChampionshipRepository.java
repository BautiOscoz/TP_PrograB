package Core.persistance;

import Core.Run.Championship;

import java.io.*;

public class ChampionshipRepository {
    private static final String FILE_PATH = "campeonato.dat";
    public void save(Championship championship) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(FILE_PATH))) {
            oos.writeObject(championship);
            System.out.println("Tournament saved successfully to " + FILE_PATH);
        } catch (IOException e) {
            System.err.println("Could not save the tournament: " + e.getMessage());
        }
    }

    public Championship load() {
        File file = new File(FILE_PATH);
        if (!file.exists()) {
            return null; // No hay una partida previa guardada
        }

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(FILE_PATH))) {
            Championship championship = (Championship) ois.readObject();
            System.out.println("Tournament loaded from " + FILE_PATH);
            return championship;
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Could not load the tournament: " + e.getMessage());
            return null;
        }
    }
    public boolean exists (){
        return new File(FILE_PATH).exists();
    }
}
