package net.bitbylogic.packetblocks.viewer.impl;

import lombok.NonNull;
import net.bitbylogic.packetblocks.viewer.PacketBlockViewer;
import net.bitbylogic.utils.location.WorldPosition;
import org.bukkit.block.data.BlockData;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class GroupPacketBlockViewer extends PacketBlockViewer<Map<WorldPosition, BlockData>> {

    public GroupPacketBlockViewer(Map<WorldPosition, BlockData> data, Supplier<Map<WorldPosition, BlockData>> dataSupplier, int breakSpeed) {
        super(new HashMap<>(data), dataSupplier, breakSpeed);
    }

    public void setBlockData(@NonNull WorldPosition position, @NonNull BlockData blockData) {
        if(!getData().containsKey(position)) {
            throw new IllegalArgumentException("Position " + position + " is not part of this group");
        }

        getData().put(position, blockData);
    }

    public void setGroupBlockData(@NonNull BlockData blockData) {
        for (Map.Entry<WorldPosition, BlockData> entry : getData().entrySet()) {
            entry.setValue(blockData);
        }
    }

}
