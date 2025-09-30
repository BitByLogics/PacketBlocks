package net.bitbylogic.packetblocks;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import lombok.Getter;
import net.bitbylogic.packetblocks.adapter.BlockBreakAdapter;
import net.bitbylogic.packetblocks.adapter.BlockPlaceAdapter;
import net.bitbylogic.packetblocks.adapter.BlockUpdateAdapter;
import net.bitbylogic.packetblocks.adapter.ChunkLoadAdapter;
import net.bitbylogic.packetblocks.listener.PacketBlockListener;
import net.bitbylogic.packetblocks.manager.PacketBlockManager;
import net.bitbylogic.packetblocks.util.BoundingBoxes;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public final class PacketBlocks extends JavaPlugin {

    @Getter
    private static PacketBlocks instance;

    private PacketBlockManager blockManager;

    @Override
    public void onLoad() {
        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this));
        PacketEvents.getAPI().load();
    }

    @Override
    public void onEnable() {
        instance = this;

        if (!Bukkit.getServer().getAllowFlight()) {
            getLogger().warning("=====================================================");
            getLogger().warning("                  !!! WARNING !!!                    ");
            getLogger().warning("                                                     ");
            getLogger().warning("   'allow-flight' is DISABLED in server.properties!  ");
            getLogger().warning("                                                     ");
            getLogger().warning("   Players WILL BE KICKED FOR FLYING when standing   ");
            getLogger().warning("   on packet/fake blocks created by this plugin.     ");
            getLogger().warning("                                                     ");
            getLogger().warning("   ACTION REQUIRED:                                  ");
            getLogger().warning("   1) Set 'allow-flight: true' in spigot.yml         ");
            getLogger().warning("   2) RESTART THE SERVER to apply the change.        ");
            getLogger().warning("                                                     ");
            getLogger().warning("   NOTE: changes to server.properties are only read  ");
            getLogger().warning("   at server startup! A restart is required for this ");
            getLogger().warning("   setting to take effect.                           ");
            getLogger().warning("                                                     ");
            getLogger().warning("=====================================================");

            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        PacketEvents.getAPI().init();
        BoundingBoxes.init(this);

        this.blockManager = new PacketBlockManager(this);

        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            PacketEvents.getAPI().getEventManager().registerListener(new ChunkLoadAdapter(blockManager), PacketListenerPriority.LOWEST);
            PacketEvents.getAPI().getEventManager().registerListener(new BlockPlaceAdapter(blockManager), PacketListenerPriority.LOWEST);
            PacketEvents.getAPI().getEventManager().registerListener(new BlockBreakAdapter(blockManager), PacketListenerPriority.LOWEST);
            PacketEvents.getAPI().getEventManager().registerListener(new BlockUpdateAdapter(blockManager), PacketListenerPriority.LOWEST);
        });

        getServer().getPluginManager().registerEvents(new PacketBlockListener(blockManager), this);
    }

    @Override
    public void onDisable() {
        PacketEvents.getAPI().terminate();
    }

}
