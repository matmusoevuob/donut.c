package main

import (
	"bytes"
	"fmt"
	"math"
	"os"
	"os/signal"
	"syscall"
	"time"
)

/**
 * donut.go - A clean, documented Go implementation of Andy Sloane's
 * famous spinning 3D ASCII donut (donut.c).
 *
 * Renders a spinning ASCII torus to the terminal using standard perspective
 * projection, Z-buffering, and surface illumination.
 */

const (
	screenWidth  = 80
	screenHeight = 22

	// Torus dimensions
	r1 = 1.0
	r2 = 2.0

	// Camera offset
	k2 = 5.0
)

var (
	// Scale factor mapping 3D space to 2D terminal grid
	k1         = float64(screenWidth) * k2 * 3.0 / (8.0 * (r1 + r2))
	shadeChars = ".,-~:;=!*#$@"
)

func main() {
	// Clear screen and hide cursor
	fmt.Print("\x1b[2J\x1b[?25l")

	// Set up channel to capture signal interrupts to restore cursor on Exit
	sigChan := make(chan os.Signal, 1)
	signal.Notify(sigChan, os.Interrupt, syscall.SIGTERM)
	go func() {
		<-sigChan
		// Restore cursor
		fmt.Print("\x1b[?25h\x1b[H\n")
		os.Exit(0)
	}()

	var a, b float64

	// Efficient ticker for frame timing (~33 FPS)
	ticker := time.NewTicker(30 * time.Millisecond)
	defer ticker.Stop()

	for range ticker.C {
		screenBuf := make([]byte, screenWidth*screenHeight)
		zBuf := make([]float64, screenWidth*screenHeight)

		// Pre-fill screen buffer with spaces
		for i := range screenBuf {
			screenBuf[i] = ' '
		}

		cosA, sinA := math.Cos(a), math.Sin(a)
		cosB, sinB := math.Cos(b), math.Sin(b)

		// theta sweeps the cross-sectional circle of the torus (0 to 2*pi)
		for theta := 0.0; theta < 2.0*math.Pi; theta += 0.07 {
			cosTheta, sinTheta := math.Cos(theta), math.Sin(theta)

			// phi sweeps the torus revolution around the Y-axis (0 to 2*pi)
			for phi := 0.0; phi < 2.0*math.Pi; phi += 0.02 {
				cosPhi, sinPhi := math.Cos(phi), math.Sin(phi)

				// 3D coordinates of the torus point before rotation
				circleX := r2 + r1*cosTheta
				circleY := r1 * sinTheta

				// 3D rotations: rotate around X-axis (angle A) and Z-axis (angle B)
				x := circleX*(cosB*cosPhi+sinA*sinB*sinPhi) - circleY*cosA*sinB
				y := circleX*(sinB*cosPhi-sinA*cosB*sinPhi) + circleY*cosA*cosB
				z := k2 + cosA*circleX*sinPhi + circleY*sinA
				ooz := 1.0 / z // One over Z (perspective projection)

				// Project 3D point onto 2D screen coordinates
				xp := int(float64(screenWidth)/2.0 + 30.0*ooz*(cosPhi*circleX*cosB-(circleY*cosA+circleX*sinA*sinPhi)*sinB))
				yp := int(float64(screenHeight)/2.0 + 15.0*ooz*(cosPhi*circleX*sinB+(circleY*cosA+circleX*sinA*sinPhi)*cosB))

				// Calculate luminance dot product with light source (0, 1, -1)
				L := cosPhi*cosTheta*sinB -
					cosA*cosTheta*sinPhi -
					sinA*sinTheta +
					cosB*(cosA*sinTheta-sinA*cosTheta*sinPhi)

				// Check bounds and project if visible
				if xp >= 0 && xp < screenWidth && yp >= 0 && yp < screenHeight {
					if L > 0.0 {
						idx := yp*screenWidth + xp
						if ooz > zBuf[idx] {
							zBuf[idx] = ooz
							shadeIdx := int(L * 8.0)
							if shadeIdx < 0 {
								shadeIdx = 0
							}
							if shadeIdx >= len(shadeChars) {
								shadeIdx = len(shadeChars) - 1
							}
							screenBuf[idx] = shadeChars[shadeIdx]
						}
					}
				}
			}
		}

		// Frame buffering: move cursor to top-left (\x1b[H) and write screen buffer
		var output bytes.Buffer
		output.WriteString("\x1b[H")
		for r := 0; r < screenHeight; r++ {
			output.Write(screenBuf[r*screenWidth : (r+1)*screenWidth])
			output.WriteByte('\n')
		}
		os.Stdout.Write(output.Bytes())

		// Increment rotation angles
		a += 0.04
		b += 0.02
	}
}
