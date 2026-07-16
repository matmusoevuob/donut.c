#!/usr/bin/env python3
"""
donut.py - A clean, documented, and optimized Python implementation of Andy Sloane's
famous spinning 3D ASCII donut (donut.c).

This script uses standard mathematical libraries to compute the torus coordinates,
projects them onto a 2D terminal grid, handles occlusion via a Z-buffer, and renders
shading based on the surface normal's relation to a light source.
"""

import math
import os
import sys
import time

# Screen dimensions
SCREEN_WIDTH = 80
SCREEN_HEIGHT = 22

# Torus structural radii
# R1 is the radius of the cross-section circle.
# R2 is the radius of the torus center-line.
R1 = 1
R2 = 2

# Distance parameters
# K2 is the distance of the donut from the camera (Z offset).
# K1 is the scaling factor projecting 3D points onto the 2D terminal grid.
K2 = 5
K1 = SCREEN_WIDTH * K2 * 3 / (8 * (R1 + R2))

# Color/Shading characters mapped from dark to light
SHADE_CHARS = ".,-~:;=!*#$@"

def render_donut():
    """
    Renders a spinning 3D ASCII torus in the terminal.
    """
    # Clear screen initially
    os.system("cls" if os.name == "nt" else "clear")
    # Hide cursor using ANSI escape code
    sys.stdout.write("\x1b[?25l")
    sys.stdout.flush()

    # Rotation angles around X and Z axes
    A = 0.0
    B = 0.0

    try:
        while True:
            # Initialize screen buffer with spaces and Z-buffer with zeros (flat plane far away)
            screen_buf = [" "] * (SCREEN_WIDTH * SCREEN_HEIGHT)
            z_buf = [0.0] * (SCREEN_WIDTH * SCREEN_HEIGHT)

            # Precompute sin/cos of rotation angles for the current frame
            cos_A = math.cos(A)
            sin_A = math.sin(A)
            cos_B = math.cos(B)
            sin_B = math.sin(B)

            # Theta sweeps the cross-sectional circle of the torus (0 to 2*pi)
            theta = 0.0
            while theta < 2 * math.pi:
                cos_theta = math.cos(theta)
                sin_theta = math.sin(theta)

                # Phi sweeps the torus revolution around the Y-axis (0 to 2*pi)
                phi = 0.0
                while phi < 2 * math.pi:
                    cos_phi = math.cos(phi)
                    sin_phi = math.sin(phi)

                    # 1. Torus 3D geometry coordinates before rotation:
                    # x = (R2 + R1*cos_theta) * cos_phi
                    # y = R1*sin_theta
                    # z = -(R2 + R1*cos_theta) * sin_phi
                    # We compute x_circle as (R2 + R1 * cos_theta)
                    x_circle = R2 + R1 * cos_theta
                    y_circle = R1 * sin_theta

                    # 2. Project 3D coordinates onto 2D screen coordinates (x_proj, y_proj).
                    # Terminal character cells are taller than they are wide.
                    # We correct this aspect ratio by multiplying y_proj's scaling factor.
                    # To be mathematically precise and match Sloane's original projection:
                    # xp = int(40 + 30 * ooz * (cos_phi * x_circle * cos_B - (y_circle * cos_A + x_circle * sin_A * sin_phi) * sin_B))
                    # yp = int(12 + 15 * ooz * (cos_phi * x_circle * sin_B + (y_circle * cos_A + x_circle * sin_A * sin_phi) * cos_B))
                    
                    # Note that z here is the distance of the point on the torus from the camera.
                    # z' = y_circle * sin_A - x_circle * cos_A * sin_phi
                    # we add K2 to keep it positive and in front of the camera.
                    z = K2 + cos_A * x_circle * sin_phi + y_circle * sin_A
                    ooz = 1.0 / z

                    # Let's compute xp and yp matching original math
                    xp = int(SCREEN_WIDTH / 2 + 30 * ooz * (cos_phi * x_circle * cos_B - (y_circle * cos_A + x_circle * sin_A * sin_phi) * sin_B))
                    yp = int(SCREEN_HEIGHT / 2 + 15 * ooz * (cos_phi * x_circle * sin_B + (y_circle * cos_A + x_circle * sin_A * sin_phi) * cos_B))

                    # Ensure coordinates are within screen boundaries
                    if 0 <= xp < SCREEN_WIDTH and 0 <= yp < SCREEN_HEIGHT:
                        # 4. Compute Luminance (L) based on the surface normal dot product with the light vector.
                        # Light source vector is assumed to point from direction (0, 1, -1) (top-back-left).
                        # L ranges from -sqrt(2) to sqrt(2). If L > 0, the surface is illuminated.
                        L = (cos_phi * cos_theta * sin_B 
                             - cos_A * cos_theta * sin_phi 
                             - sin_A * sin_theta 
                             + cos_B * (cos_A * sin_theta - sin_A * cos_theta * sin_phi))

                        if L > 0:
                            # Check if this point is closer to the viewer than whatever was rendered there before
                            if ooz > z_buf[yp * SCREEN_WIDTH + xp]:
                                z_buf[yp * SCREEN_WIDTH + xp] = ooz
                                # Map luminance (0.0 to ~1.414) to the scale of character shading (0 to 11)
                                luminance_index = int(L * 8)
                                luminance_index = min(max(luminance_index, 0), len(SHADE_CHARS) - 1)
                                screen_buf[yp * SCREEN_WIDTH + xp] = SHADE_CHARS[luminance_index]

                    phi += 0.02
                theta += 0.07

            # Print frame: move cursor to top-left of the terminal using ANSI escape (\x1b[H)
            sys.stdout.write("\x1b[H")
            for r in range(SCREEN_HEIGHT):
                row_str = "".join(screen_buf[r * SCREEN_WIDTH : (r + 1) * SCREEN_WIDTH])
                sys.stdout.write(row_str + "\n")
            sys.stdout.flush()

            # Increment rotation angles for the next frame
            A += 0.04
            B += 0.02
            
            # FPS limiter (~30 FPS)
            time.sleep(0.03)

    except KeyboardInterrupt:
        # Restore terminal cursor before exiting
        sys.stdout.write("\x1b[?25h")
        sys.stdout.flush()
        print("\nDonut animation stopped.")

if __name__ == "__main__":
    render_donut()
