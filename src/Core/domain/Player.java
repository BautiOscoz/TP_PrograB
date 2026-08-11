package Core.domain;

import Core.enums.Position;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

public class Player extends Person {
    private int shirtNumber;
    private Position position;
    private boolean isSuspended;
    private Map<String, Integer> characteristics;
    private Map<String, Integer> statistics;

    public Player(int id, String name, String lastName, String documentType, LocalDate birthDate,
                  int shirtNumber, Position position) {
        super(id, name, lastName, documentType, birthDate);
        this.shirtNumber = shirtNumber;
        this.position = position;
        this.isSuspended = false;
        this.characteristics = new HashMap<>();
        this.statistics = new HashMap<>();
    }

    // Getters and Setters
    public int getShirtNumber() { return shirtNumber; }
    public void setShirtNumber(int shirtNumber) { this.shirtNumber = shirtNumber; }

    public Position getPosition() { return position; }
    public void setPosition(Position position) { this.position = position; }

    public boolean isSuspended() { return isSuspended; }
    public void setSuspended(boolean suspended) { isSuspended = suspended; }

    public Map<String, Integer> getCharacteristics() { return characteristics; }
    public void setCharacteristics(Map<String, Integer> characteristics) { this.characteristics = characteristics; }

    public Map<String, Integer> getStatistics() { return statistics; }
    public void setStatistics(Map<String, Integer> statistics) { this.statistics = statistics; }

    public double getAverageRating() {
        if (characteristics.isEmpty()) {
            return 0;
        }
        return characteristics.values().stream().mapToInt(Integer::intValue).average().orElse(0);
    }
}