package net.bitbylogic.packetblocks.viewer;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import net.bitbylogic.packetblocks.metadata.MetadataHandler;
import net.bitbylogic.packetblocks.metadata.MetadataHolder;

import java.util.function.Supplier;

@Setter
@Getter
@AllArgsConstructor
public class PacketBlockViewer<T> implements MetadataHolder {

    private final MetadataHandler metadataHandler = new MetadataHandler();

    private T data;
    private Supplier<T> dataSupplier;
    private int breakSpeed;

    public T getSuppliedData() {
        return dataSupplier != null ? dataSupplier.get() : data;
    }

    @Override
    public @NonNull MetadataHandler getMetadataHandler() {
        return metadataHandler;
    }

}
