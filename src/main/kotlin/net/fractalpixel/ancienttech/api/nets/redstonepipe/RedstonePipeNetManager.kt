package net.fractalpixel.ancienttech.api.nets.redstonepipe

import net.fractalpixel.ancienttech.api.nets.NetManager

object RedstonePipeNetManager: NetManager<RedstonePipeNet>() {

    /**
     * Network type for redstone pipes.
     */
    const val REDSTONE_PIPE_NET_TYPE = "ancienttech:redstone_pipe"
    override val netType: String = REDSTONE_PIPE_NET_TYPE

    override fun createNet(): RedstonePipeNet = RedstonePipeNet()

}