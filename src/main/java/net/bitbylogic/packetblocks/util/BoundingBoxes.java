package net.bitbylogic.packetblocks.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.NonNull;
import net.bitbylogic.packetblocks.PacketBlocks;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.util.BoundingBox;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class BoundingBoxes {

    private static final Map<Material, BoundingBox> BOXES = new HashMap<>();

    public static void init(@NonNull PacketBlocks plugin) {
        try (InputStream in = plugin.getResource("bounding_boxes.json")) {
            if (in == null) {
                plugin.getLogger().warning("Could not find bounding_boxes.json");
                return;
            }

            JsonObject root = JsonParser.parseReader(new InputStreamReader(in)).getAsJsonObject();

            for (String key : root.keySet()) {
                Material material = Material.matchMaterial(key);
                if (material == null) continue;

                JsonObject obj = root.getAsJsonObject(key);

                double minX = obj.get("minX").getAsDouble();
                double minY = obj.get("minY").getAsDouble();
                double minZ = obj.get("minZ").getAsDouble();
                double maxX = obj.get("maxX").getAsDouble();
                double maxY = obj.get("maxY").getAsDouble();
                double maxZ = obj.get("maxZ").getAsDouble();

                BOXES.put(material, new BoundingBox(minX, minY, minZ, maxX, maxY, maxZ));
            }

            plugin.getLogger().info("Loaded " + BOXES.size() + " material bounding boxes.");
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Unable to load material bounding boxes.", e);
        }
    }

    /**
     * Retrieves the {@link BoundingBox} associated with the specified {@link Material}.
     *
     * @param material the material for which the bounding box needs to be retrieved; must not be null
     * @return the bounding box corresponding to the given material, or null if no bounding box is defined
     */
    public static BoundingBox getBox(@NonNull Material material) {
        return BOXES.get(material);
    }

    /**
     * Retrieves a {@link BoundingBox} at the specified location for the given material.
     * The method shifts the bounding box of the material to align it with the block location.
     * If the material does not have a bounding box defined, the method returns null.
     *
     * @param material the material whose bounding box is to be retrieved, must not be null
     * @param location the location where the bounding box is to be positioned, must not be null
     * @return the bounding box of the material at the specified location, or null if the material does not have a bounding box
     */
    public static BoundingBox getBoxAt(@NonNull Material material, @NonNull Location location) {
        BoundingBox boundingBox = getBox(material);

        if(boundingBox == null) {
            return null;
        }

        location = location.toBlockLocation();

        return new BoundingBox(
                boundingBox.getMinX() + location.getX(),
                boundingBox.getMinY() + location.getY(),
                boundingBox.getMinZ() + location.getZ(),
                boundingBox.getMaxX() + location.getX(),
                boundingBox.getMaxY() + location.getY(),
                boundingBox.getMaxZ() + location.getZ()
        );
    }

}
