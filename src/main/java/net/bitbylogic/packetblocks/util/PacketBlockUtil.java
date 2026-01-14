package net.bitbylogic.packetblocks.util;

import lombok.NonNull;
import net.bitbylogic.packetblocks.PacketBlocks;
import net.bitbylogic.packetblocks.block.PacketBlock;
import net.bitbylogic.packetblocks.block.PacketBlockHolder;
import net.bitbylogic.packetblocks.block.PacketBlockManager;
import net.bitbylogic.packetblocks.group.PacketBlockGroup;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class PacketBlockUtil {

    public static BlockData getBlockData(@Nullable Player player, @NonNull Location location) {
        if(location.getWorld() == null) {
            return Material.AIR.createBlockData();
        }

        Optional<PacketBlockHolder<?, ?>> optionalHolder = PacketBlocks.getInstance().getBlockManager().getBlock(location);

        if(optionalHolder.isEmpty()) {
            return location.getBlock().getBlockData();
        }

        PacketBlockHolder<?, ?> packetBlockHolder = optionalHolder.get();

        if (packetBlockHolder instanceof PacketBlock packetBlock) {
            return packetBlock.getData();
        }

        if(packetBlockHolder instanceof PacketBlockGroup group) {
            Optional<BlockData> optionalData = group.getDataAt(player, location);

            if(optionalData.isEmpty()) {
                return location.getBlock().getBlockData();
            }

            return optionalData.get();
        }

        return location.getBlock().getBlockData();
    }

    /**
     * Retrieves the material type of the block at a specific location for a given player.
     * This method first checks for any custom packet-based block at the provided location
     * and, if present, determines the block type specific to the player. If no custom block exists,
     * it returns the material type of the actual block at the location.
     *
     * @param player   the player for whom the block type is determined; must not be null
     * @param location the location of the block to check; must not be null
     * @return the material representing the block type at the specified location
     */
    public static Material getBlockType(@Nullable Player player, @NonNull Location location) {
        if(location.getWorld() == null) {
            return Material.AIR;
        }

        Optional<PacketBlockHolder<?, ?>> optionalHolder = PacketBlocks.getInstance().getBlockManager().getBlock(location);

        if(optionalHolder.isEmpty()) {
            return location.getBlock().getType();
        }

        PacketBlockHolder<?, ?> packetBlockHolder = optionalHolder.get();

        if (packetBlockHolder instanceof PacketBlock packetBlock) {
            return packetBlock.getData().getMaterial();
        }

        if(packetBlockHolder instanceof PacketBlockGroup group) {
            Optional<BlockData> optionalData = group.getDataAt(player, location);

            if(optionalData.isEmpty()) {
                return location.getBlock().getType();
            }

            return optionalData.get().getMaterial();
        }

        return location.getBlock().getType();
    }

    /**
     * Performs a ray trace from the player's eye location along their current direction up to the specified range.
     * The ray trace detects blocks in the player's world considering custom bounding boxes when relevant.
     *
     * @param player the player from whose perspective the ray trace is performed
     * @param range the maximum distance the ray trace will travel
     * @return a RayTraceResult containing information about the hit block and location, or null if no block was hit
     */
    public static RayTraceResult rayTrace(Player player, double range) {
        PacketBlockManager blockManager = PacketBlocks.getInstance().getBlockManager();

        Location eye = player.isSneaking() ? player.getEyeLocation().clone().add(0, 0.25, 0) : player.getEyeLocation();
        Vector direction = eye.getDirection().normalize();

        World world = player.getWorld();

        RayTraceResult vanillaResult = world.rayTraceBlocks(eye, direction, range, FluidCollisionMode.NEVER, false);

        Vector current = eye.toVector();
        double step = 0.02;

        for (double traveled = 0; traveled <= range; traveled += step) {
            current.add(direction.clone().multiply(step));
            Block block = world.getBlockAt(current.toLocation(world));
            PacketBlockHolder<?, ?> packetBlock = blockManager.getBlock(block.getLocation()).orElse(null);

            if(packetBlock == null) {
                if(!block.getType().isAir()) {
                    RayTraceResult boxResult = BoundingBoxes.rayTraceAt(block, block.getBlockData(), eye.toVector(), direction, range);

                    if(boxResult != null) {
                        return boxResult;
                    }
                }

                continue;
            }

            if (packetBlock instanceof PacketBlock singleBlock) {
                BlockData blockData = singleBlock.getData(player);

                RayTraceResult boxResult = BoundingBoxes.rayTraceAt(block, blockData, eye.toVector(), direction, range);

                if(boxResult == null) {
                    continue;
                }

                return new RayTraceResult(boxResult.getHitPosition(), block, boxResult.getHitBlockFace());
            }

            if (!(packetBlock instanceof PacketBlockGroup group)) {
                continue;
            }

            Optional<BlockData> optionalBlockData = group.getDataAt(player, block.getLocation());

            if (optionalBlockData.isEmpty()) {
                continue;
            }

            BlockData blockData = optionalBlockData.get();

            RayTraceResult boxResult = BoundingBoxes.rayTraceAt(block, blockData, eye.toVector(), direction, range);

            if(boxResult == null) {
                continue;
            }

            return new RayTraceResult(boxResult.getHitPosition(), block, boxResult.getHitBlockFace());
        }

        return vanillaResult;
    }

}
