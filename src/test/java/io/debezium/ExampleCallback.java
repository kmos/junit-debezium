/*
 *  Copyright Debezium Authors.
 *
 *  Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.debezium;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.debezium.engine.DebeziumEngine.ConnectorCallback;

public class ExampleCallback implements ConnectorCallback {
    private static final Logger logger = LoggerFactory.getLogger(ExampleCallback.class);

    @Override
    public void connectorStarted() {
        logger.info("Example Callback invoked");
    }

}
