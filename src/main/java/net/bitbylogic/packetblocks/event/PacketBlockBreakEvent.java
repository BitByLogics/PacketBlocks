package net.bitbylogic.packetblocks.event;

import lombok.Getter;
import lombok.Setter;
import net.bitbylogic.packetblocks.data.PacketBlock;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

@Getter
public class PacketBlockBreakEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final PacketBlock block;
    private final Location location;
    private final ItemStack tool;

    @Setter
    private boolean dropItems;

    @Setter
    private boolean update = true;

    private boolean cancelled;

    public PacketBlockBreakEvent(Player player, PacketBlock block, Location location, ItemStack tool) {
        this.player = player;
        this.block = block;
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
