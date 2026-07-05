package Core.domain;

import java.util.ArrayList;
import java.util.List;

public class TournamentZone {
    private String name;
    private List<Team> teams;

    public TournamentZone(String name) {
        this.name = name;
        this.teams = new ArrayList<>();
    }

    public void addTeam(Team team) {
        if (this.teams.size() < 4) {
            this.teams.add(team);
        } else {
            throw new IllegalStateException("Each zone can only have a maximum of 4 teams.");
        }
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public List<Team> getTeams() { return teams; }
    public void setTeams(List<Team> teams) { this.teams = teams; }
}