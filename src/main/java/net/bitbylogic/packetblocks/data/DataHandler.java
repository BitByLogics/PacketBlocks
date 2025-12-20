package net.bitbylogic.packetblocks.data;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import net.bitbylogic.packetblocks.viewer.PacketBlockViewer;
import net.bitbylogic.packetblocks.viewer.ViewerHolder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

public class DataHandler<T, V extends PacketBlockViewer<T>> {

    private final List<BoundingBox> boundingBoxes = new ArrayList<>();

    @Getter(AccessLevel.NONE)
    private final ViewerHolder<T, V> viewerHandler;
    private final Consumer<Player> updateConsumer;
    private final Function<T, List<BoundingBox>> boundingBoxProvider;

    public T data;

    private int breakSpeed = -1;

    private boolean addViewerOnJoin;
    private boolean globalBreakAnimation;

    public DataHandler(ViewerHolder<T, V> viewerHandler, Consumer<Player> updateConsumer, Function<T, List<BoundingBox>> boundingBoxProvider, T data, int breakSpeed) {
        this.viewerHandler = viewerHandler;
        this.updateConsumer = updateConsumer;
        this.data = data;
        this.breakSpeed = breakSpeed;

        this.boundingBoxProvider = t -> {
            boundingBoxes.clear();

            boundingBoxes.addAll(boundingBoxProvider.apply(t));

            return boundingBoxes;
        };
    }

    /**
     * Sets the data for all viewers and updates them accordingly.
     *
     * @param data the block data to be set for all viewers
     */
    protected void setDataForAll(@NonNull T data) {
        for (V viewer : viewerHandler.getViewers().values()) {
            viewer.setData(data);
        }

        sendUpdates();

        boundingBoxProvider.apply(data);
    }

    /**
     * Sets the block data supplier for all viewers and updates the block data
     * for each viewer based on the provided BlockData.
     *
     * @param data the BlockData object to be supplied to all viewers
     */
    protected void setDataSupplierForAll(@NonNull T data) {
        this.data = data;

        for (V viewer : viewerHandler.getViewers().values()) {
            viewer.setDataSupplier(() -> data);
        }

        sendUpdates();

        boundingBoxProvider.apply(data);
    }

    /**
     * Sets the data for a specific player.
     *
     * @param player    the player for whom the block data is being set; must not be null
     * @param data the data to associate with the player; can be null to reset or remove the block data
     */
    protected void setData(@NonNull Player player, @Nullable T data) {
        Optional<V> optionalPlayerData = viewerHandler.getViewer(player);

        if (optionalPlayerData.isEmpty()) {
            return;
        }

        optionalPlayerData.get().setData(data);
    }

    /**
     * Sets the data supplier for a specific player, allowing for dynamic control over block data.
     *
     * @param player    the player for whom the BlockData supplier is being set, must not be null
     * @param data the data object to be supplied, must not be null
     */
    protected void setDataSupplier(@NonNull Player player, @NonNull T data) {
        Optional<V> optionalPlayerData = viewerHandler.getViewer(player);

        if (optionalPlayerData.isEmpty()) {
            return;
        }

        optionalPlayerData.get().setDataSupplier(() -> data);
    }

    /**
     * Sets the block data for the specified player and sends an update.
     *
     * @param player    the player for whom the block data is being set, must not be null
     * @param data the block data to be set, can be null
     */
    protected void setBlockDataAndUpdate(@NonNull Player player, @Nullable T data) {
        setData(player, data);
        updateConsumer.accept(player);
    }

    /**
     * Calculates and retrieves the break speed for the given player.
     *
     * @param player the player whose break speed is being requested, must not be null
     * @return the calculated break speed for the player; returns a default value if not available
     */
    protected int getBreakSpeed(@NonNull Player player) {
        return viewerHandler.getViewer(player).map(PacketBlockViewer::getBreakSpeed).orElse(breakSpeed);
    }

    /**
     * Sends block updates to all the viewers currently tracking the block at the specified location.
     * <p>
     * This method iterates through all viewers stored in the `viewers` map and attempts to send a
     * block update to each. If a viewer no longer exists or is offline, they are removed from the
     * `viewers` map.
     * <p>
     * The block state sent to each viewer is dependent on the `getBlockState(Player)` implementation,
     * which determines the block's appearance based on the specific viewer.
     */
    protected void sendUpdates() {
        Iterator<UUID> viewerIterator = viewerHandler.getViewers().keySet().iterator();

        while (viewerIterator.hasNext()) {
            Player viewer = Bukkit.getPlayer(viewerIterator.next());

            if (viewer == null) {
                viewerIterator.remove();
                continue;
            }

            updateConsumer.accept(viewer);
        }
    }

    protected List<BoundingBox> getBoundingBoxes() {
        return boundingBoxes;
    }

    protected T getData() {
        return data;
    }

    protected T getData(@Nullable Player player) {
        if(player == null) {
            return data;
        }

        Optional<V> optionalViewer = viewerHandler.getViewer(player);

        if(optionalViewer.isEmpty()) {
            return data;
        }

        return optionalViewer.get().getSuppliedData();
    }

    protected int getBreakSpeed() {
        return breakSpeed;
    }

    protected boolean isAddViewerOnJoin() {
        return addViewerOnJoin;
    }

    protected boolean isGlobalBreakAnimation() {
        return globalBreakAnimation;
    }

    protected void setBreakSpeed(int breakSpeed) {
        this.breakSpeed = breakSpeed;
    }

    protected void setAddViewerOnJoin(boolean addViewerOnJoin) {
        this.addViewerOnJoin = addViewerOnJoin;
    }

    protected void setGlobalBreakAnimation(boolean globalBreakAnimation) {
        this.globalBreakAnimation = globalBreakAnimation;
    }

    protected void setData(T data) {
        this.data = data;
    }

}
