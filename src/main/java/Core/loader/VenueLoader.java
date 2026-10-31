package Core.loader;

import Core.domain.*;
import java.util.List;

/** Source of venue data, independent of the database technology. */
@FunctionalInterface
public interface VenueLoader {
    Venues load(List<Country> countries);

    record Venues(List<City> cities, List<Stadium> stadiums) {
        public Venues {
            cities = List.copyOf(cities);
            stadiums = List.copyOf(stadiums);
        }
    }
}
