import java.util.Arrays;

/**
 * Donut.java - A clean, documented, and optimized Java implementation of Andy Sloane's
 * famous spinning 3D ASCII donut (donut.c).
 *
 * Renders a spinning ASCII torus to the terminal using standard perspective
 * projection, Z-buffering, and surface illumination.
 */
public class Donut {
    private static final int SCREEN_WIDTH = 80;
    private static final int SCREEN_HEIGHT = 22;

    // Torus dimensions
    private static final double R1 = 1.0;
    private static final double R2 = 2.0;

    // Camera and projection parameters
    private static final double K2 = 5.0;
    private static final double K1 = (SCREEN_WIDTH * K2 * 3.0) / (8.0 * (R1 + R2));

    private static final String SHADE_CHARS = ".,-~:;=!*#$@";

    public static void main(String[] args) {
        // Clear screen and hide cursor
        System.out.print("\u001B[2J\u001B[?25l");
        System.out.flush();

        // Add shutdown hook to restore cursor on Exit
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.print("\u001B[?25h\u001B[H\n");
            System.out.flush();
        }));

        double A = 0.0;
        double B = 0.0;

        char[] screenBuf = new char[SCREEN_WIDTH * SCREEN_HEIGHT];
        double[] zBuf = new double[SCREEN_WIDTH * SCREEN_HEIGHT];

        try {
            while (true) {
                // Initialize screen buffer with spaces and Z-buffer with zeros
                Arrays.fill(screenBuf, ' ');
                Arrays.fill(zBuf, 0.0);

                double cosA = Math.cos(A);
                double sinA = Math.sin(A);
                double cosB = Math.cos(B);
                double sinB = Math.sin(B);

                // theta sweeps the cross-sectional circle of the torus (0 to 2*pi)
                for (double theta = 0.0; theta < 2.0 * Math.PI; theta += 0.07) {
                    double cosTheta = Math.cos(theta);
                    double sinTheta = Math.sin(theta);

                    // phi sweeps the torus revolution around the Y-axis (0 to 2*pi)
                    for (double phi = 0.0; phi < 2.0 * Math.PI; phi += 0.02) {
                        double cosPhi = Math.cos(phi);
                        double sinPhi = Math.sin(phi);

                        // 3D coordinates of the torus point before rotation
                        double circleX = R2 + R1 * cosTheta;
                        double circleY = R1 * sinTheta;

                        // 3D rotations: rotate around X-axis (angle A) and Z-axis (angle B)
                        double x = circleX * (cosB * cosPhi + sinA * sinB * sinPhi) - circleY * cosA * sinB;
                        double y = circleX * (sinB * cosPhi - sinA * cosB * sinPhi) + circleY * cosA * cosB;
                        double z = K2 + cosA * circleX * sinPhi + circleY * sinA;
                        double ooz = 1.0 / z; // One over Z (perspective projection)

                        // Project 3D point onto 2D screen coordinates
                        int xp = (int) (SCREEN_WIDTH / 2.0 + 30.0 * ooz * (cosPhi * circleX * cosB - (circleY * cosA + circleX * sinA * sinPhi) * sinB));
                        int yp = (int) (SCREEN_HEIGHT / 2.0 + 15.0 * ooz * (cosPhi * circleX * sinB + (circleY * cosA + circleX * sinA * sinPhi) * cosB));

                        // Calculate luminance dot product with light source (0, 1, -1)
                        double L = cosPhi * cosTheta * sinB 
                                 - cosA * cosTheta * sinPhi 
                                 - sinA * sinTheta 
                                 + cosB * (cosA * sinTheta - sinA * cosTheta * sinPhi);

                        // Check bounds and project if visible
                        if (xp >= 0 && xp < SCREEN_WIDTH && yp >= 0 && yp < SCREEN_HEIGHT) {
                            if (L > 0.0) {
                                int idx = yp * SCREEN_WIDTH + xp;
                                if (ooz > zBuf[idx]) {
                                    zBuf[idx] = ooz;
                                    int shadeIdx = (int) (L * 8.0);
                                    shadeIdx = Math.min(Math.max(shadeIdx, 0), SHADE_CHARS.length() - 1);
                                    screenBuf[idx] = SHADE_CHARS.charAt(shadeIdx);
                                }
                            }
                        }
                    }
                }

                // Render the frame to terminal using standard output
                // Move cursor to home position (\u001B[H) and construct screen image
                StringBuilder frame = new StringBuilder("\u001B[H");
                for (int r = 0; r < SCREEN_HEIGHT; r++) {
                    frame.append(screenBuf, r * SCREEN_WIDTH, SCREEN_WIDTH).append('\n');
                }
                System.out.print(frame.toString());
                System.out.flush();

                // Increment rotation angles
                A += 0.04;
                B += 0.02;

                // Frame rate limiter
                Thread.sleep(30);
            }
        } catch (InterruptedException e) {
            // Restore cursor before exit
            System.out.print("\u001B[?25h\n");
            System.out.flush();
        }
    }
}
