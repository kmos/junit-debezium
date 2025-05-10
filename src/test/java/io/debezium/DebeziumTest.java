package io.debezium;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;


import org.junit.jupiter.api.Test;

import io.debezium.connector.postgresql.PostgresConnector;
import io.debezium.junit.DebeziumConfiguration;
import io.debezium.junit.DebeziumIntegrationTest;
import io.debezium.junit.JunitDebeziumEngine;

/**
 * Unit test for simple App.
 */
@DebeziumIntegrationTest(value = PostgresConnector.class, resources = { PostgresResource.class },
        configuration =  {
            @DebeziumConfiguration(name = "aKey", value = "aValue"),
            @DebeziumConfiguration(name = "anotherKey", value = "anotherValue")
        })
public class DebeziumTest {

    /**
     * Rigorous Test :-)
     */
    @Test
    public void shouldEngineRunning(JunitDebeziumEngine engine) {
        assertThat(engine.isRunning()).isTrue();
    }
}
