cmake_minimum_required(VERSION 3.14)
project(wayland_support)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_FLAGS "-O3 -DNDEBUG")

find_package(JNI REQUIRED)
find_package(PkgConfig REQUIRED)

pkg_check_modules(WAYLAND_CLIENT REQUIRED wayland-client)
find_program(WAYLAND_SCANNER_EXECUTABLE wayland-scanner REQUIRED)

pkg_get_variable(WAYLAND_PROTOCOLS_DIR wayland-protocols pkgdatadir)
if(NOT WAYLAND_PROTOCOLS_DIR)
    message(FATAL_ERROR "Could not find wayland-protocols.pc. Is 'wayland-protocols' installed?")
endif()
message(STATUS "Found Wayland Protocols in: ${WAYLAND_PROTOCOLS_DIR}")

set(TEXT_INPUT_V3_XML "${WAYLAND_PROTOCOLS_DIR}/unstable/text-input/text-input-unstable-v3.xml")
set(TEXT_INPUT_V3_CLIENT_HEADER "${CMAKE_CURRENT_BINARY_DIR}/text-input-unstable-v3-client-protocol.h")
set(TEXT_INPUT_V3_PRIVATE_CODE "${CMAKE_CURRENT_BINARY_DIR}/text-input-unstable-v3-protocol.c")

add_custom_command(
        OUTPUT ${TEXT_INPUT_V3_CLIENT_HEADER} ${TEXT_INPUT_V3_PRIVATE_CODE}
        COMMAND ${WAYLAND_SCANNER_EXECUTABLE} client-header
        ${TEXT_INPUT_V3_XML} ${TEXT_INPUT_V3_CLIENT_HEADER}
        COMMAND ${WAYLAND_SCANNER_EXECUTABLE} private-code
        ${TEXT_INPUT_V3_XML} ${TEXT_INPUT_V3_PRIVATE_CODE}
        DEPENDS ${TEXT_INPUT_V3_XML}
        COMMENT "Generating Wayland protocol sources for text-input-unstable-v3"
)

add_custom_target(
        wayland_protocol_sources
        DEPENDS ${TEXT_INPUT_V3_CLIENT_HEADER} ${TEXT_INPUT_V3_PRIVATE_CODE}
)

add_library(wayland_support SHARED main.cpp ${TEXT_INPUT_V3_PRIVATE_CODE})
add_dependencies(wayland_support wayland_protocol_sources)
target_include_directories(wayland_support PRIVATE
        ${WAYLAND_CLIENT_INCLUDE_DIRS} ${CMAKE_CURRENT_BINARY_DIR}
        ${JAVA_INCLUDE_PATH} ${JAVA_INCLUDE_PATH2}
)
target_link_libraries(wayland_support PRIVATE ${WAYLAND_CLIENT_LIBRARIES})
