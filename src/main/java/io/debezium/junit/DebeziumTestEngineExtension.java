package io.debezium.junit;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.TestInstancePostProcessor;

public class DebeziumTestEngineExtension implements BeforeEachCallback, AfterEachCallback, ExecutionCondition, TestInstancePostProcessor, ParameterResolver {

    private JunitDebeziumEngine engine;
    private List<DebeziumTestResourceLifecycleManager> resources;

    @Override
    public void beforeEach(ExtensionContext context) {
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
                .flatMap(a -> getEngineProcess(a, mergedConfiguration))
                .orElseThrow();

        engine.run();
    }

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

    private Optional<JunitDebeziumEngine> getEngineProcess(Class<?> testClass, Map<String, String> configuration) {
        DebeziumIntegrationTest annotation = testClass.getAnnotation(DebeziumIntegrationTest.class);

        if (annotation == null) {
            return Optional.empty();
        }

        return Optional.of(new DefaultJunitDebeziumEngine(annotation.value(), configuration));

    }

    @Override
    public void postProcessTestInstance(Object testInstance, ExtensionContext context) throws Exception {
    }

    @Override
    public void afterEach(ExtensionContext context) throws IOException {
        engine.close();
        resources.forEach(DebeziumTestResourceLifecycleManager::stop);
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        return parameterContext.getParameter().getType().equals(JunitDebeziumEngine.class);
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        return engine;
    }
}
