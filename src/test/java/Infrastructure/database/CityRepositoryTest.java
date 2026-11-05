package Infrastructure.database;

import Core.Run.Championship;
import Core.domain.City;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class CityRepositoryTest {

    public static void main(String[] args) {

        try {
            Championship championship =
                    new Championship("torneo.json");

            CityRepository cityRepository =
                    new CityRepository();

            List<City> cities =
                    cityRepository.findAll(
                            championship.getCountries()
                    );

            System.out.println(
                    "Cities loaded: " + cities.size()
            );

            System.out.println(
                    "--------------------------------"
            );

            for (City city : cities) {
                System.out.println(
                        "ID: " + city.getId()
                );

                System.out.println(
                        "City: " + city.getName()
                );

                System.out.println(
                        "Country: "
                                + city.getCountry().getName()
                );

                System.out.println(
                        "--------------------------------"
                );
            }

        } catch (SQLException exception) {
            System.err.println(
                    "Database error: "
                            + exception.getMessage()
            );

        } catch (IOException exception) {
            System.err.println(
                    "JSON loading error: "
                            + exception.getMessage()
            );

        } catch (IllegalArgumentException exception) {
            System.err.println(
                    "Data error: "
                            + exception.getMessage()
            );
        }
    }
}