package net.bitbylogic.packetblocks.manager;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.bitbylogic.packetblocks.PacketBlocks;
import net.bitbylogic.packetblocks.data.PacketBlock;
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
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.logging.Level;

@RequiredArgsConstructor
@Getter
public class PacketBlockManager {

    private final ConcurrentHashMap<ChunkPosition, List<PacketBlock>> blockLocations = new ConcurrentHashMap<>();

    private final PacketBlocks plugin;

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

    public Optional<PacketBlock> getBlock(@NonNull Location location) {
        World world = location.getWorld();

        if (world == null) {
            return Optional.empty();
        }

        Chunk chunk = location.getChunk();
        return new ArrayList<>(getBlocks(world, chunk.getX(), chunk.getZ())).stream().filter(loc ->
                LocationUtil.matches(loc.getLocation().toBlockLocation(), location.toBlockLocation())).findFirst();
    }

    public List<PacketBlock> getBlocks(@NonNull World world) {
        List<PacketBlock> blocks = new ArrayList<>();

        blockLocations.values().forEach(packetBlocks -> {
            packetBlocks.forEach(block -> {
                World blockWorld = block.getLocation().getWorld();

                if (blockWorld == null || !blockWorld.getName().equalsIgnoreCase(world.getName())) {
                    return;
                }

                blocks.add(block);
            });
        });

        return blocks;
    }

    public List<PacketBlock> getBlocks(@NonNull World world, int chunkX, int chunkZ) {
        ChunkPosition chunkIdentifier = new ChunkPosition(world.getName(), chunkX, chunkZ);
        return blockLocations.getOrDefault(chunkIdentifier, new ArrayList<>());
    }

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

    public void updateBlocks(@NonNull Player player) {
        updateBlocksWithMeta(player, null);
    }

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

}
