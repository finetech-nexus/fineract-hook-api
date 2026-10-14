package com.nexusbank.corebanking.hook.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Interop transfers identify the account by IBAN, not by Fineract client id.
 * Notification delivery looks the device up by client id, so resolve it from
 * interop_identifier when a JDBC URL is configured.
 */
@Component
public class SavingsClientLookup {

    private static final Logger log = LoggerFactory.getLogger(SavingsClientLookup.class);

    private final String jdbcUrl;
    private final String username;
    private final String password;

    public SavingsClientLookup(
            @Value("${fineract.lookup.jdbc-url:}") String jdbcUrl,
            @Value("${fineract.lookup.username:}") String username,
            @Value("${fineract.lookup.password:}") String password) {
        this.jdbcUrl = jdbcUrl == null ? "" : jdbcUrl.trim();
        this.username = username == null ? "" : username;
        this.password = password == null ? "" : password;
    }

    public ClientAccount findByIdentifier(String identifier) {
        if (jdbcUrl.isEmpty() || identifier == null || identifier.isBlank()) {
            return null;
        }
        String sql = """
                SELECT sa.client_id, sa.id
                FROM interop_identifier i
                JOIN m_savings_account sa ON sa.id = i.account_id
                WHERE i.a_value = ?
                LIMIT 1
                """;
        try (Connection connection = DriverManager.getConnection(jdbcUrl, username, password);
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, identifier.trim());
            try (ResultSet rows = statement.executeQuery()) {
                if (!rows.next()) {
                    return null;
                }
                return new ClientAccount(String.valueOf(rows.getLong(1)), String.valueOf(rows.getLong(2)));
            }
        } catch (SQLException ex) {
            log.warn("Could not resolve transfer account {}: {}", identifier, ex.getMessage());
            return null;
        }
    }

    public record ClientAccount(String clientId, String savingsId) {
    }
}
