package net.bitbylogic.packetblocks.block;

import net.bitbylogic.packetblocks.data.DataHolder;
import net.bitbylogic.packetblocks.metadata.MetadataHolder;
import net.bitbylogic.packetblocks.viewer.PacketBlockViewer;
import net.bitbylogic.packetblocks.viewer.ViewerHolder;

public interface PacketBlockHolder<T, V extends PacketBlockViewer<T>> extends DataHolder<T, V>, ViewerHolder<T, V>, MetadataHolder {

}
