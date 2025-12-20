package net.bitbylogic.packetblocks.listener;

import lombok.RequiredArgsConstructor;
import net.bitbylogic.packetblocks.block.PacketBlock;
import net.bitbylogic.packetblocks.block.PacketBlockHolder;
import net.bitbylogic.packetblocks.block.PacketBlockManager;
import net.bitbylogic.packetblocks.event.PacketBlockInteractEvent;
import net.bitbylogic.packetblocks.group.PacketBlockGroup;
import net.bitbylogic.packetblocks.util.PacketBlockUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.util.RayTraceResult;

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
                .filter(PacketBlockHolder::isAddViewerOnJoin)
                .forEach(packetBlock -> {
                    if (packetBlock instanceof PacketBlock singleBlock) {
                        singleBlock.attemptAddViewer(player, false)
                                .ifPresent(pd -> states.add(((PacketBlock) packetBlock).getBlockState(player)));
                        return;
                    }

                    if (!(packetBlock instanceof PacketBlockGroup group)) {
                        return;
                    }

                    group.attemptAddViewer(player, false).ifPresent(pd -> states.addAll(group.getBlockStates(player)));
                });

        player.sendBlockChanges(states);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        manager.getBlocks(player.getWorld()).stream()
                .filter(PacketBlockHolder::isAddViewerOnJoin)
                .forEach(packetBlock -> packetBlock.removeViewer(player));
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        double interactRange = player.getAttribute(Attribute.BLOCK_INTERACTION_RANGE).getBaseValue();

        RayTraceResult result = PacketBlockUtil.rayTrace(event.getPlayer(), interactRange);

        if (result == null || result.getHitBlock() == null || manager.getBlock(result.getHitBlock().getLocation()).isEmpty()) {
            return;
        }

        Location location = result.getHitBlock().getLocation();

        PacketBlockInteractEvent interactEvent = new PacketBlockInteractEvent(player, event.getAction(), event.getHand(),
                result.getHitBlockFace(), manager.getBlock(location).get(), location);
        Bukkit.getPluginManager().callEvent(interactEvent);
    }

}
