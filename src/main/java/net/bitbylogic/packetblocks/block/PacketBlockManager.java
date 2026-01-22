package net.bitbylogic.packetblocks.block;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.bitbylogic.packetblocks.PacketBlocks;
import net.bitbylogic.packetblocks.data.DataHolder;
import net.bitbylogic.packetblocks.group.PacketBlockGroup;
import net.bitbylogic.utils.location.ChunkPosition;
import net.bitbylogic.utils.location.WorldPosition;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.logging.Level;

@RequiredArgsConstructor
@Getter
public class PacketBlockManager {

    private final ConcurrentHashMap<ChunkPosition, Map<WorldPosition, PacketBlockHolder<?, ?>>> blockLocations = new ConcurrentHashMap<>();

    private final PacketBlocks plugin;

    /**
     * Creates a new {@link PacketBlock} instance at the specified location with the given block data.
     * The created block is registered within the internally managed collection, ensuring it is
     * appropriately tracked for further operations.
     * <p>
     * If the block already exists in the specified chunk, no duplicate will be created,
     * and the existing instance will be returned.
     *
     * @param location the location of the block to be created; must not be null
     * @param blockData the data of the block to be created; must not be null
     * @return the newly created {@link PacketBlock} instance, or the existing instance if one already exists at the location;
     *         returns null if the location's world is null
     */
    public PacketBlock createBlock(@NonNull Location location, @NonNull BlockData blockData) {
        World world = location.getWorld();

        if (world == null) {
            plugin.getLogger().log(Level.WARNING, "Unable to create packet block, null world!: " + location.toString());
            return null;
        }

        Chunk chunk = location.getChunk();
        PacketBlock packetBlock = new PacketBlock(location, blockData);
        ChunkPosition identifier = new ChunkPosition(location.getWorld().getName(), chunk.getX(), chunk.getZ());

        if (blockLocations.containsKey(identifier)) {
            Map<WorldPosition, PacketBlockHolder<?, ?>> blocks = blockLocations.get(identifier);

            if (blocks.containsValue(packetBlock)) {
                return packetBlock;
            }

            blocks.put(packetBlock.getPosition(), packetBlock);
            blockLocations.put(identifier, blocks);
            return packetBlock;
        }

        Map<WorldPosition, PacketBlockHolder<?, ?>> newBlocks = new HashMap<>();
        newBlocks.put(packetBlock.getPosition(), packetBlock);

        blockLocations.put(identifier, newBlocks);
        return packetBlock;
    }

    public PacketBlockGroup createGroup(@NonNull Map<Location, BlockData> groupBlocks) {
        PacketBlockGroup packetGroup = new PacketBlockGroup(groupBlocks);

        for (Map.Entry<ChunkPosition, List<WorldPosition>> entry : packetGroup.getChunkPositions().entrySet()) {
            ChunkPosition chunkPosition = entry.getKey();
            List<WorldPosition> worldPositions = entry.getValue();

            Map<WorldPosition, PacketBlockHolder<?, ?>> blocks = blockLocations.computeIfAbsent(chunkPosition, k -> new HashMap<>());

            for (WorldPosition worldPosition : worldPositions) {
                blocks.put(worldPosition, packetGroup);
                blockLocations.put(chunkPosition, blocks);
            }
        }

        return packetGroup;
    }

    /**
     * Adds a collection of blocks to the specified group and updates the internal block location mappings.
     *
     * @param group     The group to which the blocks will be added. Must not be null.
     * @param locations A map containing block locations and their associated block data. Must not be null.
     */
    public void addBlocksToGroup(@NonNull PacketBlockGroup group, @NonNull Map<Location, BlockData> locations) {
        group.addLocations(locations);

        for (Map.Entry<Location, BlockData> entry : locations.entrySet()) {
            ChunkPosition chunkPosition = ChunkPosition.of(entry.getKey().getChunk());
            blockLocations.get(chunkPosition).put(WorldPosition.ofBlock(entry.getKey()), group);
        }
    }

