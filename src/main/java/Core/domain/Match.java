package Core.domain;

import Core.incidents.Incident;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class Match implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private LocalDate matchDate;
    private Team homeTeam;
    private Team awayTeam;
    private Referee referee;
    private int homeGoals;
    private int awayGoals;
    private List<Incident> incidents;
    private Lineup homeLineup;
    private Lineup awayLineup;
    private boolean played;

    public Match(LocalDate matchDate, Team homeTeam, Team awayTeam, Referee referee,Lineup homeLineup, Lineup awayLineup) {
        this.matchDate = matchDate;
        this.homeTeam = homeTeam;
        this.awayTeam = awayTeam;
        this.referee = referee;
        this.homeGoals = 0;
        this.awayGoals = 0;
        this.incidents = new ArrayList<>();
        this.homeLineup = homeLineup;
        this.awayLineup = awayLineup;
        this.played = false;
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

    public List<Incident> getIncidents() { return Collections.unmodifiableList(incidents); }

    public Lineup getHomeLineup() { return homeLineup; }
    public Lineup getAwayLineup() { return awayLineup; }

    public void setInitialLineups(Lineup homeLineup, Lineup awayLineup) {
        if (homeLineup == null || awayLineup == null || homeLineup.selectBestLineup().size() != 11 || awayLineup.selectBestLineup().size() != 11) {
            throw new IllegalArgumentException("Each initial lineup must contain exactly 11 players.");
        }
        this.homeLineup = homeLineup;
        this.awayLineup = awayLineup;
    }

    public boolean isPlayed() { return played; }
    public void setPlayed(boolean played) { this.played = played; }
}
