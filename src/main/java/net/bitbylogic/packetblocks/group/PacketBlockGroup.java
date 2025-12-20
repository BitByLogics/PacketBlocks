package net.bitbylogic.packetblocks.group;

import lombok.Getter;
import lombok.NonNull;
import net.bitbylogic.packetblocks.block.PacketBlockHolder;
import net.bitbylogic.packetblocks.data.DataHandler;
import net.bitbylogic.packetblocks.metadata.MetadataHandler;
import net.bitbylogic.packetblocks.viewer.ViewerHandler;
import net.bitbylogic.packetblocks.viewer.impl.GroupPacketBlockViewer;
import net.bitbylogic.utils.location.WorldPosition;
import org.bukkit.Location;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public class PacketBlockGroup implements PacketBlockHolder<Map<WorldPosition, BlockData>, GroupPacketBlockViewer> {

    private final Map<WorldPosition, Location> cachedLocations;

    private final List<BoundingBox> boundingBoxes;

    private final DataHandler<Map<WorldPosition, BlockData>, GroupPacketBlockViewer> dataHandler;
    private final ViewerHandler<Map<WorldPosition, BlockData>, GroupPacketBlockViewer> viewerHandler;
    private final MetadataHandler metadataHandler;

    public PacketBlockGroup(@NonNull Map<Location, BlockData> blockLocations, int breakSpeed) {
        int size = blockLocations.size();

        this.cachedLocations = new HashMap<>(size);
        this.boundingBoxes = new ArrayList<>(size);

        Map<WorldPosition, BlockData> positions = new HashMap<>(size);

        for (Map.Entry<Location, BlockData> entry : blockLocations.entrySet()) {
            Location location = entry.getKey();
            WorldPosition worldPosition = WorldPosition.ofBlock(location);

            positions.put(worldPosition, entry.getValue());
            cachedLocations.put(worldPosition, location);
        }

        this.viewerHandler = new ViewerHandler<>(
                player -> positions,
                this::sendUpdate,
                player -> {},
                () -> new GroupPacketBlockViewer(positions, () -> positions, breakSpeed)
        );

        this.dataHandler = new DataHandler<>(this, this::sendUpdate, positions, breakSpeed);

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

    /**
     * Sends a block update to the specified player at the current location.
     *
     * @param player the player to whom the block update will be sent
     */
    public void sendUpdate(@NonNull Player player) {
        player.sendBlockChanges(getBlockStates(player));
    }

}
