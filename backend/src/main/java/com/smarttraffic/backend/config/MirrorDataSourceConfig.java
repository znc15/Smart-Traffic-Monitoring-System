package com.smarttraffic.backend.config;

import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

@Configuration
public class MirrorDataSourceConfig {

    private static final Logger log = LoggerFactory.getLogger(MirrorDataSourceConfig.class);

    @ConditionalOnProperty(prefix = "app.db.mirror.mysql", name = "enabled", havingValue = "true")
    @Bean(name = "mysqlMirrorJdbcTemplate")
    public NamedParameterJdbcTemplate mysqlMirrorJdbcTemplate(MySqlMirrorProperties properties) {
        if (properties.getUrl() == null || properties.getUrl().isBlank()) {
            throw new IllegalStateException("MySQL mirror datasource enabled but app.db.mirror.mysql.url is empty");
        }
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setDriverClassName("com.mysql.cj.jdbc.Driver");
        ds.setUrl(properties.getUrl());
        ds.setUsername(properties.getUsername());
        ds.setPassword(properties.getPassword());
        return new NamedParameterJdbcTemplate(ds);
    }

    @Bean
    @ConditionalOnProperty(prefix = "app.db.mirror.mysql", name = "enabled", havingValue = "true")
    public ApplicationRunner mysqlMirrorFlywayRunner(MySqlMirrorProperties properties) {
        return args -> {
            try {
                Flyway.configure()
                        .dataSource(properties.getUrl(), properties.getUsername(), properties.getPassword())
                        .locations("classpath:db/migration-mysql")
                        .baselineOnMigrate(true)
                        .load()
                        .migrate();
            } catch (Exception ex) {
                log.warn("MySQL mirror flyway migrate failed, continue without blocking startup: {}", ex.getMessage());
            }
        };
    }

    @ConditionalOnProperty(prefix = "app.db.mirror.postgres", name = "enabled", havingValue = "true")
    @Bean(name = "postgresMirrorJdbcTemplate")
    public NamedParameterJdbcTemplate postgresMirrorJdbcTemplate(PostgresMirrorProperties properties) {
        if (properties.getUrl() == null || properties.getUrl().isBlank()) {
            throw new IllegalStateException("Postgres mirror datasource enabled but app.db.mirror.postgres.url is empty");
        }
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setDriverClassName("org.postgresql.Driver");
        ds.setUrl(properties.getUrl());
        ds.setUsername(properties.getUsername());
        ds.setPassword(properties.getPassword());
        return new NamedParameterJdbcTemplate(ds);
    }

    @Bean
    @ConditionalOnProperty(prefix = "app.db.mirror.postgres", name = "enabled", havingValue = "true")
    public ApplicationRunner postgresMirrorFlywayRunner(PostgresMirrorProperties properties) {
        return args -> {
            try {
                Flyway.configure()
                        .dataSource(properties.getUrl(), properties.getUsername(), properties.getPassword())
                        .locations("classpath:db/migration")
                        .baselineOnMigrate(true)
                        .load()
                        .migrate();
            } catch (Exception ex) {
                log.warn("Postgres mirror flyway migrate failed, continue without blocking startup: {}", ex.getMessage());
            }
        };
    }
}
