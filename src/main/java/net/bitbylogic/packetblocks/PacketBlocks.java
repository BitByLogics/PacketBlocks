package net.bitbylogic.packetblocks;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.EventManager;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.tcoded.folialib.FoliaLib;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import lombok.Getter;
import net.bitbylogic.packetblocks.adapter.BlockBreakAdapter;
import net.bitbylogic.packetblocks.adapter.BlockPlaceAdapter;
import net.bitbylogic.packetblocks.adapter.BlockUpdateAdapter;
import net.bitbylogic.packetblocks.adapter.ChunkLoadAdapter;
import net.bitbylogic.packetblocks.block.PacketBlockManager;
import net.bitbylogic.packetblocks.listener.PacketBlockListener;
import net.bitbylogic.packetblocks.util.BoundingBoxes;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public final class PacketBlocks extends JavaPlugin {

    @Getter
    private static PacketBlocks instance;

    private PacketBlockManager blockManager;
    private FoliaLib foliaLib;

    @Override
    public void onLoad() {
        this.foliaLib = new FoliaLib(this);

        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this));
        PacketEvents.getAPI().load();
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        instance = this;

        PacketEvents.getAPI().init();
        BoundingBoxes.init(this);

        this.blockManager = new PacketBlockManager(this);

        foliaLib.getScheduler().runAsync(task -> {
            EventManager eventManager = PacketEvents.getAPI().getEventManager();

            eventManager.registerListener(new ChunkLoadAdapter(blockManager), PacketListenerPriority.LOWEST);
            eventManager.registerListener(new BlockPlaceAdapter(blockManager), PacketListenerPriority.LOWEST);
            eventManager.registerListener(new BlockBreakAdapter(this), PacketListenerPriority.LOWEST);
            eventManager.registerListener(new BlockUpdateAdapter(blockManager), PacketListenerPriority.LOWEST);
        });

        getServer().getPluginManager().registerEvents(new PacketBlockListener(blockManager), this);
    }

    @Override
    public void onDisable() {
        PacketEvents.getAPI().terminate();
    }

}
