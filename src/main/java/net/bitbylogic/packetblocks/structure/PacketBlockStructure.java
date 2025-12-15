package net.bitbylogic.packetblocks.structure;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerMultiBlockChange;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerMultiBlockChange.EncodedBlock;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import net.bitbylogic.packetblocks.block.PacketBlockManager;
import net.bitbylogic.packetblocks.block.PacketBlockPlayerData;
import net.bitbylogic.packetblocks.event.PacketStructureBreakEvent;
import net.bitbylogic.utils.Pair;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.function.Supplier;

@Getter
public class PacketBlockStructure {

    private final UUID id;
    private final Location origin;
    private final World world;

    @Nullable
    private final PacketBlockManager manager;

    private final ConcurrentHashMap<Long, BlockData> blocks;
    private final ConcurrentHashMap<Long, Set<Long>> chunkIndex;

    private final Set<Predicate<Player>> viewConditions;
    private final HashMap<UUID, PacketBlockPlayerData> viewers;
    private final HashMap<String, Object> metadata;

    @Setter
    private int breakSpeed = -1;

    @Setter
    private boolean addViewerOnJoin;

    @Setter
    private boolean globalBreakAnimation;

    public PacketBlockStructure(@NonNull Location origin, @Nullable PacketBlockManager manager) {
        this.id = UUID.randomUUID();
        this.origin = origin.clone();
        this.world = origin.getWorld();
        this.manager = manager;

        this.blocks = new ConcurrentHashMap<>();
        this.chunkIndex = new ConcurrentHashMap<>();

        this.viewConditions = new HashSet<>();
        this.viewers = new HashMap<>();
        this.metadata = new HashMap<>();
    }

    private static long packBlockKey(int x, int y, int z) {
        return ((long) x & 0x3FFFFF) | (((long) z & 0x3FFFFF) << 22) | (((long) y & 0xFFFFF) << 44);
    }

    private static int unpackX(long key) {
        int val = (int) (key & 0x3FFFFF);
        return (val << 10) >> 10;
    }

    private static int unpackZ(long key) {
        int val = (int) ((key >> 22) & 0x3FFFFF);
        return (val << 10) >> 10;
    }

    private static int unpackY(long key) {
        int val = (int) ((key >> 44) & 0xFFFFF);
        return (val << 12) >> 12;
    }

    private static long packChunkKey(int chunkX, int chunkZ) {
        return ((long) chunkX & 0xFFFFFFFF) | (((long) chunkZ & 0xFFFFFFFF) << 32);
    }

    private static int unpackChunkX(long key) {
        return (int) key;
    }

    private static int unpackChunkZ(long key) {
        return (int) (key >> 32);
    }

    private static long packSectionKey(int sectionX, int sectionY, int sectionZ) {
        return ((long) sectionX & 0x3FFFFF) | (((long) sectionZ & 0x3FFFFF) << 22) | (((long) sectionY & 0xFFFFF) << 44);
    }

    private static int unpackSectionX(long key) {
        int val = (int) (key & 0x3FFFFF);
        return (val << 10) >> 10;
    }

    private static int unpackSectionZ(long key) {
        int val = (int) ((key >> 22) & 0x3FFFFF);
        return (val << 10) >> 10;
    }

    private static int unpackSectionY(long key) {
        int val = (int) ((key >> 44) & 0xFFFFF);
        return (val << 12) >> 12;
    }

    public void setBlock(int relX, int relY, int relZ, @NonNull BlockData blockData) {
        long blockKey = packBlockKey(relX, relY, relZ);
        blocks.put(blockKey, blockData);

        int absX = origin.getBlockX() + relX;
        int absZ = origin.getBlockZ() + relZ;
        int chunkX = absX >> 4;
        int chunkZ = absZ >> 4;
        long chunkKey = packChunkKey(chunkX, chunkZ);

        boolean wasEmpty = !chunkIndex.containsKey(chunkKey) || chunkIndex.get(chunkKey).isEmpty();
        chunkIndex.computeIfAbsent(chunkKey, k -> ConcurrentHashMap.newKeySet()).add(blockKey);

        if (wasEmpty && manager != null) {
            manager.indexStructureChunk(this, chunkX, chunkZ);
        }
    }

