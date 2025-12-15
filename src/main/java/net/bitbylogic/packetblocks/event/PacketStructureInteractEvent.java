package net.bitbylogic.packetblocks.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.bitbylogic.packetblocks.structure.PacketBlockStructure;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Called when a player interacts with a block within a {@link PacketBlockStructure}.
 *
 * <p>This event is fired by the PacketBlocks plugin whenever a
 * player right or left-clicks a block that belongs to a {@link PacketBlockStructure} instance.
 *
 * <p>Handlers may choose to listen to this event to add custom
 * interaction behavior to structure blocks.
 *
 * <p>The event provides access to the player, the structure, the relative position
 * of the interacted block, the interaction action, the hand used, and the hit block face.
 */
@Getter
@RequiredArgsConstructor
public class PacketStructureInteractEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    @NotNull
    private final Player player;

    @NotNull
    private final PacketBlockStructure structure;

    @NotNull
    private final Vector relativePosition;

    @NotNull
    private final Action action;

    @Nullable
    private final EquipmentSlot hand;

    @Nullable
    private final BlockFace blockFace;

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

}