    /**
     * Adds a block to the specified block group at the given location with the provided block data.
     *
     * @param group The block group to which the block will be added. Must not be null.
     * @param location The location where the block will be added. Must not be null.
     * @param blockData The data representing the block to be added. Must not be null.
     */
    public void addBlockToGroup(@NonNull PacketBlockGroup group, @NonNull Location location, @NonNull BlockData blockData) {
        group.addLocation(location, blockData);

        ChunkPosition chunkPosition = ChunkPosition.of(location.getChunk());
        blockLocations.get(chunkPosition).put(WorldPosition.ofBlock(location), group);
    }

    /**
     * Removes the specified list of block locations from the given block group and updates the internal tracking structure.
     *
     * @param group      The block group from which locations should be removed. Must not be null.
     * @param locations  The list of locations to be removed from the group. Must not be null.
     */
    public void removeBlocksFromGroup(@NonNull PacketBlockGroup group, @NonNull List<Location> locations) {
        group.removeLocations(locations);

        for (Location location : locations) {
            ChunkPosition chunkPosition = ChunkPosition.of(location.getChunk());
            blockLocations.get(chunkPosition).remove(WorldPosition.ofBlock(location));
        }
    }

    /**
     * Removes a block from a specified group at a given location.
     *
     * @param group    The PacketBlockGroup from which the block will be removed. Must not be null.
     * @param location The Location of the block to be removed. Must not be null.
     */
    public void removeBlockFromGroup(@NonNull PacketBlockGroup group, @NonNull Location location) {
        group.removeLocation(location);

        ChunkPosition chunkPosition = ChunkPosition.of(location.getChunk());
        blockLocations.get(chunkPosition).remove(WorldPosition.ofBlock(location));
    }

    /**
     * Retrieves the block data for a specific player and location, if available.
     *
     * @param player   the player for whom the block data is retrieved; can be null if player-specific data is not required.
     * @param location the location of the block whose data is to be retrieved; must not be null.
     * @return an Optional containing the block data for the given player and location, or an empty Optional if no data is available.
     */
    public Optional<BlockData> getBlockData(@Nullable Player player, @NonNull Location location) {
        PacketBlockHolder<?, ?> packetBlock = getBlock(location).orElse(null);

        if (packetBlock == null) {
            return Optional.empty();
        }

        if (packetBlock instanceof PacketBlock singleBlock) {
            return Optional.of(singleBlock.getData(player));
        }

        if (!(packetBlock instanceof PacketBlockGroup group)) {
            return Optional.empty();
        }

        return group.getDataAt(player, location);
    }

    /**
     * Removes the specified {@link PacketBlock} from the blockLocations map and updates its visual
     * state for all associated viewers. If the {@link PacketBlock} exists in any chunk's block list,
     * it is removed from that list, and its associated viewers are sent a block update to reset the
     * affected block's state.
     *
     * @param packetBlock the {@link PacketBlock} to be removed; must not be null
     */
    public void removeBlock(@NonNull PacketBlockHolder<?, ?> packetBlock) {
        for (UUID uuid : new ArrayList<>(packetBlock.getViewers().keySet())) {
            Player player = Bukkit.getPlayer(uuid);

            if (player != null) {
                packetBlock.removeViewer(player);
            }
        }

        if(packetBlock instanceof PacketBlock singleBlock) {
            ChunkPosition chunk = singleBlock.getChunk();

            Map<WorldPosition, PacketBlockHolder<?, ?>> blocks = blockLocations.get(chunk);

            if (blocks == null) {
                return;
            }

            blocks.remove(singleBlock.getPosition());
            return;
        }

        if (!(packetBlock instanceof PacketBlockGroup group)) {
            return;
        }

        for (Map.Entry<ChunkPosition, List<WorldPosition>> entry : group.getChunkPositions().entrySet()) {
            ChunkPosition chunkPosition = entry.getKey();
            List<WorldPosition> worldPositions = entry.getValue();

            Map<WorldPosition, PacketBlockHolder<?, ?>> blocks = blockLocations.get(chunkPosition);

            if (blocks == null) {
                return;
            }

            for (WorldPosition worldPosition : worldPositions) {
                blocks.remove(worldPosition);
            }
        }
    }

