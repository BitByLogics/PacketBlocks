package net.bitbylogic.packetblocks.group;

import lombok.Getter;
import lombok.NonNull;
import net.bitbylogic.packetblocks.block.PacketBlockHolder;
import net.bitbylogic.packetblocks.data.DataHandler;
import net.bitbylogic.packetblocks.metadata.MetadataHandler;
import net.bitbylogic.packetblocks.util.BoundingBoxes;
import net.bitbylogic.packetblocks.viewer.ViewerHandler;
import net.bitbylogic.packetblocks.viewer.impl.GroupPacketBlockViewer;
import net.bitbylogic.utils.location.ChunkPosition;
import net.bitbylogic.utils.location.WorldPosition;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.Nullable;

import java.util.*;

@Getter
public class PacketBlockGroup implements PacketBlockHolder<Map<WorldPosition, BlockData>, GroupPacketBlockViewer> {

    private final List<String> worldNames = new ArrayList<>();
    private final Map<ChunkPosition, List<WorldPosition>> chunkPositions;
    private final Map<WorldPosition, Location> cachedLocations;

    private final DataHandler<Map<WorldPosition, BlockData>, GroupPacketBlockViewer> dataHandler;
    private final ViewerHandler<Map<WorldPosition, BlockData>, GroupPacketBlockViewer> viewerHandler;
    private final MetadataHandler metadataHandler;

    public PacketBlockGroup(@NonNull Map<Location, BlockData> blockLocations) {
        this(blockLocations, -1);
    }

    public PacketBlockGroup(@NonNull Map<Location, BlockData> blockLocations, int breakSpeed) {
        int size = blockLocations.size();

        this.chunkPositions = new HashMap<>(size);
        this.cachedLocations = new HashMap<>(size);

        Map<WorldPosition, BlockData> positions = new HashMap<>(size);

        for (Map.Entry<Location, BlockData> entry : blockLocations.entrySet()) {
            Location location = entry.getKey().toBlockLocation();
            WorldPosition worldPosition = WorldPosition.ofBlock(location);
            ChunkPosition chunkPosition = worldPosition.toChunkPosition();

            if (!worldNames.contains(worldPosition.worldName())) {
                worldNames.add(worldPosition.worldName());
            }

            chunkPositions.computeIfAbsent(chunkPosition, k -> new ArrayList<>()).add(worldPosition);

            positions.put(worldPosition, entry.getValue());
            cachedLocations.put(worldPosition, location);
        }

        this.viewerHandler = new ViewerHandler<>(
                player -> positions,
                this::sendUpdate,
                player -> {
                    List<BlockState> states = new ArrayList<>();

                    for (Map.Entry<WorldPosition, BlockData> entry : getData().entrySet()) {
                        Location location = cachedLocations.get(entry.getKey());
                        states.add(location.getBlock().getState());
                    }

                    player.sendBlockChanges(states);
                },
                () -> new GroupPacketBlockViewer(positions, () -> positions, breakSpeed)
        );

        this.dataHandler = new DataHandler<>(this, this::sendUpdate, map -> {
            List<BoundingBox> boundingBoxes = new ArrayList<>();

            for (Map.Entry<WorldPosition, BlockData> entry : map.entrySet()) {
                boundingBoxes.addAll(BoundingBoxes.getBoxesAt(entry.getValue(), cachedLocations.get(entry.getKey())));
            }

            return boundingBoxes;
        }, positions, breakSpeed);

        this.metadataHandler = new MetadataHandler();
    }

    public List<BlockState> getBlockStates(@NonNull Player player) {
        List<BlockState> states = new ArrayList<>();

        GroupPacketBlockViewer viewer = getViewer(player).orElse(null);

        if(viewer == null) {
            for (Map.Entry<WorldPosition, BlockData> entry : getData().entrySet()) {
                Location location = cachedLocations.get(entry.getKey());
                states.add(entry.getValue().createBlockState().copy(location));
            }

            return states;
        }

        Map<WorldPosition, BlockData> data = viewer.getData() == null ? viewer.getDataSupplier().get() : viewer.getData();

        for (Map.Entry<WorldPosition, BlockData> entry : data.entrySet()) {
            Location location = cachedLocations.get(entry.getKey());
            states.add(entry.getValue().createBlockState().copy(location));
        }

        return states;
    }

    public Optional<BlockData> getDataAt(@Nullable Player player, @NonNull Location location) {
        if (player == null) {
            return Optional.ofNullable(getData().get(WorldPosition.ofBlock(location)));
        }

        Optional<GroupPacketBlockViewer> optionalViewer = getViewer(player);

        if(optionalViewer.isPresent()) {
            return Optional.ofNullable(optionalViewer.get().getData().get(WorldPosition.ofBlock(location)));
        }

        return Optional.ofNullable(getData().get(WorldPosition.ofBlock(location)));
    }

    /**
     * Sends a block update to the specified player at the current location.
     *
     * @param player the player to whom the block update will be sent
     */
    @Override
    public void sendUpdate(@NonNull Player player) {
        player.sendBlockChanges(getBlockStates(player));
    }

    @Override
    public boolean existsIn(@NonNull World world) {
        return worldNames.contains(world.getName());
    }

    @Override
    public boolean existsAt(@NonNull Location location) {
        return getData().containsKey(WorldPosition.ofBlock(location));
    }

}
