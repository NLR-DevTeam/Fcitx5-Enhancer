# Fcitx5-Enhancer

This is a mod made for Minecraft 1.20.x & 1.21.x, which provides compatibility with Fcitx5.

## The Problem

When using IME in Minecraft, pressing a key that is also a game hotkey (e.g. Tab, Enter) causes the key event to be
handled by both the input method and the game.

And then you'll be interrupted. That's crazy. So I wrote this mod, in order to deal with this disrupting problem.

## Extra Features

We provides a highly configurable IMBlocker, with a visual element selector.

Besides, we added IME support for native Wayland environment.

## Requirements

Fabric Loader and a JVM shipped with Unsafe (for 1.20 and 1.20.1) are required to run this mod.

[Cloth Config](https://modrinth.com/mod/cloth-config) is required too. To access the config screen, you need
to install [Mod Menu](https://modrinth.com/mod/modmenu) as well.

## Building this Mod

To build the mod from its source code, run the following commands:

```shell
git clone https://github.com/NLR-DevTeam/Fcitx5-Enhancer --depth 1 --recursive
cd Fcitx5-Enhancer
./gradlew build --no-daemon
```

Then you'll find the artifacts inside `fabricWrapper/build/libs`.

## Notice

This mod relies on [native libraries](/src/native) to implement its functionalities.
And the built-in libraries were compiled for Linux x86_64 (glibc 2.31, from Debian 11).  
If you are using a different architecture (e.g. aarch64) or an incompatible system, you must compile them yourself.

## Compiling the Native Libraries

### Base Library (`libfcitx5_detector.so`)

Requirements:

- A valid JDK installation (with JNI headers)
- CMake 3.10+

Run this in your terminal:

```shell
cd Fcitx5-Enhancer/src/native/base
mkdir build && cd build
cmake ..
make
```

Eventually you'll see a shared library file named `libfcitx5_detector.so` inside the `build` folder.  
Then, place it into folder `.minecraft/.fcitx5-enhancer`.

### Wayland Support (`libwayland_support.so`) (Optional)

Requirements:

- All prerequisites from the base library
- Wayland Protocols (`libwayland-dev wayland-protocols` for Debian)
- PkgConfig Tool (`pkg-config` for Debian)

Run this in your terminal:

```shell
cd Fcitx5-Enhancer/src/native/wayland
mkdir build && cd build
cmake ..
make
```

Finally you'll see a shared library file named `libwayland_support.so` inside the `build` folder.  
Simply place it into folder `.minecraft/.fcitx5-enhancer`.

## License

This project is licensed under [MIT License](/LICENSE.txt).
