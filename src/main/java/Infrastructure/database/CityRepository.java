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

    public City create(String name, Country country) throws SQLException {
        validateCity(name, country);
        String sql = """
                INSERT INTO ciudad (nombre, id_pais)
                VALUES (?, ?)
                RETURNING id
                """;

        try (Connection connection = DatabaseConnection.connect()) {
            long countryId = findCountryId(connection, country.getName());
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, name.trim());
                statement.setLong(2, countryId);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (!resultSet.next()) {
                        throw new SQLException("The city could not be created.");
                    }
                    return new City(resultSet.getLong("id"), name.trim(), country);
                }
            }
        }
    }

    public boolean update(City city) throws SQLException {
        if (city == null) {
            throw new IllegalArgumentException("A city is required.");
        }
        validateId(city.getId());
        validateCity(city.getName(), city.getCountry());
        String sql = """
                UPDATE ciudad
                SET nombre = ?, id_pais = ?
                WHERE id = ?
                """;

        try (Connection connection = DatabaseConnection.connect()) {
            long countryId = findCountryId(connection, city.getCountry().getName());
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, city.getName().trim());
                statement.setLong(2, countryId);
                statement.setLong(3, city.getId());
                return statement.executeUpdate() == 1;
            }
        }
    }

    public boolean delete(long cityId) throws SQLException {
        validateId(cityId);
        String stadiumSql = "SELECT COUNT(*) FROM estadio WHERE ciudad = ?";
        String deleteSql = "DELETE FROM ciudad WHERE id = ?";

        try (Connection connection = DatabaseConnection.connect()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement statement = connection.prepareStatement(stadiumSql)) {
                    statement.setLong(1, cityId);
                    try (ResultSet resultSet = statement.executeQuery()) {
                        resultSet.next();
                        if (resultSet.getInt(1) > 0) {
                            throw new IllegalStateException(
                                    "The city cannot be deleted because it has associated stadiums."
                            );
                        }
                    }
                }
                boolean deleted;
                try (PreparedStatement statement = connection.prepareStatement(deleteSql)) {
                    statement.setLong(1, cityId);
                    deleted = statement.executeUpdate() == 1;
                }
                connection.commit();
                return deleted;
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    private long findCountryId(Connection connection, String countryName) throws SQLException {
        String sql = "SELECT id FROM pais WHERE LOWER(nombre) = LOWER(?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, countryName);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new IllegalArgumentException(
                            "Country was not found in PostgreSQL: " + countryName
                    );
                }
                return resultSet.getLong("id");
            }
        }
    }

    private void validateCity(String name, Country country) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("The city name is required.");
        }
        if (country == null) {
            throw new IllegalArgumentException("A country is required.");
        }
    }

    private void validateId(long id) {
        if (id <= 0) {
            throw new IllegalArgumentException("The city ID must be positive.");
        }
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
