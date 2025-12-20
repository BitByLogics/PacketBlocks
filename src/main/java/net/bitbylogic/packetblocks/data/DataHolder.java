package net.bitbylogic.packetblocks.data;

import lombok.NonNull;
import net.bitbylogic.packetblocks.viewer.PacketBlockViewer;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface DataHolder<T, V extends PacketBlockViewer<T>> {

    @NonNull DataHandler<T, V> getDataHandler();

    /**
     * Sets the data for all viewers and updates them accordingly.
     *
     * @param data the block data to be set for all viewers
     */
    default void setDataForAll(@NonNull T data) {
        getDataHandler().setDataForAll(data);
    }

    /**
     * Sets the block data supplier for all viewers and updates the block data
     * for each viewer based on the provided BlockData.
     *
     * @param data the BlockData object to be supplied to all viewers
     */
    default void setDataSupplierForAll(@NonNull T data) {
        getDataHandler().setDataSupplierForAll(data);
    }

    /**
     * Sets the data for a specific player.
     *
     * @param player    the player for whom the block data is being set; must not be null
     * @param data the data to associate with the player; can be null to reset or remove the block data
     */
    default void setData(@NonNull Player player, @Nullable T data) {
        getDataHandler().setData(player, data);
    }

    /**
     * Sets the data supplier for a specific player, allowing for dynamic control over block data.
     *
     * @param player    the player for whom the BlockData supplier is being set, must not be null
     * @param data the data object to be supplied, must not be null
     */
    default void setDataSupplier(@NonNull Player player, @NonNull T data) {
        getDataHandler().setDataSupplier(player, data);
    }

    /**
     * Sets the block data for the specified player and sends an update.
     *
     * @param player    the player for whom the block data is being set, must not be null
     * @param data the block data to be set, can be null
     */
    default void setBlockDataAndUpdate(@NonNull Player player, @Nullable T data) {
        getDataHandler().setBlockDataAndUpdate(player, data);
    }

    /**
     * Calculates and retrieves the break speed for the given player.
     *
     * @param player the player whose break speed is being requested, must not be null
     * @return the calculated break speed for the player; returns a default value if not available
     */
    default int getBreakSpeed(@NonNull Player player) {
        return getDataHandler().getBreakSpeed(player);
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
    default void sendUpdates() {
        getDataHandler().sendUpdates();
    }

    default List<BoundingBox> getBoundingBoxes() {
        return getDataHandler().getBoundingBoxes();
    }

    default T getData() {
        return getDataHandler().getData();
    }

    default T getData(@Nullable Player player) {
        return getDataHandler().getData(player);
    }

    default boolean isAddViewerOnJoin() {
        return getDataHandler().isAddViewerOnJoin();
    }

    default boolean isGlobalBreakAnimation() {
        return getDataHandler().isGlobalBreakAnimation();
    }

    default int getBreakSpeed() {
        return getDataHandler().getBreakSpeed();
    }

    default void setBreakSpeed(int breakSpeed) {
        getDataHandler().setBreakSpeed(breakSpeed);
    }

    default void setAddViewerOnJoin(boolean addViewerOnJoin) {
        getDataHandler().setAddViewerOnJoin(addViewerOnJoin);
    }

    default void setGlobalBreakAnimation(boolean globalBreakAnimation) {
        getDataHandler().setGlobalBreakAnimation(globalBreakAnimation);
    }
    
    default void setData(T data) {
        getDataHandler().setData(data);
    }

}
