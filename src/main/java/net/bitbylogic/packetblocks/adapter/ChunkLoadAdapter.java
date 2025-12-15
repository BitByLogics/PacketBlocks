package net.bitbylogic.packetblocks.adapter;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.world.chunk.BaseChunk;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerChunkData;
import lombok.RequiredArgsConstructor;
import net.bitbylogic.packetblocks.block.PacketBlock;
import net.bitbylogic.packetblocks.block.PacketBlockManager;
import net.bitbylogic.packetblocks.structure.PacketBlockStructure;
import org.bukkit.Location;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public class ChunkLoadAdapter implements PacketListener {

    private final PacketBlockManager manager;

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacketType() != PacketType.Play.Server.CHUNK_DATA) return;

        Player player = event.getPlayer();
        WrapperPlayServerChunkData packet = new WrapperPlayServerChunkData(event);

        int chunkX = packet.getColumn().getX();
        int chunkZ = packet.getColumn().getZ();

        List<PacketBlock> blocks = new ArrayList<>(manager.getBlocks(player.getWorld(), chunkX, chunkZ));
        List<PacketBlockStructure> structures = manager.getStructuresInChunk(player.getWorld(), chunkX, chunkZ);

        if (blocks.isEmpty() && structures.isEmpty()) return;

        BaseChunk[] sections = packet.getColumn().getChunks();
        int absMinHeight = Math.abs(player.getWorld().getMinHeight());

        for (PacketBlock packetBlock : blocks) {
            if (!packetBlock.isViewer(player)) continue;

            Location loc = packetBlock.getLocation();
            int xInChunk = loc.getBlockX() & 0xF;
            int y = loc.getBlockY();
            int zInChunk = loc.getBlockZ() & 0xF;

            int sectionIndex = (y >> 4) + (absMinHeight >> 4);
            int yInSection = y & 0xF;

            if (sectionIndex < 0 || sectionIndex >= sections.length) continue;

            BaseChunk section = sections[sectionIndex];
            if (section == null) continue;

            BlockData bukkitData = packetBlock.getBlockState(player).getBlockData();
            WrappedBlockState wrappedState = WrappedBlockState.getByString(bukkitData.getAsString());

            section.set(
                    PacketEvents.getAPI().getServerManager().getVersion().toClientVersion(),
                    xInChunk,
                    yInSection,
                    zInChunk,
                    wrappedState.getGlobalId()
            );
        }

        int originX = chunkX << 4;
        int originZ = chunkZ << 4;
        int originY = player.getWorld().getMinHeight();

        for (PacketBlockStructure structure : structures) {
            if (!structure.isViewer(player)) continue;

            structure.forEachBlockInChunk(chunkX, chunkZ, (relX, relY, relZ, baseData) -> {
                int absX = structure.getOrigin().getBlockX() + relX;
                int absY = structure.getOrigin().getBlockY() + relY;
                int absZ = structure.getOrigin().getBlockZ() + relZ;

                int xInChunk = absX & 0xF;
                int zInChunk = absZ & 0xF;
                int sectionIndex = (absY >> 4) + (absMinHeight >> 4);
                int yInSection = absY & 0xF;

                if (sectionIndex < 0 || sectionIndex >= sections.length) return;

                BaseChunk section = sections[sectionIndex];
                if (section == null) return;

                BlockData blockData = structure.getBlockData(player, relX, relY, relZ);
                WrappedBlockState wrappedState = WrappedBlockState.getByString(blockData.getAsString());

                section.set(
                        PacketEvents.getAPI().getServerManager().getVersion().toClientVersion(),
                        xInChunk,
                        yInSection,
                        zInChunk,
                        wrappedState.getGlobalId()
                );
            });
        }
    }

}
