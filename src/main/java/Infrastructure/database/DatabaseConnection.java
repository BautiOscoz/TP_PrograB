package Infrastructure.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class DatabaseConnection {

    private static final String DRIVER =
            "org.postgresql.Driver";

    private static final String URL =
            "jdbc:postgresql://localhost:5432/Championship";

    private static final String USER =
            "postgres";

    private DatabaseConnection() {
    }

    public static Connection connect()
            throws SQLException {

        try {
            Class.forName(DRIVER);
        } catch (ClassNotFoundException exception) {
            throw new IllegalStateException(
                    "PostgreSQL driver was not found.",
                    exception
            );
        }

        String password =
                System.getenv("CHAMPIONSHIP_DB_PASSWORD");

        if (password == null || password.isBlank()) {
            throw new IllegalStateException(
                    "Environment variable "
                            + "CHAMPIONSHIP_DB_PASSWORD is missing."
            );
        }

        return DriverManager.getConnection(
                URL,
                USER,
                password
        );
    }
}
