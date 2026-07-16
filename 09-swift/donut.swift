import Foundation

/**
 * donut.swift - A clean, documented Swift implementation of Andy Sloane's
 * famous spinning 3D ASCII donut (donut.c).
 *
 * Renders a spinning ASCII torus to the terminal using standard perspective
 * projection, Z-buffering, and surface illumination.
 */

let screenWidth = 80
let screenHeight = 22

// Torus dimensions
let r1: Double = 1.0
let r2: Double = 2.0

// Camera and projection parameters
let k2: Double = 5.0
let k1: Double = Double(screenWidth) * k2 * 3.0 / (8.0 * (r1 + r2))

let shadeChars = Array(".,-~:;=!*#$@")

func main() {
    // Clear screen and hide cursor
    print("\u{001B}[2J\u{001B}[?25l", terminator: "")
    fflush(nil)

    // Set up exit handlers to restore cursor
    signal(SIGINT) { _ in
        print("\u{001B}[?25h\u{001B}[H")
        fflush(nil)
        exit(0)
    }
    signal(SIGTERM) { _ in
        print("\u{001B}[?25h\u{001B}[H")
        fflush(nil)
        exit(0)
    }

    var a: Double = 0.0
    var b: Double = 0.0

    while true {
        var screenBuf = Array(repeating: " ", count: screenWidth * screenHeight)
        var zBuf = Array(repeating: 0.0, count: screenWidth * screenHeight)

        let cosA = cos(a)
        let sinA = sin(a)
        let cosB = cos(b)
        let sinB = sin(b)

        // theta sweeps the cross-sectional circle of the torus (0 to 2*pi)
        var theta: Double = 0.0
        while theta < 2.0 * Double.pi {
            let cosTheta = cos(theta)
            let sinTheta = sin(theta)

            // phi sweeps the torus revolution around the Y-axis (0 to 2*pi)
            var phi: Double = 0.0
            while phi < 2.0 * Double.pi {
                let cosPhi = cos(phi)
                let sinPhi = sin(phi)

                // 3D coordinates of the torus point before rotation
                let circleX = r2 + r1 * cosTheta
                let circleY = r1 * sinTheta

                // 3D rotations: rotate around X-axis (angle A) and Z-axis (angle B)
                let x = circleX * (cosB * cosPhi + sinA * sinB * sinPhi) - circleY * cosA * sinB
                let y = circleX * (sinB * cosPhi - sinA * cosB * sinPhi) + circleY * cosA * cosB
                let z = k2 + cosA * circleX * sinPhi + circleY * sinA
                let ooz = 1.0 / z // One over Z (perspective projection)

                // Project 3D point onto 2D screen coordinates
                let xp = Int(Double(screenWidth) / 2.0 + 30.0 * ooz * (cosPhi * circleX * cosB - (circleY * cosA + circleX * sinA * sinPhi) * sinB))
                let yp = Int(Double(screenHeight) / 2.0 + 15.0 * ooz * (cosPhi * circleX * sinB + (circleY * cosA + circleX * sinA * sinPhi) * cosB))

                // Calculate luminance dot product with light source (0, 1, -1)
                let L = cosPhi * cosTheta * sinB
                      - cosA * cosTheta * sinPhi
                      - sinA * sinTheta
                      + cosB * (cosA * sinTheta - sinA * cosTheta * sinPhi)

                // Check bounds and project if visible
                if xp >= 0 && xp < screenWidth && yp >= 0 && yp < screenHeight {
                    if L > 0.0 {
                        let idx = yp * screenWidth + xp
                        if ooz > zBuf[idx] {
                            zBuf[idx] = ooz
                            let shadeIdx = Int(L * 8.0)
                            let clampedIdx = min(max(shadeIdx, 0), shadeChars.count - 1)
                            screenBuf[idx] = String(shadeChars[clampedIdx])
                        }
                    }
                }

                phi += 0.02
            }
            theta += 0.07
        }

        // Render the frame to terminal: move cursor to home position (\u{001B}[H) and print buffer
        var output = "\u{001B}[H"
        for r in 0..<screenHeight {
            let row = screenBuf[r * screenWidth ..< (r + 1) * screenWidth].joined()
            output += row + "\n"
        }
        print(output, terminator: "")
        fflush(nil)

        // Increment rotation angles
        a += 0.04
        b += 0.02

        // Limit frame rate to ~30 FPS (30,000 microseconds)
        usleep(30000)
    }
}

main()
