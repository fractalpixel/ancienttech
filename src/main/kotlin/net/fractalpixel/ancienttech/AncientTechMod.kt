package net.fractalpixel.ancienttech

import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.block.FabricBlockSettings
import net.fractalpixel.ancienttech.blocks.RedstoneGateBlock
import net.fractalpixel.ancienttech.blocks.RedstonePipeBlock
import net.minecraft.block.Material
import net.minecraft.item.BlockItem
import net.minecraft.item.Item
import net.minecraft.item.ItemGroup
import net.minecraft.sound.BlockSoundGroup
import net.minecraft.util.Identifier
import net.minecraft.util.registry.Registry
import org.apache.logging.log4j.LogManager

/**
 * Ancient Technologies mod for Fabric Minecraft mod loader.
 *
 * This is the main entry point of the mod, it registers the blocks and items provided by the mod.
 *
 * The api package contains interfaces and registries that other mods can use to interface with this mod.
 */
object AncientTechMod: ModInitializer {

    // Logger for this mod
    val logger = LogManager.getLogger("AncientTech")

    // Identifiers
    val MODID = "ancienttech"
    val REDSTONE_PIPE_ID = "redstone_pipe"
    val REDSTONE_GATE_ID = "redstone_gate"

    // Blocks
    val REDSTONE_PIPE = RedstonePipeBlock(FabricBlockSettings
            .of(Material.BAMBOO)
            .sounds(BlockSoundGroup.BAMBOO)
            .strength(0.5f, 0.5f)
            .build())
    val REDSTONE_GATE = RedstoneGateBlock(FabricBlockSettings
            .of(Material.BAMBOO)
            .sounds(BlockSoundGroup.BAMBOO)
            .strength(1f, 1f)
            .build())

    // Items
    val REDSTONE_PIPE_ITEM = BlockItem(REDSTONE_PIPE, Item.Settings()
            .maxCount(32)
            .group(ItemGroup.REDSTONE))
    val REDSTONE_GATE_ITEM = BlockItem(REDSTONE_GATE, Item.Settings()
            .maxCount(16)
            .group(ItemGroup.REDSTONE))

    // Initializer for mod
    override fun onInitialize() {
        logger.info("Registering block and item types for Ancient Tech mod")
        Registry.register(Registry.BLOCK, Identifier(MODID, REDSTONE_PIPE_ID), REDSTONE_PIPE)
        Registry.register(Registry.ITEM, Identifier(MODID, REDSTONE_PIPE_ID), REDSTONE_PIPE_ITEM)
        Registry.register(Registry.BLOCK, Identifier(MODID, REDSTONE_GATE_ID), REDSTONE_GATE)
        Registry.register(Registry.ITEM, Identifier(MODID, REDSTONE_GATE_ID), REDSTONE_GATE_ITEM)

        logger.info("registering done")
    }

}