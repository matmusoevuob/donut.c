import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.max

/**
 * donut.kt - A clean, documented Kotlin implementation of Andy Sloane's
 * famous spinning 3D ASCII donut (donut.c).
 *
 * Renders a spinning ASCII torus to the terminal using standard perspective
 * projection, Z-buffering, and surface illumination.
 */

const val SCREEN_WIDTH = 80
const val SCREEN_HEIGHT = 22

// Torus dimensions
const val R1 = 1.0
const val R2 = 2.0

// Camera and projection parameters
const val K2 = 5.0
const val K1 = (SCREEN_WIDTH * K2 * 3.0) / (8.0 * (R1 + R2))

const val SHADE_CHARS = ".,-~:;=!*#$@"

fun main() {
    // Clear screen and hide cursor
    print("\u001B[2J\u001B[?25l")
    System.out.flush()

    // Register shutdown hook to restore cursor on Exit
    Runtime.getRuntime().addShutdownHook(Thread {
        print("\u001B[?25h\u001B[H\n")
        System.out.flush()
    })

    var aAngle = 0.0
    var bAngle = 0.0

    val screenBuf = CharArray(SCREEN_WIDTH * SCREEN_HEIGHT)
    val zBuf = DoubleArray(SCREEN_WIDTH * SCREEN_HEIGHT)

    try {
        while (true) {
            // Reset buffers
            screenBuf.fill(' ')
            zBuf.fill(0.0)

            val cosA = cos(aAngle)
            val sinA = sin(aAngle)
            val cosB = cos(bAngle)
            val sinB = sin(bAngle)

            // theta sweeps the cross-sectional circle of the torus (0 to 2*pi)
            var theta = 0.0
            while (theta < 2.0 * PI) {
                val cosTheta = cos(theta)
                val sinTheta = sin(theta)

                // phi sweeps the torus revolution around the Y-axis (0 to 2*pi)
                var phi = 0.0
                while (phi < 2.0 * PI) {
                    val cosPhi = cos(phi)
                    val sinPhi = sin(phi)

                    // 3D coordinates of the torus point before rotation
                    val circleX = R2 + R1 * cosTheta
                    val circleY = R1 * sinTheta

                    // 3D rotations: rotate around X-axis (angle A) and Z-axis (angle B)
                    val x = circleX * (cosB * cosPhi + sinA * sinB * sinPhi) - circleY * cosA * sinB
                    val y = circleX * (sinB * cosPhi - sinA * cosB * sinPhi) + circleY * cosA * cosB
                    val z = K2 + cosA * circleX * sinPhi + circleY * sinA
                    val ooz = 1.0 / z // One over Z (perspective projection)

                    // Project 3D point onto 2D screen coordinates
                    val xp = (SCREEN_WIDTH / 2.0 + 30.0 * ooz * (cosPhi * circleX * cosB - (circleY * cosA + circleX * sinA * sinPhi) * sinB)).toInt()
                    val yp = (SCREEN_HEIGHT / 2.0 + 15.0 * ooz * (cosPhi * circleX * sinB + (circleY * cosA + circleX * sinA * sinPhi) * cosB)).toInt()

                    // Calculate luminance dot product with light source (0, 1, -1)
                    val l = cosPhi * cosTheta * sinB -
                            cosA * cosTheta * sinPhi -
                            sinA * sinTheta +
                            cosB * (cosA * sinTheta - sinA * cosTheta * sinPhi)

                    // Check bounds and project if visible
                    if (xp in 0 until SCREEN_WIDTH && yp in 0 until SCREEN_HEIGHT) {
                        if (l > 0.0) {
                            val idx = yp * SCREEN_WIDTH + xp
                            if (ooz > zBuf[idx]) {
                                zBuf[idx] = ooz
                                val shadeIdx = (l * 8.0).toInt()
                                val clampedIdx = min(max(shadeIdx, 0), SHADE_CHARS.length - 1)
                                screenBuf[idx] = SHADE_CHARS[clampedIdx]
                            }
                        }
                    }

                    phi += 0.02
                }
                theta += 0.07
            }

            // Render the frame to terminal: move cursor to home position (\u001B[H) and print buffer
            val frame = StringBuilder("\u001B[H")
            for (r in 0 until SCREEN_HEIGHT) {
                frame.append(screenBuf, r * SCREEN_WIDTH, SCREEN_WIDTH).append('\n')
            }
            print(frame.toString())
            System.out.flush()

            // Increment rotation angles
            aAngle += 0.04
            bAngle += 0.02

            // Limit frame rate to ~30 FPS (30ms sleep)
            Thread.sleep(30)
        }
    } catch (e: InterruptedException) {
        print("\u001B[?25h\n")
        System.out.flush()
    }
}
