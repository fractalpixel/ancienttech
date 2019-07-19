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
    * [ ] Tune texture (shadows)
    * [ ] Do not make redstone rotate?
    * [ ] Space for more gates?
    * [ ] If there are no adjacent inputs, extend input to opposite output by default? -> read blocks.
    * [ ] Implement rotate, mirror - update facing, refresh inputs. 
* [ ] Redstone Pipe
    * plain, OR:s incoming, if no incoming connections, read block opposite output
* [ ] Branch
    * Like pipe, but add one additional output in the direction player watching when placing block (or up if facing straight forward)
* [ ] Timer?
* [ ] XOR-like gate
    * output 1 if off number of active inputs (for toggling things with multiple switches)
    * Could the basic gate include this?
* [ ] (Always on/off-gate)
    * Ignores inputs.  Dubious value.          
* [ ] Recipes
* [ ] Document
* [ ] Release

## License

LGPL 3.0

## Frequently asked Questions

#### Can I include this mod in my mod pack?
Yes.

#### Can I fork this mod?
Yes, but please name it something else, and if possible include a link to the original mod in the description.