    /**
     * Removes {@link PacketBlock} instances from the managed collection if they satisfy a specified condition.
     * The condition is defined by the provided {@link Predicate}.
     * Additionally, it schedules a visual update for players viewing the affected blocks.
     *
     * @param removePredicate the condition used to determine which {@link PacketBlock} instances should be removed;
     *                        must not be null.
     */
    public void removeIf(Predicate<PacketBlockHolder<?, ?>> removePredicate) {
        List<Map.Entry<ChunkPosition, Map<WorldPosition, PacketBlockHolder<?, ?>>>> chunkEntries =
                new ArrayList<>(blockLocations.entrySet());

        for (Map.Entry<ChunkPosition, Map<WorldPosition, PacketBlockHolder<?, ?>>> entry : chunkEntries) {
            Map<WorldPosition, PacketBlockHolder<?, ?>> blocks = entry.getValue();

            List<PacketBlockHolder<?, ?>> toRemove = new ArrayList<>();

            for (PacketBlockHolder<?, ?> block : blocks.values()) {
                if (removePredicate.test(block)) {
                    toRemove.add(block);
                }
            }

            for (PacketBlockHolder<?, ?> packetBlock : toRemove) {
                for (UUID uuid : new ArrayList<>(packetBlock.getViewers().keySet())) {
                    Player player = Bukkit.getPlayer(uuid);
                    if (player != null) {
                        Bukkit.getScheduler().runTaskLater(plugin, () -> packetBlock.removeViewer(player), 1);
                    }
                }

                blocks.values().remove(packetBlock);
            }

            blockLocations.put(entry.getKey(), blocks);
        }
    }

