package net.bitbylogic.packetblocks.data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.bukkit.block.data.BlockData;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.function.Supplier;

@Getter
@Setter
@AllArgsConstructor
public class PacketBlockPlayerData {

    private final HashMap<String, Object> metadata = new HashMap<>();

    private BlockData blockData;
    private Supplier<BlockData> blockDataSupplier;
    private int breakSpeed;

    public void addMetadata(@NonNull String key, @NonNull Object object) {
        if (metadata.containsKey(key)) {
            return;
        }

        metadata.put(key, object);
    }

    public void removeMetadata(@NonNull String key) {
        metadata.remove(key);
    }

    public boolean hasMetadata(@NonNull String key) {
        return metadata.containsKey(key);
    }

    public Object getMetadata(@NonNull String key) {
        return getMetadata(key, null);
    }

    public <T> T getMetadataAs(@NonNull String key, @NonNull Class<T> clazz) {
        return (T) getMetadata(key, null);
    }

    public Object getMetadata(@NonNull String key, @Nullable Object fallback) {
        return metadata.getOrDefault(key, fallback);
    }

}
