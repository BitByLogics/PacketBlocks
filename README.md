# Packet Blocks

A Spigot/Paper library for creating and managing client-side blocks.  
PacketBlocks lets you display blocks to players without modifying the real world, while still providing APIs for interaction, metadata, block breaking, and per-player visibility.

---

## Features
- Spawn blocks visible only to specific players
- Fully per-player block states (`BlockData`, suppliers, metadata)
- Break simulation (`PacketBlockBreakEvent`) with configurable item drops
- Per-viewer visibility conditions
- Utilities for bounding boxes, collision handling, and animations
- Automatic viewer management

---

## Requirements
- **Server:** Paper 1.21+ (or a compatible fork)
- **Dependency:** [PacketEvents](https://github.com/retrooper/packetevents) `2.9.5+`

---

## Installation

**Maven:**
```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>com.github.BitByLogics</groupId>
        <artifactId>PacketBlocks</artifactId>
        <version>1.2.4</version>
        <scope>provided</scope>
    </dependency>
</dependencies>
```

**Gradle:**
```groovy
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    compileOnly 'com.github.BitByLogics:PacketBlocks:1.2.4'
}
```

---

## Quick Start

```java
Location loc = player.getLocation().add(0, 0, 1);
BlockData blockData = Bukkit.createBlockData(Material.DIAMOND_BLOCK);

// Get the block manager
PacketBlockManager blockManager = PacketBlocks.getInstance().getBlockManager();

// Create the packet block
PacketBlock packetBlock = blockManager.createBlock(loc, blockData);

// Add permission view requirement
packetBlock.addViewCondition(player -> player.hasPermission("packetblocks.view"));

// Make it visible to a player
packetBlock.addAndUpdateViewer(player);

// Update for all viewers
packetBlock.setDataForAll(Material.GOLD_BLOCK.createBlockData());

// Simulate break (fires PacketBlockBreakEvent and drops items by default)
packetBlock.simulateBreak(player);
```

---

## Documentation

Full API documentation is available [here](https://bitbylogics.github.io/PacketBlocks/)

---

## Simulate breaks & drops

`simulateBreak(...)` triggers `PacketBlockBreakEvent`. This event:

- Is cancellable
- By default, will drop items based on the `BlockState` and tool used
- If you do not want items to drop, call `event.setDropItems(false)` in your event listener

---

## PacketBlockManager Overview

The PacketBlockManager is the main API for managing PacketBlocks on the server. You can get the manager using:

```java
PacketBlockManager blockManager = PacketBlocks.getInstance().getBlockManager();
```

Some important methods:

- `createBlock(Location location, BlockData blockData)` – Creates a new PacketBlock at the specified location
- `removeBlock(PacketBlockHolder<?, ?> packetBlock)` – Removes a block and resets the world block for all viewers
- `removeIf(Predicate<PacketBlock> removePredicate)` – Removes blocks that match a given condition
- `getBlock(Location location)` – Gets the PacketBlockHolder at a specific location, if it exists
- `getBlocks(World world)` – Gets all PacketBlockHolders in a world
- `getBlocks(World world, int chunkX, int chunkZ)` – Gets all PacketBlockHolders in a specific chunk
- `getBlocksByViewer(Player player)` – Gets all PacketBlockHolders currently visible to a player
- `getBlocksByViewerWithMeta(Player player, String metaKey)` – Gets all PacketBlockHolders visible to a player that contain a specific metadata key

---

## Development

Build the project:
```bash
mvn clean install
```

---

## License

MIT License, see [LICENSE](./LICENSE).
