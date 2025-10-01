package net.bitbylogic.packetblocks.adapter;

import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBlockChange;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerMultiBlockChange;
import lombok.RequiredArgsConstructor;
import net.bitbylogic.packetblocks.block.PacketBlock;
import net.bitbylogic.packetblocks.block.PacketBlockManager;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class BlockUpdateAdapter implements PacketListener {

    private final PacketBlockManager manager;

    @Override
    public void onPacketSend(PacketSendEvent event) {
        Player player = event.getPlayer();

        if (event.getPacketType() == PacketType.Play.Server.BLOCK_CHANGE) {
            WrapperPlayServerBlockChange packet = new WrapperPlayServerBlockChange(event);
            Vector3i position = packet.getBlockPosition();
            Location blockPos = new Location(player.getWorld(), position.getX(), position.getY(), position.getZ());

            Location bukkitLoc = new Location(
                    player.getWorld(),
                    blockPos.getX(),
                    blockPos.getY(),
                    blockPos.getZ()
            );

            Optional<PacketBlock> optionalBlock = manager.getBlock(bukkitLoc);

            if (optionalBlock.isPresent() && optionalBlock.get().isViewer(player)) {
                PacketBlock block = optionalBlock.get();
                packet.setBlockState(WrappedBlockState.getByString(block.getBlockData().getAsString()));
            }
        }

        else if (event.getPacketType() == PacketType.Play.Server.MULTI_BLOCK_CHANGE) {
            WrapperPlayServerMultiBlockChange packet = new WrapperPlayServerMultiBlockChange(event);

            WrapperPlayServerMultiBlockChange.EncodedBlock[] blocks = packet.getBlocks();
            List<WrapperPlayServerMultiBlockChange.EncodedBlock> modifiedBlocks = new ArrayList<>();

            for (WrapperPlayServerMultiBlockChange.EncodedBlock block : blocks) {
                Location loc = new Location(player.getWorld(), block.getX(), block.getY(), block.getZ());
                Optional<PacketBlock> pb = manager.getBlock(loc);

                if (pb.isPresent() && pb.get().isViewer(player)) {
                    modifiedBlocks.add(new WrapperPlayServerMultiBlockChange.EncodedBlock(
                            WrappedBlockState.getByString(pb.get().getBlockData().getAsString()),
                            block.getX(), block.getY(), block.getZ()));
                } else {
                    modifiedBlocks.add(block);
                }
            }

            packet.setBlocks(modifiedBlocks.toArray(new WrapperPlayServerMultiBlockChange.EncodedBlock[0]));
        }
    }

}
