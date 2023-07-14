cmake_minimum_required(VERSION 3.10)
project(displayutil)

set(CMAKE_CPP_STANDARD 20)

find_package(X11 REQUIRED)

set(JAVA_INCLUDE_PATH "/usr/lib/jvm/java-17-openjdk-amd64/include")
find_package(JNI REQUIRED)
if (JNI_FOUND)
    message (STATUS "JNI_INCLUDE_DIRS=${JNI_INCLUDE_DIRS}")
    message (STATUS "JNI_LIBRARIES=${JNI_LIBRARIES}")
endif()

include_directories(.)
include_directories(${JNI_INCLUDE_DIRS})

add_library(displayutil SHARED displayutil.cpp)
target_link_libraries(displayutil ${X11_LIBRARIES})

