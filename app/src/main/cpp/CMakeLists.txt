cmake_minimum_required(VERSION 3.10.2)
project("launcher_bridge")

add_library(launcher_bridge SHARED launcher_bridge.c)

find_library(log-lib log)
find_library(android-lib android)

target_link_libraries(launcher_bridge ${log-lib} ${android-lib})