    public void setBlock(@NonNull Vector relativePos, @NonNull BlockData blockData) {
        setBlock(relativePos.getBlockX(), relativePos.getBlockY(), relativePos.getBlockZ(), blockData);
    }

    public void removeBlock(int relX, int relY, int relZ) {
        long blockKey = packBlockKey(relX, relY, relZ);

        if (!blocks.containsKey(blockKey)) {
            return;
        }

        blocks.remove(blockKey);

        int absX = origin.getBlockX() + relX;
        int absZ = origin.getBlockZ() + relZ;
        int chunkX = absX >> 4;
        int chunkZ = absZ >> 4;
        long chunkKey = packChunkKey(chunkX, chunkZ);

        Set<Long> chunkBlocks = chunkIndex.get(chunkKey);
        if (chunkBlocks != null) {
            chunkBlocks.remove(blockKey);
            if (chunkBlocks.isEmpty()) {
                chunkIndex.remove(chunkKey);
                if (manager != null) {
                    manager.unindexStructureChunk(this, chunkX, chunkZ);
                }
            }
        }
    }

    public void removeBlock(@NonNull Vector relativePos) {
        removeBlock(relativePos.getBlockX(), relativePos.getBlockY(), relativePos.getBlockZ());
    }

    public Optional<BlockData> getBlock(int relX, int relY, int relZ) {
        return Optional.ofNullable(blocks.get(packBlockKey(relX, relY, relZ)));
    }

    public Optional<BlockData> getBlock(@NonNull Vector relativePos) {
        return getBlock(relativePos.getBlockX(), relativePos.getBlockY(), relativePos.getBlockZ());
    }

    public Optional<BlockData> getBlockAt(@NonNull Location location) {
        int relX = location.getBlockX() - origin.getBlockX();
        int relY = location.getBlockY() - origin.getBlockY();
        int relZ = location.getBlockZ() - origin.getBlockZ();
        return getBlock(relX, relY, relZ);
    }

    public void getAbsoluteLocation(int relX, int relY, int relZ, @NonNull Location dest) {
        dest.setWorld(world);
        dest.setX(origin.getBlockX() + relX);
        dest.setY(origin.getBlockY() + relY);
        dest.setZ(origin.getBlockZ() + relZ);
    }

    public Location getAbsoluteLocation(int relX, int relY, int relZ) {
        return new Location(world, origin.getBlockX() + relX, origin.getBlockY() + relY, origin.getBlockZ() + relZ);
    }

    public Location getAbsoluteLocation(@NonNull Vector relativePos) {
        return getAbsoluteLocation(relativePos.getBlockX(), relativePos.getBlockY(), relativePos.getBlockZ());
    }

    public Vector toRelativeVector(@NonNull Location location) {
        return new Vector(
                location.getBlockX() - origin.getBlockX(),
                location.getBlockY() - origin.getBlockY(),
                location.getBlockZ() - origin.getBlockZ()
        );
    }

    public boolean containsBlock(int relX, int relY, int relZ) {
        return blocks.containsKey(packBlockKey(relX, relY, relZ));
    }

    public boolean containsLocation(@NonNull Location location) {
        if (!location.getWorld().equals(world)) {
            return false;
        }
        int relX = location.getBlockX() - origin.getBlockX();
        int relY = location.getBlockY() - origin.getBlockY();
        int relZ = location.getBlockZ() - origin.getBlockZ();
        return containsBlock(relX, relY, relZ);
    }

