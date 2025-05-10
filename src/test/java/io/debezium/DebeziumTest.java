/*
 *  Copyright Debezium Authors.
 *
 *  Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.debezium;

import static org.assertj.core.api.Assertions.assertThat;


import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.debezium.connector.postgresql.PostgresConnector;
import io.debezium.junit.DebeziumConfiguration;
import io.debezium.junit.DebeziumIntegrationTest;
import io.debezium.junit.JunitDebeziumEngine;


@DebeziumIntegrationTest(value = PostgresConnector.class, resources = { PostgresResource.class },
        configuration =  {
            @DebeziumConfiguration(name = "aKey", value = "aValue"),
            @DebeziumConfiguration(name = "anotherKey", value = "anotherValue")
        })
public class DebeziumTest {

    @Test
    @DisplayName("should debezium engine running with additional configuration")
    public void shouldEngineRunningWithAdditionalConfiguration(JunitDebeziumEngine engine) {
        assertThat(engine.getConfigurationValue("aKey")).isEqualTo("aValue");
        assertThat(engine.getConfigurationValue("anotherKey")).isEqualTo("anotherValue");

        assertThat(engine.isRunning()).isTrue();
    }
}
