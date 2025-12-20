package net.bitbylogic.packetblocks.block;

import lombok.NonNull;
import net.bitbylogic.packetblocks.data.DataHolder;
import net.bitbylogic.packetblocks.metadata.MetadataHolder;
import net.bitbylogic.packetblocks.viewer.PacketBlockViewer;
import net.bitbylogic.packetblocks.viewer.ViewerHolder;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

public interface PacketBlockHolder<T, V extends PacketBlockViewer<T>> extends DataHolder<T, V>, ViewerHolder<T, V>, MetadataHolder {

    void sendUpdate(@NonNull Player player);

    boolean existsIn(@NonNull World world);

    boolean existsAt(@NonNull Location location);

}
