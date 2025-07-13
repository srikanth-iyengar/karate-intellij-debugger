package in.srikanthk.devlabs.kchopdebugger.agent;

import com.fasterxml.jackson.core.type.TypeReference;
import java.nio.charset.StandardCharsets;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intuit.karate.Runner;
import in.srikanthk.devlabs.kchopdebugger.agent.communication.DebugClient;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.nio.file.Files;
import java.nio.file.Path;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.stream.Collectors;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final SessionState sessionState = SessionState.getInstance();

    public static void main(String[] args) {
        if (args.length < 4) {
            logger.error("Expected arguments: <featureClassPath> <projectBasePath> <breakpointsJson> <classpathUrls>");
            System.exit(1);
        }

        final String featureClassPath = args[0];
        final String projectBasePath = args[1];
        final String breakpointsJson = args[2];
        final String classpathUrls = args[3];

        try {
            List<URL> jars = parseClasspathUrls(classpathUrls);
            logger.info("Loading classpath JARs: {}", jars);

            ClassLoader customLoader = createClassLoader(jars);
            initializeSessionState(featureClassPath, projectBasePath, breakpointsJson);

            logger.info("Debug port: {}", System.getProperty("debug.port"));
            DebugClient client = new DebugClient("localhost", NumberUtils.toInt(System.getProperty("debug.port")));

            var th = new Thread(() -> {
               executeSuite(customLoader);
            });
            th.setContextClassLoader(customLoader);
            th.start();
            th.join();

            client.stop();
        } catch (Exception e) {
            logger.error("Execution failed", e);
            System.exit(2);
        }
    }

    private static List<URL> parseClasspathUrls(String cpUrls) {
        return Arrays.stream(cpUrls.split(";"))
                .map(File::new)
                .map(file -> {
                    try {
                        return file.toURI().toURL();
                    } catch (Exception e) {
                        throw new RuntimeException("Invalid classpath URL: " + file, e);
                    }
                })
                .collect(Collectors.toList());
    }

    private static void initializeSessionState(String featurePath, String basePath, String breakpointPath) throws IOException {
        sessionState.setFeatureClassPath(featurePath);
        sessionState.setProjectPath(basePath);

        String json = Files.readString(Path.of(breakpointPath), StandardCharsets.UTF_8);

        Map<String, Object> breakpoints = objectMapper.readValue(json, new TypeReference<>() {
        });
        breakpoints.forEach((key, value) -> {
            if (key instanceof String) {
                sessionState.getBreakpoints()
                        .computeIfAbsent(key, (k) -> new TreeSet<>())
                        .addAll((Collection<Integer>) value);
            }
        });
    }

    private static ClassLoader createClassLoader(List<URL> jars) {
        return new URLClassLoader(jars.toArray(new URL[0]), Thread.currentThread().getContextClassLoader());
    }

    private static void executeSuite(ClassLoader loader) {
        Runner.builder()
                .path("classpath:" + sessionState.getFeatureClassPath())
                .hook(new DebugHook())
                .backupReportDir(false)
                .classLoader(loader)
                .reportDir(new File(sessionState.getProjectPath(), "karate-report").getAbsolutePath())
                .parallel(1);
    }
}