    public void forEachBlockInChunk(int chunkX, int chunkZ, @NonNull BlockConsumer consumer) {
        long chunkKey = packChunkKey(chunkX, chunkZ);
        Set<Long> blockKeys = chunkIndex.get(chunkKey);
        if (blockKeys == null) return;

        for (Long blockKey : blockKeys) {
            int relX = unpackX(blockKey);
            int relY = unpackY(blockKey);
            int relZ = unpackZ(blockKey);
            BlockData data = blocks.get(blockKey);
            if (data != null) {
                consumer.accept(relX, relY, relZ, data);
            }
        }
    }

    public boolean hasBlocksInChunk(int chunkX, int chunkZ) {
        long chunkKey = packChunkKey(chunkX, chunkZ);
        Set<Long> blockKeys = chunkIndex.get(chunkKey);
        return blockKeys != null && !blockKeys.isEmpty();
    }

    public Set<Pair<Integer, Integer>> getOccupiedChunks() {
        Set<Pair<Integer, Integer>> result = new HashSet<>();
        for (Long chunkKey : chunkIndex.keySet()) {
            result.add(new Pair<>(unpackChunkX(chunkKey), unpackChunkZ(chunkKey)));
        }
        return result;
    }

    public boolean canView(@NonNull Player player) {
        return viewConditions.stream().allMatch(viewRequirement -> viewRequirement.test(player));
    }

    public boolean isViewer(@NonNull Player player) {
        return viewers.containsKey(player.getUniqueId());
    }

    public Optional<PacketBlockPlayerData> getViewer(@NonNull Player player) {
        return Optional.ofNullable(viewers.get(player.getUniqueId()));
    }

    public Optional<PacketBlockPlayerData> attemptAddViewer(@NonNull Player player, boolean sendUpdate) {
        if (!canView(player)) {
            return Optional.empty();
        }

        if (isViewer(player)) {
            return getViewer(player);
        }

        PacketBlockPlayerData playerData = new PacketBlockPlayerData(null, () -> null, breakSpeed);
        viewers.put(player.getUniqueId(), playerData);

        if (sendUpdate) {
            sendUpdates(player);
        }

        return Optional.of(playerData);
    }

    public PacketBlockPlayerData addViewer(@NonNull Player player) {
        PacketBlockPlayerData playerData = new PacketBlockPlayerData(null, () -> null, breakSpeed);
        viewers.put(player.getUniqueId(), playerData);
        return playerData;
    }

    public PacketBlockPlayerData addAndUpdateViewer(@NonNull Player player) {
        PacketBlockPlayerData playerData = addViewer(player);
        sendUpdates(player);
        return playerData;
    }

    public void removeViewer(@NonNull Player player) {
        if (!viewers.containsKey(player.getUniqueId())) {
            return;
        }

        viewers.remove(player.getUniqueId());
        sendRealBlockUpdates(player);
    }

    private void sendRealBlockUpdates(@NonNull Player player) {
        Map<Long, List<EncodedBlock>> sectionBlocks = new HashMap<>();

        blocks.keySet().forEach(blockKey -> {
            int relX = unpackX(blockKey);
            int relY = unpackY(blockKey);
            int relZ = unpackZ(blockKey);

            int absX = origin.getBlockX() + relX;
            int absY = origin.getBlockY() + relY;
            int absZ = origin.getBlockZ() + relZ;

            Location loc = new Location(world, absX, absY, absZ);
            BlockData realData = loc.getBlock().getBlockData();

            long sectionKey = packSectionKey(absX >> 4, absY >> 4, absZ >> 4);
            WrappedBlockState wrappedState = WrappedBlockState.getByString(realData.getAsString());

            sectionBlocks.computeIfAbsent(sectionKey, k -> new ArrayList<>())
                    .add(new EncodedBlock(wrappedState, absX, absY, absZ));
        });

        sectionBlocks.forEach((sectionKey, encodedBlocks) -> {
            int sectionX = unpackSectionX(sectionKey);
            int sectionY = unpackSectionY(sectionKey);
            int sectionZ = unpackSectionZ(sectionKey);

            Vector3i chunkPos = new Vector3i(sectionX, sectionY, sectionZ);
            WrapperPlayServerMultiBlockChange packet = new WrapperPlayServerMultiBlockChange(
                    chunkPos, false, encodedBlocks.toArray(new EncodedBlock[0])
            );
            PacketEvents.getAPI().getPlayerManager().sendPacket(player, packet);
        });
    }

