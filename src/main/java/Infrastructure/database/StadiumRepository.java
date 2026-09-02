package Infrastructure.database;

import Core.domain.City;
import Core.domain.Stadium;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class StadiumRepository {

    public List<Stadium> findAll(
            List<City> cities
    ) throws SQLException {

        String sql = """
                SELECT
                    estadio.id,
                    estadio.nombre AS estadio,
                    ciudad.id AS ciudad_id
                FROM estadio
                JOIN ciudad
                    ON estadio.ciudad = ciudad.id
                JOIN pais
                    ON ciudad.id_pais = pais.id
                ORDER BY
                    pais.nombre,
                    ciudad.nombre,
                    estadio.nombre
                """;

        List<Stadium> stadiums =
                new ArrayList<>();

        try (
                Connection connection =
                        DatabaseConnection.connect();

                PreparedStatement statement =
                        connection.prepareStatement(sql);

                ResultSet resultSet =
                        statement.executeQuery()
        ) {
            while (resultSet.next()) {

                long stadiumId =
                        resultSet.getLong("id");

                String stadiumName =
                        resultSet.getString("estadio");

                long cityId =
                        resultSet.getLong("ciudad_id");

                City city =
                        findCity(cities, cityId);

                Stadium stadium =
                        new Stadium(
                                stadiumId,
                                stadiumName,
                                city
                        );

                stadiums.add(stadium);
            }
        }

        return stadiums;
    }

    private City findCity(
            List<City> cities,
            long cityId
    ) {
        return cities.stream()
                .filter(city ->
                        city.getId() == cityId
                )
                .findFirst()
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "City from stadium "
                                        + "was not loaded: "
                                        + cityId
                        )
                );
    }
}