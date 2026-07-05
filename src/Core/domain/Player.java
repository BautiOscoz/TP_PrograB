package Core.domain;

import Core.enums.Position;

public class Player extends Person {
    private int shirtNumber;
    private Position position;
    private boolean isSuspended;

    public Player(int id, String name, String lastName, int shirtNumber, Position position) {
        super(id, name, lastName);
        this.shirtNumber = shirtNumber;
        this.position = position;
        this.isSuspended = false;
    }

    // Getters and Setters
    public int getShirtNumber() { return shirtNumber; }
    public void setShirtNumber(int shirtNumber) { this.shirtNumber = shirtNumber; }

    public Position getPosition() { return position; }
    public void setPosition(Position position) { this.position = position; }

    public boolean isSuspended() { return isSuspended; }
    public void setSuspended(boolean suspended) { isSuspended = suspended; }
}