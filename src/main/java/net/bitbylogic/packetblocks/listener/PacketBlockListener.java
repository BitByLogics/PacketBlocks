package net.bitbylogic.packetblocks.listener;

import lombok.RequiredArgsConstructor;
import net.bitbylogic.packetblocks.block.PacketBlock;
import net.bitbylogic.packetblocks.block.PacketBlockManager;
import net.bitbylogic.packetblocks.event.PacketBlockInteractEvent;
import net.bitbylogic.packetblocks.event.PacketStructureInteractEvent;
import net.bitbylogic.packetblocks.structure.PacketBlockStructure;
import net.bitbylogic.packetblocks.util.PacketBlockUtil;
import net.bitbylogic.utils.Pair;
import org.bukkit.Bukkit;
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
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Optional;
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

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        double interactRange = player.getAttribute(Attribute.BLOCK_INTERACTION_RANGE).getBaseValue();

        RayTraceResult result = PacketBlockUtil.rayTrace(event.getPlayer(), interactRange);

        if (result == null || result.getHitBlock() == null) {
            return;
        }

        Optional<PacketBlock> packetBlockOpt = manager.getBlock(result.getHitBlock().getLocation());
        if (packetBlockOpt.isPresent()) {
            PacketBlockInteractEvent interactEvent = new PacketBlockInteractEvent(player, event.getAction(), event.getHand(),
                    result.getHitBlockFace(), packetBlockOpt.get());
            Bukkit.getPluginManager().callEvent(interactEvent);
            return;
        }

        Optional<Pair<PacketBlockStructure, Vector>> structureBlockOpt = manager.getStructureBlockAt(result.getHitBlock().getLocation());
        if (structureBlockOpt.isPresent()) {
            Pair<PacketBlockStructure, Vector> pair = structureBlockOpt.get();
            PacketBlockStructure structure = pair.getKey();
            Vector relativePos = pair.getValue();

            if (!structure.isViewer(player)) {
                return;
            }

            PacketStructureInteractEvent interactEvent = new PacketStructureInteractEvent(
                    player, structure, relativePos, event.getAction(), event.getHand(), result.getHitBlockFace()
            );
            Bukkit.getPluginManager().callEvent(interactEvent);
        }
    }

}
