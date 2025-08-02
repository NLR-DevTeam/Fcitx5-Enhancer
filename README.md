# Fcitx5-Enhancer

This is a mod made for Minecraft, which provides compatibility with Fcitx5.

## Why?

When typing using fcitx5 in Minecraft, and when the hotkeys that the IME uses (e.g. Backspace, Tab, Enter) are pressed,
the IME processes the key, and at the same time, Minecraft processes the key too.

And then you'll be interrupted. That's crazy. So I wrote this mod, in order to deal with this annoying problem.

---

We provides a highly configurable IMBlocker. (Mod Menu & Cloth Config are required to open the config screen.)

Besides, we added IME support for native Wayland environment.

## Building this Mod

Simply run this in your terminal:

```shell
git clone https://github.com/NLR-DevTeam/Fcitx5-Enhancer --depth 1 --recursive
cd Fcitx5-Enhancer
./gradlew build --no-daemon
```

Then you'll find the artifacts inside `fabricWrapper/build/libs`.

## Notice

This mod uses [native libraries](/src/native) to implement its functionalities.
And the built-in libraries is compiled for Linux x86_64 (glibc 2.31, from Debian 11).  
if your system is incompatible with it, please compile one yourself as follows.

## Compiling the Native Libraries

### Base

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

### Wayland Support (Optional)

Requirements:

- A valid JDK installation (with JNI headers)
- Wayland Protocols (`libwayland-dev wayland-protocols` for Debian)
- PkgConfig Tool (`pkg-config` for Debian)
- CMake 3.10+

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