    /**
     * Retrieves an {@link Optional} of {@link PacketBlock} located at the specified {@link Location}.
     * If the provided location's world is null or no matching block exists, an empty {@link Optional} is returned.
     *
     * @param location the {@link Location} at which to find the {@link PacketBlock}; must not be null
     * @return an {@link Optional} containing the matching {@link PacketBlock}, or an empty {@link Optional} if none is found
     */
    public Optional<PacketBlockHolder<?, ?>> getBlock(@NonNull Location location) {
        World world = location.getWorld();

        if (world == null) {
            return Optional.empty();
        }

        Chunk chunk = location.getChunk();
        ChunkPosition chunkPosition = ChunkPosition.of(chunk);

        Map<WorldPosition, PacketBlockHolder<?, ?>> blocks = blockLocations.get(chunkPosition);

        if (blocks == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(blocks.get(WorldPosition.ofBlock(location)));
    }

    /**
     * Retrieves a list of {@link PacketBlock} instances associated with the specified world.
     * Only blocks that belong to the given world will be included in the returned list.
     *
     * @param world the world for which the packet blocks are being queried; must not be null
     * @return a list of {@link PacketBlock} instances that exist in the specified world
     */
    public List<PacketBlockHolder<?, ?>> getBlocks(@NonNull World world) {
        List<PacketBlockHolder<?, ?>> blocks = new ArrayList<>();

        blockLocations.values().forEach(packetBlocks -> {
            packetBlocks.values().forEach(block -> {
                if (!block.existsIn(world)) {
                    return;
                }

                blocks.add(block);
            });
        });

        return blocks;
    }

    /**
     * Retrieves a list of {@link PacketBlock} instances located within the specified chunk
     * in the given world. The chunk is identified by its X and Z coordinates.
     *
     * @param world the world in which the blocks are being queried; must not be null
     * @param chunkX the X-coordinate of the chunk
     * @param chunkZ the Z-coordinate of the chunk
     * @return a list of {@link PacketBlock} instances within the specified chunk,
     *         or an empty list if no blocks are found
     */
    public Map<WorldPosition, PacketBlockHolder<?, ?>> getBlocks(@NonNull World world, int chunkX, int chunkZ) {
        ChunkPosition chunkIdentifier = new ChunkPosition(world.getName(), chunkX, chunkZ);
        return blockLocations.getOrDefault(chunkIdentifier, new HashMap<>());
    }

    /**
     * Retrieves a list of {@link PacketBlock} instances that are visible to the specified player.
     * These blocks are determined based on the player's unique identifier and their visibility status.
     *
     * @param player the player for whom the visible blocks are being queried; must not be null
     * @return a list of {@link PacketBlock} instances that the specified player can view
     */
    public List<PacketBlockHolder<?, ?>> getBlocksByViewer(@NonNull Player player) {
        List<PacketBlockHolder<?, ?>> blocks = new ArrayList<>();

        blockLocations.forEach((identifier, value) -> {
            value.values().forEach(block -> {
                if (!block.getViewers().containsKey(player.getUniqueId())) {
                    return;
                }

                blocks.add(block);
            });
        });

        return blocks;
    }

    /**
     * Retrieves a list of {@link PacketBlock} instances that are visible to the specified player
     * and contain the specified metadata key.
     *
     * @param player the player whose visible blocks are being queried; must not be null.
     * @param metaKey the metadata key that the blocks must have to be included in the results; must not be null.
     * @return a list of {@link PacketBlock} instances that are both visible to the specified player
     *         and contain the specified metadata key.
     */
    public List<PacketBlockHolder<?, ?>> getBlocksByViewerWithMeta(@NonNull Player player, @NonNull String metaKey) {
        List<PacketBlockHolder<?, ?>> blocks = new ArrayList<>();

        blockLocations.forEach((identifier, value) -> {
            value.values().forEach(block -> {
                if (!block.getViewers().containsKey(player.getUniqueId()) || !block.hasMetadata(metaKey)) {
                    return;
                }

                blocks.add(block);
            });
        });

        return blocks;
    }

    /**
     * Retrieves a list of {@link PacketBlock} instances that contain the specified metadata key.
     *
     * @param key the metadata key to filter blocks by; must not be null.
     * @return a list of {@link PacketBlock} instances that contain the specified metadata key.
     */
    public List<PacketBlockHolder<?, ?>> getBlocksByMetadata(@NonNull String key) {
        List<PacketBlockHolder<?, ?>> blocks = new ArrayList<>();

        blockLocations.forEach((identifier, value) -> {
            value.values().forEach(block -> {
                if (!block.hasMetadata(key)) {
                    return;
                }

                blocks.add(block);
            });
        });

        return blocks;
    }

    /**
     * Retrieves a list of {@link PacketBlock} instances that intersect with the specified
     * bounding box within a given world.
     *
     * @param world the world in which the blocks are being queried; must not be null
     * @param boundingBox the bounding box used to filter the blocks; must not be null
     * @return a list of {@link PacketBlock} instances that overlap with the specified bounding box
     */
    public List<PacketBlockHolder<?, ?>> getHitBlocks(@NonNull World world, @NonNull BoundingBox boundingBox) {
        List<PacketBlockHolder<?, ?>> blocks = new ArrayList<>();

        getChunksInBoundingBox(world, boundingBox).forEach(chunk -> {
            getBlocks(world, chunk.getX(), chunk.getZ()).values().forEach(block -> {
                for (BoundingBox box : block.getBoundingBoxes()) {
                    if(!box.overlaps(boundingBox)) {
                        continue;
                    }

                    blocks.add(block);
                    break;
                }
            });
        });

        return blocks;
    }

    /**
     * Retrieves all chunks that intersect with the provided bounding box in the given world.
     *
     * @param world the world in which to search for chunks, must not be null
     * @param box the bounding box to define the search area, must not be null
     * @return a set of chunks that intersect with the specified bounding box
     */
    public Set<Chunk> getChunksInBoundingBox(World world, BoundingBox box) {
        Set<Chunk> chunks = new HashSet<>();

        int minChunkX = (int) Math.floor(box.getMinX()) >> 4;
        int maxChunkX = (int) Math.floor(box.getMaxX()) >> 4;
        int minChunkZ = (int) Math.floor(box.getMinZ()) >> 4;
        int maxChunkZ = (int) Math.floor(box.getMaxZ()) >> 4;

        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                Chunk chunk = world.getChunkAt(chunkX, chunkZ);
                chunks.add(chunk);
            }
        }

