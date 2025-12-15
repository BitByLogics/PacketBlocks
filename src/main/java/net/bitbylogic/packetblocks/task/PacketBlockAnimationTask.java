package net.bitbylogic.packetblocks.task;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.potion.PotionTypes;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBlockBreakAnimation;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityEffect;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerRemoveEntityEffect;
import net.bitbylogic.packetblocks.block.PacketBlock;
import net.bitbylogic.packetblocks.structure.PacketBlockStructure;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public class PacketBlockAnimationTask extends BukkitRunnable {

    private final Map<Player, BlockAnimationContext> blockMap = new HashMap<>();
    private final Map<Player, StructureAnimationContext> structureMap = new HashMap<>();

    @Override
    public void run() {
        runBlockAnimations();
        runStructureAnimations();
    }

    private void runBlockAnimations() {
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

            if (packetBlock.isGlobalBreakAnimation()) {
                sendAnimationToAllViewers(packetBlock, stage);
            } else {
                sendAnimation(player, packetBlock, stage);
            }

            context.setStage(stage);
        }
    }

    private void runStructureAnimations() {
        for (Map.Entry<Player, StructureAnimationContext> entry : structureMap.entrySet()) {
            Player player = entry.getKey();
            StructureAnimationContext context = entry.getValue();
            PacketBlockStructure structure = context.getStructure();

            int breakSpeed = structure.getBreakSpeed(player);
            int ticksTaken = context.getTicksTaken();
            context.setTicksTaken(ticksTaken + 1);

            float progress = (float) context.getTicksTaken() / (float) breakSpeed;

            if (breakSpeed <= 0 || progress >= 1f) {
                sendFinishBreak(player, structure, context.getRelativePosition());
                continue;
            }

            int stage = (int) (progress * 10f);

            if (stage == context.getStage()) {
                continue;
            }

            if (structure.isGlobalBreakAnimation()) {
                sendAnimationToAllViewers(structure, context.getRelativePosition(), stage);
            } else {
                sendAnimation(player, structure, context.getRelativePosition(), stage);
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

        if (context != null) {
            sendFinishBreak(player, context.getBlock());
        }

        StructureAnimationContext structureContext = structureMap.remove(player);

        if (structureContext != null) {
            sendFinishBreak(player, structureContext.getStructure(), structureContext.getRelativePosition());
        }

        if (context != null || structureContext != null) {
            WrapperPlayServerRemoveEntityEffect removeEffect = new WrapperPlayServerRemoveEntityEffect(player.getEntityId(), PotionTypes.MINING_FATIGUE);
            PacketEvents.getAPI().getPlayerManager().sendPacket(player, removeEffect);
        }
    }

    public void addStructureEntry(Player player, PacketBlockStructure structure, Vector relativePosition) {
        WrapperPlayServerEntityEffect effectPacket = new WrapperPlayServerEntityEffect(player.getEntityId(), PotionTypes.MINING_FATIGUE,
                127, Integer.MAX_VALUE, (byte) 1);

        PacketEvents.getAPI().getPlayerManager().sendPacket(player, effectPacket);

        structureMap.put(player, new StructureAnimationContext(structure, relativePosition));
    }

    public void removeStructureEntry(Player player) {
        StructureAnimationContext context = structureMap.remove(player);

        if (context == null) return;

        sendFinishBreak(player, context.getStructure(), context.getRelativePosition());

        WrapperPlayServerRemoveEntityEffect removeEffect = new WrapperPlayServerRemoveEntityEffect(player.getEntityId(), PotionTypes.MINING_FATIGUE);
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, removeEffect);
    }

    private void sendAnimation(Player player, PacketBlock block, int stage) {
        Location blockLocation = block.getLocation();
        Vector3i position = new Vector3i(blockLocation.getBlockX(), blockLocation.getBlockY(), blockLocation.getBlockZ());

        WrapperPlayServerBlockBreakAnimation animation = new WrapperPlayServerBlockBreakAnimation(player.getEntityId(), position, (byte) stage);
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, animation);
    }

    private void sendAnimation(Player player, PacketBlockStructure structure, Vector relativePosition, int stage) {
        Location blockLocation = structure.getAbsoluteLocation(relativePosition);
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

    private void sendAnimationToAllViewers(PacketBlockStructure structure, Vector relativePosition, int stage) {
        Iterator<UUID> it = structure.getViewers().keySet().iterator();

        while (it.hasNext()) {
            Player viewer = Bukkit.getPlayer(it.next());
            if (viewer == null) {
                it.remove();
                continue;
            }

            sendAnimation(viewer, structure, relativePosition, stage);
        }
    }

    private void sendFinishBreak(Player player, PacketBlock block) {
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

    private void sendFinishBreak(Player player, PacketBlockStructure structure, Vector relativePosition) {
        sendAnimation(player, structure, relativePosition, -1);

        if (structure.isGlobalBreakAnimation()) {
            Iterator<UUID> it = structure.getViewers().keySet().iterator();

            while (it.hasNext()) {
                Player viewer = Bukkit.getPlayer(it.next());
                if (viewer == null) {
                    it.remove();
                    continue;
                }
                sendAnimation(viewer, structure, relativePosition, -1);
            }
        }
    }
}
