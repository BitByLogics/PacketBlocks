package net.bitbylogic.packetblocks.task;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import net.bitbylogic.packetblocks.block.PacketBlock;
import net.bitbylogic.packetblocks.block.PacketBlockHolder;

@Getter
@Setter
public class BlockAnimationContext {

    private final PacketBlockHolder<?, ?> block;

    private int stage = -1;
    private int ticksTaken = 0;

    public BlockAnimationContext(@NonNull PacketBlockHolder<?, ?> block) {
        this.block = block;
    }

}
