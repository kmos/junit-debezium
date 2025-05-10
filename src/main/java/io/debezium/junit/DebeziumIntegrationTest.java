package io.debezium.junit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.apache.kafka.connect.source.SourceConnector;
import org.junit.jupiter.api.extension.ExtendWith;

@Target(ElementType.TYPE)
@ExtendWith({ DebeziumTestEngineExtension.class })
@Retention(RetentionPolicy.RUNTIME)
public @interface DebeziumIntegrationTest {
    Class<? extends SourceConnector> value();

    Class<? extends DebeziumTestResourceLifecycleManager>[] resources() default {};

    DebeziumConfiguration[] configuration() default {};
}
