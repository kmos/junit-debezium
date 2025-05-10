/*
 *  Copyright Debezium Authors.
 *
 *  Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.debezium.junit;

import io.debezium.engine.DebeziumEngine;

public class NoOpConnectorCallback implements DebeziumEngine.ConnectorCallback {
    @Override
    public void connectorStarted() {
        // ignore
    }

    @Override
    public void connectorStopped() {
        // ignore
    }

    @Override
    public void taskStarted() {
        // ignore
    }

    @Override
    public void taskStopped() {
        // ignore
    }
}
