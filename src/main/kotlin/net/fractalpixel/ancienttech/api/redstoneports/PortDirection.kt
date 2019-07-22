package net.fractalpixel.ancienttech.api.redstoneports

/**
 * Describes the direction redstone signals (or other things) can be sent out or in from a blocks side.
 * Also includes information on whether the port is currently active, or potentially active (e.g. if something connects to it)
 */
enum class PortDirection(val input: Boolean,
                         val output: Boolean,
                         val active: Boolean) {

    /**
     * No connection on the side.
     */
    NONE(false, false, false),

    /**
     * The block outputs in the direction, but it doesn't accept/read any input from the side.
     */
    OUT(false, true, true),

    /**
     * The block reads from the direction, but it doesn't output anything to the side.
     */
    IN(true, false, true),

    /**
     * The block both reads and writes from this side.
     */
    IN_OUT(true, true, true),

    /**
     * If connected to, the block may output in the direction, but it doesn't accept/read any input from the side.
     * However, at this moment it does not output in the direction.
     */
    POTENTIAL_OUT(false, true, false),

    /**
     * If connected to, the block may read from the direction, but it doesn't output anything to the side.
     * However, at this moment it does not read from the direction.
     */
    POTENTIAL_IN(true, false, false),

    /**
     * If connected to, the block may both read and write from this side.
     * However, at this moment it does not read or write in the direction.
     */
    POTENTIAL_IN_OUT(true, true, false);

    /**
     * Returns true if this port type is compatible with the specified port type.
     * E.g. an IN port is compatible with an OUT port, or an IN with and IN_OUT, but an IN and an IN are not compatible.
     */
    fun canConnectTo(otherPort: PortDirection): Boolean {
        if (this == NONE || otherPort == NONE) return false
        return (this.input && otherPort.output) ||
               (this.output && otherPort.input)
    }

}