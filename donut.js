#!/usr/bin/env node
/**
 * donut.js - A clean, documented Node.js implementation of Andy Sloane's
 * famous spinning 3D ASCII donut (donut.c).
 *
 * Renders a spinning ASCII torus to the terminal using standard perspective
 * projection, Z-buffering, and surface illumination.
 */

const SCREEN_WIDTH = 80;
const SCREEN_HEIGHT = 22;

// Torus dimensions
const R1 = 1;
const R2 = 2;

// Distance scale factors
const K2 = 5;
const K1 = (SCREEN_WIDTH * K2 * 3) / (8 * (R1 + R2));

const SHADE_CHARS = ".,-~:;=!*#$@";

// Rotation angles
let A = 0;
let B = 0;

// Setup terminal
process.stdout.write("\x1b[2J"); // Clear screen once
process.stdout.write("\x1b[?25l"); // Hide cursor

// Restore cursor on exit
const cleanup = () => {
    process.stdout.write("\x1b[?25h\n");
    process.exit(0);
};
process.on("SIGINT", cleanup);
process.on("SIGTERM", cleanup);

function renderFrame() {
    // Buffers
    const screenBuf = Array(SCREEN_WIDTH * SCREEN_HEIGHT).fill(" ");
    const zBuf = Array(SCREEN_WIDTH * SCREEN_HEIGHT).fill(0);

    const cosA = Math.cos(A);
    const sinA = Math.sin(A);
    const cosB = Math.cos(B);
    const sinB = Math.sin(B);

    // theta loops over the circle slice of the torus
    for (let theta = 0; theta < 2 * Math.PI; theta += 0.07) {
        const cosTheta = Math.cos(theta);
        const sinTheta = Math.sin(theta);

        // phi loops over the center revolution of the torus
        for (let phi = 0; phi < 2 * Math.PI; phi += 0.02) {
            const cosPhi = Math.cos(phi);
            const sinPhi = Math.sin(phi);

            // 3D coordinates before rotation
            const circleX = R2 + R1 * cosTheta;
            const circleY = R1 * sinTheta;

            // Apply rotations A (around X axis) and B (around Z axis)
            const x = circleX * (cosB * cosPhi + sinA * sinB * sinPhi) - circleY * cosA * sinB;
            const y = circleX * (sinB * cosPhi - sinA * cosB * sinPhi) + circleY * cosA * cosB;
            const z = K2 + cosA * circleX * sinPhi + circleY * sinA;
            const ooz = 1 / z; // Inverse depth

            // Project 3D point onto 2D screen
            const xp = Math.floor(SCREEN_WIDTH / 2 + 30 * ooz * (cosPhi * circleX * cosB - (circleY * cosA + circleX * sinA * sinPhi) * sinB));
            const yp = Math.floor(SCREEN_HEIGHT / 2 + 15 * ooz * (cosPhi * circleX * sinB + (circleY * cosA + circleX * sinA * sinPhi) * cosB));

            // Calculate luminance dot product with light source (0, 1, -1)
            const L = (
                cosPhi * cosTheta * sinB -
                cosA * cosTheta * sinPhi -
                sinA * sinTheta +
                cosB * (cosA * sinTheta - sinA * cosTheta * sinPhi)
            );

            // Ensure coordinates are within bounds
            if (xp >= 0 && xp < SCREEN_WIDTH && yp >= 0 && yp < SCREEN_HEIGHT) {
                if (L > 0) {
                    const idx = yp * SCREEN_WIDTH + xp;
                    // Check if this point is closer than the current depth buffer value
                    if (ooz > zBuf[idx]) {
                        zBuf[idx] = ooz;
                        const shadeIdx = Math.floor(L * 8);
                        const clampedShadeIdx = Math.min(Math.max(shadeIdx, 0), SHADE_CHARS.length - 1);
                        screenBuf[idx] = SHADE_CHARS[clampedShadeIdx];
                    }
                }
            }
        }
    }

    // Draw the frame
    let output = "\x1b[H"; // Move cursor to top-left
    for (let r = 0; r < SCREEN_HEIGHT; r++) {
        output += screenBuf.slice(r * SCREEN_WIDTH, (r + 1) * SCREEN_WIDTH).join("") + "\n";
    }
    process.stdout.write(output);

    // Update rotation angles for the next frame
    A += 0.04;
    B += 0.02;
}

// Start rendering loop at ~30 FPS (33ms interval)
setInterval(renderFrame, 33);
