package Core.persistance;

import Core.Run.Championship;

import java.io.*;

public class ChampionshipRepository {
    private static final String FILE_PATH = "campeonato.dat";
    public void save(Championship championship) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(FILE_PATH))) {
            oos.writeObject(championship);
            System.out.println("Estado del torneo guardado exitosamente en " + FILE_PATH);
        } catch (IOException e) {
            System.err.println("Error al serializar el campeonato: " + e.getMessage());
        }
    }

    public Championship load() {
        File file = new File(FILE_PATH);
        if (!file.exists()) {
            return null; // No hay una partida previa guardada
        }

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(FILE_PATH))) {
            Championship championship = (Championship) ois.readObject();
            System.out.println("Estado del torneo cargado desde " + FILE_PATH);
            return championship;
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error al deserializar el campeonato: " + e.getMessage());
            return null;
        }
    }
    public boolean exists (){
        return new File(FILE_PATH).exists();
    }
}
