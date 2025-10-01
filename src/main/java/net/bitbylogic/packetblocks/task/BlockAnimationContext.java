package net.bitbylogic.packetblocks.task;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import net.bitbylogic.packetblocks.block.PacketBlock;

@Getter
@Setter
public class BlockAnimationContext {

    private final PacketBlock block;

    private int stage = -1;
    private int ticksTaken = 0;

    public BlockAnimationContext(@NonNull PacketBlock block) {
        this.block = block;
    }

}
