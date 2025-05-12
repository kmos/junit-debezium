# junit-debezium

Library that simplify the testing for Debezium Connectors.


## Installation

add the dependency in your `pom.xml`:

```xml
        <dependency>
            <groupId>io.debezium</groupId>
            <artifactId>junit-debezium</artifactId>
        </dependency>
```

## Introduction

In debezium there are two kind of test (both [integration](https://martinfowler.com/articles/practical-test-pyramid.html)):

- `DebeziumIntegrationTest`: tests against `source -> connector -> debezium engine`
- `KafkaConnectIntegrationTest`: tests agains `source -> connector-> kafka connect`

These tests exists because Debezium Connectors can work in `Debezium Server` or in `Kafka Connect`.

## Usage

here an example of `KafkaConnectIntegrationTest`:

```java
@DebeziumIntegrationTest(value = PostgresConnector.class, resources = { PostgresResource.class },
        configuration =  {
            @DebeziumConfiguration(name = "aKey", value = "aValue"),
            @DebeziumConfiguration(name = "anotherKey", value = "anotherValue")
        },
        connectorCallback = ExampleCallback.class)
public class DebeziumConnectorTest {

    @Test
    @DisplayName("should debezium engine running with additional configuration")
    public void shouldEngineRunningWithAdditionalConfiguration(JunitDebeziumEngine engine) {
        assertThat(engine.getConfigurationValue("aKey")).isEqualTo("aValue");
        assertThat(engine.getConfigurationValue("anotherKey")).isEqualTo("anotherValue");

        assertThat(engine.isRunning()).isTrue();
    }
}
```

in the `@DebeziumIntegrationTest` you define:

- the connector (in the example `PostgresConnector.class`)
- additional configuration for the correct work of `debezium engine`
- the resources necessary to execute the correct test (usually the source in which the connector is capturing events)

a Debezium Resource should implements `DebeziumTestResourceLifecycle` as follow:

```java
public class PostgresResource implements DebeziumTestResourceLifecycleManager {
    private static final String POSTGRES_IMAGE = "quay.io/debezium/postgres:15";

    private static final DockerImageName POSTGRES_DOCKER_IMAGE_NAME = DockerImageName.parse(POSTGRES_IMAGE)
            .asCompatibleSubstituteFor("postgres");

    private static final PostgreSQLContainer<?> postgresContainer = new PostgreSQLContainer<>(POSTGRES_DOCKER_IMAGE_NAME)
            .waitingFor(Wait.forLogMessage(".*database system is ready to accept connections.*", 2))
            .withUsername("postgres")
            .withPassword("postgres")
            .withDatabaseName("postgres")
            .withEnv("POSTGRES_INITDB_ARGS", "-E UTF8")
            .withEnv("LANG", "en_US.utf8")
            .withStartupTimeout(Duration.ofSeconds(30));

    @Override
    public Map<String, String> start() {
        try {
            postgresContainer.start();

            return Map.of(
                    "database.hostname", postgresContainer.getHost(),
                    "database.user", postgresContainer.getUsername(),
                    "database.password", postgresContainer.getPassword(),
                    "database.dbname", postgresContainer.getDatabaseName(),
                    "database.port", postgresContainer.getMappedPort(5432).toString());
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void stop() {
        try {
            if (postgresContainer != null) {
                postgresContainer.stop();
            }
        }
        catch (Exception ignore) {
        }
    }
}
```

It's possible to inject also callbacks for testing purpose.

`KafkaConnectIntegrationTest` works in the same way with same parameters.