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
import net.bitbylogic.packetblocks.block.PacketBlockHolder;
import net.bitbylogic.packetblocks.block.PacketBlockManager;
import net.bitbylogic.packetblocks.group.PacketBlockGroup;
import org.bukkit.Location;
import org.bukkit.block.data.BlockData;
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

            Optional<PacketBlockHolder<?, ?>> optionalBlock = manager.getBlock(bukkitLoc);

            if (optionalBlock.isPresent() && optionalBlock.get().isViewer(player)) {
                PacketBlockHolder<?, ?> block = optionalBlock.get();

                if(block instanceof PacketBlock singleBlock) {
                    packet.setBlockState(WrappedBlockState.getByString(singleBlock.getData(player).getAsString()));
                    return;
                }

                if(!(block instanceof PacketBlockGroup group)) {
                    return;
                }

                Optional<BlockData> optionalBlockData = group.getDataAt(player, bukkitLoc);

                if (optionalBlockData.isEmpty()) {
                    return;
                }

                BlockData blockData = optionalBlockData.get();
                packet.setBlockState(WrappedBlockState.getByString(blockData.getAsString()));
            }
        }

        else if (event.getPacketType() == PacketType.Play.Server.MULTI_BLOCK_CHANGE) {
            WrapperPlayServerMultiBlockChange packet = new WrapperPlayServerMultiBlockChange(event);

            WrapperPlayServerMultiBlockChange.EncodedBlock[] blocks = packet.getBlocks();
            List<WrapperPlayServerMultiBlockChange.EncodedBlock> modifiedBlocks = new ArrayList<>();

            for (WrapperPlayServerMultiBlockChange.EncodedBlock encodedBlock : blocks) {
                Location loc = new Location(player.getWorld(), encodedBlock.getX(), encodedBlock.getY(), encodedBlock.getZ());
                Optional<PacketBlockHolder<?, ?>> pb = manager.getBlock(loc);

                if (pb.isPresent() && pb.get().isViewer(player)) {
                    PacketBlockHolder<?, ?> block = pb.get();

                    if(block instanceof PacketBlock singleBlock) {
                        modifiedBlocks.add(new WrapperPlayServerMultiBlockChange.EncodedBlock(
                                WrappedBlockState.getByString(singleBlock.getData(player).getAsString()),
                                encodedBlock.getX(), encodedBlock.getY(), encodedBlock.getZ()));
                        return;
                    }

                    if(!(block instanceof PacketBlockGroup group)) {
                        return;
                    }

                    Optional<BlockData> optionalBlockData = group.getDataAt(player, loc);

                    if (optionalBlockData.isEmpty()) {
                        return;
                    }

                    BlockData blockData = optionalBlockData.get();
                    modifiedBlocks.add(new WrapperPlayServerMultiBlockChange.EncodedBlock(
                            WrappedBlockState.getByString(blockData.getAsString()),
                            encodedBlock.getX(), encodedBlock.getY(), encodedBlock.getZ()));
                } else {
                    modifiedBlocks.add(encodedBlock);
                }
            }

            packet.setBlocks(modifiedBlocks.toArray(new WrapperPlayServerMultiBlockChange.EncodedBlock[0]));
        }
    }

}
