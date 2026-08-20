package Core.domain;

import Core.incidents.Incident;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public abstract class Match {
    private LocalDate matchDate;
    private Team homeTeam;
    private Team awayTeam;
    private Referee referee;
    private int homeGoals;
    private int awayGoals;
    private List<Incident> incidents;

    public Match(LocalDate matchDate, Team homeTeam, Team awayTeam, Referee referee) {
        this.matchDate = matchDate;
        this.homeTeam = homeTeam;
        this.awayTeam = awayTeam;
        this.referee = referee;
        this.homeGoals = 0;
        this.awayGoals = 0;
        this.incidents = new ArrayList<>();
    }

    public void addIncident(Incident incident) {
        this.incidents.add(incident);
    }

    // Getters and Setters
    public LocalDate getMatchDate() { return matchDate; }
    public void setMatchDate(LocalDate matchDate) { this.matchDate = matchDate; }

    public Team getHomeTeam() { return homeTeam; }
    public void setHomeTeam(Team homeTeam) { this.homeTeam = homeTeam; }

    public Team getAwayTeam() { return awayTeam; }
    public void setAwayTeam(Team awayTeam) { this.awayTeam = awayTeam; }

    public Referee getReferee() { return referee; }
    public void setReferee(Referee referee) { this.referee = referee; }

    public int getHomeGoals() { return homeGoals; }
    public void setHomeGoals(int homeGoals) { this.homeGoals = homeGoals; }

    public int getAwayGoals() { return awayGoals; }
    public void setAwayGoals(int awayGoals) { this.awayGoals = awayGoals; }

    public List<Incident> getIncidents() { return incidents; }
    public void setIncidents(List<Incident> incidents) { this.incidents = incidents; }
}