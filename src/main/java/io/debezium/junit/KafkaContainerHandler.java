/*
 *  Copyright Debezium Authors.
 *
 *  Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.debezium.junit;


import java.util.Collections;
import java.util.Map;
import java.util.stream.Stream;

import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.lifecycle.Startables;
import org.testcontainers.utility.DockerImageName;

import io.debezium.testing.testcontainers.Connector;
import io.debezium.testing.testcontainers.ConnectorConfiguration;
import io.debezium.testing.testcontainers.DebeziumContainer;

class KafkaContainerHandler implements DebeziumTestResourceLifecycleManager {

    private static final String KAFKA_IMAGE = "confluentinc/cp-kafka:7.2.0";
    private static final String CONNECT_IMAGE = "debezium/connect-base:1.9.5.Final";

    private static final Network network = Network.newNetwork();
    private static final KafkaContainer kafkaContainer = new KafkaContainer(DockerImageName.parse(KAFKA_IMAGE))
            .withNetwork(network);

    public static DebeziumContainer connectContainer = new DebeziumContainer(CONNECT_IMAGE)
            .withFileSystemBind("target/kcetcd-connector", "/kafka/connect/test-connector")
            .withNetwork(network)
            .withKafka(kafkaContainer)
            .dependsOn(kafkaContainer);

    @Override
    public Map<String, String> start() {
        Startables.deepStart(Stream.of(kafkaContainer, connectContainer));

        ConnectorConfiguration connector = ConnectorConfiguration
                .from(Collections.emptyMap())
                .with("connector.class", "dev.morling.kcetcd.source.EtcdSourceConnector")
                .with("clusters", "test-etcd=http://etcd:2379")
                .with("tasks.max", "2")
                .with("key.converter", "org.apache.kafka.connect.storage.StringConverter")
                .with("value.converter", "org.apache.kafka.connect.storage.StringConverter");

        connectContainer.registerConnector("test-connector", connector);
        connectContainer.ensureConnectorTaskState("test-connector", 0, Connector.State.RUNNING);

        return Map.of();
    }

    @Override
    public void stop() {

    }
}