    public void sendUpdates(@NonNull Player player) {
        if (!isViewer(player)) {
            return;
        }

        Map<Long, List<EncodedBlock>> sectionBlocks = new HashMap<>();

        blocks.forEach((blockKey, blockData) -> {
            int relX = unpackX(blockKey);
            int relY = unpackY(blockKey);
            int relZ = unpackZ(blockKey);

            int absX = origin.getBlockX() + relX;
            int absY = origin.getBlockY() + relY;
            int absZ = origin.getBlockZ() + relZ;

            long sectionKey = packSectionKey(absX >> 4, absY >> 4, absZ >> 4);
            BlockData data = getBlockData(player, relX, relY, relZ);
            WrappedBlockState wrappedState = WrappedBlockState.getByString(data.getAsString());

            sectionBlocks.computeIfAbsent(sectionKey, k -> new ArrayList<>())
                    .add(new EncodedBlock(wrappedState, absX, absY, absZ));
        });

        sectionBlocks.forEach((sectionKey, encodedBlocks) -> {
            int sectionX = unpackSectionX(sectionKey);
            int sectionY = unpackSectionY(sectionKey);
            int sectionZ = unpackSectionZ(sectionKey);

            Vector3i chunkPos = new Vector3i(sectionX, sectionY, sectionZ);
            WrapperPlayServerMultiBlockChange packet = new WrapperPlayServerMultiBlockChange(
                    chunkPos, false, encodedBlocks.toArray(new EncodedBlock[0])
            );
            PacketEvents.getAPI().getPlayerManager().sendPacket(player, packet);
        });
    }

    public void sendUpdates() {
        Iterator<UUID> viewerIterator = viewers.keySet().iterator();

        while (viewerIterator.hasNext()) {
            Player viewer = Bukkit.getPlayer(viewerIterator.next());

            if (viewer == null) {
                viewerIterator.remove();
                continue;
            }

            sendUpdates(viewer);
        }
    }

    public void sendChunkUpdates(@NonNull Player player, int chunkX, int chunkZ) {
        if (!isViewer(player)) {
            return;
        }

        Map<Long, List<EncodedBlock>> sectionBlocks = new HashMap<>();

        forEachBlockInChunk(chunkX, chunkZ, (relX, relY, relZ, data) -> {
            int absX = origin.getBlockX() + relX;
            int absY = origin.getBlockY() + relY;
            int absZ = origin.getBlockZ() + relZ;

            long sectionKey = packSectionKey(absX >> 4, absY >> 4, absZ >> 4);
            BlockData blockData = getBlockData(player, relX, relY, relZ);
            WrappedBlockState wrappedState = WrappedBlockState.getByString(blockData.getAsString());

            sectionBlocks.computeIfAbsent(sectionKey, k -> new ArrayList<>())
                    .add(new EncodedBlock(wrappedState, absX, absY, absZ));
        });

        sectionBlocks.forEach((sectionKey, encodedBlocks) -> {
            int sectionX = unpackSectionX(sectionKey);
            int sectionY = unpackSectionY(sectionKey);
            int sectionZ = unpackSectionZ(sectionKey);

            Vector3i chunkPos = new Vector3i(sectionX, sectionY, sectionZ);
            WrapperPlayServerMultiBlockChange packet = new WrapperPlayServerMultiBlockChange(
                    chunkPos, false, encodedBlocks.toArray(new EncodedBlock[0])
            );
            PacketEvents.getAPI().getPlayerManager().sendPacket(player, packet);
        });
    }

