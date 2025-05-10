package io.debezium;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
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
public class AppTest {

    @BeforeEach
    public void beforeEach(JunitDebeziumEngine engine) {
        System.out.println(engine);
    }

    /**
     * Rigorous Test :-)
     */
    @Test
    public void shouldAnswerWithTrue() throws InterruptedException {
        Thread.sleep(10000);
        assertTrue(true);
    }
}
