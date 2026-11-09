# Resting Mending

A small, vanilla-style Paper 26.3 plugin. No client mods, commands, permissions, UI, or extra plugins required.

## How it works

1. Hold a **damaged Mending item in your main hand**.
2. **Sneak and stand still for 1.5 seconds**.
3. Your item repairs **2 durability every tick (0.05 seconds at 20 TPS, or 40 durability per second)**, costing **1 stored XP point** per repair.
4. A quiet vanilla experience-orb pickup sound plays on the first repair, then at a steady **0.5-second interval** while repairs continue. Sound timing is independent of repair speed and stops when repairs stop.

Moving (including mob knockback), releasing sneak, changing the held slot, reaching full durability, or running out of XP stops repairs. Looking around and taking damage *without moving* do not interrupt. Ordinary walking while sneaking does not repair.

This is the vanilla **XP-to-durability ratio**, with a fixed repair interval; it is not vanilla orb-pickup timing. Natural orb-based Mending remains unchanged.

## Installation

Download **resting-mending-1.0.2.jar** from the [v1.0.2 release](https://github.com/Dayto-G/resting-mending/releases/tag/v1.0.2). The JAR is also available from the latest successful [GitHub Actions build](../../actions/workflows/build.yml) in the **RestingMending-1.0.2** artifact ZIP. Place the JAR in your server's `plugins/` folder and restart Paper.

Requires **Paper 26.3** and **Java 25**.

## Building locally

With JDK 25 and Gradle 9.3.1 installed:

```sh
gradle clean build
```

The plugin JAR is in `build/libs/`.

## Scope

The mechanic works on damaged Mending items in the **main hand** only. It does not modify enchantments or item data, and never spends XP on an already fully repaired item.
