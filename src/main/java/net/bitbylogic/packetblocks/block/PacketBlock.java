package net.bitbylogic.packetblocks.block;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import net.bitbylogic.packetblocks.PacketBlocks;
import net.bitbylogic.packetblocks.event.PacketBlockBreakEvent;
import net.bitbylogic.packetblocks.util.BoundingBoxes;
import net.bitbylogic.utils.location.ChunkPosition;
import net.bitbylogic.utils.location.WorldPosition;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Predicate;
import java.util.function.Supplier;

@Getter
public class PacketBlock {

    private final WorldPosition position;
    private final ChunkPosition chunk;

    private final Location location;

    private final List<BoundingBox> boundingBoxes;

    private final Set<Predicate<Player>> viewConditions;
    private final HashMap<UUID, PacketBlockPlayerData> viewers;
    private final HashMap<String, Object> metadata;

    private BlockData blockData;

    @Setter
    private int breakSpeed = -1;

    @Setter
    private boolean addViewerOnJoin;

    @Setter
    private boolean globalBreakAnimation;

    /**
     * Constructs a new PacketBlock instance associated with a specific location and block data.
     * The PacketBlock represents a virtual block that interacts with players based on certain
     * conditions, metadata, and viewer states.
     *
     * @param location The location of the block in the world. Must not be null.
     * @param blockData The visual and physical characteristics of the block. Must not be null.
     */
    protected PacketBlock(@NonNull Location location, @NonNull BlockData blockData) {
        this.position = WorldPosition.ofBlock(location);
        this.chunk = position.toChunkPosition();

        this.location = location;

        this.boundingBoxes = BoundingBoxes.getBoxes(blockData);
        this.blockData = blockData;

        this.viewConditions = new HashSet<>();
        this.viewers = new HashMap<>();
        this.metadata = new HashMap<>();
    }

    /**
     * Checks if the specified player meets all the conditions required to view this Packet Block.
     * The conditions are evaluated using the stream of view requirements associated with this block.
     *
     * @param player The player whose eligibility to view the block is being checked. Must not be null.
     * @return True if the player satisfies all the view conditions, otherwise false.
     */
    public boolean canView(@NonNull Player player) {
        return viewConditions.stream().allMatch(viewRequirement -> viewRequirement.test(player));
    }

