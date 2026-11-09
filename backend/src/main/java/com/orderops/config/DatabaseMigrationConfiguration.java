package com.orderops.config;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("!unit")
public class DatabaseMigrationConfiguration {
    @Bean
    FlywayMigrationStrategy safeMigration(DataSource dataSource,
            @Value("${orderops.database-name}") String expectedDatabase) {
        return flyway -> {
            try (Connection connection = dataSource.getConnection();
                    var statement = connection.createStatement();
                    ResultSet result = statement.executeQuery("SELECT current_database()")) {
                if (!result.next() || !expectedDatabase.equals(result.getString(1))) {
                    throw new IllegalStateException("Refusing migration: unexpected database name");
                }
            } catch (SQLException e) {
                throw new IllegalStateException("Cannot verify database before migration", e);
            }
            flyway.migrate();
        };
    }
}