        return chunks;
    }

    /**
     * Retrieves a list of {@link PacketBlock} instances that are visible to the specified player
     * and overlap with the given bounding box.
     *
     * @param player the player whose visible blocks are being queried, must not be null
     * @param boundingBox the bounding box used to filter the blocks, must not be null
     * @return a list of {@link PacketBlock} instances that are both visible to the player and
     *         intersect with the specified bounding box
     */
    public List<PacketBlockHolder<?, ?>> getHitBlocksByViewer(@NonNull Player player, @NonNull BoundingBox boundingBox) {
        List<PacketBlockHolder<?, ?>> blocks = new ArrayList<>();

        blockLocations.forEach((identifier, value) -> {
            value.values().forEach(block -> {
                if (!block.getViewers().containsKey(player.getUniqueId())) {
                    return;
                }

                boolean hit = false;

                for (BoundingBox box : block.getBoundingBoxes()) {
                    if(!box.overlaps(boundingBox)) {
                        continue;
                    }

                    hit = true;
                }

                if (!hit) {
                    return;
                }

                blocks.add(block);
            });
        });

        return blocks;
    }

    /**
     * Retrieves a list of {@link PacketBlock} instances that are within a specified
     * bounding box, are visible to the specified player, and contain the given metadata key.
     *
     * @param player the player for whom the blocks' visibility is checked; must not be null.
     * @param boundingBox the bounding box defining the area to search for blocks; must not be null.
     * @param metaKey the metadata key that the blocks must have to be included in the results; must not be null.
     * @return a list of {@link PacketBlock} instances satisfying the conditions of being visible
     *         to the player, located within the bounding box, and containing the specified metadata key.
     */
    public List<PacketBlockHolder<?, ?>> getHitBlocksByViewerWithMeta(@NonNull Player player, @NonNull BoundingBox boundingBox, @NonNull String metaKey) {
        List<PacketBlockHolder<?, ?>> blocks = new ArrayList<>();

        blockLocations.forEach((identifier, value) -> {
            value.values().forEach(block -> {
                if (!block.getViewers().containsKey(player.getUniqueId())) {
                    return;
                }

                if (!block.hasMetadata(metaKey)) {
                    return;
                }

                boolean hit = false;

                for (BoundingBox box : block.getBoundingBoxes()) {
                    if(!box.overlaps(boundingBox)) {
                        continue;
                    }

                    hit = true;
                }

                if (!hit) {
                    return;
                }

                blocks.add(block);
            });
        });

        return blocks;
    }

    /**
     * Updates all visually modified blocks for the specified player in their visible region.
     * This method uses the player's current view to determine which blocks to update.
     *
     * @param player the player for whom the block updates should be performed; must not be null
     */
    public void updateBlocks(@NonNull Player player) {
        updateBlocksWithMeta(player, null);
    }

    /**
     * Updates the visual state of blocks for a specific player, optionally filtering
     * by metadata. If a metadata key is provided, only blocks associated with that
     * metadata will be updated. If the metadata key is null, all relevant blocks for
     * the player will be updated.
     *
     * @param player the player for whom the blocks' visual states are being updated
     * @param metaKey an optional metadata key used to filter the blocks being updated;
     *                if null, all relevant blocks are updated
     */
    public void updateBlocksWithMeta(@NonNull Player player, @Nullable String metaKey) {
        if (metaKey == null) {
            getBlocksByViewer(player).forEach(DataHolder::sendUpdates);
            return;
        }

        getBlocksByViewerWithMeta(player, metaKey).forEach(DataHolder::sendUpdates);
    }

}
