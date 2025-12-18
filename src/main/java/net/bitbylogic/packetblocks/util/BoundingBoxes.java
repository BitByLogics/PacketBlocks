package net.bitbylogic.packetblocks.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.NonNull;
import net.bitbylogic.packetblocks.PacketBlocks;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public class BoundingBoxes {

    private static final Map<String, List<BoundingBox>> CACHE = new LinkedHashMap<>(500, 0.75f, true) {

        @Override
        protected boolean removeEldestEntry(Map.Entry<String, List<BoundingBox>> eldest) {
            return size() > 500;
        }

    };

    private static JsonObject BOUNDING_BOXES;

    public static void init(@NonNull PacketBlocks plugin) {
        try (InputStream in = plugin.getResource("bounding_boxes.json")) {
            if (in == null) {
                plugin.getLogger().warning("Could not find bounding_boxes.json");
                return;
            }

            BOUNDING_BOXES = JsonParser.parseReader(new InputStreamReader(in)).getAsJsonObject();
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Unable to load block state bounding boxes.", e);
        }
    }

    /**
     * Retrieves a list of bounding boxes associated with the given block data.
     * The method fetches cached bounding boxes if available, otherwise parses
     * and generates bounding boxes from a predefined JSON structure.
     *
     * @param blockData the block data for which the bounding boxes are retrieved; must not be null
     * @return a list of bounding boxes associated with the given block data, or null if no bounding boxes are defined
     */
    public static List<BoundingBox> getBoxes(@NonNull BlockData blockData) {
        String key = blockData.getAsString();

        List<BoundingBox> cached = CACHE.get(key);

        if (cached != null) {
            return cached;
        }

        if (BOUNDING_BOXES == null || !BOUNDING_BOXES.has(key)) {
            return null;
        }

        JsonArray boxArray = BOUNDING_BOXES.getAsJsonArray(key);
        List<BoundingBox> boxes = new ArrayList<>();

        for (JsonElement element : boxArray) {
            JsonObject obj = element.getAsJsonObject();

            double minX = obj.get("minX").getAsDouble();
            double minY = obj.get("minY").getAsDouble();
            double minZ = obj.get("minZ").getAsDouble();
            double maxX = obj.get("maxX").getAsDouble();
            double maxY = obj.get("maxY").getAsDouble();
            double maxZ = obj.get("maxZ").getAsDouble();

            boxes.add(new BoundingBox(minX, minY, minZ, maxX, maxY, maxZ));
        }

        CACHE.put(key, boxes);

        return boxes;
    }

    /**
     * Retrieves a list of bounding boxes associated with the given block.
     * This method internally resolves block data from the provided block
     * and delegates the operation to fetch the bounding boxes.
     *
     * @param block the block for which the bounding boxes are retrieved; must not be null
     * @return a list of bounding boxes associated with the provided block, or null if no bounding boxes are defined
     */
    public static List<BoundingBox> getBoxes(@NonNull Block block) {
        return getBoxes(block.getBlockData());
    }

    /**
     * Calculates a list of world-space bounding boxes for the provided block data at the given location.
     * The method retrieves bounding boxes defined for the block data, adjusts their positions based on
     * the block's location in the world, and returns the resulting list.
     *
     * @param blockData the block data used to retrieve and calculate bounding boxes; must not be null
     * @param location the location of the block in the world, used to translate the bounding boxes; must not be null
     * @return a list of bounding boxes translated to world coordinates; an empty list if no bounding boxes are defined
     */
    public static List<BoundingBox> getBoxesAt(@NonNull BlockData blockData, @NonNull Location location) {
        List<BoundingBox> boxes = getBoxes(blockData);

        if (boxes == null || boxes.isEmpty()) {
            return new ArrayList<>();
        }

        location = location.toBlockLocation();
        List<BoundingBox> worldBoxes = new ArrayList<>();

        for (BoundingBox box : boxes) {
            worldBoxes.add(new BoundingBox(
                    box.getMinX() + location.getX(),
                    box.getMinY() + location.getY(),
                    box.getMinZ() + location.getZ(),
                    box.getMaxX() + location.getX(),
                    box.getMaxY() + location.getY(),
                    box.getMaxZ() + location.getZ()
            ));
        }

        return worldBoxes;
    }

    /**
     * Retrieves a list of world-space bounding boxes at the specified block's location.
     * This method resolves the block data from the provided block and calculates the
     * bounding boxes based on the block's location in the world.
     *
     * @param block the block for which the bounding boxes are to be calculated; must not be null
     * @return a list of bounding boxes aligned to the world-space location of the specified block,
     *         or an empty list if no bounding boxes are defined
     */
    public static List<BoundingBox> getBoxesAt(@NonNull Block block) {
        return getBoxesAt(block.getBlockData(), block.getLocation());
    }

    /**
     * Retrieves a list of bounding boxes associated with the given location.
     * The method resolves the block at the specified location and retrieves
     * its bounding boxes by delegating the operation to another method.
     *
     * @param location the location for which the bounding boxes are retrieved; must not be null
     * @return a list of bounding boxes associated with the block at the given location
     */
    public static List<BoundingBox> getBoxesAt(@NonNull Location location) {
        return getBoxesAt(location.getBlock());
    }

    /**
     * Performs a ray trace operation against the bounding boxes of the specified block and block data.
     * This method calculates the world-space bounding boxes for the given block data at the block's
     * location, then performs a ray trace on each bounding box to find the closest intersection point.
     *
     * @param block the block for which the ray trace is performed; must not be null
     * @param blockData the block data of the block; must not be null
     * @param start the starting position of the ray; must not be null
     * @param direction the direction vector of the ray; must not be null
     * @param maxDistance the maximum distance to trace; must be a positive double
     * @return the closest {@code RayTraceResult} if an intersection is found, or {@code null}
     *         if no intersection occurs within the specified bounds
     */
    public static RayTraceResult rayTraceAt(@NonNull Block block, @NonNull BlockData blockData, @NonNull Vector start, @NonNull Vector direction, double maxDistance) {
        List<BoundingBox> boxes = getBoxesAt(blockData, block.getLocation());

        if (boxes.isEmpty()) {
            return null;
        }

        RayTraceResult closestResult = null;
        double closestDistance = Double.MAX_VALUE;

        for (BoundingBox box : boxes) {
            RayTraceResult result = box.rayTrace(start, direction, maxDistance);

            if (result != null) {
                double distance = result.getHitPosition().distanceSquared(start);

                if (distance > closestDistance) {
                    continue;
                }

                closestDistance = distance;
                closestResult = result;
            }
        }

        return closestResult;
    }

}