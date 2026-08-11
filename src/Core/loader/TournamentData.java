package Core.loader;

import Core.domain.Referee;
import Core.domain.Team;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TournamentData {
    private final List<Team> teams;
    private final List<Referee> referees;

    public TournamentData(List<Team> teams, List<Referee> referees) {
        this.teams = new ArrayList<>(teams);
        this.referees = new ArrayList<>(referees);
    }

    public List<Team> getTeams() {
        return Collections.unmodifiableList(teams);
    }

    public List<Referee> getReferees() {
        return Collections.unmodifiableList(referees);
    }
}
