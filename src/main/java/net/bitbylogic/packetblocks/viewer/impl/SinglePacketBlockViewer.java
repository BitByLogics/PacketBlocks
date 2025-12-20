package net.bitbylogic.packetblocks.viewer.impl;

import net.bitbylogic.packetblocks.viewer.PacketBlockViewer;
import org.bukkit.block.data.BlockData;

import java.util.function.Supplier;

public class SinglePacketBlockViewer extends PacketBlockViewer<BlockData> {

    public SinglePacketBlockViewer(BlockData data, Supplier<BlockData> dataSupplier, int breakSpeed) {
        super(data, dataSupplier, breakSpeed);
    }

}
