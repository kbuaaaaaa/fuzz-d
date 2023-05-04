# _fuzz-d_

_fuzz-d_ is a random program generator for the [Dafny](dafny.org) language, intended for fuzzing the Dafny compiler in order to find miscompilation bugs
and crashes across its backends. This repository contains the source code for the generator, as well as information for installing and using it.

## Dependencies
- Building _fuzz-d_ from source requires Java version 19 or higher. More information about installing JDK19 is available on the [OpenJDK](https://openjdk.org/projects/jdk/19/) website.
- Running programs generated by _fuzz-d_ requires a working `dafny` executable, with all backend dependencies. Information about installing Dafny and its dependencies is available on the [dafny-lang](https://github.com/dafny-lang/dafny/wiki/INSTALL#install-the-binaries) GitHub repository. 

## Build from Source
```shell
git clone --recurse-submodules git@github.com:fuzz-d/fuzz-d.git
cd fuzz-d
./gradlew build
```
This will create an executable jar in `app/build/libs` - this can be invoked using the command `java -jar app/build/libs/app.jar`.

## Usage
_fuzz-d_ has three available commands:
- `fuzz` - for generating programs and (optionally) running differential testing over the backends on the program.
- `interpret` - using the in-built interpreter to run a Dafny program and generate an output.
- `recondition` - annotate a given Dafny program with mechanisms to ensure runtime safety.

The `interpret` and `recondition` commands are built for use with programs generated by `fuzz-d`, but may work on other Dafny programs with limited success.

### Usage for Fuzzing
```
Usage: fuzzd fuzz options_list
Options:
--seed, -s LONG     Generation Seed
--advanced, -a      Use advanced reconditioning to reduce use of safety wrappers
--instrument, -i    Instrument control flow with print statements for debugging program paths
--swarm, -sw        Run with swarm testing enabled
--noRun, -n         Generate a program without running differential testing on it
--help, -h          Usage info
```
### Usage for Interpreting
```
Usage: fuzzd interpret options_list
Arguments:
file -> path to .dfy file to interpret { String }
Options:
--help, -h -> Usage info
```

### Usage for Reconditioning
```
Usage: fuzzd recondition options_list
Arguments: 
    file -> path to .dfy file to recondition { String }
Options: 
    --advanced, -a -> Use advanced reconditioning to reduce use of safety wrappers 
    --help, -h -> Usage info
```