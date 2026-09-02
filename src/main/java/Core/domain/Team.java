package Core.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Team {
    private int id;
    private String name;
    private Country country;
    private int ranking;
    private Coach coach;
    private List<Player> players;

    public Team(int id, String name, Country country, int ranking, Coach coach) {
        this.id = id;
        this.name = name;
        this.country = country;
        this.ranking = ranking;
        this.coach = coach;
        this.players = new ArrayList<>();
    }

    public void addPlayer(Player player) {
        if (player == null) {
            throw new IllegalArgumentException("A team cannot contain a null player.");
        }
        if (players.size() >= 18) {
            throw new IllegalStateException("A team can only have 18 players.");
        }
        this.players.add(player);
    }

    public double getAveragePlayerRating() {
        return players.stream().mapToDouble(Player::getAverageRating).average().orElse(0);
    }

    public double getCompetitiveStrength() {
        double rankingScore = Math.max(0, 101 - ranking);
        double coachScore = Math.min(100, coach.getTitlesWon() * 5.0);
        return rankingScore * 0.35 + getAveragePlayerRating() * 0.55 + coachScore * 0.10;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Country getCountry() { return country; }
    public void setCountry(Country country) { this.country = country; }

    public int getRanking() { return ranking; }
    public void setRanking(int ranking) { this.ranking = ranking; }

    public Coach getCoach() { return coach; }
    public void setCoach(Coach coach) { this.coach = coach; }

    public List<Player> getPlayers() { return Collections.unmodifiableList(players); }
}
