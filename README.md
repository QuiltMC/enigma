# Enigma

A tool for deobfuscation of Java bytecode. Forked from <https://bitbucket.org/cuchaz/enigma>, copyright Jeff Martin.

## License

Enigma is distributed under the [LGPL-3.0](LICENSE).

Enigma includes the following open-source libraries:
 - [Vineflower](https://github.com/Vineflower/vineflower) (Apache-2.0)
 - [Procyon](https://github.com/mstrobel/procyon) (Apache-2.0)
 - A [modified version](https://github.com/fabricmc/cfr) of [CFR](https://github.com/leibnitz27/cfr) (MIT)
 - [Guava](https://github.com/google/guava) (Apache-2.0)
 - [SyntaxPane](https://github.com/Sciss/SyntaxPane) (Apache-2.0)
 - [FlatLaf](https://github.com/JFormDesigner/FlatLaf) (Apache-2.0)
 - [jopt-simple](https://github.com/jopt-simple/jopt-simple) (MIT)
 - [ASM](https://asm.ow2.io/) (BSD-3-Clause)

## Usage

Pre-compiled jars can be found on the [Quilt maven](https://maven.quiltmc.org/repository/release/org/quiltmc/).

### Launching the GUI

`java -jar enigma.jar`

### On the command line

`java -cp enigma.jar command.org.quiltmc.enigma.Main`
