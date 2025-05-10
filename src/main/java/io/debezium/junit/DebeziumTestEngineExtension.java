package io.debezium.junit;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.TestInstancePostProcessor;

import io.debezium.engine.DebeziumEngine.ConnectorCallback;

public class DebeziumTestEngineExtension implements BeforeAllCallback, AfterAllCallback, ExecutionCondition, TestInstancePostProcessor, ParameterResolver {

    private JunitDebeziumEngine engine;
    private List<DebeziumTestResourceLifecycleManager> resources;

    private Map<String, String> getAdditionalConfiguration(Class<?> testClass) {
        DebeziumIntegrationTest annotation = testClass.getAnnotation(DebeziumIntegrationTest.class);

        if (annotation == null) {
            return Collections.emptyMap();
        }

        return Arrays.stream(annotation.configuration())
                .map(a -> Map.entry(a.name(), a.value()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (v1, v2) -> v2));
    }

    private List<DebeziumTestResourceLifecycleManager> getResources(Class<?> testClass) {
        DebeziumIntegrationTest annotation = testClass.getAnnotation(DebeziumIntegrationTest.class);

        if (annotation == null) {
            return Collections.emptyList();
        }

        return Arrays.stream(annotation.resources())
                .sequential()
                .map(clazz -> {
                    try {
                        return (DebeziumTestResourceLifecycleManager) clazz.getDeclaredConstructor().newInstance();
                    } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                             NoSuchMethodException e) {
                        throw new RuntimeException(e);
                    }
                })
                .toList();

    }

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        return context.getTestClass()
                .map(testClass -> testClass.getAnnotation(DebeziumIntegrationTest.class))
                .map(clazz -> ConditionEvaluationResult.enabled("Test enabled"))
                .orElse(ConditionEvaluationResult.disabled("SourceConnector not found"));
    }

    private Optional<JunitDebeziumEngine> getEngineProcess(Class<?> testClass, Map<String, String> configuration) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        DebeziumIntegrationTest annotation = testClass.getAnnotation(DebeziumIntegrationTest.class);

        if (annotation == null) {
            return Optional.empty();
        }

        ConnectorCallback connectorCallback = annotation.connectorCallback()
                .getDeclaredConstructor()
                .newInstance();

        return Optional.of(new DefaultJunitDebeziumEngine(annotation.value(), configuration, connectorCallback));

    }

    @Override
    public void postProcessTestInstance(Object testInstance, ExtensionContext context) throws Exception {
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        return parameterContext.getParameter().getType().equals(JunitDebeziumEngine.class);
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() -> engine.isRunning());

        return engine;
    }

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        Map<String, String> additionalConfiguration = context.getTestClass()
                .map(this::getAdditionalConfiguration)
                .orElse(Collections.emptyMap());

        resources = context.getTestClass()
                .map(this::getResources)
                .stream()
                .flatMap(Collection::stream)
                .toList();

        Map<String, String> configuration = resources
                .stream()
                .map(DebeziumTestResourceLifecycleManager::start)
                .flatMap(configurations -> configurations.entrySet().stream())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (v1, v2) -> v2));

        Map<String, String> mergedConfiguration = Stream.of(additionalConfiguration, configuration)
                .flatMap(map -> map.entrySet().stream())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (v1, v2) -> v1));

        engine = context.getTestClass()
                .flatMap(clazz -> {
                    try {
                        return getEngineProcess(clazz, mergedConfiguration);
                    } catch (NoSuchMethodException | InvocationTargetException | InstantiationException |
                             IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                })
                .orElseThrow();

        engine.run();
    }

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        engine.close();
        resources.forEach(DebeziumTestResourceLifecycleManager::stop);
    }
}
