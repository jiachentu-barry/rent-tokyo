package com.jiachentu.rent_tokyo.service;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PropertyImportScheduler {

    private final Environment environment;

    @Scheduled(cron = "${rent-tokyo.import.cron:0 0 3 * * *}")
    public void runDailyImport() {
        if (!Boolean.parseBoolean(environment.getProperty("rent-tokyo.import.enabled", "true"))) {
            log.info("Daily property import is disabled.");
            return;
        }

        List<String> command = new ArrayList<>();
        command.addAll(splitCommand(environment.getProperty("rent-tokyo.import.python-command", "py")));
        command.add(environment.getProperty("rent-tokyo.import.script-path", "scripts/suumo_to_properties.py"));
        command.addAll(splitCommand(environment.getProperty("rent-tokyo.import.args", "--all-wards --pages 1")));

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.directory(new File(System.getProperty("user.dir")));
        processBuilder.redirectErrorStream(true);

        String datasourceUrl = environment.getProperty("spring.datasource.url", "");
        String datasourceUsername = environment.getProperty("spring.datasource.username", "");
        String datasourcePassword = environment.getProperty("spring.datasource.password", "");

        processBuilder.environment().putIfAbsent("DB_URL", datasourceUrl);
        processBuilder.environment().putIfAbsent("DB_USERNAME", datasourceUsername);
        processBuilder.environment().putIfAbsent("DB_PASSWORD", datasourcePassword);

        try {
            log.info("Starting scheduled property import: {}", String.join(" ", command));
            Process process = processBuilder.start();
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                log.info("Scheduled property import completed successfully. Output: {}", output.trim());
            } else {
                log.error("Scheduled property import failed with exitCode={}. Output: {}", exitCode, output.trim());
            }
        } catch (Exception ex) {
            log.error("Scheduled property import failed to start", ex);
        }
    }

    private List<String> splitCommand(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }

        return Arrays.stream(value.trim().split("\\s+"))
                .filter(part -> !part.isBlank())
                .toList();
    }
}
