package net.bitbylogic.packetblocks.listener;

import lombok.RequiredArgsConstructor;
import net.bitbylogic.packetblocks.manager.PacketBlockManager;
import net.bitbylogic.packetblocks.data.PacketBlock;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashSet;
import java.util.Set;

@RequiredArgsConstructor
public class PacketBlockListener implements Listener {

    private final PacketBlockManager manager;

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockBreak(BlockBreakEvent event) {
        manager.getBlock(event.getBlock().getLocation()).ifPresent(packetBlock -> {
            if(!packetBlock.isViewer(event.getPlayer())) {
                return;
            }

            event.setCancelled(true);
        });
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Set<BlockState> states = new HashSet<>();

        manager.getBlocks(player.getWorld()).stream()
                .filter(PacketBlock::isAddViewerOnJoin)
                .forEach(packetBlock -> {
                    packetBlock.attemptAddViewer(player, false)
                            .ifPresent(pd -> states.add(packetBlock.getBlockState(player)));
                });

        player.sendBlockChanges(states);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        manager.getBlocks(player.getWorld()).stream()
                .filter(PacketBlock::isAddViewerOnJoin)
                .forEach(packetBlock -> packetBlock.removeViewer(player));
    }

}
