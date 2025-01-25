package com.simulation.traffic;

import javax.swing.*;
import java.awt.*;

// We break this big file into these classes:
//   1) public class Prog   (with main(...))
//   2) class ImagePanel               (renders the map + simulation)
//   3) class RouteSegment             (defines a route with points)
//   4) class Vehicle                  (blue circle tracking route)
//   5) class TrafficLight             (with distinct intervals + FSM)
//   6) enum LightState                (RED, GREEN, YELLOW)
//   7) class BookkeepingForStoppedVehicles
//   8) [some expansions for route and traffic logic, including
//      random next-route selection, collision avoidance, etc.]
// ---------------------------------------------------------------------

/***********************************************************************
 * PUBLIC CLASS: Prog
 *
 * Main Entry Point:
 *  - Creates a JFrame
 *  - Adds an ImagePanel (the main simulation code) in the center
 *  - Builds a top control panel with "Start", "Stop", "Pause",
 *    "Resume", and "Clear" buttons, plus a "Max Vehicles" field
 *    with "Enter" button, plus a speed factor drop-down (0.5x..5x).
 *  - Sets the frame size to match the image exactly, then disables
 *    resizing.
 ***********************************************************************/
public class Prog {
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Image Zoom and Pan + Traffic Simulation");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            // Create the panel that shows the image + simulation
            ImagePanel imagePanel = new ImagePanel("src/main/resources/images/img-map.png");

            // Use BorderLayout so we can put the controls at the top
            frame.setLayout(new BorderLayout());
            frame.add(imagePanel, BorderLayout.CENTER);

            // Build a small top control panel
            JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

            JButton startBtn = new JButton("Start");
            JButton stopBtn = new JButton("Stop");
            JButton pauseBtn = new JButton("Pause");
            JButton resumeBtn = new JButton("Resume");
            JButton clearBtn = new JButton("Clear");
            
            JLabel maxVehLabel = new JLabel("Max Vehicles:");
            JTextField maxVehField = new JTextField("10", 5);
            JButton enterBtn = new JButton("Enter");

            // Speed drop-down
            JLabel speedLabel = new JLabel("Speed:");
            String[] speedOptions = {"0.5x","1x","1.5x","2x","3x","5x"};
            JComboBox<String> speedCombo = new JComboBox<>(speedOptions);
            speedCombo.setSelectedItem("1x");

            // Add components to control panel
            controlPanel.add(startBtn);
            controlPanel.add(stopBtn);
            controlPanel.add(pauseBtn);
            controlPanel.add(resumeBtn);
            controlPanel.add(clearBtn);
            controlPanel.add(maxVehLabel);
            controlPanel.add(maxVehField);
            controlPanel.add(enterBtn);
            controlPanel.add(speedLabel);
            controlPanel.add(speedCombo);

            // Simulation control listeners:
            startBtn.addActionListener(_ -> imagePanel.startSimulation());
            stopBtn.addActionListener(_ -> {
                // “Stop” means clear vehicles and stop the simulation.
                imagePanel.stopSimulation();
            });
            pauseBtn.addActionListener(_ -> imagePanel.pauseSimulation());
            resumeBtn.addActionListener(_ -> imagePanel.resumeSimulation());
            clearBtn.addActionListener(_ -> imagePanel.clearVehicles());

            // “Enter” button to set max vehicles
            enterBtn.addActionListener(_ -> {
                String text = maxVehField.getText().trim();
                try {
                    int requested = Integer.parseInt(text);
                    imagePanel.setMaxVehicles(requested);
                } catch (NumberFormatException ex) {
                    // ignore if invalid
                }
            });

            // Speed combo box to set simulation speed factor
            speedCombo.addActionListener(_ -> {
                String chosen = (String) speedCombo.getSelectedItem();
                double factor = 1.0;
                if (chosen != null) {
                    switch (chosen) {
                        case "0.5x": factor = 0.5; break;
                        case "1x": factor = 1.0; break;
                        case "1.5x": factor = 1.5; break;
                        case "2x": factor = 2.0; break;
                        case "3x": factor = 3.0; break;
                        case "5x": factor = 5.0; break;
                        default: factor = 1.0; break;
                    }
                }
                imagePanel.setSpeedFactor(factor);
            });

            // Add the control panel at top
            frame.add(controlPanel, BorderLayout.NORTH);

            // Frame size is EXACT to the image’s width/height
            // so there’s no extra space on right or bottom.
            int w = imagePanel.getImageWidth();
            int h = imagePanel.getImageHeight();
            frame.pack(); // Let layout run first, then override size:
            // We do a small guess for insets. We'll measure the difference:
            Insets insets = frame.getInsets();
            int totalWidth = w + insets.left + insets.right;
            int totalHeight = h + insets.top + insets.bottom + controlPanel.getHeight();
            frame.setSize(totalWidth, totalHeight);

            // Center the frame on the screen
            frame.setLocationRelativeTo(null);

            // Disable resizing
            frame.setResizable(false);

            frame.setVisible(true);
        });
    }
}

// ---------------------------------------------------------------------
// The GUI will appear, sized exactly to the background image plus the
// top control panel. The top panel has five buttons: Start, Stop,
// Pause, Resume, Clear; plus a “Max Vehicles” text field with an
// “Enter” button, plus a speed factor drop-down (0.5x..5x).
// 
//   • Start: starts the simulation timer (vehicles spawn, traffic
//     lights cycle).
//   • Stop: clears vehicles and stops the simulation timer, so the
//     user must press Start again to resume. 
//   • Pause: stops the simulation timer but doesn’t remove vehicles.
//   • Resume: continues from paused state.
//   • Clear: removes vehicles but keeps the simulation timer going
//     and traffic lights cycling.
//   • Max Vehicles: sets the maximum; if set to 0, no new vehicles
//     appear. 
//   • Enter: sets that maximum. 
//   • Speed drop-down: adjusts how quickly vehicles move and how
//     fast the traffic lights cycle. 0.5x = half speed, 5x = five
//     times normal speed, etc.
// 
// Vehicles:
//   • Are spawned on any route labeled “SPAWN VEHICLE” if we are
//     below “maxVehicles.” Spawning is random each tick, with a low
//     probability. 
//   • They follow the route’s points in order. If “END OF ROAD,” the
//     vehicle is removed upon reaching the end. If not, and if the
//     route’s end coordinate matches the start of one or more routes,
//     we randomly pick the next route. 
//   • If a route ends “NEAR TRAFFIC LIGHT,” the vehicle will stop if
//     that light is red. It only proceeds on green/yellow. 
//   • If two vehicles get within 10 px, they both slow to half speed
//     to avoid collisions. 
//   • Each is drawn as a small blue circle. Clicking a circle
//     displays how many seconds that vehicle has traveled since its
//     spawn time (the label is near the top-left corner, under the
//     coordinates label).
// 
// Traffic Lights:
//   • Each is a small black rectangle with three colored circles (red
//     on top, yellow in middle, green below). Only one circle is lit
//     (or none, if it’s offset?), but we offset or treat negative as
//     red. 
//   • We display a small countdown in “##s” format above the
//     rectangle, indicating how many ticks remain in that color until
//     the next color. Because times are scaled by speedFactor, we
//     store partial increments in float. 
//   • The user can see the lights turning from RED → GREEN → YELLOW,
//     then back to RED, etc. 
//
// END OF FILE Prog.java
// ---------------------------------------------------------------------