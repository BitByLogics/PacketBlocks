# 🧱 PacketBlocks

A Spigot/Paper library for creating and managing **client-side (packet-based) blocks**.  
PacketBlocks lets you display blocks to players without modifying the real world, while still providing APIs for interaction, metadata, block breaking, and per-player visibility.

---

## ✨ Features
- Spawn blocks visible only to specific players
- Fully per-player block states (`BlockData`, suppliers, metadata)
- Break simulation (`PacketBlockBreakEvent`) with configurable item drops
- Per-viewer visibility conditions (filters)
- Utilities for bounding boxes, collision handling, and animations
- Automatic viewer management

---

## ⚙️ Requirements
- **Server:** Paper 1.21+ (or a compatible fork)
- **Java:** 21+
- **Dependency:** [PacketEvents](https://github.com/retrooper/packetevents) `2.9.5+`

---

## 🛠 Installation

This project is **not published to a repository host**. You must clone it and install it manually:

```bash
git clone https://github.com/BitByLogics/PacketBlocks.git
cd PacketBlocks
mvn clean install
```

This installs the library into your local Maven repository (`~/.m2/repository`).

Add it as a dependency in your plugin project:

```xml
<dependency>
    <groupId>net.bitbylogic</groupId>
    <artifactId>packetblocks</artifactId>
    <version>1.0.0</version>
    <scope>provided</scope>
</dependency>
```

Replace `1.0.0` with the actual project version.

---

## 🚀 Quick Start

```java
Location loc = player.getLocation().add(0, 0, 1);
BlockData blockData = Bukkit.createBlockData(Material.DIAMOND_BLOCK);

// Create the packet block
PacketBlock packetBlock = new PacketBlock(loc, blockData);

// Make it visible to a player
packetBlock.addAndUpdateViewer(player);

// Update for all viewers
packetBlock.setBlockDataForAll(Material.GOLD_BLOCK.createBlockData());

// Simulate break (fires PacketBlockBreakEvent and drops items by default)
packetBlock.simulateBreak(player);
```

---

## ⚡ Simulate breaks & drops

`simulateBreak(...)` triggers `PacketBlockBreakEvent`. This event:

- Is cancellable
- By default, **will drop items based on the `BlockState` and tool used**
- If you do **not** want items to drop, call `event.setDropItems(false)` in your event listener

---

## 🗂 PacketBlockManager Overview

The `PacketBlockManager` is the main API for managing PacketBlocks on the server. You can get the manager using:

```java
PacketBlockManager blockManager = PacketBlocks.getInstance().getBlockManager();
```

Some important methods:

- `createBlock(Location location, BlockData blockData)` – Creates a new PacketBlock at the specified location
- `removeBlock(PacketBlock packetBlock)` – Removes a block and resets the world block for all viewers
- `removeIf(Predicate<PacketBlock> removePredicate)` – Removes blocks that match a given condition
- `getBlock(Location location)` – Gets the PacketBlock at a specific location, if it exists
- `getBlocks(World world)` – Gets all PacketBlocks in a world
- `getBlocks(World world, int chunkX, int chunkZ)` – Gets all PacketBlocks in a specific chunk
- `getBlocksByViewer(Player player)` – Gets all PacketBlocks currently visible to a player
- `getBlocksByViewerWithMeta(Player player, String metaKey)` – Gets all PacketBlocks visible to a player that contain a specific metadata key

---

## 🛠 Development

Build the project:
```bash
mvn clean install
```

---

## 🤝 Contributing

Contributions are welcome:

1. Fork the repository
2. Create a branch for your feature/fix
3. Open a Pull Request with a clear description and tests/examples when appropriate

---

## 📜 License

MIT License — see [LICENSE](./LICENSE).

---

## 👤 Author

**BitByLogic**  
Discord: `@BitByLogic`
