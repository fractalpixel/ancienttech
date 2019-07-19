package net.fractalpixel.redtech

import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.block.FabricBlockSettings
import net.minecraft.block.Material
import net.minecraft.item.BlockItem
import net.minecraft.item.Item
import net.minecraft.item.ItemGroup
import net.minecraft.sound.BlockSoundGroup
import net.minecraft.util.Identifier
import net.minecraft.util.registry.Registry

class RedTechMod(): ModInitializer {

    override fun onInitialize() {
        Registry.register(Registry.BLOCK, Identifier(MODID, redstone_gate_ID), redstone_gate)
        Registry.register(Registry.ITEM, Identifier(MODID, redstone_gate_ID), redstone_gate_ITEM)
    }

    companion object {
        val MODID = "redtech"
        val redstone_gate_ID = "redstone_gate"


        val redstone_gate = RedstoneGateBlock(FabricBlockSettings
                .of(Material.BAMBOO)
                .sounds(BlockSoundGroup.BAMBOO)
                .strength(1f, 1f)
                .build())

        val redstone_gate_ITEM = BlockItem(redstone_gate, Item.Settings()
                .maxCount(16)
                .group(ItemGroup.REDSTONE))
    }
}