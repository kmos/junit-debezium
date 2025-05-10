/*
 *  Copyright Debezium Authors.
 *
 *  Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.debezium;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.given;


import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import io.debezium.connector.postgresql.PostgresConnector;
import io.debezium.junit.DebeziumConfiguration;
import io.debezium.junit.DebeziumIntegrationTest;
import io.debezium.junit.JunitDebeziumEngine;


@DebeziumIntegrationTest(value = PostgresConnector.class, resources = { PostgresResource.class },
        configuration =  {
            @DebeziumConfiguration(name = "aKey", value = "aValue"),
            @DebeziumConfiguration(name = "anotherKey", value = "anotherValue")
        },
        connectorCallback = ExampleCallback.class
        )
public class DebeziumTest {
    private final ListAppender<ILoggingEvent> logWatcher = new ListAppender<>();

    @BeforeEach
    void setUp() {
        logWatcher.start();
    }

    @Test
    @DisplayName("should debezium engine running with additional configuration")
    public void shouldEngineRunningWithAdditionalConfiguration(JunitDebeziumEngine engine) {
        assertThat(engine.getConfigurationValue("aKey")).isEqualTo("aValue");
        assertThat(engine.getConfigurationValue("anotherKey")).isEqualTo("anotherValue");

        assertThat(engine.isRunning()).isTrue();
    }

    @Test
    @DisplayName("should debezium engine running with callback")
    void shouldEngineRunningWithCallback() {
        Logger logger = (Logger) LoggerFactory.getLogger(ExampleCallback.class);
        logger.addAppender(logWatcher);

        given().ignoreException(NoSuchElementException.class)
                .await()
                .atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> assertThat(logWatcher.list.getFirst().getFormattedMessage())
                        .isEqualTo("Example Callback invoked"));

        logger.detachAppender(logWatcher);
    }
}
