package africa.flot.infrastructure.config;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import org.jboss.logging.Logger;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@ApplicationScoped
public class LogInitializer {
    private static final Logger LOG = Logger.getLogger(LogInitializer.class);

    void onStart(@Observes StartupEvent ev) {
        initializeLogDirectories();
    }

    private void initializeLogDirectories() {
        try {
            // Création de la structure de dossiers style Laravel
            Path storageDir = Paths.get("storage");
            Path logsDir = storageDir.resolve("logs");

            // Création des dossiers s'ils n'existent pas
            Files.createDirectories(logsDir);

            // Application des permissions (744 pour les fichiers, 755 pour les dossiers)
            logsDir.toFile().setExecutable(true, false);
            logsDir.toFile().setReadable(true, false);
            logsDir.toFile().setWritable(true, true);

            LOG.info("Dossiers de logs initialisés avec succès");

        } catch (Exception e) {
            LOG.error("Erreur lors de l'initialisation des dossiers de logs", e);
        }
    }
}