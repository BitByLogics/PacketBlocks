package net.bitbylogic.packetblocks.block;

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

    /**
     * Adds a metadata entry with the specified key and object.
     * If the key already exists in the metadata map, the method does nothing.
     *
     * @param key the key used to identify the metadata entry; must not be null
     * @param object the object to be associated with the key; must not be null
     */
    public void addMetadata(@NonNull String key, @NonNull Object object) {
        if (metadata.containsKey(key)) {
            return;
        }

        metadata.put(key, object);
    }

    /**
     * Removes metadata associated with the specified key from the metadata map.
     *
     * @param key the key whose associated metadata is to be removed. Must not be null.
     */
    public void removeMetadata(@NonNull String key) {
        metadata.remove(key);
    }

    /**
     * Checks if a metadata entry with the specified key exists.
     *
     * @param key the key to check for metadata presence, must not be null
     * @return true if metadata with the given key exists, false otherwise
     */
    public boolean hasMetadata(@NonNull String key) {
        return metadata.containsKey(key);
    }

    /**
     * Retrieves the metadata associated with the specified key.
     *
     * @param key the key to look up in the metadata map; must not be null.
     * @return the metadata value associated with the given key, or null if no value is present for the key.
     */
    public Object getMetadata(@NonNull String key) {
        return getMetadata(key, null);
    }

    /**
     * Retrieves the metadata associated with the given key and casts it to the specified type.
     *
     * @param key the key for the metadata entry, must not be null
     * @param clazz the class type to cast the metadata value to, must not be null
     * @return the metadata value associated with the provided key, cast to the desired type,
     *         or null if the key does not exist or the value could not be cast
     * @throws ClassCastException if the metadata value cannot be cast to the specified type
     */
    public <T> T getMetadataAs(@NonNull String key, @NonNull Class<T> clazz) {
        return (T) getMetadata(key, null);
    }

    /**
     * Retrieves the metadata associated with the given key. If no metadata exists for the key,
     * returns the specified fallback value.
     *
     * @param key the key whose associated metadata is to be returned. Must not be null.
     * @param fallback the value to return if no metadata is found for the given key. May be null.
     * @return the metadata associated with the specified key, or the fallback value if no metadata is found.
     */
    public Object getMetadata(@NonNull String key, @Nullable Object fallback) {
        return metadata.getOrDefault(key, fallback);
    }

}
