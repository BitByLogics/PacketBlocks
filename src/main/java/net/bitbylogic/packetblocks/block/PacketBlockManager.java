package net.bitbylogic.packetblocks.block;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.bitbylogic.packetblocks.PacketBlocks;
import net.bitbylogic.packetblocks.structure.PacketBlockStructure;
import net.bitbylogic.utils.Pair;
import net.bitbylogic.utils.location.ChunkPosition;
import net.bitbylogic.utils.location.LocationUtil;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.logging.Level;

@RequiredArgsConstructor
@Getter
public class PacketBlockManager {

    private final ConcurrentHashMap<ChunkPosition, List<PacketBlock>> blockLocations = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, PacketBlockStructure> structures = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<ChunkPosition, Set<PacketBlockStructure>> structureChunkIndex = new ConcurrentHashMap<>();

    private final PacketBlocks plugin;

    /**
     * Creates a new {@link PacketBlock} instance at the specified location with the given block data.
     * The created block is registered within the internally managed collection, ensuring it is
     * appropriately tracked for further operations.
     *
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
            List<PacketBlock> blocks = blockLocations.get(identifier);

            if (blocks.contains(packetBlock)) {
                return packetBlock;
            }

            blocks.add(packetBlock);
            blockLocations.put(identifier, blocks);
            return packetBlock;
        }

        List<PacketBlock> newBlocks = new ArrayList<>();
        newBlocks.add(packetBlock);

        blockLocations.put(identifier, newBlocks);
        return packetBlock;
    }

    /**
     * Removes the specified {@link PacketBlock} from the blockLocations map and updates its visual
     * state for all associated viewers. If the {@link PacketBlock} exists in any chunk's block list,
     * it is removed from that list, and its associated viewers are sent a block update to reset the
     * affected block's state.
     *
     * @param packetBlock the {@link PacketBlock} to be removed; must not be null
     */
    public void removeBlock(@NonNull PacketBlock packetBlock) {
        for (Map.Entry<ChunkPosition, List<PacketBlock>> entry : blockLocations.entrySet()) {
            List<PacketBlock> blocks = entry.getValue();

            if (!blocks.contains(packetBlock)) {
                continue;
            }

            blocks.remove(packetBlock);

            blockLocations.put(entry.getKey(), blocks);
        }

        packetBlock.getViewers().keySet().forEach(uuid -> {
            Player player = Bukkit.getPlayer(uuid);

            if (player == null) {
                return;
            }

            player.sendBlockChange(packetBlock.getLocation(), packetBlock.getLocation().getBlock().getBlockData());
        });
    }

