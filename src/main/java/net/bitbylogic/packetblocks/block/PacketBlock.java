package net.bitbylogic.packetblocks.block;

import lombok.Getter;
import lombok.NonNull;
import net.bitbylogic.packetblocks.PacketBlocks;
import net.bitbylogic.packetblocks.data.DataHandler;
import net.bitbylogic.packetblocks.event.PacketBlockBreakEvent;
import net.bitbylogic.packetblocks.metadata.MetadataHandler;
import net.bitbylogic.packetblocks.util.BoundingBoxes;
import net.bitbylogic.packetblocks.viewer.ViewerHandler;
import net.bitbylogic.packetblocks.viewer.impl.SinglePacketBlockViewer;
import net.bitbylogic.utils.location.ChunkPosition;
import net.bitbylogic.utils.location.WorldPosition;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@Getter
public class PacketBlock implements PacketBlockHolder<BlockData, SinglePacketBlockViewer> {

    private final WorldPosition position;
    private final ChunkPosition chunk;

    private final Location location;

    private final List<BoundingBox> boundingBoxes;

    private final DataHandler<BlockData, SinglePacketBlockViewer> dataHandler;
    private final ViewerHandler<BlockData, SinglePacketBlockViewer> viewerHandler;
    private final MetadataHandler metadataHandler;

    /**
     * Constructs a new PacketBlock instance associated with a specific location and block data.
     * The PacketBlock represents a virtual block that interacts with players based on certain
     * conditions, metadata, and viewer states.
     *
     * @param location  The location of the block in the world. Must not be null.
     * @param blockData The visual and physical characteristics of the block. Must not be null.
     */
    protected PacketBlock(@NonNull Location location, @NonNull BlockData blockData) {
        this(location, blockData, -1);
    }

    /**
     * Constructs a new PacketBlock instance associated with a specific location and block data.
     * The PacketBlock represents a virtual block that interacts with players based on certain
     * conditions, metadata, and viewer states.
     *
     * @param location  The location of the block in the world. Must not be null.
     * @param blockData The visual and physical characteristics of the block. Must not be null.
     * @param breakSpeed The speed at which the block breaks, in ticks.
     */
    protected PacketBlock(@NonNull Location location, @NonNull BlockData blockData, int breakSpeed) {
        this.position = WorldPosition.ofBlock(location);
        this.chunk = position.toChunkPosition();

        this.location = location;

        this.boundingBoxes = BoundingBoxes.getBoxes(blockData);

        this.viewerHandler = new ViewerHandler<>(
                player -> blockData,
                this::sendUpdate,
                player -> player.sendBlockChange(location, location.getBlock().getBlockData()),
                () -> new SinglePacketBlockViewer(blockData, () -> blockData, breakSpeed)
        );

        this.dataHandler = new DataHandler<>(this, this::sendUpdate, blockData, breakSpeed);

        this.metadataHandler = new MetadataHandler();
    }

    /**
     * Retrieves the BlockState associated with a specific player. This method
     * takes into account the player's data and determines the appropriate BlockState,
     * using either their pre-existing block data or a supplier to generate new data.
     * If no player-specific data is available, a default BlockState is created
     * based on the block data and location.
     *
     * @param player the player for whom the BlockState is being retrieved
     * @return the BlockState associated with the given player
     */
    public BlockState getBlockState(@NonNull Player player) {
        return getViewer(player)
                .map(playerData -> playerData.getData() == null ? playerData.getDataSupplier().get() : playerData.getData())
                .orElse(getData()).createBlockState().copy(location);
    }

    /**
     * Sends a block update to the specified player at the current location.
     *
     * @param player the player to whom the block update will be sent
     */
    public void sendUpdate(@NonNull Player player) {
        player.sendBlockChange(location, getBlockState(player).getBlockData());
    }

    /**
     * Simulates the breaking action for the specified player.
     * This method triggers a simulation of a break event for the provided player.
     *
     * @param player The player for whom the breaking action is being simulated.
     *               Must not be null.
     */
    public void simulateBreak(@NonNull Player player) {
        simulateBreak(player, null);
    }

    /**
     * Simulates the block-breaking process, triggering a custom event and handling item drops based on the event state.
     *
     * @param player The player performing the block-breaking action. Must not be null.
     * @param tool   The tool used by the player to break the block. Can be null if no tool is used.
     */
    public void simulateBreak(@NonNull Player player, @Nullable ItemStack tool) {
        Bukkit.getScheduler().runTask(PacketBlocks.getInstance(), () -> {
            PacketBlockBreakEvent breakEvent = new PacketBlockBreakEvent(player, this, location, tool);
            Bukkit.getPluginManager().callEvent(breakEvent);

            if (breakEvent.isCancelled()) {
                sendUpdate(player);
                return;
            }

            if (!breakEvent.isDropItems()) {
                return;
            }

            getBlockState(player).getBlock()
                    .getDrops(player.getInventory().getItemInMainHand(), player)
                    .forEach(drop -> player.getWorld().dropItemNaturally(location, drop));
        });
    }

}
