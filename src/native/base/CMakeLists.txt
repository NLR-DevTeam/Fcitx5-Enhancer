cmake_minimum_required(VERSION 3.10)
project(fcitx5_detector)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_FLAGS "-O3 -DNDEBUG")

find_package(X11 REQUIRED)
find_package(JNI REQUIRED)

add_library(fcitx5_detector SHARED main.cpp)

target_link_libraries(fcitx5_detector PRIVATE X11)
include_directories(${JAVA_INCLUDE_PATH} ${JAVA_INCLUDE_PATH2})
