# AncientTech Mod

AncientTech is a mod for Minecraft/Fabric that aims to provide some redstone components to make constructing redstone machinery more space-efficient and easy.

## Status

No longer developed (and hasn't been for quite a few years).  Feel free to reuse parts if they are of any value.  I will probably delete this repo in a few years.

## Depends on

* [Fabric Mod Loader for Minecraft](https://www.fabricmc.net/)
* [Fabric API](https://www.curseforge.com/minecraft/mc-mods/fabric-api)
* [Fabric Language Kotlin](https://www.curseforge.com/minecraft/mc-mods/fabric-language-kotlin)

## Installation

Install the [Fabric mod loader](https://www.fabricmc.net/) if you haven't already, copy the AncientTech jar and the jars of the dependencies to the mod directory of the minecraft install you are running, then (re)start minecraft.  

## Usage

*TODO*

## To be done

* [x] Redstone Gate
    * [x] Tune texture (shadows)
    * [x] Turn front redstone separately (invert output, redstone 45 deg), and turn base for selecting gate
        * Support gates: OR, TWO, AND, XOR (and their negations)
        * XOR outputs 1 if odd number of active inputs (useful for toggling things with multiple switches)
        * TWO is true if two or more inputs are on
    * [x] Update texture with new gates, build models    
    * [x] If there are no adjacent inputs, extend input to opposite output by default? -> read blocks.
    * [x] Implement rotate, mirror.
    * [x] Rename from redstone_pipe to redstone_gate or similar
* [x] Refactor out common functionality of redstone components.     
* [x] Rename from RedTech to AncientTech
* [ ] Redstone Pipe
    * Power is max of inputted powers, no distance limits.
    * [ ] For working updates, perhaps output needs to be updated one tick after input change detected?
    * [ ] Make pipes branch by applying pipe item to neighboring block side
* [ ] Fix updates, pipe connections, nets, and all other issues
* [ ] Recipes
* [ ] Config file
* [ ] Add in-game book that sometimes is available as loot / drops from libraries, or is craftable? - explains recipes and usage
* [ ] Document
* [ ] Release
* Version 2
    * [ ] Color pipes for visual clarity?  (And also stops autoconnects?)
    * [ ] Timer?
    * [ ] Delay?
    * [ ] 8 bit shift register, bus connection (separate in & out, or freeze content with latch/lock?), redstone in & out, redstone for stepping one step, redstone for direction?
        * Not enough sides.. Perhaps skip direction selection?
            * Bus (North)
            * Latch (Top) (non-locked by default)
            * Step (South)
            * In (West)
            * Out (East)
            * Toggle input/output from/to bus (Bottom)
        * [ ] Manual control to toggle input/output mode too     
        * [ ] Visualization of state
        * [ ] When in output mode, provide manual controls for toggling the bits too
        * [ ] Maybe manual controls for latch and step too? (if they are unconnected) for full manual bit manipulation editor
        * [ ] Could provide manual toggling of direction too?
        * State size: 8(data) + 2(facing) + 1(step) + 1(latch) + 1(shift dir) + 1(in-out mode) = 14bits = 16k states 
            * No need to keep state for input, read it when step changes (rising edge logic?)
            * State of out is same as last bit in that direction
            * (Latch can be read when bus or data changes to check if we should update one or the other - in this case we can't visualize the state of it though)
            * Quite a lot of models, but maybe acceptable? 
            
          
    * [ ] Multi-channel pipes? - 8 bits?          
    * [ ] Display blocks
        * [ ] Alphanumerical
        * [ ] Graphical
        


## License

MIT

## Frequently asked Questions

#### Can I include this mod in my mod pack?
Yes.

#### Can I fork this mod?
Yes, but please name it something else, and if possible include a link to the original mod in the description.

