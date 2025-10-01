package net.bitbylogic.packetblocks.event;

import lombok.Getter;
import net.bitbylogic.packetblocks.block.PacketBlock;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Called when a player starts breaking a {@link PacketBlock}.
 *
 * <p>This event is triggered as soon as the player begins
 * interacting with the block (e.g. starts mining it),
 * before the block is actually broken.
 *
 * <p>Handlers may cancel this event to prevent the player
 * from starting the break action.
 */
@Getter
public class PacketBlockStartBreakEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final PacketBlock block;
    private final Location location;

    private boolean cancelled;

    public PacketBlockStartBreakEvent(Player player, PacketBlock block, Location location) {
        this.player = player;
        this.block = block;
        this.location = location;
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
