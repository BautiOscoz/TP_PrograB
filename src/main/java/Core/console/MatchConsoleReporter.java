package Core.console;

import Core.domain.Match;
import Core.domain.Player;
import Core.domain.Team;
import Core.incidents.Goal;
import Core.incidents.Incident;
import Core.incidents.RedCard;
import Core.incidents.Substitution;
import Core.incidents.YellowCard;

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
        System.out.println("------------------------------------------------------------");
        System.out.println("MATCH EVENTS");

        List<Incident> timeline = new ArrayList<>(match.getIncidents());
        timeline.sort(Comparator.comparingInt(Incident::getMinute));
        if (timeline.isEmpty()) {
            System.out.println("No incidents recorded.");
        } else {
            timeline.forEach(incident -> System.out.println(formatIncident(match, incident)));
        }

        System.out.println("------------------------------------------------------------");
        System.out.printf("FINAL SCORE: %s %d - %d %s%n",
                match.getHomeTeam().getName(), match.getHomeGoals(),
                match.getAwayGoals(), match.getAwayTeam().getName());
        System.out.println("============================================================");
    }

    private String formatIncident(Match match, Incident incident) {
        String minute = String.format("%2d'", incident.getMinute());
        if (incident instanceof Goal goal) {
            String kind = goal.isOwnGoal() ? "OWN GOAL" : goal.isPenalty() ? "PENALTY GOAL" : "GOAL";
            return minute + "  " + kind + " - " + fullName(goal.getScorer())
                    + " [" + teamName(match, goal.getScorer()) + "]";
        }
        if (incident instanceof YellowCard card) {
            return minute + "  YELLOW CARD - " + fullName(card.getPlayerWarned())
                    + " [" + teamName(match, card.getPlayerWarned()) + "]";
        }
        if (incident instanceof RedCard card) {
            return minute + "  RED CARD - " + fullName(card.getPenalizedPlayer())
                    + " [" + teamName(match, card.getPenalizedPlayer()) + "]";
        }
        if (incident instanceof Substitution substitution) {
            return minute + "  SUBSTITUTION [" + teamName(match, substitution.getPlayerIn()) + "] - IN: "
                    + fullName(substitution.getPlayerIn()) + ", OUT: " + fullName(substitution.getPlayerOut());
        }
        return minute + "  " + incident.getClass().getSimpleName();
    }

    private String teamName(Match match, Player player) {
        Team home = match.getHomeTeam();
        return home.getPlayers().contains(player) ? home.getName() : match.getAwayTeam().getName();
    }

    private String fullName(Core.domain.Person person) {
        return person.getName() + " " + person.getLastName();
    }
}
