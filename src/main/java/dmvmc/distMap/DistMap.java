package dmvmc.distMap;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.scheduler.BukkitRunnable;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

public final class DistMap extends JavaPlugin {

    private static String API_ENDPOINT;
    private static String API_KEY;
    private static Long UPDATE_FREQUENCY;
    private final Map<Integer, Set<Integer>> sendQueue = new ConcurrentHashMap<>();

    @Override
    public void onEnable() {

        saveDefaultConfig();
        loadNewConfig();

        getCommand("updateconfig").setExecutor(new UpdateConfigCommand(this));
        getCommand("updateconfig").setTabCompleter(new UpdateConfigCommandTabCompleter(this));
        getServer().getPluginManager().registerEvents(new ChunkLoadListener(this), this);
        manageSendQueue();
        getLogger().info("DistMap has been enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("DistMap has been disabled!");
    }

    public void loadNewConfig() {

        reloadConfig();
        FileConfiguration config = getConfig();

        API_ENDPOINT = config.getString("API_ENDPOINT");
        API_KEY = config.getString("API_KEY");
        UPDATE_FREQUENCY = config.getInt("UPDATE_FREQUENCY") * 20L;

        getLogger().info("SEND EVERY " + UPDATE_FREQUENCY + "ms");

    }

    public void queueSend(int x, int z) {
        sendQueue.computeIfAbsent(x, k -> new CopyOnWriteArraySet<>()).add(z);
    }

    private void sendMCA(int x, int z) {

        // Locate chunk file
        File worldFolder = Bukkit.getWorlds().getFirst().getWorldFolder();
        File regionFile = new File(worldFolder, "region/r." + x + "." + z + ".mca");

        if (!regionFile.exists()) {
            getLogger().warning("Region file not found: " + "r." + x + "." + z + ".mca");
            return;
        }

        // Create http connection to the API server
        HttpURLConnection connection;
        String boundary = "----DistMapFile";

        try {

            // Build connection and set properties
            connection = (HttpURLConnection) new URI(API_ENDPOINT).toURL().openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("API-Key", API_KEY);
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

        } catch (URISyntaxException | MalformedURLException | ProtocolException e) {
            getLogger().info("Invalid API URL!");
            return;
        } catch (IOException e) {
            getLogger().info("Error opening connection to server");
            return;
        }

        // Write file to http connection
        try (OutputStream regionFileOutputStream = connection.getOutputStream()) {
            PrintWriter writer = new PrintWriter(new OutputStreamWriter(regionFileOutputStream, StandardCharsets.UTF_8), true);

            // Add form field
            writer.append("--").append(boundary).append("\r\n");
            writer.append("Content-Disposition: form-data; name=\"file\"; filename=\"").append(regionFile.getName()).append("\"\r\n");
            writer.append("Content-Type: application/octet-stream\r\n\r\n");
            writer.flush();

            // Write file data to request
            Files.copy(regionFile.toPath(), regionFileOutputStream);
            regionFileOutputStream.flush();

            // Finish request
            writer.append("\r\n--").append(boundary).append("--\r\n");
            writer.flush();

        } catch (IOException e) {
            getLogger().info("Error sending region file!");
            return;
        }

        // Return response observation
        try {
            int responseCode = connection.getResponseCode();
            if (responseCode != 200)
                getLogger().info("DistMap error response code: " + responseCode);
        } catch (IOException e) {
            getLogger().info("Error connecting to server!");
        }

    }

    private void manageSendQueue() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (sendQueue.isEmpty())
                    return;

                sendQueue.forEach((x, k) -> k.forEach(z -> sendMCA(x, z)));
                sendQueue.clear();

            }
        }.runTaskTimer(this, UPDATE_FREQUENCY, UPDATE_FREQUENCY);
    }

}
