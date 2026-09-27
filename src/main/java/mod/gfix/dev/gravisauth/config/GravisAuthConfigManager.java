package mod.gfix.dev.gravisauth.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public class GravisAuthConfigManager {

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    private static final Path CONFIG_PATH =
            Path.of("config", "gravisauth.json");

    public static GravisAuthConfig load() {

        try {
            Files.createDirectories(CONFIG_PATH.getParent());

            if (!Files.exists(CONFIG_PATH)) {
                GravisAuthConfig config = new GravisAuthConfig();

                try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                    GSON.toJson(config, writer);
                }

                System.out.println("[GravisAuth] Created config: " + CONFIG_PATH);

                return config;
            }

            try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
                return GSON.fromJson(reader, GravisAuthConfig.class);
            }

        } catch (IOException e) {
            throw new RuntimeException(
                    "Failed to load GravisAuth configuration",
                    e
            );
        }
    }
}