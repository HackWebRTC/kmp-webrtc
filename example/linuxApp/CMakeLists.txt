cmake_minimum_required(VERSION 3.10)
project(linuxApp)

set(CMAKE_CXX_STANDARD 11)
set(CMAKE_CXX_STANDARD_REQUIRED True)

find_package(Qt5 REQUIRED COMPONENTS Widgets)

include_directories(${Qt5Widgets_INCLUDE_DIRS})
include_directories(${CMAKE_CURRENT_SOURCE_DIR}/../../libs/linux/x64)
include_directories(${CMAKE_CURRENT_SOURCE_DIR}/../../libs/windows_linux/include)
include_directories(${CMAKE_CURRENT_SOURCE_DIR}/../common/cpp)

set(CMAKE_AUTOMOC ON)
set(CMAKE_AUTORCC ON)
set(CMAKE_AUTOUIC ON)

add_definitions(-DWEBRTC_LINUX=1)

file(GLOB deps
        ${CMAKE_CURRENT_SOURCE_DIR}/../../libs/linux/x64/libkmp_webrtc.so
        ${CMAKE_CURRENT_SOURCE_DIR}/../../libs/linux/x64/liblinux_pc_client.so
        )

add_executable(${PROJECT_NAME}
        main.cpp
        main_window.cpp
        main_window.h
        loopback.cpp
        ${CMAKE_CURRENT_SOURCE_DIR}/../common/cpp/libKmpWebrtc.cpp
        )

target_link_libraries(${PROJECT_NAME}
        ${Qt5Widgets_LIBRARIES}
        ${deps}
        )

add_executable(loopback
        console_app.cpp
        loopback.cpp
        ${CMAKE_CURRENT_SOURCE_DIR}/../common/cpp/libKmpWebrtc.cpp
        )

target_link_libraries(loopback
        ${deps}
        )
