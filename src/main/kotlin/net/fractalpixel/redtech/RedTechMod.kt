package net.fractalpixel.redtech

import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.block.FabricBlockSettings
import net.minecraft.block.Block
import net.minecraft.block.Material
import net.minecraft.item.BlockItem
import net.minecraft.item.Item
import net.minecraft.item.ItemGroup
import net.minecraft.sound.BlockSoundGroup
import net.minecraft.util.Identifier
import net.minecraft.util.registry.Registry

class RedTechMod(): ModInitializer {

    override fun onInitialize() {
        Registry.register(Registry.BLOCK, Identifier(MODID, REDSTONE_PIPE_ID), REDSTONE_PIPE)
        Registry.register(Registry.ITEM, Identifier(MODID, REDSTONE_PIPE_ID), REDSTONE_PIPE_ITEM)
    }

    companion object {
        val MODID = "redtech"
        val REDSTONE_PIPE_ID = "redstone_pipe"


        val REDSTONE_PIPE = RedstonePipeBlock(FabricBlockSettings
                .of(Material.BAMBOO)
                .sounds(BlockSoundGroup.BAMBOO)
                .build())

        val REDSTONE_PIPE_ITEM = BlockItem(REDSTONE_PIPE, Item.Settings()
                .maxCount(16)
                .group(ItemGroup.REDSTONE))
    }
}