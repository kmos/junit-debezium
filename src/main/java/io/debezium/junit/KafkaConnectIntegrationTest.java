/*
 *  Copyright Debezium Authors.
 *
 *  Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.debezium.junit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;

@Target(ElementType.TYPE)
@ExtendWith({ KafkaConnectTestExtension.class })
@Retention(RetentionPolicy.RUNTIME)
public @interface KafkaConnectIntegrationTest {
}
