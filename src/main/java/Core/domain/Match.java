package Core.domain;

import Core.incidents.Incident;

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
    private Stadium stadium;
    private int homeGoals;
    private int awayGoals;
    private List<Incident> incidents;
    private Lineup homeLineup;
    private Lineup awayLineup;
    private boolean played;
    private String administrativeReason;
    private int postponements;

    public boolean isForfeit() { return administrativeReason != null; }
    public String getAdministrativeReason() { return administrativeReason; }
    public int getPostponements() { return postponements; }

    /** A resolved fixture, but no football was played and no player earns minutes. */
    public void awardForfeit(boolean homeUnableToPlay) {
        if (played) throw new IllegalStateException("The match is already resolved.");
        homeGoals = homeUnableToPlay ? 0 : 3;
        awayGoals = homeUnableToPlay ? 3 : 0;
        administrativeReason = (homeUnableToPlay ? homeTeam : awayTeam).getName()
                + " cannot field a valid lineup. Administrative result (0-3 defeat).";
        homeLineup = null;
        awayLineup = null;
        incidents.clear();
        played = true;
    }

    public void awardRandomForfeit(boolean homeWins) {
        awardForfeit(!homeWins);
        administrativeReason = "Neither team can field a valid lineup. Winner selected by random draw: "
                + (homeWins ? homeTeam : awayTeam).getName() + ". Administrative result (3-0 victory).";
    }

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

            incident.applyToLineup(team, playersOnField);
        }

        return Collections.unmodifiableList(playersOnField);
    }

    // Getters and Setters
    public LocalDate getMatchDate() { return matchDate; }
    public void setMatchDate(LocalDate matchDate) { this.matchDate = matchDate; }

    public Team getHomeTeam() { return homeTeam; }

    public Team getAwayTeam() { return awayTeam; }

    public Referee getReferee() { return referee; }
    public void setReferee(Referee referee) { this.referee = referee; }

    public int getHomeGoals() { return homeGoals; }
    public void setHomeGoals(int homeGoals) {
        requirePending();
        requireValidGoals(homeGoals);
        this.homeGoals = homeGoals;
    }

    public int getAwayGoals() { return awayGoals; }
    public void setAwayGoals(int awayGoals) {
        requirePending();
        requireValidGoals(awayGoals);
        this.awayGoals = awayGoals;
    }

    /** Records both scores and closes the match in one validated operation. */
    public void complete(int homeGoals, int awayGoals) {
        requirePending();
        requireValidGoals(homeGoals);
        requireValidGoals(awayGoals);
        this.homeGoals = homeGoals;
        this.awayGoals = awayGoals;
        played = true;
    }

    private void requirePending() {
        if (played) throw new IllegalStateException("The match is already resolved.");
    }

    private void requireValidGoals(int goals) {
        if (goals < 0) throw new IllegalArgumentException("Goals cannot be negative.");
    }

    public List<Incident> getIncidents() { return Collections.unmodifiableList(incidents); }

    public Lineup getHomeLineup() { return homeLineup; }
    public Lineup getAwayLineup() { return awayLineup; }

    public void setInitialLineups(Lineup homeLineup, Lineup awayLineup) {
        if (this.homeLineup != null || this.awayLineup != null) {
            throw new IllegalStateException("Initial lineups have already been recorded.");
        }
        if (homeLineup == null || awayLineup == null
                || homeLineup.getPlayers().size() != 11
                || awayLineup.getPlayers().size() != 11) {
            throw new IllegalArgumentException("Each initial lineup must contain exactly 11 players.");
        }
        if (homeLineup.getTeam() != homeTeam || awayLineup.getTeam() != awayTeam) {
            throw new IllegalArgumentException("Each lineup must belong to the corresponding team.");
        }
        this.homeLineup = homeLineup;
        this.awayLineup = awayLineup;
    }

    public boolean isPlayed() { return played; }
    public void setPlayed(boolean played) {
        if (this.played && !played) throw new IllegalStateException("A resolved match cannot be reopened.");
        this.played = played;
    }

    public abstract String getStageDescription();

    public boolean isGroupStage() { return false; }
    public FirstLegMatch asFirstLeg() { return null; }
    public SecondLegMatch asSecondLeg() { return null; }
    public FinalMatch asFinal() { return null; }
    public boolean isSettledByPenalties() { return false; }
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
                    "The stadium of a played match cannot be changed."
            );
        }

        if (stadium == null) {
            throw new IllegalArgumentException(
                    "A stadium is required."
            );
        }
        this.stadium = new Stadium(
                stadium.getId(),
                stadium.getName(),
                stadium.getCity()
        );
    }
}
