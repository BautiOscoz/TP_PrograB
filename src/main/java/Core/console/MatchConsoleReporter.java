package Core.console;

import Core.domain.Match;
import Core.incidents.Incident;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class MatchConsoleReporter {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public void printMatch(Match match) {
        System.out.println();
        System.out.println("============================================================");
        System.out.println(match.getHomeTeam().getName() + " vs " + match.getAwayTeam().getName());
        System.out.println("Date: " + match.getMatchDate().format(DATE_FORMAT));
        System.out.println("Referee: " + fullName(match.getReferee()));
        System.out.println("Home formation: " + match.getHomeLineup().getFormation());
        System.out.println("Away formation: " + match.getAwayLineup().getFormation());
        System.out.println("------------------------------------------------------------");
        System.out.println("MATCH EVENTS");

        List<Incident> timeline = new ArrayList<>(match.getIncidents());
        timeline.sort(Comparator.comparingInt(Incident::getMinute));
        if (timeline.isEmpty()) {
            System.out.println("No incidents recorded.");
        } else {
            timeline.forEach(incident ->
                    System.out.println(formatIncident(incident))
            );
        }

        System.out.println("------------------------------------------------------------");
        System.out.printf("FINAL SCORE: %s %d - %d %s%n",
                match.getHomeTeam().getName(), match.getHomeGoals(),
                match.getAwayGoals(), match.getAwayTeam().getName());
        System.out.println("============================================================");
    }

    private String formatIncident(Incident incident) {
        return String.format("%2d'  %s", incident.getMinute(), incident.getDescription());
    }

    private String fullName(Core.domain.Person person) {
        return person.getName() + " " + person.getLastName();
    }
}
