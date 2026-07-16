use std::f64::consts::PI;
use std::io::{self, stdout, Write, BufWriter};
use std::thread;
use std::time::Duration;

/**
 * donut.rs - A clean, documented, and optimized Rust implementation of Andy Sloane's
 * famous spinning 3D ASCII donut (donut.c).
 *
 * Renders a spinning ASCII torus to the terminal using standard perspective
 * projection, Z-buffering, and surface illumination.
 */

// Screen dimensions
const SCREEN_WIDTH: usize = 80;
const SCREEN_HEIGHT: usize = 22;

// Torus dimensions
const R1: f64 = 1.0;
const R2: f64 = 2.0;

// Camera and projection parameters
const K2: f64 = 5.0;
const K1: f64 = (SCREEN_WIDTH as f64 * K2 * 3.0) / (8.0 * (R1 + R2));

const SHADE_CHARS: &[u8] = b".,-~:;=!*#$@";

fn main() -> io::Result<()> {
    let stdout = stdout();
    let mut lock = BufWriter::new(stdout.lock());

    // Clear screen and hide cursor on start
    write!(lock, "\x1b[2J\x1b[?25l")?;
    lock.flush()?;

    // Rotation angles
    let mut a: f64 = 0.0;
    let mut b: f64 = 0.0;

    // Use a helper struct to restore cursor on exit (RAII)
    struct CursorRestorer;
    impl Drop for CursorRestorer {
        fn drop(&mut self) {
            let mut stdout = stdout();
            let _ = write!(stdout, "\x1b[?25h\x1b[H\n");
            let _ = stdout.flush();
        }
    }
    let _restorer = CursorRestorer;

    loop {
        // Buffers for screen characters and Z depth
        let mut screen_buf = [b' '; SCREEN_WIDTH * SCREEN_HEIGHT];
        let mut z_buf = [0.0f64; SCREEN_WIDTH * SCREEN_HEIGHT];

        let cos_a = a.cos();
        let sin_a = a.sin();
        let cos_b = b.cos();
        let sin_b = b.sin();

        // theta sweeps the cross-sectional circle of the torus (0 to 2*pi)
        let mut theta = 0.0;
        while theta < 2.0 * PI {
            let cos_theta = theta.cos();
            let sin_theta = theta.sin();

            // phi sweeps the torus revolution around the Y-axis (0 to 2*pi)
            let mut phi = 0.0;
            while phi < 2.0 * PI {
                let cos_phi = phi.cos();
                let sin_phi = phi.sin();

                // 3D coordinates of the torus point before rotation
                let circle_x = R2 + R1 * cos_theta;
                let circle_y = R1 * sin_theta;

                // 3D rotations: rotate around X-axis (angle A) and Z-axis (angle B)
                let x = circle_x * (cos_b * cos_phi + sin_a * sin_b * sin_phi) - circle_y * cos_a * sin_b;
                let y = circle_x * (sin_b * cos_phi - sin_a * cos_b * sin_phi) + circle_y * cos_a * cos_b;
                let z = K2 + cos_a * circle_x * sin_phi + circle_y * sin_a;
                let ooz = 1.0 / z; // One over Z (perspective projection)

                // Project 3D point onto 2D screen coordinates
                let xp = (SCREEN_WIDTH as f64 / 2.0 + 30.0 * ooz * (cos_phi * circle_x * cos_b - (circle_y * cos_a + circle_x * sin_a * sin_phi) * sin_b)) as isize;
                let yp = (SCREEN_HEIGHT as f64 / 2.0 + 15.0 * ooz * (cos_phi * circle_x * sin_b + (circle_y * cos_a + circle_x * sin_a * sin_phi) * cos_b)) as isize;

                // Calculate luminance dot product with light source (0, 1, -1)
                let l = cos_phi * cos_theta * sin_b 
                      - cos_a * cos_theta * sin_phi 
                      - sin_a * sin_theta 
                      + cos_b * (cos_a * sin_theta - sin_a * cos_theta * sin_phi);

                // Check bounds and project if visible
                if xp >= 0 && xp < SCREEN_WIDTH as isize && yp >= 0 && yp < SCREEN_HEIGHT as isize {
                    let xp = xp as usize;
                    let yp = yp as usize;
                    if l > 0.0 {
                        let idx = yp * SCREEN_WIDTH + xp;
                        if ooz > z_buf[idx] {
                            z_buf[idx] = ooz;
                            let shade_idx = (l * 8.0) as usize;
                            let clamped_idx = shade_idx.min(SHADE_CHARS.len() - 1);
                            screen_buf[idx] = SHADE_CHARS[clamped_idx];
                        }
                    }
                }

                phi += 0.02;
            }
            theta += 0.07;
        }

        // Draw the frame: move cursor to home position and print the buffer
        write!(lock, "\x1b[H")?;
        for r in 0..SCREEN_HEIGHT {
            let row_bytes = &screen_buf[r * SCREEN_WIDTH..(r + 1) * SCREEN_WIDTH];
            lock.write_all(row_bytes)?;
            lock.write_all(b"\n")?;
        }
        lock.flush()?;

        // Increment rotation angles
        a += 0.04;
        b += 0.02;

        // Limit frame rate to ~30 FPS (30ms sleep)
        thread::sleep(Duration::from_millis(30));
    }
}
