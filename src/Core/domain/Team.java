package Core.domain;

import java.util.ArrayList;
import java.util.List;

public class Team {
    private int id;
    private String name;
    private Coach coach;
    private List<Player> players;

    public Team(int id, String name, Coach coach) {
        this.id = id;
        this.name = name;
        this.coach = coach;
        this.players = new ArrayList<>();
    }

    public void addPlayer(Player player) {
        this.players.add(player);
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Coach getCoach() { return coach; }
    public void setCoach(Coach coach) { this.coach = coach; }

    public List<Player> getPlayers() { return players; }
    public void setPlayers(List<Player> players) { this.players = players; }
}
