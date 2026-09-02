package Infrastructure.database;

import Core.domain.City;
import Core.domain.Country;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CityRepository {

    public List<City> findAll(
            List<Country> countries
    ) throws SQLException {

        String sql = """
                SELECT
                    ciudad.id,
                    ciudad.nombre AS ciudad,
                    pais.nombre AS pais
                FROM ciudad
                JOIN pais
                    ON ciudad.id_pais = pais.id
                ORDER BY
                    pais.nombre,
                    ciudad.nombre
                """;

        List<City> cities = new ArrayList<>();

        try (
                Connection connection =
                        DatabaseConnection.connect();

                PreparedStatement statement =
                        connection.prepareStatement(sql);

                ResultSet resultSet =
                        statement.executeQuery()
        ) {
            while (resultSet.next()) {

                long cityId =
                        resultSet.getLong("id");

                String cityName =
                        resultSet.getString("ciudad");

                String countryName =
                        resultSet.getString("pais");

                Country country =
                        findCountry(
                                countries,
                                countryName
                        );

                City city = new City(
                        cityId,
                        cityName,
                        country
                );

                cities.add(city);
            }
        }

        return cities;
    }

    private Country findCountry(
            List<Country> countries,
            String countryName
    ) {
        return countries.stream()
                .filter(country ->
                        country.getName()
                                .equalsIgnoreCase(countryName)
                )
                .findFirst()
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Country from database "
                                        + "was not found in JSON: "
                                        + countryName
                        )
                );
    }
}