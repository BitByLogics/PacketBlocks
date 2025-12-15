package net.bitbylogic.packetblocks.adapter;

import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.world.BlockFace;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerBlockPlacement;
import lombok.RequiredArgsConstructor;
import net.bitbylogic.packetblocks.block.PacketBlockManager;
import net.bitbylogic.packetblocks.block.PacketBlock;
import net.bitbylogic.packetblocks.structure.PacketBlockStructure;
import net.bitbylogic.packetblocks.util.PacketBlockUtil;
import net.bitbylogic.utils.Pair;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockSupport;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.Optional;

@RequiredArgsConstructor
public class BlockPlaceAdapter implements PacketListener {

    private final PacketBlockManager manager;

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT) {
            return;
        }

        Player player = event.getPlayer();

        Material mainHandType = player.getInventory().getItemInMainHand().getType();
        Material offHandType = player.getInventory().getItemInOffHand().getType();

        Material blockType = mainHandType.isBlock() ? mainHandType : offHandType.isBlock() ? offHandType : Material.AIR;

        if (blockType == Material.AIR) {
            return;
        }

        WrapperPlayClientPlayerBlockPlacement wrapper = new WrapperPlayClientPlayerBlockPlacement(event);

        BlockFace direction = wrapper.getFace();
        Vector3i position = wrapper.getBlockPosition();

        Location originalLoc = new Location(player.getWorld(), position.getX(), position.getY(), position.getZ());

        if (originalLoc.getBlock().getType().isInteractable()) {
            return;
        }

        Location location = originalLoc.clone();
        org.bukkit.block.BlockFace bukkitFace = org.bukkit.block.BlockFace.valueOf(direction.name());
        Material currentBlockType = PacketBlockUtil.getBlockType(player, location);
        boolean shifted = false;

        if(!currentBlockType.isAir() && location.getBlock().getBlockData().isFaceSturdy(bukkitFace, BlockSupport.FULL)) {
            location.add(bukkitFace.getDirection());
            shifted = true;
        }

        if (blockType.createBlockData().getCollisionShape(location).overlaps(player.getBoundingBox()) && blockType.isCollidable()) {
            return;
        }

        Optional<PacketBlock> optionalBlock = manager.getBlock(location);
        Optional<Pair<PacketBlockStructure, Vector>> structureBlock = manager.getStructureBlockAt(location);

        BlockData targetBlockData = null;
        boolean isPacketBlock = false;

        if (optionalBlock.isPresent() && optionalBlock.get().isViewer(player)) {
            PacketBlock packetBlock = optionalBlock.get();
            targetBlockData = packetBlock.getBlockState(player).getBlockData();
            isPacketBlock = true;
        } else if (structureBlock.isPresent()) {
            PacketBlockStructure structure = structureBlock.get().getKey();
            if (structure.isViewer(player)) {
                Vector relativePos = structureBlock.get().getValue();
                targetBlockData = structure.getBlockData(player, relativePos);
                isPacketBlock = true;
            }
        }

        if (!isPacketBlock) {
            return;
        }

        location.add(bukkitFace.getDirection());

        Material newCurrentBlockType = PacketBlockUtil.getBlockType(player, location);

        if(shifted || !newCurrentBlockType.isAir()) {
            event.setCancelled(true);
            return;
        }

        wrapper.setBlockPosition(new Vector3i(location.getBlockX(), location.getBlockY(), location.getBlockZ()));
    }

}
