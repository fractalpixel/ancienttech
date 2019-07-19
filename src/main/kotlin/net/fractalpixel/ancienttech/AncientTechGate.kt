package net.fractalpixel.ancienttech

import net.minecraft.util.StringIdentifiable

/**
 * The different gates that a gate block may have.
 */
enum class AncientTechGate(val gateName: String,
                           val calculate: (activeInputs: Int, totalInputs: Int) -> Boolean): StringIdentifiable {
    OR( "or",  { activeInputs, _ -> activeInputs >= 1 }),
    TWO("two", { activeInputs, _ -> activeInputs >= 2 }),
    AND("and", { activeInputs, totalInputs -> activeInputs >= 1 && activeInputs == totalInputs }),
    XOR("xor", { activeInputs, _ -> (activeInputs % 2) == 1 });

    override fun asString(): String {
        return gateName
    }
}