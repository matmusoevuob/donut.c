<?php
/**
 * donut.php - A clean, documented PHP CLI implementation of Andy Sloane's
 * famous spinning 3D ASCII donut (donut.c).
 *
 * Renders a spinning ASCII torus to the terminal using standard perspective
 * projection, Z-buffering, and surface illumination.
 */

define('SCREEN_WIDTH', 80);
define('SCREEN_HEIGHT', 22);

// Torus dimensions
define('R1', 1.0);
define('R2', 2.0);

// Camera and projection parameters
define('K2', 5.0);
define('K1', (SCREEN_WIDTH * K2 * 3.0) / (8.0 * (R1 + R2)));

$shadeChars = ".,-~:;=!*#$@";

// Clear screen and hide cursor
echo "\x1b[2J\x1b[?25l";

// Restore cursor on exit
register_shutdown_function(function() {
    echo "\x1b[?25h\x1b[H\n";
});

// Setup signal handling if supported (posix)
if (function_exists('pcntl_signal')) {
    declare(ticks = 1);
    pcntl_signal(SIGINT, function() {
        exit(0);
    });
    pcntl_signal(SIGTERM, function() {
        exit(0);
    });
}

$A = 0.0;
$B = 0.0;

while (true) {
    // Reset buffers
    $screenBuf = array_fill(0, SCREEN_WIDTH * SCREEN_HEIGHT, ' ');
    $zBuf = array_fill(0, SCREEN_WIDTH * SCREEN_HEIGHT, 0.0);

    $cosA = cos($A);
    $sinA = sin($A);
    $cosB = cos($B);
    $sinB = sin($B);

    // theta sweeps the cross-sectional circle of the torus (0 to 2*pi)
    for ($theta = 0.0; $theta < 2.0 * M_PI; $theta += 0.07) {
        $cosTheta = cos($theta);
        $sinTheta = sin($theta);

        // phi sweeps the torus revolution around the Y-axis (0 to 2*pi)
        for ($phi = 0.0; $phi < 2.0 * M_PI; $phi += 0.02) {
            $cosPhi = cos($phi);
            $sinPhi = sin($phi);

            // 3D coordinates of the torus point before rotation
            $circleX = R2 + R1 * $cosTheta;
            $circleY = R1 * $sinTheta;

            // 3D rotations: rotate around X-axis (angle A) and Z-axis (angle B)
            $x = $circleX * ($cosB * $cosPhi + $sinA * $sinB * $sinPhi) - $circleY * $cosA * $sinB;
            $y = $circleX * ($sinB * $cosPhi - $sinA * $cosB * $sinPhi) + $circleY * $cosA * $cosB;
            $z = K2 + $cosA * $circleX * $sinPhi + $circleY * $sinA;
            $ooz = 1.0 / $z; // One over Z (perspective projection)

            // Project 3D point onto 2D screen coordinates
            $xp = (int)(SCREEN_WIDTH / 2.0 + 30.0 * $ooz * ($cosPhi * $circleX * $cosB - ($circleY * $cosA + $circleX * $sinA * $sinPhi) * $sinB));
            $yp = (int)(SCREEN_HEIGHT / 2.0 + 15.0 * $ooz * ($cosPhi * $circleX * $sinB + ($circleY * $cosA + $circleX * $sinA * $sinPhi) * $cosB));

            // Calculate luminance dot product with light source (0, 1, -1)
            $L = $cosPhi * $cosTheta * $sinB
               - $cosA * $cosTheta * $sinPhi
               - $sinA * $sinTheta
               + $cosB * ($cosA * $sinTheta - $sinA * $cosTheta * $sinPhi);

            // Check bounds and project if visible
            if ($xp >= 0 && $xp < SCREEN_WIDTH && $yp >= 0 && $yp < SCREEN_HEIGHT) {
                if ($L > 0.0) {
                    $idx = $yp * SCREEN_WIDTH + $xp;
                    if ($ooz > $zBuf[$idx]) {
                        $zBuf[$idx] = $ooz;
                        $shadeIdx = (int)($L * 8.0);
                        $clampedIdx = min(max($shadeIdx, 0), strlen($shadeChars) - 1);
                        $screenBuf[$idx] = $shadeChars[$clampedIdx];
                    }
                }
            }
        }
    }

    // Render the frame to terminal: move cursor to home position (\x1b[H) and print buffer
    $output = "\x1b[H";
    for ($r = 0; $r < SCREEN_HEIGHT; $r++) {
        $row = "";
        for ($c = 0; $c < SCREEN_WIDTH; $c++) {
            $row .= $screenBuf[$r * SCREEN_WIDTH + $c];
        }
        $output .= $row . "\n";
    }
    echo $output;

    // Increment rotation angles
    $A += 0.04;
    $B += 0.02;

    // Limit frame rate to ~30 FPS (30ms = 30000 microseconds)
    usleep(30000);
}