    /**
     * Removes {@link PacketBlock} instances from the managed collection if they satisfy a specified condition.
     * The condition is defined by the provided {@link Predicate}.
     * Additionally, it schedules a visual update for players viewing the affected blocks.
     *
     * @param removePredicate the condition used to determine which {@link PacketBlock} instances should be removed;
     *                        must not be null.
     */
    public void removeIf(Predicate<PacketBlock> removePredicate) {
        for (Map.Entry<ChunkPosition, List<PacketBlock>> entry : blockLocations.entrySet()) {
            List<PacketBlock> blocks = entry.getValue();

            blocks.stream().filter(removePredicate).forEach(packetBlock -> {
                packetBlock.getViewers().keySet().forEach(uuid -> {
                    Player player = Bukkit.getPlayer(uuid);

                    if (player == null) {
                        return;
                    }

                    Bukkit.getScheduler().runTaskLater(plugin, () -> player.sendBlockChange(packetBlock.getLocation(), packetBlock.getLocation().getBlock().getBlockData()), 1);
                });
            });

            blocks.removeIf(removePredicate);
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
    public Optional<PacketBlock> getBlock(@NonNull Location location) {
        World world = location.getWorld();

        if (world == null) {
            return Optional.empty();
        }

        Chunk chunk = location.getChunk();
        return new ArrayList<>(getBlocks(world, chunk.getX(), chunk.getZ())).stream()
                .filter(loc -> loc != null && loc.getLocation() != null && LocationUtil.matches(loc.getLocation().toBlockLocation(), location.toBlockLocation()))
                .findFirst();
    }

    /**
     * Retrieves a list of {@link PacketBlock} instances associated with the specified world.
     * Only blocks that belong to the given world will be included in the returned list.
     *
     * @param world the world for which the packet blocks are being queried; must not be null
     * @return a list of {@link PacketBlock} instances that exist in the specified world
     */
    public List<PacketBlock> getBlocks(@NonNull World world) {
        List<PacketBlock> blocks = new ArrayList<>();

        blockLocations.values().forEach(packetBlocks -> {
            packetBlocks.forEach(block -> {
                if(block == null || block.getLocation() == null) {
                    return;
                }

                World blockWorld = block.getLocation().getWorld();

                if (blockWorld == null || !blockWorld.getName().equalsIgnoreCase(world.getName())) {
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
    public List<PacketBlock> getBlocks(@NonNull World world, int chunkX, int chunkZ) {
        ChunkPosition chunkIdentifier = new ChunkPosition(world.getName(), chunkX, chunkZ);
        return blockLocations.getOrDefault(chunkIdentifier, new ArrayList<>());
    }

    /**
     * Retrieves a list of {@link PacketBlock} instances that are visible to the specified player.
     * These blocks are determined based on the player's unique identifier and their visibility status.
     *
     * @param player the player for whom the visible blocks are being queried; must not be null
     * @return a list of {@link PacketBlock} instances that the specified player can view
     */
    public List<PacketBlock> getBlocksByViewer(@NonNull Player player) {
        List<PacketBlock> blocks = new ArrayList<>();

        blockLocations.forEach((identifier, value) -> {
            value.forEach(block -> {
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
    public List<PacketBlock> getBlocksByViewerWithMeta(@NonNull Player player, @NonNull String metaKey) {
        List<PacketBlock> blocks = new ArrayList<>();

        blockLocations.forEach((identifier, value) -> {
            value.forEach(block -> {
                if (!block.getViewers().containsKey(player.getUniqueId()) || !block.getMetadata().containsKey(metaKey)) {
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
    public List<PacketBlock> getBlocksByMetadata(@NonNull String key) {
        List<PacketBlock> blocks = new ArrayList<>();

        blockLocations.forEach((identifier, value) -> {
            value.forEach(block -> {
                if (!block.getMetadata().containsKey(key)) {
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
    public List<PacketBlock> getHitBlocks(@NonNull World world, @NonNull BoundingBox boundingBox) {
        List<PacketBlock> blocks = new ArrayList<>();

        getChunksInBoundingBox(world, boundingBox).forEach(chunk -> {
            getBlocks(world, chunk.getX(), chunk.getZ()).forEach(block -> {
                if (!block.getBoundingBox().overlaps(boundingBox)) {
                    return;
                }

                blocks.add(block);
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

        // Calculate the chunk coordinates for the bounding box's min and max corners
        int minChunkX = (int) Math.floor(box.getMinX()) >> 4;
        int maxChunkX = (int) Math.floor(box.getMaxX()) >> 4;
        int minChunkZ = (int) Math.floor(box.getMinZ()) >> 4;
        int maxChunkZ = (int) Math.floor(box.getMaxZ()) >> 4;

        // Iterate through all chunk coordinates in this range
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                // Add the chunk at (chunkX, chunkZ) to the set
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
    public List<PacketBlock> getHitBlocksByViewer(@NonNull Player player, @NonNull BoundingBox boundingBox) {
        List<PacketBlock> blocks = new ArrayList<>();

        blockLocations.forEach((identifier, value) -> {
            value.forEach(block -> {
                if (!block.getViewers().containsKey(player.getUniqueId())) {
                    return;
                }

                BoundingBox box = BoundingBox.of(block.getLocation().clone(), block.getLocation().clone().add(1, 1, 1));

                if (!box.overlaps(boundingBox)) {
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
    public List<PacketBlock> getHitBlocksByViewerWithMeta(@NonNull Player player, @NonNull BoundingBox boundingBox, @NonNull String metaKey) {
        List<PacketBlock> blocks = new ArrayList<>();

        blockLocations.forEach((identifier, value) -> {
            value.forEach(block -> {
                if (!block.getViewers().containsKey(player.getUniqueId())) {
                    return;
                }

                if (!block.getMetadata().containsKey(metaKey)) {
                    return;
                }

                BoundingBox box = BoundingBox.of(block.getLocation().clone(), block.getLocation().clone().add(1, 1, 1));

                if (!box.overlaps(boundingBox)) {
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
        Set<BlockState> states = new HashSet<>();

        if (metaKey == null) {
            getBlocksByViewer(player).forEach(packetBlock -> states.add(packetBlock.getBlockState(player)));
            player.sendBlockChanges(states);
            return;
        }

        getBlocksByViewerWithMeta(player, metaKey).forEach(packetBlock -> states.add(packetBlock.getBlockState(player)));
        player.sendBlockChanges(states);
    }

    /**
     * Creates a new {@link PacketBlockStructure} instance with the specified origin location.
     * The created structure is registered within the internally managed collection.
     *
     * @param origin the origin location for the structure; must not be null
     * @return the newly created {@link PacketBlockStructure} instance,
     *         or null if the origin's world is null
     */
    public PacketBlockStructure createStructure(@NonNull Location origin) {
        World world = origin.getWorld();

        if (world == null) {
            plugin.getLogger().log(Level.WARNING, "Unable to create packet block structure, null world!: " + origin.toString());
            return null;
        }

        PacketBlockStructure structure = new PacketBlockStructure(origin, this);
        structures.put(structure.getId(), structure);
        return structure;
    }

    /**
     * Removes the specified {@link PacketBlockStructure} from the structures collection
     * and clears all associated blocks.
     *
     * @param structure the {@link PacketBlockStructure} to be removed; must not be null
     */
    public void removeStructure(@NonNull PacketBlockStructure structure) {
        structures.remove(structure.getId());
        unindexStructureChunks(structure);
        structure.clear();
    }

    /**
     * Removes the {@link PacketBlockStructure} with the specified ID from the structures collection.
     *
     * @param structureId the UUID of the structure to be removed; must not be null
     */
    public void removeStructure(@NonNull UUID structureId) {
        PacketBlockStructure structure = structures.remove(structureId);
        if (structure != null) {
            unindexStructureChunks(structure);
            structure.clear();
        }
    }

    /**
     * Indexes a structure for a specific chunk. Called by PacketBlockStructure when blocks are added.
     *
     * @param structure the structure to index
     * @param chunkX the chunk X coordinate
     * @param chunkZ the chunk Z coordinate
     */
    public void indexStructureChunk(@NonNull PacketBlockStructure structure, int chunkX, int chunkZ) {
        ChunkPosition pos = new ChunkPosition(structure.getWorld().getName(), chunkX, chunkZ);
        structureChunkIndex.computeIfAbsent(pos, k -> ConcurrentHashMap.newKeySet()).add(structure);
    }

    /**
     * Removes a structure's index for a specific chunk. Called by PacketBlockStructure when chunks become empty.
     *
     * @param structure the structure to unindex
     * @param chunkX the chunk X coordinate
     * @param chunkZ the chunk Z coordinate
     */
    public void unindexStructureChunk(@NonNull PacketBlockStructure structure, int chunkX, int chunkZ) {
        ChunkPosition pos = new ChunkPosition(structure.getWorld().getName(), chunkX, chunkZ);
        Set<PacketBlockStructure> set = structureChunkIndex.get(pos);
        if (set != null) {
            set.remove(structure);
            if (set.isEmpty()) {
                structureChunkIndex.remove(pos);
            }
        }
    }

    /**
     * Removes all chunk index entries for a structure. Called when removing a structure.
     *
     * @param structure the structure to fully unindex
     */
    private void unindexStructureChunks(@NonNull PacketBlockStructure structure) {
        structure.getOccupiedChunks().forEach(chunkKey ->
                unindexStructureChunk(structure, chunkKey.getKey(), chunkKey.getValue())
        );
    }

    /**
     * Retrieves an {@link Optional} of {@link PacketBlockStructure} by its ID.
     *
     * @param structureId the UUID of the structure to find; must not be null
     * @return an {@link Optional} containing the matching structure, or empty if not found
     */
    public Optional<PacketBlockStructure> getStructure(@NonNull UUID structureId) {
        return Optional.ofNullable(structures.get(structureId));
    }

    /**
     * Retrieves all registered structures.
     *
     * @return an unmodifiable collection of all {@link PacketBlockStructure} instances
     */
    public Collection<PacketBlockStructure> getStructures() {
        return Collections.unmodifiableCollection(structures.values());
    }

    /**
     * Retrieves all structures that have blocks in the specified chunk.
     * Uses spatial index for O(1) chunk lookup.
     *
     * @param world the world to query; must not be null
     * @param chunkX the X-coordinate of the chunk
     * @param chunkZ the Z-coordinate of the chunk
     * @return a list of structures that have blocks in the specified chunk
     */
    public List<PacketBlockStructure> getStructuresInChunk(@NonNull World world, int chunkX, int chunkZ) {
        ChunkPosition pos = new ChunkPosition(world.getName(), chunkX, chunkZ);
        Set<PacketBlockStructure> set = structureChunkIndex.get(pos);
        return set == null ? Collections.emptyList() : new ArrayList<>(set);
    }

    /**
     * Finds a structure that contains a block at the specified location.
     * Uses chunk-based spatial index for efficient lookup.
     *
     * @param location the location to search for; must not be null
     * @return an {@link Optional} containing a Pair of the structure and relative Vector,
     *         or empty if no structure contains a block at that location
     */
    public Optional<Pair<PacketBlockStructure, Vector>> getStructureBlockAt(@NonNull Location location) {
        World world = location.getWorld();

        if (world == null) {
            return Optional.empty();
        }

        int chunkX = location.getBlockX() >> 4;
        int chunkZ = location.getBlockZ() >> 4;

        for (PacketBlockStructure structure : getStructuresInChunk(world, chunkX, chunkZ)) {
            if (structure.containsLocation(location)) {
                Vector relative = structure.toRelativeVector(location);
                return Optional.of(new Pair<>(structure, relative));
            }
        }

        return Optional.empty();
    }

    /**
     * Checks if a location contains either a PacketBlock or a block from a structure.
     * First checks individual blocks, then checks structures.
     *
     * @param location the location to check; must not be null
     * @return true if either a PacketBlock or structure block exists at the location
     */
    public boolean hasBlockAt(@NonNull Location location) {
        if (getBlock(location).isPresent()) {
            return true;
        }
        return getStructureBlockAt(location).isPresent();
    }

    /**
     * Retrieves the BlockData at a location, checking both individual blocks and structures.
     *
     * @param location the location to check; must not be null
     * @param player the player for whom to get the block data; must not be null
     * @return an {@link Optional} containing the BlockData, or empty if no block exists
     */
    public Optional<BlockData> getBlockDataAt(@NonNull Location location, @NonNull Player player) {
        Optional<PacketBlock> packetBlock = getBlock(location);
        if (packetBlock.isPresent()) {
            return Optional.of(packetBlock.get().getBlockState(player).getBlockData());
        }

        Optional<Pair<PacketBlockStructure, Vector>> structureBlock = getStructureBlockAt(location);
        if (structureBlock.isPresent()) {
            PacketBlockStructure structure = structureBlock.get().getKey();
            Vector relativePos = structureBlock.get().getValue();
            return Optional.of(structure.getBlockData(player, relativePos));
        }

        return Optional.empty();
    }

}