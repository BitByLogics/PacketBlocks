package net.bitbylogic.packetblocks.data;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import net.bitbylogic.packetblocks.PacketBlocks;
import net.bitbylogic.packetblocks.event.PacketBlockBreakEvent;
import net.bitbylogic.utils.Pair;
import net.bitbylogic.utils.condition.parsed.ParsedCondition;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
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

    private final World world;
    private final Location location;
    private final Pair<Integer, Integer> chunk;
    private final BoundingBox boundingBox;

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

    public PacketBlock(@NonNull Location location, @NonNull BlockData blockData) {
        this.world = location.getWorld();
        this.location = location;
        this.chunk = new Pair<>(location.getChunk().getX(), location.getChunk().getZ());
        this.boundingBox = BoundingBox.of(location.clone(), location.clone().add(1, 1, 1));
        this.blockData = blockData;

        this.viewConditions = new HashSet<>();
        this.viewers = new HashMap<>();
        this.metadata = new HashMap<>();
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

    /**
     * Attempts to add a viewer to this Packet Block.
     * This will check to see that the Player matches all
     * {@link ParsedCondition}'s.
     * The {@link Optional} will be empty if the player doesn't
     * match one of the view requirements.
     *
     * @param player The player to attempt to add as a viewer
     * @param sendUpdate Whether to send a block update packet to the Player
     * @return An {@link Optional} that is either empty or contains {@link PacketBlockPlayerData}
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
     * Add a viewer to this Packet Block.
     * NOTE: This bypasses any view requirements that are set.
     *
     * @param player The player to add as a viewer
     * @return The {@link PacketBlockPlayerData} instance
     */
    public PacketBlockPlayerData addViewer(@NonNull Player player) {
        PacketBlockPlayerData playerData = new PacketBlockPlayerData(null, () -> blockData, breakSpeed);
        viewers.put(player.getUniqueId(), playerData);
        return playerData;
    }

    public PacketBlockPlayerData addAndUpdateViewer(@NonNull Player player) {
        PacketBlockPlayerData playerData = addViewer(player);
        sendUpdate(player);
        return playerData;
    }

    public PacketBlockPlayerData addViewer(@NonNull Player player, @NonNull Supplier<BlockData> blockDataSupplier) {
        PacketBlockPlayerData playerData = new PacketBlockPlayerData(null, blockDataSupplier, breakSpeed);
        viewers.put(player.getUniqueId(), playerData);

        return playerData;
    }

    public PacketBlockPlayerData addAndUpdateViewer(@NonNull Player player, @NonNull Supplier<BlockData> blockDataSupplier) {
        PacketBlockPlayerData playerData = addViewer(player, blockDataSupplier);
        sendUpdate(player);

        return playerData;
    }

    public void removeViewer(@NonNull Player player) {
        if(!viewers.containsKey(player.getUniqueId())) {
            return;
        }

        viewers.remove(player.getUniqueId());
        player.sendBlockChange(location, location.getBlock().getBlockData());
    }

    public void setBlockDataForAll(@NonNull BlockData blockData) {
        this.blockData = blockData;
        viewers.values().forEach(playerData -> playerData.setBlockData(blockData));
        sendUpdates();
    }

    public void setBlockDataSupplierForAll(@NonNull BlockData blockData) {
        this.blockData = blockData;
        viewers.values().forEach(playerData -> playerData.setBlockDataSupplier(() -> blockData));
        sendUpdates();
    }

    public void setBlockData(@NonNull Player player, @Nullable BlockData blockData) {
        Optional<PacketBlockPlayerData> optionalPlayerData = getViewer(player);

        if (optionalPlayerData.isEmpty()) {
            return;
        }

        optionalPlayerData.get().setBlockData(blockData);
    }

    public void setBlockDataSupplier(@NonNull Player player, @NonNull BlockData blockData) {
        Optional<PacketBlockPlayerData> optionalPlayerData = getViewer(player);

        if (optionalPlayerData.isEmpty()) {
            return;
        }

        optionalPlayerData.get().setBlockDataSupplier(() -> blockData);
    }

    public void setBlockDataAndUpdate(@NonNull Player player, @Nullable BlockData blockData) {
        setBlockData(player, blockData);
        sendUpdate(player);
    }

    public BlockState getBlockState(@NonNull Player player) {
        return getViewer(player)
                .map(playerData -> playerData.getBlockData() == null ? playerData.getBlockDataSupplier().get() : playerData.getBlockData())
                .orElse(blockData).createBlockState().copy(location);
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

    public void addMetadata(@NonNull Player player, @NonNull String key, @NonNull Object object) {
        getViewer(player).ifPresent(playerData -> playerData.addMetadata(key, object));
    }

    public void removeMetadata(@NonNull Player player, @NonNull String key) {
        getViewer(player).ifPresent(playerData -> playerData.removeMetadata(key));
    }

    public boolean hasMetadata(@NonNull Player player, @NonNull String key) {
        return getViewer(player).map(playerData -> playerData.hasMetadata(key)).orElse(false);
    }

    public Object getMetadata(@NonNull Player player, @NonNull String key) {
        return getMetadataAs(player, key, null);
    }

    public Object getMetadataAs(@NonNull Player player, @NonNull String key, @Nullable Object fallback) {
        return getViewer(player).map(playerData -> playerData.getMetadata(key)).orElse(fallback);
    }

    public void sendUpdate(@NonNull Player player) {
        player.sendBlockChange(location, getBlockState(player).getBlockData());
    }

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

    public int getBreakSpeed(@NonNull Player player) {
        return getViewer(player).map(PacketBlockPlayerData::getBreakSpeed).orElse(breakSpeed);
    }

    public void simulateBreak(@NonNull Player player) {
        simulateBreak(player, null);
    }

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

    public void addViewCondition(@NonNull Predicate<Player> condition) {
        if(viewConditions.contains(condition)) {
            return;
        }

        viewConditions.add(condition);
    }

}
