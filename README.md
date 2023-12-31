# bitwig-docked-plugins

Allows Bitwig Studio users to dock their floating third-party VST/CLAP plugins with the Bitwig devices view, just like Bitwig-native devices.

https://github.com/dreddi/bitwig-docked-plugins/assets/2444423/dacd2ad7-af43-4234-a11f-129989f35198

![screenshot](doc/screenshot.png)


## Requirements
> ⚠️ **WARNING** ⚠️ This is a proof-of-concept demo, and is only compatible with Bitwig Studio 4.3.10.

- Bitwig Studio 4.3.10 (Ubuntu, deb, exact version required for now)
- Linux, with an X11 environment (Ubuntu 22.04 works)
- OpenJDK JRE build 17.0.7+7-Ubuntu-0ubuntu122.04.2, installed at `/usr/lib/jvm/java-17-openjdk-amd64/bin/java` (other versions may work, not tested.)

## Build instructions

```bash
git clone github.com/dreddi/bitwig-docked-plugins
cd bitwig-docked-plugins

#1. Build the JNI library
cmake -DCMAKE_BUILD_TYPE=Debug -S src/main/native/ -B ./cmake-build-debug
cmake --build cmake-build-debug/ --target displayutil -- -j 6

#2. Build the rest of the project
mvn clean package
```

## Usage

Requires `sudo`. Build the project once, then run:
```bash
./start-bitwig.sh
```

## ~~Features~~ Feature

- [x] Click the parameter list button on the desired third-party plugin, to toggle its docked state.
