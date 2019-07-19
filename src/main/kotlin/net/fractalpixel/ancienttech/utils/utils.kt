


/**
 * @return this modulus d, with always a positive result (if the result would be negative, d is added to it).
 */
fun Double.modPositive(d: Double): Double {
    val result = this % d
    return if (result >= 0) {
        result
    } else {
        result + d
    }
}

