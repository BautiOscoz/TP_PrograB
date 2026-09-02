package Infrastructure.database;

import Core.Run.Championship;
import Core.domain.City;
import Core.domain.Stadium;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class StadiumRepositoryTest {

    public static void main(String[] args) {

        try {

            Championship championship = new Championship("torneo.json");

            CityRepository cityRepository = new CityRepository();

            StadiumRepository stadiumRepository = new StadiumRepository();

            List<City> cities = cityRepository.findAll(championship.getCountries());


            List<Stadium> stadiums = stadiumRepository.findAll(cities);


            championship.loadVenues(cities, stadiums);

            // 5. Mostrar los resultados
            System.out.println(
                    "Cities loaded: "
                            + championship.getCities().size()
            );

            System.out.println(
                    "Stadiums loaded: "
                            + championship.getStadiums().size()
            );

            System.out.println(
                    "--------------------------------"
            );

            for (Stadium stadium : championship.getStadiums()) {

                System.out.println(
                        "Stadium ID: "
                                + stadium.getId()
                );

                System.out.println(
                        "Stadium: "
                                + stadium.getName()
                );

                System.out.println(
                        "City: "
                                + stadium.getCity().getName()
                );

                System.out.println(
                        "Country: "
                                + stadium
                                .getCity()
                                .getCountry()
                                .getName()
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
                    "Data relationship error: "
                            + exception.getMessage()
            );
        }
    }
}