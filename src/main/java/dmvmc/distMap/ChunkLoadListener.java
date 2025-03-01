package dmvmc.distMap;

import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.Chunk;

public class ChunkLoadListener implements Listener {

    private final DistMap plugin;
    public ChunkLoadListener(DistMap plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        Chunk chunk = event.getChunk();
        int fileX = chunk.getX() >> 5;
        int fileZ = chunk.getZ() >> 5;
        plugin.queueSend(fileX, fileZ);
    }

}
