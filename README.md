<div align="center">

# DynaTech

**A Slimefun addon full of machines, generators and tools that make life easier, and some that are just for fun.**

![Minecraft](https://img.shields.io/badge/Minecraft-26.1%2B-62B47A?style=flat-square)
![Java](https://img.shields.io/badge/Java-25-E76F00?style=flat-square)
![Slimefun](https://img.shields.io/badge/Slimefun-addon-3B8EEA?style=flat-square)
[![Build](https://github.com/mybezo/DynaTech/actions/workflows/maven.yml/badge.svg)](https://github.com/mybezo/DynaTech/actions/workflows/maven.yml)

[Download](#download) · [Features](#features) · [Configuration](#configuration) · [Building](#building-from-source) · [Credits](#credits)

</div>

---

> [!NOTE]
> Some of these items may be overpowered and some underpowered. Balance them to suit your server.

## Requirements

| Requirement | Version |
| --- | --- |
| Server | Paper (or a fork of it) for **Minecraft 26.1 or later**. Built against 26.3. |
| Java | **25** or later |
| [Slimefun](https://github.com/SlimefunGuguProject/Slimefun4) | A build that supports your Minecraft version. For 26.3, use the latest SlimefunGuguProject dev build. |
| [GuizhanLibPlugin](https://github.com/ybw0014/GuizhanLib) | **Required.** DynaTech disables itself without it. |

**Optional integrations:** [ExoticGarden](https://github.com/SlimefunGuguProject/ExoticGarden), [InfinityExpansion](https://github.com/SlimefunGuguProject/InfinityExpansion), [Gastronomicon](https://github.com/SchnTgaiSpock/Gastronomicon)

> [!IMPORTANT]
> This version only runs on Minecraft 26.x. Servers on 1.19 to 1.21 need an older build of DynaTech.

## Download

- **Stable builds:** [resources.guizhanss.com](https://resources.guizhanss.com/plugin/DynaTech)
- **Development builds:** open the latest successful run under [Actions](https://github.com/mybezo/DynaTech/actions/workflows/maven.yml) and download the `DynaTech` artifact.

### Installation

1. Install Slimefun and GuizhanLibPlugin.
2. Put `DynaTech.jar` in your server's `plugins/` folder.
3. Restart the server. The items appear in the Slimefun Guide under **DynaTech**.

## Features

### ⚙️ Machines

| Machine | Description |
| --- | --- |
| **Auto Kitchen** | Crafts any Kitchen recipe automatically. Only available when ExoticGarden is installed. |
| **Growth Chambers** | Grow plants automatically. Variants exist for the Overworld, Ocean, Nether and End, each with an MK2 version. Supports ExoticGarden saplings, plants and bushes. |
| **Weather Controller** | Changes the weather when you give it a key item: Sunflower for clear, Lilac for rain, Creeper Head for thunder. |
| **Potion Sprinkler** | Applies potion effects to players within 10 blocks. |
| **Barbed Wire** | Pushes mobs away within 9 blocks. Useful for mob farms. |
| **Material Hive** | Generates materials endlessly using bees and power. Place the bees and a stack of the material you want to produce. More bees make it faster (see [Bees](#-bees)). |
| **Mineralized Apiaries** | One apiary per material. Each produces its material every **30 seconds** using power alone, with no bees needed. |
| **Wireless Charger** | Charges rechargeable items in the inventories of players within 16 blocks. |
| **Seed Plucker** | Extracts seeds from plant-based items. Supports ExoticGarden fruits but not essences. |
| **Item Band Manager** | Applies Item Bands to items and removes them. |
| **Orechid** | Turns nearby Stone and Netherrack into Overworld and Nether ores. |
| **Wireless Energy Bank / Point** | Stores energy in the Bank and sends it wirelessly to a linked Point. |
| **Wireless Item Input / Output** | One-way wireless item transfer. |
| **Tesseract** | Two-way wireless transfer of both items and energy. |
| **External Heater** | Heats furnace-like blocks from outside. |

### ⚡ Generators

| Generator | Description |
| --- | --- |
| **Hydro Generator** | Generates power from water. Waterlog the generator to use it. |
| **Dragon Egg Generator** | Generates power from the warmth of a dragon egg placed on top. |
| **Chipping Generator** | Consumes durability from damaged or durable items for power. |
| **Culinary Generator** | Burns food for power. Supports all ExoticGarden foods. |
| **Stardust Reactor** | Produces a large amount of power from Star Dust. |

### 🛠️ Tools

| Tool | Description |
| --- | --- |
| **Picnic Basket** | An upgraded Cooler. Automatically feeds you vanilla or ExoticGarden food. Its blacklist is configurable in Slimefun's `Items.yml`. A Soulbound version is also available. |
| **Electrical Stimulator** | Uses power to keep you fed. |
| **Inventory Filter** | Deletes items you pick up if they match an item in the filter. |
| **Scoop** | Catches bees and turns them into items. |
| **Dimensional Home** | Teleports you to a small personal home in another dimension. Using it there sends you back to your bed spawn. |
| **Tesseract Binder** | Links Tesseracts together. |
| **Portable Liquid Tank** | Holds up to 16 buckets of a liquid and can place it again. |

### 🐝 Bees

Bees speed up the **Material Hive**:

| Bee | Time saved per bee |
| --- | --- |
| **Bee** (caught with the Scoop) | −2 seconds |
| **Robotic Bee** | −2 seconds |
| **Advanced Robotic Bee** | −10 seconds |

### 💍 Item Bands

| Item Band | Effect when worn or held in the main hand |
| --- | --- |
| **Healthy Item Band** | Health Boost II (+4 hearts) |
| **Hasty Item Band** | Haste II |

### 🔌 Integrations

| Addon | What you get |
| --- | --- |
| **InfinityExpansion** | **Vex Mob Data Card** for Ghostly Essence and Vex Gems, and **Phantom Mob Data Card** for Phantom Membrane. |
| **ExoticGarden** | Auto Kitchen, plus ExoticGarden support in the Growth Chambers, Seed Plucker, Culinary Generator and Picnic Basket. |

## Configuration

`plugins/DynaTech/config.yml`:

```yaml
options:
    # Download and install updates automatically from the build site (applies after a restart)
    auto-update: false
    # Set to true to disable the Dimensional Home world
    disable-dimensionalhome-world: false
```

Per-item settings, such as the Picnic Basket blacklist or the materials the Material Hive accepts, live in Slimefun's `plugins/Slimefun/Items.yml`.

## Building from source

You need **JDK 25** and **Maven**.

```bash
git clone https://github.com/mybezo/DynaTech.git
cd DynaTech
mvn clean package
```

The plugin is written to `target/DynaTech.jar`.

## Credits

Originally created by [ProfElements](https://github.com/ProfElements/DynaTech). This fork is based on the [SlimefunGuguProject](https://github.com/SlimefunGuguProject/DynaTech) edition.

- [NCBPFluffyBear](https://github.com/ncbpfluffybear): the autocrafter code that helped a lot with the Auto Kitchen.
- [Mooy1](https://github.com/mooy1): the hydro generator code that showed how to handle waterlogging properly.
- [Seggan](https://github.com/seggan): showed how to build a good container class instead of using Slimefun's default.
- [WalshyDev](https://github.com/WalshyDev): answered many questions in the programming help channel.
- [Slimefun Discord](https://slimefun.dev/discord): put up with all the questions in the programming help channel.
- [Slimefun](https://github.com/slimefun/slimefun4): an intuitive platform for building addons.

## License

DynaTech is released under the [MIT License](LICENSE).
