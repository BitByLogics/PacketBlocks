package net.bitbylogic.packetblocks.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.bitbylogic.packetblocks.block.PacketBlock;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.block.Action;
import org.jetbrains.annotations.NotNull;

/**
 * Called when a player interacts with a {@link PacketBlock}.
 *
 * <p>This event is fired by the PacketBlocks plugin whenever a
 * player right or left-clicks a {@link PacketBlock} instance in the world.
 *
 * <p>Handlers may choose to listen to this event to add custom
 * interaction behavior to packet blocks.
 *
 * <p>The event provides access to the player, the interaction action,
 * and the packet block that was interacted with.
 */
@Getter
@RequiredArgsConstructor
public class PacketBlockInteractEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final Action action;
    private final PacketBlock block;

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

}
