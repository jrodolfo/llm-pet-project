package net.jrodolfo.llm.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;

@ConfigurationProperties(prefix = "app.storage")
public record AppStorageProperties(
        String sessionsDirectory,
        String reportsDirectory
) {
    public Path resolvedSessionsDirectory() {
        return resolveAgainstProjectRoot(sessionsDirectory);
    }

    public Path resolvedReportsDirectory() {
        return resolveAgainstProjectRoot(reportsDirectory);
    }

    private Path resolveAgainstProjectRoot(String configuredPath) {
        Path candidate = Path.of(configuredPath);
        if (candidate.isAbsolute()) {
            return candidate.toAbsolutePath().normalize();
        }
        return findProjectRoot().resolve(candidate).normalize();
    }

    private Path findProjectRoot() {
        Path current = Path.of(System.getProperty("user.dir", ".")).toAbsolutePath().normalize();
        while (current != null) {
            if (looksLikeProjectRoot(current)) {
                return current;
            }
            current = current.getParent();
        }
        return Path.of(System.getProperty("user.dir", ".")).toAbsolutePath().normalize();
    }

    private boolean looksLikeProjectRoot(Path candidate) {
        return candidate.resolve("backend/pom.xml").toFile().isFile()
                && candidate.resolve("frontend/package.json").toFile().isFile()
                && candidate.resolve("README.md").toFile().isFile();
    }
}
