package io.debezium.junit;

import java.util.Map;

public interface DebeziumTestResourceLifecycleManager {

    Map<String, String> start();

    void stop();
}
