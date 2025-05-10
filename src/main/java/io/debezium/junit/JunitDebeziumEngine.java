package io.debezium.junit;

import org.apache.kafka.connect.source.SourceRecord;

import io.debezium.engine.DebeziumEngine;
import io.debezium.engine.RecordChangeEvent;

public interface JunitDebeziumEngine extends DebeziumEngine<RecordChangeEvent<SourceRecord>> {
    boolean isRunning();
}
