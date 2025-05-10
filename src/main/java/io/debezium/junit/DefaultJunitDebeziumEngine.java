package io.debezium.junit;

import static io.debezium.embedded.EmbeddedEngineConfig.OFFSET_STORAGE;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.kafka.connect.source.SourceConnector;
import org.apache.kafka.connect.source.SourceRecord;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionTimeoutException;

import io.debezium.config.CommonConnectorConfig;
import io.debezium.config.Configuration;
import io.debezium.embedded.Connect;
import io.debezium.embedded.EmbeddedEngineConfig;
import io.debezium.engine.DebeziumEngine;
import io.debezium.engine.RecordChangeEvent;
import io.debezium.engine.format.ChangeEventFormat;

class DefaultJunitDebeziumEngine implements JunitDebeziumEngine {
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final AtomicBoolean isEngineRunning = new AtomicBoolean(false);
    private final DebeziumEngine<RecordChangeEvent<SourceRecord>> engine;

    private final Class<?> sourceConnectorClass;
    private final static Map<String, String> baseConfiguration;
    private final JunitEngineCallback junitEngineCallback = new JunitEngineCallback();

    static {
        baseConfiguration = new HashMap<>();
        baseConfiguration.put(EmbeddedEngineConfig.ENGINE_NAME.name(), "testing-connector");
        baseConfiguration.put(CommonConnectorConfig.TOPIC_PREFIX.name(), "testing");
        baseConfiguration.put(EmbeddedEngineConfig.OFFSET_FLUSH_INTERVAL_MS.name(), String.valueOf(100));
        baseConfiguration.put(OFFSET_STORAGE.name(), "org.apache.kafka.connect.storage.MemoryOffsetBackingStore");
    }
    private final Configuration configuration;


    DefaultJunitDebeziumEngine(Class<? extends SourceConnector> sourceConnectorClass,
                               Map<String, String> configuration,
                               ConnectorCallback connectorCallback
    ) {
        this.sourceConnectorClass = sourceConnectorClass;
        this.configuration = createConfiguration(configuration, sourceConnectorClass);

        if (connectorCallback == null) {
            this.engine = DebeziumEngine.create(ChangeEventFormat.of(Connect.class))
                    .using(this.configuration.asProperties())
                    .using(getClass().getClassLoader())
                    .using(this.junitEngineCallback)
                    .notifying((ignore) -> {
                    })
                    .build();
            return;
        }

        this.engine = DebeziumEngine.create(ChangeEventFormat.of(Connect.class))
                .using(this.configuration.asProperties())
                .using(getClass().getClassLoader())
                .using(new ComposableCallbacks(List.of(new JunitEngineCallback(), connectorCallback)))
                .notifying((ignore) -> {
                })
                .build();
    }

    @Override
    public void close() throws IOException {
        try {
            engine.close();
            Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() -> !isEngineRunning.get());
        } catch (IOException | ConditionTimeoutException e) {
            executorService.shutdownNow();
        }
    }

    @Override
    public void run() {
        executorService.submit(engine);
    }

    @Override
    public Signaler getSignaler() {
        return engine.getSignaler();
    }


    private Configuration createConfiguration(Map<String, String> externalConfiguration, Class<?> clazz) {
        if (externalConfiguration == null) {
            return Configuration.from(baseConfiguration).edit()
                    .with(EmbeddedEngineConfig.CONNECTOR_CLASS, clazz)
                    .build();
        }

        Map<String, String> mergedConfiguration = Stream.of(externalConfiguration, baseConfiguration)
                .flatMap(map -> map.entrySet().stream())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (v1, v2) -> v1));

        return Configuration.from(mergedConfiguration).edit()
                .with(EmbeddedEngineConfig.CONNECTOR_CLASS, clazz)
                .build();
    }


    @Override
    public boolean isRunning() {
        return junitEngineCallback.isRunning();
    }

    @Override
    public String getConfigurationValue(String key) {
        return configuration.getString(key);
    }

    private class JunitEngineCallback implements ConnectorCallback {

        @Override
        public void connectorStarted() {
            isEngineRunning.compareAndExchange(false, true);
        }

        @Override
        public void connectorStopped() {
            isEngineRunning.set(false);
        }

        public boolean isRunning() {
            return isEngineRunning.get();
        }
    }

    private static class ComposableCallbacks implements ConnectorCallback {
        private final List<ConnectorCallback> callbacks;

        private ComposableCallbacks(List<ConnectorCallback> callbacks) {
            this.callbacks = callbacks;
        }

        @Override
        public void connectorStarted() {
            callbacks.forEach(ConnectorCallback::connectorStarted);
        }

        @Override
        public void connectorStopped() {
            callbacks.forEach(ConnectorCallback::connectorStopped);
        }
    }
}
