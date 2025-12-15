package net.bitbylogic.packetblocks.event;

import lombok.Getter;
import lombok.Setter;
import net.bitbylogic.packetblocks.structure.PacketBlockStructure;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Called when a block within a {@link PacketBlockStructure} is broken by a player.
 *
 * <p>This event is fired by the PacketBlocks plugin whenever a
 * player breaks a block that belongs to a {@link PacketBlockStructure} instance.
 *
 * <p>Handlers may choose to cancel this event to prevent the block
 * from being broken.
 *
 * <p>In addition, handlers may modify whether items are dropped
 * and whether the block updates its state after being broken.
 *
 * <p>Note: This event does NOT automatically remove the block from the structure.
 * The event listener should decide whether to call {@code structure.removeBlock()}
 * based on their requirements.
 */
@Getter
public class PacketStructureBreakEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final PacketBlockStructure structure;
    private final Vector relativePosition;
    private final Location location;
    private final ItemStack tool;

    @Setter
    private boolean dropItems;

    @Setter
    private boolean update = true;

    private boolean cancelled;

    public PacketStructureBreakEvent(@NotNull Player player,
                                     @NotNull PacketBlockStructure structure,
                                     @NotNull Vector relativePosition,
                                     @NotNull Location location,
                                     @Nullable ItemStack tool) {
        this.player = player;
        this.structure = structure;
        this.relativePosition = relativePosition;
        this.location = location;
        this.tool = tool;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

}
