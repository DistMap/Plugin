package dmvmc.distMap;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public final class DistMap extends JavaPlugin {

    private static final String API_SERVER = "http://localhost:3000";
    private static final String API_KEY = "42";

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(new ChunkLoadListener(this), this);
        getLogger().info("DistMap has been enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("DistMap has been disabled!");
    }

    public boolean sendChunkData(Chunk chunk) {

        // Get chunk location
        int regionX = chunk.getX() >> 5;
        int regionZ = chunk.getZ() >> 5;

        // Locate chunk file
        File worldFolder = Bukkit.getWorlds().getFirst().getWorldFolder();
        File regionFile = new File(worldFolder, "region/r." + regionX + "." + regionZ + ".mca");

        if (!regionFile.exists()) {
            getLogger().warning("Region file not found: " + "r." + regionX + "." + regionZ + ".mca");
            return false;
        }

        // Create http connection to the API server
        HttpURLConnection connection;
        String boundary = "----DistMapFile";

        try {

            // Build connection and set properties
            connection = (HttpURLConnection) new URI(API_SERVER).toURL().openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("API-Key", API_KEY);
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

        } catch (URISyntaxException | MalformedURLException | ProtocolException e) {
            getLogger().info("Invalid API URL!");
            return false;
        }
        catch (IOException e) {
            getLogger().info("Error opening connection to server");
            return false;
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
            return false;
        }

        // Return response observation
        try {
            int responseCode = connection.getResponseCode();
            return responseCode == 200;
        } catch (IOException e) {
            getLogger().info("Error connecting to server!");
            return false;
        }

    }

}
