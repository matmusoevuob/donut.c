# Spinning 3D ASCII Torus (donut.c) in Multiple Languages

![Spinning ASCII Donut](output.gif)

This repository contains clean, modern, and heavily-commented implementations of Andy Sloane's famous 2006 **donut.c** (the spinning 3D ASCII torus) across the top 10 programming languages.

Instead of the original obfuscated shape, these implementations prioritize clean code structure, readability, and performance while retaining the exact core mathematical algorithms (torus equations, 3D rotations, perspective projection, Z-buffering depth resolution, and surface normal luminance shading).

---

## 🚀 How to Run the Implementations

### 1. Python
Uses standard libraries and standard console printing to render a smooth frame rate.
```bash
python 01-python/donut.py
```
- **File**: [`donut.py`](file:///c:/Users/matmu/main-git/donut/donut.c/01-python/donut.py)
- **Requirements**: Python 3.x (Standard library only)

### 2. JavaScript / Node.js
Uses Node.js standard process buffers and ANSI terminal escapes.
```bash
node 02-javascript/donut.js
```
- **File**: [`donut.js`](file:///c:/Users/matmu/main-git/donut/donut.c/02-javascript/donut.js)
- **Requirements**: Node.js

### 3. Java
Uses standard mathematical methods and a single consolidated `StringBuilder` buffer flushed to standard output to avoid screen flickering.
```bash
java 03-java/Donut.java
```
- **File**: [`Donut.java`](file:///c:/Users/matmu/main-git/donut/donut.c/03-java/Donut.java)
- **Requirements**: Java Development Kit (JDK 11+)

### 4. C#
Uses standard `System.Math` functions and cancellation handlers to restore the console cursor upon exit.
```bash
csc 04-csharp/Donut.cs
./Donut.exe
```
- **File**: [`Donut.cs`](file:///c:/Users/matmu/main-git/donut/donut.c/04-csharp/Donut.cs)
- **Requirements**: .NET SDK or C# compiler (`csc`)

### 5. C++
Modern modern C++ implementation utilizing standard threads, high-performance console rendering, and clean RAII cursor restoration.
```bash
g++ -O3 -std=c++17 05-cpp/donut.cpp -o donut
./donut
```
- **File**: [`donut.cpp`](file:///c:/Users/matmu/main-git/donut/donut.c/05-cpp/donut.cpp)
- **Requirements**: C++17 compatible compiler (e.g., GCC, Clang, MSVC)

### 6. Go
Uses a high-performance Go rendering pipeline, custom buffers, and `time.Ticker` for smooth timing loops.
```bash
go run 06-go/donut.go
```
- **File**: [`donut.go`](file:///c:/Users/matmu/main-git/donut/donut.c/06-go/donut.go)
- **Requirements**: Go 1.16+

### 7. Rust
An extremely performant and safe implementation using Rust's `BufWriter` for high-throughput console I/O and RAII for terminal state safety.
```bash
rustc 07-rust/donut.rs -o donut
./donut
```
- **File**: [`donut.rs`](file:///c:/Users/matmu/main-git/donut/donut.c/07-rust/donut.rs)
- **Requirements**: Rust Compiler (`rustc`)

### 8. Kotlin
A modern JVM-style implementation using standard Kotlin mathematical functions and runtime shutdown hooks.
```bash
kotlinc 08-kotlin/donut.kt -include-runtime -d donut.jar
java -jar donut.jar
```
- **File**: [`donut.kt`](file:///c:/Users/matmu/main-git/donut/donut.c/08-kotlin/donut.kt)
- **Requirements**: Kotlin Compiler (`kotlinc`) & JRE

### 9. Swift
Native command-line Swift script utilising core Foundation math and portable signal trap routines.
```bash
swift 09-swift/donut.swift
```
- **File**: [`donut.swift`](file:///c:/Users/matmu/main-git/donut/donut.c/09-swift/donut.swift)
- **Requirements**: Swift Toolchain

### 10. PHP
A CLI-optimized script leveraging microtime delay loops and standard console escaping sequences.
```bash
php 10-php/donut.php
```
- **File**: [`donut.php`](file:///c:/Users/matmu/main-git/donut/donut.c/10-php/donut.php)
- **Requirements**: PHP 7.4+ CLI

---

## 🧮 How the Mathematics Works

The math behind rendering a 3D torus onto a 2D ASCII screen operates in four main steps:

### 1. Torus Geometry
We generate points on the surface of a torus by taking a circle of radius $R_1$ centered at $(R_2, 0, 0)$ and rotating it around the Y-axis. 
* $\theta$ (theta) sweeps the circle's cross-section.
* $\phi$ (phi) sweeps the circle around the center of the torus.

$$
\begin{aligned}
x_{circle} &= R_2 + R_1 \cos\theta \\
y_{circle} &= R_1 \sin\theta
\end{aligned}
$$

### 2. 3D Rotation Matrix
To rotate the torus in 3D space over time, we rotate the coordinates by angle $A$ (around the X-axis) and angle $B$ (around the Z-axis):

$$
\begin{aligned}
x &= x_{circle} (\cos B \cos\phi + \sin A \sin B \sin\phi) - y_{circle} \cos A \sin B \\
y &= x_{circle} (\sin B \cos\phi - \sin A \cos B \sin\phi) + y_{circle} \cos A \cos B \\
z &= K_2 + \cos A x_{circle} \sin\phi + y_{circle} \sin A
\end{aligned}
$$

### 3. Perspective Projection & Depth buffering (Z-buffer)
To map the 3D coordinates $(x, y, z)$ to a 2D grid $(x_{proj}, y_{proj})$, we compute the depth inverse $ooz = 1/z$. Points further away project closer to the center, while points closer project outward. We keep a `z_buf` depth map to ensure that closer points override further ones.

### 4. Shading and Luminance
We calculate the surface normal at each point and take its dot product with a light source vector (shining from $(0, 1, -1)$). The resulting luminance $L$ determines the brightness character from the ramp:

`.,-~:;=!*#$@`
