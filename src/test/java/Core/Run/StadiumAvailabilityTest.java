package Core.Run;

import Core.domain.City;
import Core.domain.Country;
import Core.domain.Match;
import Core.domain.Stadium;

import java.time.LocalDate;
import java.util.List;

public class StadiumAvailabilityTest {
    public static void main(String[] args) throws Exception {
        Championship championship = new Championship("torneo.json", 2026L);
        Country country = championship.getCountries().get(0);
        City city = new City(1L, "Test City", country);
        Stadium first = new Stadium(1L, "First Stadium", city);
        Stadium second = new Stadium(2L, "Second Stadium", city);
        Stadium third = new Stadium(3L, "Third Stadium", city);
        championship.loadVenues(List.of(city), List.of(first, second, third));

        championship.simulateGroupStage();
        LocalDate quarterFinalDate = championship.getMatches().stream()
                .filter(Match::isGroupStage)
                .map(Match::getMatchDate)
                .max(LocalDate::compareTo)
                .orElseThrow()
                .plusDays(7);
        championship.scheduleQuarterFinals(quarterFinalDate);

        Match playedKnockoutMatch = championship.getMatches().stream()
                .filter(match -> !match.isGroupStage())
                .findFirst()
                .orElseThrow();
        playedKnockoutMatch.setStadium(first);
        playedKnockoutMatch.setPlayed(true);

        List<Stadium> unused = championship.getUnusedStadiums();
        require(unused.size() == 2, "Only unused stadiums must remain available.");
        require(unused.stream().noneMatch(stadium -> stadium.getId() == first.getId()),
                "A stadium from a played knockout match cannot be selected again.");
        require(unused.stream().anyMatch(stadium -> stadium.getId() == second.getId()),
                "An unused stadium must remain available.");
        require(unused.stream().anyMatch(stadium -> stadium.getId() == third.getId()),
                "Every unused stadium must remain available.");

        System.out.println("All stadium availability checks passed.");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
