package net.bitbylogic.packetblocks.task;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import net.bitbylogic.packetblocks.structure.PacketBlockStructure;
import org.bukkit.util.Vector;

@Getter
@Setter
public class StructureAnimationContext {

    private final PacketBlockStructure structure;
    private final Vector relativePosition;

    private int stage = -1;
    private int ticksTaken = 0;

    public StructureAnimationContext(@NonNull PacketBlockStructure structure, @NonNull Vector relativePosition) {
        this.structure = structure;
        this.relativePosition = relativePosition;
    }

}