    public BlockData getBlockData(@NonNull Player player, int relX, int relY, int relZ) {
        return getViewer(player)
                .map(playerData -> {
                    BlockData pd = playerData.getBlockData();
                    if (pd != null) return pd;
                    Supplier<BlockData> supplier = playerData.getBlockDataSupplier();
                    if (supplier != null) {
                        BlockData supplied = supplier.get();
                        if (supplied != null) return supplied;
                    }
                    return blocks.get(packBlockKey(relX, relY, relZ));
                })
                .orElse(blocks.get(packBlockKey(relX, relY, relZ)));
    }

    public BlockData getBlockData(@NonNull Player player, @NonNull Vector relativePos) {
        return getBlockData(player, relativePos.getBlockX(), relativePos.getBlockY(), relativePos.getBlockZ());
    }

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

    public void addViewCondition(@NonNull Predicate<Player> condition) {
        if (viewConditions.contains(condition)) {
            return;
        }
        viewConditions.add(condition);
    }

    public int getBreakSpeed(@NonNull Player player) {
        return getViewer(player).map(PacketBlockPlayerData::getBreakSpeed).orElse(breakSpeed);
    }

    public int getBlockCount() {
        return blocks.size();
    }

    /**
     * Simulates a block break at the specified relative position within this structure.
     *
     * <p>This method fires a {@link PacketStructureBreakEvent} and handles the break logic:
     * <ul>
     *   <li>If the event is cancelled, resends the block update to the player to cancel the break visual.</li>
     *   <li>If dropItems is true, calculates and drops items from the BlockData at the absolute location.</li>
     * </ul>
     *
     * <p><b>Important:</b> This method does NOT automatically remove the block from the structure.
     * The event listener should decide whether to call {@code removeBlock()} based on their requirements.
     *
     * @param player the player who is breaking the block; must not be null
     * @param tool the tool used to break the block; may be null
     * @param relativePos the relative position of the block within the structure; must not be null
     */
    public void simulateBreak(@NonNull Player player, @Nullable ItemStack tool, @NonNull Vector relativePos) {
        int relX = relativePos.getBlockX();
        int relY = relativePos.getBlockY();
        int relZ = relativePos.getBlockZ();

        Optional<BlockData> blockDataOpt = getBlock(relX, relY, relZ);
        if (blockDataOpt.isEmpty()) {
            return;
        }

        Location absoluteLocation = getAbsoluteLocation(relX, relY, relZ);
        BlockData blockData = blockDataOpt.get();

        PacketStructureBreakEvent breakEvent = new PacketStructureBreakEvent(
                player, this, relativePos.clone(), absoluteLocation, tool
        );
        Bukkit.getPluginManager().callEvent(breakEvent);

        if (breakEvent.isCancelled()) {
            if (breakEvent.isUpdate()) {
                player.sendBlockChange(absoluteLocation, getBlockData(player, relX, relY, relZ));
            }
            return;
        }

        if (breakEvent.isDropItems()) {
            absoluteLocation.getBlock().setBlockData(blockData);
            absoluteLocation.getBlock()
                    .getDrops(tool, player)
                    .forEach(drop -> world.dropItemNaturally(absoluteLocation.clone().add(0.5, 0.5, 0.5), drop));
            absoluteLocation.getBlock().setBlockData(world.getBlockAt(absoluteLocation).getBlockData());
        }
    }

    public void clear() {
        viewers.keySet().forEach(uuid -> {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) return;
            sendRealBlockUpdates(player);
        });

        blocks.clear();
        chunkIndex.clear();
        viewers.clear();
    }

    @FunctionalInterface
    public interface BlockConsumer {
        void accept(int relX, int relY, int relZ, BlockData blockData);
    }

}