    /**
     * Checks whether the specified player is a viewer of this Packet Block.
     *
     * @param player The player to check. Must not be null.
     * @return True if the player is a viewer of this Packet Block, false otherwise.
     */
    public boolean isViewer(@NonNull Player player) {
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
    public Optional<PacketBlockPlayerData> getViewer(@NonNull Player player) {
        return Optional.ofNullable(viewers.get(player.getUniqueId()));
    }

    /**
     * Attempts to add the specified player as a viewer to this Packet Block if they meet the viewing conditions.
     * If the player is successfully added, an optional containing the associated {@link PacketBlockPlayerData}
     * instance is returned. If the player is already a viewer, their existing {@link PacketBlockPlayerData} is returned.
     * No viewer is added if the player does not meet the viewing conditions.
     *
     * @param player The player to attempt to add as a viewer.
     * @param sendUpdate Whether to send a block update to the player upon successfully adding them as a viewer.
     * @return An {@link Optional} containing the {@link PacketBlockPlayerData} associated with the player
     *         if they meet the conditions and are added as a viewer, or an empty optional otherwise.
     */
    public Optional<PacketBlockPlayerData> attemptAddViewer(@NonNull Player player, boolean sendUpdate) {
        if (!canView(player)) {
            return Optional.empty();
        }

        if(isViewer(player)) {
            return getViewer(player);
        }

        PacketBlockPlayerData playerData = new PacketBlockPlayerData(null, () -> blockData, breakSpeed);
        viewers.put(player.getUniqueId(), playerData);

        if(sendUpdate) {
            sendUpdate(player);
        }

        return Optional.of(playerData);
    }

    /**
     * Adds the specified player as a viewer to this Packet Block.
     * This method bypasses any view requirements that are set.
     *
     * @param player The player to add as a viewer.
     * @return The {@link PacketBlockPlayerData} instance associated with the added player.
     */
    public PacketBlockPlayerData addViewer(@NonNull Player player) {
        PacketBlockPlayerData playerData = new PacketBlockPlayerData(null, () -> blockData, breakSpeed);
        viewers.put(player.getUniqueId(), playerData);
        return playerData;
    }

    /**
     * Adds the specified player as a viewer to this Packet Block if they are not already added
     * and updates their block state to reflect the current state of the Packet Block.
     * This method bypasses any view requirements that are set.
     *
     * @param player The player to add as a viewer and send block updates to
     * @return The {@link PacketBlockPlayerData} instance associated with the added player
     */
    public PacketBlockPlayerData addAndUpdateViewer(@NonNull Player player) {
        PacketBlockPlayerData playerData = addViewer(player);
        sendUpdate(player);
        return playerData;
    }

    /**
     * Adds a viewer to this Packet Block with a custom BlockData supplier.
     * This method bypasses any view requirements that are set.
     *
     * @param player The player to add as a viewer.
     * @param blockDataSupplier A supplier that provides the BlockData for this player.
     * @return The {@link PacketBlockPlayerData} instance associated with the added player.
     */
    public PacketBlockPlayerData addViewer(@NonNull Player player, @NonNull Supplier<BlockData> blockDataSupplier) {
        PacketBlockPlayerData playerData = new PacketBlockPlayerData(null, blockDataSupplier, breakSpeed);
        viewers.put(player.getUniqueId(), playerData);

        return playerData;
    }

    /**
     * Adds a viewer for the block data and sends an update to the specified player.
     *
     * @param player the player to be added as a viewer, must not be null
     * @param blockDataSupplier a supplier for providing the block data, must not be null
     * @return the PacketBlockPlayerData object associated with the player
     */
    public PacketBlockPlayerData addAndUpdateViewer(@NonNull Player player, @NonNull Supplier<BlockData> blockDataSupplier) {
        PacketBlockPlayerData playerData = addViewer(player, blockDataSupplier);
        sendUpdate(player);

        return playerData;
    }

    /**
     * Removes the specified player from the list of viewers if they are present.
     * If the player is successfully removed, it sends a block change notification
     * to the player for a specific location.
     *
     * @param player the player to be removed from the viewers list; must not be null
     */
    public void removeViewer(@NonNull Player player) {
        if(!viewers.containsKey(player.getUniqueId())) {
            return;
        }

        viewers.remove(player.getUniqueId());
        player.sendBlockChange(location, location.getBlock().getBlockData());
    }

    /**
     * Sets the block data for all viewers and updates them accordingly.
     *
     * @param blockData the block data to be set for all viewers
     */
    public void setBlockDataForAll(@NonNull BlockData blockData) {
        this.blockData = blockData;
        viewers.values().forEach(playerData -> playerData.setBlockData(blockData));
        sendUpdates();
    }

    /**
     * Sets the block data supplier for all viewers and updates the block data
     * for each viewer based on the provided BlockData.
     *
     * @param blockData the BlockData object to be supplied to all viewers
     */
    public void setBlockDataSupplierForAll(@NonNull BlockData blockData) {
        this.blockData = blockData;
        viewers.values().forEach(playerData -> playerData.setBlockDataSupplier(() -> blockData));
        sendUpdates();
    }

    /**
     * Sets the block data for a specific player.
     *
     * @param player the player for whom the block data is being set; must not be null
     * @param blockData the block data to associate with the player; can be null to reset or remove the block data
     */
    public void setBlockData(@NonNull Player player, @Nullable BlockData blockData) {
        Optional<PacketBlockPlayerData> optionalPlayerData = getViewer(player);

        if (optionalPlayerData.isEmpty()) {
            return;
        }

        optionalPlayerData.get().setBlockData(blockData);
    }

    /**
     * Sets the BlockData supplier for a specific player, allowing for dynamic control over block data.
     *
     * @param player the player for whom the BlockData supplier is being set, must not be null
     * @param blockData the BlockData object to be supplied, must not be null
     */
    public void setBlockDataSupplier(@NonNull Player player, @NonNull BlockData blockData) {
        Optional<PacketBlockPlayerData> optionalPlayerData = getViewer(player);

        if (optionalPlayerData.isEmpty()) {
            return;
        }

        optionalPlayerData.get().setBlockDataSupplier(() -> blockData);
    }

    /**
     * Sets the block data for the specified player and sends an update.
     *
     * @param player the player for whom the block data is being set, must not be null
     * @param blockData the block data to be set, can be null
     */
    public void setBlockDataAndUpdate(@NonNull Player player, @Nullable BlockData blockData) {
        setBlockData(player, blockData);
        sendUpdate(player);
    }

    /**
     * Retrieves the BlockState associated with a specific player. This method
     * takes into account the player's data and determines the appropriate BlockState,
     * using either their pre-existing block data or a supplier to generate new data.
     * If no player-specific data is available, a default BlockState is created
     * based on the block data and location.
     *
     * @param player the player for whom the BlockState is being retrieved
     * @return the BlockState associated with the given player
     */
    public BlockState getBlockState(@NonNull Player player) {
        return getViewer(player)
                .map(playerData -> playerData.getBlockData() == null ? playerData.getBlockDataSupplier().get() : playerData.getBlockData())
                .orElse(blockData).createBlockState().copy(location);
    }

    /**
     * Adds metadata to the PacketBlock. The metadata is a key-value pair where the key is a unique identifier,
     * and the value is associated data. If the specified key already exists in the metadata, the method will
     * do nothing.
     *
     * @param key The unique key for the metadata. Must not be null.
     * @param object The value associated with the metadata key. Must not be null.
     */
    public void addMetadata(@NonNull String key, @NonNull Object object) {
        if (metadata.containsKey(key)) {
            return;
        }

        metadata.put(key, object);
    }

    /**
     * Removes the metadata associated with the specified key. If the key exists in the metadata map,
     * it will be removed.
     *
     * @param key the key of the metadata to be removed; must not be null
     */
    public void removeMetadata(@NonNull String key) {
        metadata.remove(key);
    }

    /**
     * Checks if the metadata contains the specified key.
     *
     * @param key the key to check for in the metadata; must not be null
     * @return true if the metadata contains the specified key, false otherwise
     */
    public boolean hasMetadata(@NonNull String key) {
        return metadata.containsKey(key);
    }

    /**
     * Retrieves the metadata value associated with the specified key.
     * If the key does not exist, a default fallback value is used.
     *
     * @param key The key for which the metadata value is to be retrieved. Must not be null.
     * @return The metadata value associated with the given key, or null if no value exists and no fallback is provided.
     */
    public Object getMetadata(@NonNull String key) {
        return getMetadata(key, null);
    }

    /**
     * Retrieves metadata associated with the specified key and casts it to the desired type.
     * If the key is not present or the cast fails, exceptions may be thrown.
     *
     * @param key The key used to reference the metadata. Must not be null.
     * @param clazz The class type to which the metadata should be cast. Must not be null.
     * @param <T> The type to which the metadata will be cast.
     * @return The metadata object corresponding to the specified key, cast to the specified type.
     *         Returns null if the key is not present or the metadata value itself is null.
     */
    public <T> T getMetadataAs(@NonNull String key, @NonNull Class<T> clazz) {
        return (T) getMetadata(key, null);
    }

    /**
     * Retrieves the metadata value associated with the given key.
     * If the key is not present in the metadata, the fallback value is returned.
     *
     * @param key The key to look up in the metadata. Must not be null.
     * @param fallback The fallback value to return if the key is not present in the metadata. Can be null.
     * @return The metadata value associated with the key, or the provided fallback value if the key is not present.
     */
    public Object getMetadata(@NonNull String key, @Nullable Object fallback) {
        return metadata.getOrDefault(key, fallback);
    }

    /**
     * Adds a metadata key-value pair to the specific player's {@link PacketBlockPlayerData}, if the player is a viewer.
     *
     * @param player The player whose metadata is being updated. Must not be null.
     * @param key The key for the metadata entry. Must not be null.
     * @param object The value associated with the specified key. Must not be null.
     */
    public void addMetadata(@NonNull Player player, @NonNull String key, @NonNull Object object) {
        getViewer(player).ifPresent(playerData -> playerData.addMetadata(key, object));
    }

    /**
     * Removes the metadata associated with a specific key for the given player, if the player is a viewer
     * of this PacketBlock. This allows targeted removal of metadata entries tied to individual players.
     *
     * @param player The player whose associated metadata is to be removed. Must not be null.
     * @param key The key of the metadata to be removed. Must not be null.
     */
    public void removeMetadata(@NonNull Player player, @NonNull String key) {
        getViewer(player).ifPresent(playerData -> playerData.removeMetadata(key));
    }

    /**
     * Checks if the specified player has metadata associated with the given key within this Packet Block.
     *
     * @param player The player whose metadata association is to be checked. Must not be null.
     * @param key The key of the metadata to check. Must not be null.
     * @return True if the specified player has metadata associated with the given key, otherwise false.
     */
    public boolean hasMetadata(@NonNull Player player, @NonNull String key) {
        return getViewer(player).map(playerData -> playerData.hasMetadata(key)).orElse(false);
    }

    /**
     * Retrieves the metadata associated with a specific player and a given key.
     *
     * @param player The player for whom metadata is being retrieved. Must not be null.
     * @param key The key identifying the metadata to be retrieved. Must not be null.
     * @return The metadata object associated with the specified player and key, or null if no metadata exists for the key.
     */
    public Object getMetadata(@NonNull Player player, @NonNull String key) {
        return getMetadataAs(player, key, null);
    }

    /**
     * Retrieves metadata associated with a given key for a specified player. If no metadata is found,
     * a fallback value will be returned.
     *
     * @param player the player whose metadata is to be retrieved; must not be null
     * @param key the key associated with the metadata to retrieve; must not be null
     * @param fallback the value to return if no associated metadata is found; can be null
     * @return the metadata value associated with the specified key, or the fallback value if no metadata is found
     */
    public Object getMetadataAs(@NonNull Player player, @NonNull String key, @Nullable Object fallback) {
        return getViewer(player).map(playerData -> playerData.getMetadata(key)).orElse(fallback);
    }

    /**
     * Sends a block update to the specified player at the current location.
     *
     * @param player the player to whom the block update will be sent
     */
    public void sendUpdate(@NonNull Player player) {
        player.sendBlockChange(location, getBlockState(player).getBlockData());
    }

    /**
     * Sends block updates to all the viewers currently tracking the block at the specified location.
     *
     * This method iterates through all viewers stored in the `viewers` map and attempts to send a
     * block update to each. If a viewer no longer exists or is offline, they are removed from the
     * `viewers` map.
     *
     * The block state sent to each viewer is dependent on the `getBlockState(Player)` implementation,
     * which determines the block's appearance based on the specific viewer.
     */
    public void sendUpdates() {
        Iterator<UUID> viewerIterator = viewers.keySet().iterator();

        while (viewerIterator.hasNext()) {
            Player viewer = Bukkit.getPlayer(viewerIterator.next());

            if (viewer == null) {
                viewerIterator.remove();
                continue;
            }

            viewer.sendBlockChange(location, getBlockState(viewer).getBlockData());
        }
    }

    /**
     * Calculates and retrieves the break speed for the given player.
     *
     * @param player the player whose break speed is being requested, must not be null
     * @return the calculated break speed for the player; returns a default value if not available
     */
    public int getBreakSpeed(@NonNull Player player) {
        return getViewer(player).map(PacketBlockPlayerData::getBreakSpeed).orElse(breakSpeed);
    }

    /**
     * Simulates the breaking action for the specified player.
     * This method triggers a simulation of a break event for the provided player.
     *
     * @param player The player for whom the breaking action is being simulated.
     *               Must not be null.
     */
    public void simulateBreak(@NonNull Player player) {
        simulateBreak(player, null);
    }

    /**
     * Simulates the block-breaking process, triggering a custom event and handling item drops based on the event state.
     *
     * @param player The player performing the block-breaking action. Must not be null.
     * @param tool The tool used by the player to break the block. Can be null if no tool is used.
     */
    public void simulateBreak(@NonNull Player player, @Nullable ItemStack tool) {
        Bukkit.getScheduler().runTask(PacketBlocks.getInstance(), () -> {
            PacketBlockBreakEvent breakEvent = new PacketBlockBreakEvent(player, this, location, tool);
            Bukkit.getPluginManager().callEvent(breakEvent);

            if (breakEvent.isCancelled()) {
                sendUpdate(player);
                return;
            }

            if (!breakEvent.isDropItems()) {
                return;
            }

            getBlockState(player).getBlock()
                    .getDrops(player.getInventory().getItemInMainHand(), player)
                    .forEach(drop -> player.getWorld().dropItemNaturally(location, drop));
        });
    }

    /**
     * Adds a viewing condition to the list of conditions if it is not already present.
     *
     * @param condition the condition to be added, represented as a {@code Predicate<Player>}.
     *                  This condition evaluates to determine whether a player meets the viewing criteria.
     */
    public void addViewCondition(@NonNull Predicate<Player> condition) {
        if(viewConditions.contains(condition)) {
            return;
        }

        viewConditions.add(condition);
    }

}
