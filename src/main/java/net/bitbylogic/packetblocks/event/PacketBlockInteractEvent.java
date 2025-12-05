package net.bitbylogic.packetblocks.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.bitbylogic.packetblocks.block.PacketBlock;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.block.Action;
import org.jetbrains.annotations.NotNull;

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
