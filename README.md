# RedTech Mod

RedTech is a mod for Minecraft/Fabric that aims to provide some redstone components to make constructing redstone machinery more space-efficient and easy.

## Depends on

* [Fabric Mod Loader for Minecraft](https://www.fabricmc.net/)
* [Fabric API](https://www.curseforge.com/minecraft/mc-mods/fabric-api)
* [Fabric Language Kotlin](https://www.curseforge.com/minecraft/mc-mods/fabric-language-kotlin)

## Installation

Install the [Fabric mod loader](https://www.fabricmc.net/) if you haven't already, copy the redtech jar and the jars of the dependencies to the mod directory of the minecraft install you are running, then (re)start minecraft.  

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
    * [ ] Implement rotate, mirror - update facing, refresh inputs.
    * [ ] Rename from redstone_pipe to redstone_gate or similar 
* [ ] Redstone Pipe
    * plain, OR:s incoming, if no incoming connections, read block opposite output
* [ ] Branch
    * Like pipe, but add one additional output in the direction player watching when placing block (or up if facing straight forward)
* [ ] Redstone sensor
    * Detects if the block in front emits redstone signal into sensor, if so, output redstone to any outputs
    * Detect potential outputs - redstone, redstone gates, pipes.  If no output, output in opposite direction?     
* [ ] Timer?
* [ ] Rename from RedTech to AncientTech?  Or BambooTech?
* [ ] Recipes
* [ ] Config file
* [ ] Add in-game book that sometimes is available as loot / drops from libraries? - explains recipes and usage
* [ ] Document
* [ ] Release

## License

LGPL 3.0

## Frequently asked Questions

#### Can I include this mod in my mod pack?
Yes.

#### Can I fork this mod?
Yes, but please name it something else, and if possible include a link to the original mod in the description.

