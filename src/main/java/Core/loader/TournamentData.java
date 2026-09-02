package Core.loader;

import Core.domain.Country;
import Core.domain.Referee;
import Core.domain.Team;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TournamentData {
    private final List<Team> teams;
    private final List<Referee> referees;
    private final List<Country> countries;

    public TournamentData(List<Team> teams, List<Referee> referees, List<Country> countries) {
        this.teams = new ArrayList<>(teams);
        this.referees = new ArrayList<>(referees);
        this.countries = new ArrayList<>(countries);
    }

    public List<Team> getTeams() {
        return Collections.unmodifiableList(teams);
    }

    public List<Referee> getReferees() {
        return Collections.unmodifiableList(referees);
    }

    public List<Country> getCountries() {
        return Collections.unmodifiableList(countries);
    }
}
