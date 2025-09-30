package net.bitbylogic.packetblocks.task;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.potion.PotionTypes;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBlockBreakAnimation;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityEffect;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerRemoveEntityEffect;
import net.bitbylogic.packetblocks.data.PacketBlock;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public class PacketBlockAnimationTask extends BukkitRunnable {

    private final Map<Player, BlockAnimationContext> blockMap = new HashMap<>();

    @Override
    public void run() {
        for (Map.Entry<Player, BlockAnimationContext> entry : blockMap.entrySet()) {
            Player player = entry.getKey();
            BlockAnimationContext context = entry.getValue();
            PacketBlock packetBlock = context.getBlock();

            int breakSpeed = packetBlock.getBreakSpeed(player);
            int ticksTaken = context.getTicksTaken();
            context.setTicksTaken(ticksTaken + 1);

            float progress = (float) context.getTicksTaken() / (float) breakSpeed;

            if (breakSpeed <= 0 || progress >= 1f) {
                sendFinishBreak(player, packetBlock);
                continue;
            }

            int stage = (int) (progress * 10f);

            if (stage == context.getStage()) {
                continue;
            }

            // Send block break animation
            if (packetBlock.isGlobalBreakAnimation()) {
                sendAnimationToAllViewers(packetBlock, stage);
            } else {
                sendAnimation(player, packetBlock, stage);
            }

            context.setStage(stage);
        }
    }

    public void addEntry(Player player, PacketBlock block) {
        // Apply Mining Fatigue effect via PacketEvents
        WrapperPlayServerEntityEffect effectPacket = new WrapperPlayServerEntityEffect(player.getEntityId(), PotionTypes.MINING_FATIGUE,
                127, Integer.MAX_VALUE, (byte) 1);

        PacketEvents.getAPI().getPlayerManager().sendPacket(player, effectPacket);

        blockMap.put(player, new BlockAnimationContext(block));
    }

    public void removeEntry(Player player) {
        BlockAnimationContext context = blockMap.remove(player);

        if (context == null) return;

        PacketBlock block = context.getBlock();

        // Remove break animation
        sendFinishBreak(player, block);

        // Remove Mining Fatigue
        WrapperPlayServerRemoveEntityEffect removeEffect = new WrapperPlayServerRemoveEntityEffect(player.getEntityId(), PotionTypes.MINING_FATIGUE);

        PacketEvents.getAPI().getPlayerManager().sendPacket(player, removeEffect);
    }

    private void sendAnimation(Player player, PacketBlock block, int stage) {
        Location blockLocation = block.getLocation();
        Vector3i position = new Vector3i(blockLocation.getBlockX(), blockLocation.getBlockY(), blockLocation.getBlockZ());

        WrapperPlayServerBlockBreakAnimation animation = new WrapperPlayServerBlockBreakAnimation(player.getEntityId(), position, (byte) stage);
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, animation);
    }

    private void sendAnimationToAllViewers(PacketBlock block, int stage) {
        Iterator<UUID> it = block.getViewers().keySet().iterator();

        while (it.hasNext()) {
            Player viewer = Bukkit.getPlayer(it.next());
            if (viewer == null) {
                it.remove();
                continue;
            }

            sendAnimation(viewer, block, stage);
        }
    }

    private void sendFinishBreak(Player player, PacketBlock block) {
        // Send break animation stage -1 to indicate finished
        sendAnimation(player, block, -1);

        if (block.isGlobalBreakAnimation()) {
            Iterator<UUID> it = block.getViewers().keySet().iterator();

            while (it.hasNext()) {
                Player viewer = Bukkit.getPlayer(it.next());
                if (viewer == null) {
                    it.remove();
                    continue;
                }
                sendAnimation(viewer, block, -1);
            }
        }
    }
}
