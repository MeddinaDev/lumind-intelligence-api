package com.lumind.api.support;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class PostgreSqlSessionTimeZoneIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private DataSource dataSource;

    @Test
    void pooledConnection_usesUtcSessionTimeZone() throws Exception {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SHOW TIME ZONE")) {

            assertThat(resultSet.next()).isTrue();
            assertThat(resultSet.getString(1)).isEqualTo("UTC");
        }
    }
}
