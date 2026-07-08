package com.rama.mudstock.service;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class MysqlDumpService {

    private static final DateTimeFormatter FILE_TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    @Value("${spring.datasource.url}")
    private String datasourceUrl;

    @Value("${spring.datasource.username}")
    private String datasourceUsername;

    @Value("${spring.datasource.password:}")
    private String datasourcePassword;

    public Path dumpToLocation(String outputLocation) throws IOException, InterruptedException {
        if (outputLocation == null || outputLocation.isBlank()) {
            throw new IllegalArgumentException("DailyMysqlDBDump location is blank");
        }

        MysqlConnectionInfo connectionInfo = parseDatasourceUrl(datasourceUrl);
        Path outputDirectory = Path.of(outputLocation.trim());
        Files.createDirectories(outputDirectory);

        String fileName = connectionInfo.databaseName() + "-" + FILE_TIMESTAMP.format(LocalDateTime.now()) + ".sql";
        Path outputFile = outputDirectory.resolve(fileName);

        List<String> command = new ArrayList<>();
        command.add("mysqldump");
        command.add("--no-defaults");
        command.add("--host=" + connectionInfo.host());
        command.add("--port=" + connectionInfo.port());
        command.add("--user=" + datasourceUsername);
        command.add("--single-transaction");
        command.add("--skip-lock-tables");
        command.add(connectionInfo.databaseName());

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectOutput(outputFile.toFile());
        if (datasourcePassword != null && !datasourcePassword.isBlank()) {
            processBuilder.environment().put("MYSQL_PWD", datasourcePassword);
        }

        Process process = processBuilder.start();
        int exitCode = process.waitFor();
        String stderr = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8).trim();

        if (exitCode != 0) {
            Files.deleteIfExists(outputFile);
            throw new IOException("mysqldump failed with exit code " + exitCode + (stderr.isBlank() ? "" : ": " + stderr));
        }

        return outputFile;
    }

    private MysqlConnectionInfo parseDatasourceUrl(String jdbcUrl) {
        if (jdbcUrl == null || jdbcUrl.isBlank() || !jdbcUrl.startsWith("jdbc:")) {
            throw new IllegalArgumentException("Unsupported datasource url: " + jdbcUrl);
        }

        URI uri = URI.create(jdbcUrl.substring("jdbc:".length()));
        String host = uri.getHost();
        int port = uri.getPort() > 0 ? uri.getPort() : 3306;
        String path = uri.getPath();
        String databaseName = path == null ? "" : path.replaceFirst("^/", "");

        if (host == null || host.isBlank() || databaseName.isBlank()) {
            throw new IllegalArgumentException("Could not parse datasource host/database from url: " + jdbcUrl);
        }

        return new MysqlConnectionInfo(host, port, databaseName);
    }

    private record MysqlConnectionInfo(String host, int port, String databaseName) {
    }
}