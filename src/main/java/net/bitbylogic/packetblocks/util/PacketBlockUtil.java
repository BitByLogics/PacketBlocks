package net.bitbylogic.packetblocks.util;

import lombok.NonNull;
import net.bitbylogic.packetblocks.PacketBlocks;
import net.bitbylogic.packetblocks.manager.PacketBlockManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

public class PacketBlockUtil {

    public static Material getBlockType(@NonNull Player player, @NonNull Location location) {
        if(location.getWorld() == null) {
            return Material.AIR;
        }

        return PacketBlocks.getInstance().getBlockManager().getBlock(location)
                .map(packetBlock -> packetBlock.getBlockState(player).getBlockData().getMaterial()).orElse(location.getBlock().getType());
    }

    public static RayTraceResult rayTrace(Player player, double range) {
        PacketBlockManager PacketBlockManager = PacketBlocks.getInstance().getBlockManager();

        Location eye = player.getEyeLocation();
        Vector direction = eye.getDirection().normalize();

        Vector current = eye.toVector();
        World world = player.getWorld();

        double step = 0.1;
        double eps = 1e-6;

        for (double traveled = 0; traveled <= range; traveled += step) {
            current.add(direction.clone().multiply(step));
            Block block = world.getBlockAt(current.getBlockX(), current.getBlockY(), current.getBlockZ());

            BoundingBox pointBox = new BoundingBox(
                    current.getX() - eps, current.getY() - eps, current.getZ() - eps,
                    current.getX() + eps, current.getY() + eps, current.getZ() + eps
            );

            if (PacketBlockManager.getBlock(block.getLocation()).isPresent()) {
                Material blockMaterial = PacketBlockManager.getBlock(block.getLocation()).get().getBlockData().getMaterial();
                BoundingBox boundingBox = BoundingBoxes.getBoxAt(blockMaterial, block.getLocation());

                if(boundingBox == null || !boundingBox.overlaps(pointBox)) {
                    continue;
                }

                return new RayTraceResult(current, block, null);
            }

            if (!block.isEmpty()) {
                return new RayTraceResult(current, block, null);
            }
        }

        return null;
    }

}
