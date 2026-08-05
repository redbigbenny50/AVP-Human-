# AGENTS.md

This file provides guidance to Codex (Codex.ai/code) when working with code in this repository.

## Project Overview

AVP-Human is a Minecraft 1.21.1 mod that adds human content (Marines, weapons, armor) to the AVP (Aliens vs. Predator) universe. It uses a multi-loader architecture supporting both Fabric and NeoForge.

## Build Commands

```bash
# Build the project (run before PRs to format code with Spotless)
./gradlew build

# Run data generation for both loaders
./gradlew runAllDatagen

# Run Fabric client
./gradlew :fabric:runClient

# Run NeoForge client
./gradlew :neoforge:runClient

# Run Fabric server
./gradlew :fabric:runServer

# Run NeoForge server
./gradlew :neoforge:runServer
```

## Project Structure

- **common/** - Platform-agnostic code (all gameplay logic goes here)
- **fabric/** - Fabric loader-specific code
- **neoforge/** - NeoForge loader-specific code
- **buildSrc/** - Shared Gradle build logic

The `common` project compiles against vanilla Minecraft and cannot access loader-specific APIs. Loader projects (`fabric`, `neoforge`) can access all common code.

## Architecture

### Main Entry Point
`com.human.Human` - Initializes all subsystems: registries, AI graphs, event listeners, power system.

### Key Packages (`common/src/main/java/com/human/common/`)

| Package | Purpose |
|---------|---------|
| `gameplay/entity/` | Entity definitions (Marine, machines, projectiles) |
| `gameplay/entity/ai/` | GOAP-based AI system |
| `gameplay/item/` | Items including guns with fire modes, armor, syringes |
| `gameplay/block/` | Blocks organized by material type |
| `registry/init/` | Registry classes (HumanBlocks, HumanItems, HumanEntityTypes, etc.) |
| `network/packet/` | Client-server packet definitions |
| `config/` | Configuration management |

### AI System (GOAP)

Marines use **Goal-Oriented Action Planning** via the Just-GOAP library. The AI architecture:

- **MarineGOAP.java** - Builds the static AI graph with all behaviors
- Each behavior package follows the pattern: `Sensors.java`, `Actions.java`, `Goals.java`
- Behavior packages: Combat, Fire Resistance, Armor Equipping, Fire Extinguishing, Leader Following, Idle

**Adding new AI behavior:**
1. Create a new package under `marine/ai/`
2. Add Sensors (perception), Actions (behaviors), Goals (desired states)
3. Register in `MarineGOAP.java`

**Key AI concepts:**
- `BiomeSenseCache` / `EntitySenseCache` - Cached world perception (performance optimization)
- `ItemStrategy` pattern - Pluggable evaluation for weapons, armor, potions
- `MoveToPosAction` - Reusable pathfinding wrapper

### Entity Hierarchy

```
PathfinderMob
└── AbstractHuman (base human features: appearance, movement analysis)
    └── Marine (inventory, GOAP AI, leadership system, equipment)
```

Marines implement:
- `BLibInventoryHolder` - 27-slot inventory
- `GOAPUser<Marine>` - GOAP AI integration
- `ItemCooldownUser` - Item cooldown management

### Registry Pattern

Registries follow naming convention: `Human{Category}` (e.g., `HumanBlocks`, `HumanItems`, `HumanGunItems`).

Block registries are organized by material: `HumanSteelBlocks`, `HumanTitaniumBlocks`, etc.

### Dependencies

Key libraries:
- **BLib** - Foundation library (GOAP, inventory, networking)
- **AzureLib** - Animation and rendering
- **Just-GOAP** - AI planning framework

Compatibility mods:
- AVP-Alien, AVP-Predator - Other AVP modules

## Development Notes

- Java 21 required
- IntelliJ IDEA recommended (Eclipse/VSCode not supported)
- Generated resources go to `common/src/main/generated/`
- Run `./gradlew build` before PRs to apply Spotless formatting
