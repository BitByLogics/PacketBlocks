package net.bitbylogic.packetblocks.viewer;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.bitbylogic.packetblocks.block.PacketBlockPlayerData;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

@RequiredArgsConstructor
public class ViewerHandler<V, T extends PacketBlockViewer<V>> {

    private final Set<Predicate<Player>> viewConditions = new HashSet<>();
    private final HashMap<UUID, T> viewers = new HashMap<>();

    private final Function<Player, V> playerDataSupplier;

    private final Consumer<Player> updateConsumer;
    private final Consumer<Player> removeConsumer;

    private final Supplier<T> dataSupplier;

    protected Map<UUID, T> getViewers() {
        return Collections.unmodifiableMap(viewers);
    }

    /**
     * Checks if the specified player meets all the conditions required to view this Packet Block.
     * The conditions are evaluated using the stream of view requirements associated with this block.
     *
     * @param player The player whose eligibility to view the block is being checked. Must not be null.
     * @return True if the player satisfies all the view conditions, otherwise false.
     */
    protected boolean canView(@NonNull Player player) {
        return viewConditions.stream().allMatch(viewRequirement -> viewRequirement.test(player));
    }

    /**
     * Checks whether the specified player is a viewer of this Packet Block.
     *
     * @param player The player to check. Must not be null.
     * @return True if the player is a viewer of this Packet Block, false otherwise.
     */
    protected boolean isViewer(@NonNull Player player) {
        return viewers.containsKey(player.getUniqueId());
    }

    /**
     * Retrieves the {@link PacketBlockPlayerData} associated with the given player if they are a viewer
     * of this Packet Block.
     *
     * @param player The player whose viewer data is to be retrieved. Must not be null.
     * @return An {@link Optional} containing the {@link PacketBlockPlayerData} associated with the player
     *         if they are a viewer, or an empty optional if they are not a viewer.
     */
    protected Optional<T> getViewer(@NonNull Player player) {
        return Optional.ofNullable(viewers.get(player.getUniqueId()));
    }

    /**
     * Attempts to add the specified player as a viewer to this Packet Block if they meet the viewing conditions.
     * If the player is successfully added, an optional containing the associated {@link T}
     * instance is returned. If the player is already a viewer, their existing {@link T} is returned.
     * No viewer is added if the player does not meet the viewing conditions.
     *
     * @param player The player to attempt to add as a viewer.
     * @param sendUpdate Whether to send a block update to the player upon successfully adding them as a viewer.
     * @return An {@link Optional} containing the {@link T} associated with the player
     *         if they meet the conditions and are added as a viewer, or an empty optional otherwise.
     */
    protected Optional<T> attemptAddViewer(@NonNull Player player, boolean sendUpdate) {
        if (!canView(player)) {
            return Optional.empty();
        }

        if(isViewer(player)) {
            return getViewer(player);
        }

        T data = dataSupplier.get();
        viewers.put(player.getUniqueId(), data);

        if(sendUpdate) {
            updateConsumer.accept(player);
        }

        return Optional.of(data);
    }

    /**
     * Adds the specified player as a viewer to this Packet Block.
     * This method bypasses any view requirements that are set.
     *
     * @param player The player to add as a viewer.
     * @return The {@link PacketBlockPlayerData} instance associated with the added player.
     */
    protected T addViewer(@NonNull Player player) {
        T data = dataSupplier.get();
        viewers.put(player.getUniqueId(), data);
        return data;
    }

    /**
     * Adds the specified player as a viewer to this Packet Block if they are not already added
     * and updates their block state to reflect the current state of the Packet Block.
     * This method bypasses any view requirements that are set.
     *
     * @param player The player to add as a viewer and send block updates to
     * @return The {@link PacketBlockPlayerData} instance associated with the added player
     */
    protected T addAndUpdateViewer(@NonNull Player player) {
        T data = addViewer(player);
        playerDataSupplier.apply(player);
        return data;
    }

    /**
     * Removes the specified player from the list of viewers if they are present.
     * If the player is successfully removed, it sends a block change notification
     * to the player for a specific location.
     *
     * @param player the player to be removed from the viewers list; must not be null
     */
    protected void removeViewer(@NonNull Player player) {
        if(!viewers.containsKey(player.getUniqueId())) {
            return;
        }

        viewers.remove(player.getUniqueId());
        removeConsumer.accept(player);
    }

    /**
     * Adds a viewing condition to the list of conditions if it is not already present.
     *
     * @param condition the condition to be added, represented as a {@code Predicate<Player>}.
     *                  This condition evaluates to determine whether a player meets the viewing criteria.
     */
    protected void addViewCondition(@NonNull Predicate<Player> condition) {
        if(viewConditions.contains(condition)) {
            return;
        }

        viewConditions.add(condition);
    }

    /**
     * Adds a metadata key-value pair to the specific player's {@link PacketBlockPlayerData}, if the player is a viewer.
     *
     * @param player The player whose metadata is being updated. Must not be null.
     * @param key    The key for the metadata entry. Must not be null.
     * @param object The value associated with the specified key. Must not be null.
     */
    protected void addMetadata(@NonNull Player player, @NonNull String key, @NonNull Object object) {
        getViewer(player).ifPresent(playerData -> playerData.addMetadata(key, object));
    }

    /**
     * Removes the metadata associated with a specific key for the given player, if the player is a viewer
     * of this PacketBlock. This allows targeted removal of metadata entries tied to individual players.
     *
     * @param player The player whose associated metadata is to be removed. Must not be null.
     * @param key    The key of the metadata to be removed. Must not be null.
     */
    protected void removeMetadata(@NonNull Player player, @NonNull String key) {
        getViewer(player).ifPresent(playerData -> playerData.removeMetadata(key));
    }

    /**
     * Checks if the specified player has metadata associated with the given key within this Packet Block.
     *
     * @param player The player whose metadata association is to be checked. Must not be null.
     * @param key    The key of the metadata to check. Must not be null.
     * @return True if the specified player has metadata associated with the given key, otherwise false.
     */
    protected boolean hasMetadata(@NonNull Player player, @NonNull String key) {
        return getViewer(player).map(playerData -> playerData.hasMetadata(key)).orElse(false);
    }

    /**
     * Retrieves the metadata associated with a specific player and a given key.
     *
     * @param player The player for whom metadata is being retrieved. Must not be null.
     * @param key    The key identifying the metadata to be retrieved. Must not be null.
     * @return The metadata object associated with the specified player and key, or null if no metadata exists for the key.
     */
    protected Object getMetadata(@NonNull Player player, @NonNull String key) {
        return getMetadataAs(player, key, null);
    }

    /**
     * Retrieves metadata associated with a given key for a specified player. If no metadata is found,
     * a fallback value will be returned.
     *
     * @param player   the player whose metadata is to be retrieved; must not be null
     * @param key      the key associated with the metadata to retrieve; must not be null
     * @param fallback the value to return if no associated metadata is found; can be null
     * @return the metadata value associated with the specified key, or the fallback value if no metadata is found
     */
    protected Object getMetadataAs(@NonNull Player player, @NonNull String key, @Nullable Object fallback) {
        return getViewer(player).map(playerData -> playerData.getMetadata(key)).orElse(fallback);
    }

}
