#include <iostream>
#include <vector>
#include <cmath>
#include <thread>
#include <chrono>
#include <string>
#include <csignal>
#include <algorithm>

// Screen dimensions
constexpr int SCREEN_WIDTH = 80;
constexpr int SCREEN_HEIGHT = 22;

// Torus dimensions
constexpr double R1 = 1.0;
constexpr double R2 = 2.0;

// Camera and projection parameters
constexpr double K2 = 5.0;
constexpr double K1 = (SCREEN_WIDTH * K2 * 3.0) / (8.0 * (R1 + R2));

const std::string SHADE_CHARS = ".,-~:;=!*#$@";

/**
 * Helper class for RAII-based cursor restoration on exit.
 */
struct CursorRestorer {
    CursorRestorer() {
        // Clear screen and hide cursor
        std::cout << "\x1b[2J\x1b[?25l" << std::flush;
    }
    ~CursorRestorer() {
        // Show cursor and move to next line
        std::cout << "\x1b[?25h\x1b[H\n" << std::flush;
    }
};

void signalHandler(int signum) {
    std::exit(signum);
}

int main() {
    // Register signal handlers to trigger destructor of global/static variables if exit is called
    std::signal(SIGINT, signalHandler);
    std::signal(SIGTERM, signalHandler);

    CursorRestorer restorer;

    double A = 0.0;
    double B = 0.0;

    std::vector<char> screen_buf(SCREEN_WIDTH * SCREEN_HEIGHT, ' ');
    std::vector<double> z_buf(SCREEN_WIDTH * SCREEN_HEIGHT, 0.0);

    const double pi = 3.14159265358979323846;

    while (true) {
        // Reset buffers
        std::fill(screen_buf.begin(), screen_buf.end(), ' ');
        std::fill(z_buf.begin(), z_buf.end(), 0.0);

        double cos_A = std::cos(A);
        double sin_A = std::sin(A);
        double cos_B = std::cos(B);
        double sin_B = std::sin(B);

        // theta sweeps the cross-sectional circle of the torus (0 to 2*pi)
        for (double theta = 0.0; theta < 2.0 * pi; theta += 0.07) {
            double cos_theta = std::cos(theta);
            double sin_theta = std::sin(theta);

            // phi sweeps the torus revolution around the Y-axis (0 to 2*pi)
            for (double phi = 0.0; phi < 2.0 * pi; phi += 0.02) {
                double cos_phi = std::cos(phi);
                double sin_phi = std::sin(phi);

                // 3D coordinates of the torus point before rotation
                double circle_x = R2 + R1 * cos_theta;
                double circle_y = R1 * sin_theta;

                // 3D rotations: rotate around X-axis (angle A) and Z-axis (angle B)
                double x = circle_x * (cos_B * cos_phi + sin_A * sin_B * sin_phi) - circle_y * cos_A * sin_B;
                double y = circle_x * (sin_B * cos_phi - sin_A * cos_B * sin_phi) + circle_y * cos_A * cos_B;
                double z = K2 + cos_A * circle_x * sin_phi + circle_y * sin_A;
                double ooz = 1.0 / z; // One over Z (perspective projection)

                // Project 3D point onto 2D screen coordinates
                int xp = static_cast<int>(SCREEN_WIDTH / 2.0 + 30.0 * ooz * (cos_phi * circle_x * cos_B - (circle_y * cos_A + circle_x * sin_A * sin_phi) * sin_B));
                int yp = static_cast<int>(SCREEN_HEIGHT / 2.0 + 15.0 * ooz * (cos_phi * circle_x * sin_B + (circle_y * cos_A + circle_x * sin_A * sin_phi) * cos_B));

                // Calculate luminance dot product with light source (0, 1, -1)
                double L = cos_phi * cos_theta * sin_B 
                         - cos_A * cos_theta * sin_phi 
                         - sin_A * sin_theta 
                         + cos_B * (cos_A * sin_theta - sin_A * cos_theta * sin_phi);

                // Check bounds and project if visible
                if (xp >= 0 && xp < SCREEN_WIDTH && yp >= 0 && yp < SCREEN_HEIGHT) {
                    if (L > 0.0) {
                        int idx = yp * SCREEN_WIDTH + xp;
                        if (ooz > z_buf[idx]) {
                            z_buf[idx] = ooz;
                            int shade_idx = static_cast<int>(L * 8.0);
                            int clamped_idx = std::min(std::max(shade_idx, 0), static_cast<int>(SHADE_CHARS.length()) - 1);
                            screen_buf[idx] = SHADE_CHARS[clamped_idx];
                        }
                    }
                }
            }
        }

        // Render the frame to terminal: move cursor to home position (\x1b[H) and print buffer
        std::string frame = "\x1b[H";
        for (int r = 0; r < SCREEN_HEIGHT; ++r) {
            frame.append(&screen_buf[r * SCREEN_WIDTH], SCREEN_WIDTH);
            frame.push_back('\n');
        }
        std::cout << frame << std::flush;

        // Increment rotation angles
        A += 0.04;
        B += 0.02;

        // Limit frame rate to ~30 FPS
        std::this_thread::sleep_for(std::chrono::milliseconds(30));
    }

    return 0;
}
