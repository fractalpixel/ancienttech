package net.fractalpixel.ancienttech.blocks

import net.minecraft.util.StringIdentifiable

/**
 * The different gates that a [RedstoneGateBlock] block may have, and their calculation logic.
 */
enum class RedstoneGateLogic(val gateId: String,
                             val calculate: (activeInputs: Int, totalInputs: Int) -> Boolean): StringIdentifiable {
    OR( "or",  { activeInputs, _ -> activeInputs >= 1 }),
    TWO("two", { activeInputs, _ -> activeInputs >= 2 }),
    AND("and", { activeInputs, totalInputs -> activeInputs >= 1 && activeInputs == totalInputs }),
    XOR("xor", { activeInputs, _ -> (activeInputs % 2) == 1 });

    override fun asString(): String {
        return gateId
    }

    /**
     * Returns next gate in order after this one, or the first one if this is the last one.
     */
    fun nextGate(): RedstoneGateLogic {
        val nextIndex = (values().indexOf(this) + 1) % values().size
        return values()[nextIndex]
    }

    companion object {
        /**
         * Deserialize from [asString] output.
         */
        @JvmStatic
        fun forName(gateId: String): RedstoneGateLogic {
            for (value in values()) {
                if (gateId == value.asString()) {
                    return value
                }
            }
            // Not found, use default
            return OR
        }
    }
}