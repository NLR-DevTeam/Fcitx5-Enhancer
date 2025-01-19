# Fcitx5-Enhancer

This is a mod made for Minecraft, which provides compatibility with Fcitx5.

## Why?

When typing using fcitx5 in Minecraft, and when the hotkeys that the IME uses (e.g. Backspace, Tab, Enter) are pressed,
the IME processes the key, and at the same time, Minecraft processes the key too.

And then you'll be interrupted. That's crazy.

So, I wrote this mod, in order to deal with this annoying problem.

## Notice

This mod uses [a native library](/src/native/) to detect fcitx5 input window.  
And the default one is designed for Linux x86_64 (glibc 2.40, from Arch Linux).  
if your system is incompatible with it, please compile one yourself as following.

## Compiling the native library

Requirements:
 - A valid JDK installation (with JNI headers)
 - CMake

First, clone this repository, using:
```shell
git clone https://github.com/NLR-DevTeam/Fcitx5-Enhancer --depth 1
```

Then, compile the native library using:
```shell
cd Fcitx5-Enhancer/src/native
mkdir build && cd build
cmake ..
make
```

And finally, you'll see the library named `libfcitx5_detector.so` under the `build` folder.  
Rename it to `native.so` and place it inside folder `.minecraft/.fcitx5-enhancer`.

## License
This project is licensed under [MIT License](/LICENSE.txt).