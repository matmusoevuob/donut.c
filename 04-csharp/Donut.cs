using System;
using System.Text;
using System.Threading;

namespace SpinningDonut
{
    /**
     * Donut.cs - A clean, documented, and optimized C#/.NET implementation of Andy Sloane's
     * famous spinning 3D ASCII donut (donut.c).
     *
     * Renders a spinning ASCII torus to the terminal using standard perspective
     * projection, Z-buffering, and surface illumination.
     */
    class Program
    {
        private const int SCREEN_WIDTH = 80;
        private const int SCREEN_HEIGHT = 22;

        // Torus dimensions
        private const double R1 = 1.0;
        private const double R2 = 2.0;

        // Camera and projection parameters
        private const double K2 = 5.0;
        private const double K1 = (SCREEN_WIDTH * K2 * 3.0) / (8.0 * (R1 + R2));

        private const string SHADE_CHARS = ".,-~:;=!*#$@";

        static void Main(string[] args)
        {
            // Clear screen and hide cursor
            Console.Write("\x1b[2J\x1b[?25l");
            Console.Out.Flush();

            // Set up console cancellation to restore cursor on Exit
            Console.CancelKeyPress += (sender, e) =>
            {
                Console.Write("\x1b[?25h\x1b[H\n");
                Console.Out.Flush();
                Environment.Exit(0);
            };

            double A = 0.0;
            double B = 0.0;

            char[] screenBuf = new char[SCREEN_WIDTH * SCREEN_HEIGHT];
            double[] zBuf = new double[SCREEN_WIDTH * SCREEN_HEIGHT];

            try
            {
                while (true)
                {
                    // Initialize screen buffer with spaces and Z-buffer with zeros
                    Array.Fill(screenBuf, ' ');
                    Array.Fill(zBuf, 0.0);

                    double cosA = Math.Cos(A);
                    double sinA = Math.Sin(A);
                    double cosB = Math.Cos(B);
                    double sinB = Math.Sin(B);

                    // theta sweeps the cross-sectional circle of the torus (0 to 2*pi)
                    for (double theta = 0.0; theta < 2.0 * Math.PI; theta += 0.07)
                    {
                        double cosTheta = Math.Cos(theta);
                        double sinTheta = Math.Sin(theta);

                        // phi sweeps the torus revolution around the Y-axis (0 to 2*pi)
                        for (double phi = 0.0; phi < 2.0 * Math.PI; phi += 0.02)
                        {
                            double cosPhi = Math.Cos(phi);
                            double sinPhi = Math.Sin(phi);

                            // 3D coordinates of the torus point before rotation
                            double circleX = R2 + R1 * cosTheta;
                            double circleY = R1 * sinTheta;

                            // 3D rotations: rotate around X-axis (angle A) and Z-axis (angle B)
                            double x = circleX * (cosB * cosPhi + sinA * sinB * sinPhi) - circleY * cosA * sinB;
                            double y = circleX * (sinB * cosPhi - sinA * cosB * sinPhi) + circleY * cosA * cosB;
                            double z = K2 + cosA * circleX * sinPhi + circleY * sinA;
                            double ooz = 1.0 / z; // One over Z (perspective projection)

                            // Project 3D point onto 2D screen coordinates
                            int xp = (int)(SCREEN_WIDTH / 2.0 + 30.0 * ooz * (cosPhi * circleX * cosB - (circleY * cosA + circleX * sinA * sinPhi) * sinB));
                            int yp = (int)(SCREEN_HEIGHT / 2.0 + 15.0 * ooz * (cosPhi * circleX * sinB + (circleY * cosA + circleX * sinA * sinPhi) * cosB));

                            // Calculate luminance dot product with light source (0, 1, -1)
                            double L = cosPhi * cosTheta * sinB 
                                     - cosA * cosTheta * sinPhi 
                                     - sinA * sinTheta 
                                     + cosB * (cosA * sinTheta - sinA * cosTheta * sinPhi);

                            // Check bounds and project if visible
                            if (xp >= 0 && xp < SCREEN_WIDTH && yp >= 0 && yp < SCREEN_HEIGHT)
                            {
                                if (L > 0.0)
                                {
                                    int idx = yp * SCREEN_WIDTH + xp;
                                    if (ooz > zBuf[idx])
                                    {
                                        zBuf[idx] = ooz;
                                        int shadeIdx = (int)(L * 8.0);
                                        shadeIdx = Math.Min(Math.Max(shadeIdx, 0), SHADE_CHARS.Length - 1);
                                        screenBuf[idx] = SHADE_CHARS[shadeIdx];
                                    }
                                }
                            }
                        }
                    }

                    // Render the frame to terminal using standard output
                    // Move cursor to home position (\x1b[H) and construct screen image
                    StringBuilder frame = new StringBuilder("\x1b[H");
                    for (int r = 0; r < SCREEN_HEIGHT; r++)
                    {
                        frame.Append(screenBuf, r * SCREEN_WIDTH, SCREEN_WIDTH).Append('\n');
                    }
                    Console.Write(frame.ToString());
                    Console.Out.Flush();

                    // Increment rotation angles
                    A += 0.04;
                    B += 0.02;

                    // Frame rate limiter (~30 FPS)
                    Thread.Sleep(30);
                }
            }
            catch (Exception)
            {
                // Restore cursor before exit
                Console.Write("\x1b[?25h\n");
                Console.Out.Flush();
            }
        }
    }
}
