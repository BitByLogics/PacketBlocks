package net.bitbylogic.packetblocks.adapter;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;
import lombok.NonNull;
import net.bitbylogic.packetblocks.PacketBlocks;
import net.bitbylogic.packetblocks.data.PacketBlock;
import net.bitbylogic.packetblocks.event.PacketBlockBreakEvent;
import net.bitbylogic.packetblocks.event.PacketBlockStartBreakEvent;
import net.bitbylogic.packetblocks.manager.PacketBlockManager;
import net.bitbylogic.packetblocks.task.PacketBlockAnimationTask;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class BlockBreakAdapter implements PacketListener {

    private final PacketBlockManager manager;
    private final PacketBlockAnimationTask task;
    private final Set<UUID> cancelledBreaks;

    public BlockBreakAdapter(@NonNull PacketBlockManager manager) {
        this.manager = manager;
        this.task = new PacketBlockAnimationTask();
        this.task.runTaskTimerAsynchronously(manager.getPlugin(), 1, 1);
        this.cancelledBreaks = new HashSet<>();
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.PLAYER_DIGGING) return;

        Player player = (Player) event.getPlayer();
        WrapperPlayClientPlayerDigging packet = new WrapperPlayClientPlayerDigging(event);

        Vector3i position = packet.getBlockPosition();
        Location location = new Location(player.getWorld(), position.getX(), position.getY(), position.getZ());
        Optional<PacketBlock> optionalBlock = manager.getBlock(location);
        if (optionalBlock.isEmpty()) return;

        PacketBlock packetBlock = optionalBlock.get();
        if (!packetBlock.isViewer(player)) return;

        int breakSpeed = packetBlock.getBreakSpeed(player);
        float vanillaHardness = packetBlock.getBlockState(player).getType().getHardness();

        switch (packet.getAction()) {
            case START_DIGGING -> handleStartDestroy(player, packetBlock, location, packet, breakSpeed, vanillaHardness);
            case CANCELLED_DIGGING -> {
                if (breakSpeed != -1) task.removeEntry(player);
            }
            case FINISHED_DIGGING -> handleStopDestroy(player, packetBlock, location, packet);
        }
    }

    private void handleStartDestroy(@NonNull Player player,
                                    @NonNull PacketBlock packetBlock,
                                    @NonNull Location location,
                                    @NonNull WrapperPlayClientPlayerDigging packet,
                                    int breakSpeed,
                                    float vanillaHardness) {

        Bukkit.getScheduler().runTask(PacketBlocks.getInstance(), () -> {
            PacketBlockStartBreakEvent breakStartEvent = new PacketBlockStartBreakEvent(player, packetBlock, location);
            Bukkit.getPluginManager().callEvent(breakStartEvent);

            if (breakStartEvent.isCancelled()) {
                cancelledBreaks.add(player.getUniqueId());
                packetBlock.sendUpdate(player);
                PacketEvents.getAPI().getPlayerManager().receivePacket(player, packet);
                return;
            }

            if (player.getGameMode() == GameMode.CREATIVE || (breakSpeed == -1 && vanillaHardness == 0)) {
                handleStopDestroy(player, packetBlock, location, packet);
                return;
            }

            if (breakSpeed != -1) task.addEntry(player, packetBlock);
        });
    }

    private void handleStopDestroy(@NonNull Player player,
                                   @NonNull PacketBlock packetBlock,
                                   @NonNull Location location,
                                   @NonNull WrapperPlayClientPlayerDigging packet) {

        Bukkit.getScheduler().runTask(PacketBlocks.getInstance(), () -> {
            ItemStack heldItem = player.getInventory().getItemInMainHand();
            PacketBlockBreakEvent breakEvent = new PacketBlockBreakEvent(player, packetBlock, location, heldItem);
            Bukkit.getPluginManager().callEvent(breakEvent);

            if (breakEvent.isCancelled() || cancelledBreaks.contains(player.getUniqueId())) {
                cancelledBreaks.remove(player.getUniqueId());
                if (breakEvent.isUpdate()) packetBlock.sendUpdate(player);
                return;
            }

            if (breakEvent.isDropItems()) {
                packetBlock.getBlockState(player).getBlock()
                        .getDrops(player.getInventory().getItemInMainHand(), player)
                        .forEach(drop -> player.getWorld().dropItemNaturally(location, drop));
            }
        });
    }
}
