package Infrastructure.database;

import Core.domain.Country;
import Core.loader.VenueLoader;
import java.sql.SQLException;
import java.util.List;

/** Reads both tables before handing a consistent snapshot to the tournament. */
public final class DatabaseVenueLoader implements VenueLoader {
    @Override
    public Venues load(List<Country> countries) {
        try {
            var cities = new CityRepository().findAll(countries);
            return new Venues(cities, new StadiumRepository().findAll(cities));
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not load stadiums from PostgreSQL.", exception);
        }
    }
}
