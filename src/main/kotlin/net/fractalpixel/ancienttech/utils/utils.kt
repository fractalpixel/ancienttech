package net.fractalpixel.ancienttech.utils

import java.util.*

/*
    Generic utility functions.
 */


// Extension functions for Random:

/**
 * Create random value in the range 0 .. specified value. (works for negative values too).
 */
fun Random.nextDouble(max: Double): Double {
    return this.nextDouble() * max
}

/**
 * Create random value in a range.
 */
fun Random.nextDouble(min: Double, max: Double): Double {
    return this.nextDouble().mixTo(min, max)
}

/**
 * Random boolean with the specified probability (1 = always true, 0 = always false, 0.5 = 50%)
 */
fun Random.nextBoolean(probability: Double): Boolean {
    return this.nextDouble() < probability
}