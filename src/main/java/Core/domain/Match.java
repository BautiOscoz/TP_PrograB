package Core.domain;

import Core.incidents.Incident;
import Core.incidents.RedCard;
import Core.incidents.Substitution;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
    private Stadium stadium;

    public Match(LocalDate matchDate, Team homeTeam, Team awayTeam, Referee referee) {
        this.matchDate = matchDate;
        this.homeTeam = homeTeam;
        this.awayTeam = awayTeam;
        this.referee = referee;
        this.homeGoals = 0;
        this.awayGoals = 0;
        this.incidents = new ArrayList<>();
        this.played = false;
    }

    public void addIncident(Incident incident) {
        this.incidents.add(incident);
        this.incidents.sort(
                Comparator.comparingInt(Incident::getMinute)
        );
    }

    public List<Player> getPlayersOnField(Team team, int minute) {
        if (minute < 0) {
            throw new IllegalArgumentException(
                    "The minute cannot be negative."
            );
        }

        Lineup initialLineup;

        if (team == homeTeam) {
            initialLineup = homeLineup;
        } else if (team == awayTeam) {
            initialLineup = awayLineup;
        } else {
            throw new IllegalArgumentException(
                    "The team does not belong to this match."
            );
        }

        if (initialLineup == null) {
            throw new IllegalStateException(
                    "The initial lineups have not been assigned."
            );
        }

        List<Player> playersOnField =
                new ArrayList<>(initialLineup.getPlayers());

        for (Incident incident : incidents) {
            if (incident.getMinute() > minute) {
                break;
            }

            if (incident instanceof Substitution substitution) {
                if (team.getPlayers().contains(
                        substitution.getPlayerIn()
                )) {
                    playersOnField.remove(
                            substitution.getPlayerOut()
                    );
                    if (!playersOnField.contains(substitution.getPlayerIn())) {
                        playersOnField.add(
                                substitution.getPlayerIn()
                        );
                    }
                }
            }

            if (incident instanceof RedCard redCard) {
                if (team.getPlayers().contains(
                        redCard.getPenalizedPlayer()
                )) {
                    playersOnField.remove(
                            redCard.getPenalizedPlayer()
                    );
                }
            }
        }

        return Collections.unmodifiableList(playersOnField);
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
        if (homeLineup == null || awayLineup == null
                || homeLineup.getPlayers().size() != 11
                || awayLineup.getPlayers().size() != 11) {
            throw new IllegalArgumentException("Each initial lineup must contain exactly 11 players.");
        }
        this.homeLineup = homeLineup;
        this.awayLineup = awayLineup;
    }

    public boolean isPlayed() { return played; }
    public void setPlayed(boolean played) { this.played = played; }

    public boolean isGroupStage() { return false; }
    public int getMatchday() { return 0; }
    public void setMatchday(int matchday) {
        throw new UnsupportedOperationException("This match does not belong to the group stage.");
    }
    public Stadium getStadium() {
        return stadium;
    }

    public void setStadium(Stadium stadium) {
        if (played) {
            throw new IllegalStateException(
                    "No se puede cambiar el estadio de un partido jugado."
            );
        }

        if (stadium == null) {
            throw new IllegalArgumentException(
                    "El estadio es obligatorio."
            );
        }
        this.stadium = new Stadium(
                stadium.getId(),
                stadium.getName(),
                stadium.getCity()
        );
    }
}
