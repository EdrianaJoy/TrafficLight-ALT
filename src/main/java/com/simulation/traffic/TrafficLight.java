package com.simulation.traffic;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

// ---------------------------------------------------------------------
// CLASS: TrafficLight
//
// Each light has:
//   - location (Point)
//   - offsetTicks (how many ticks from the start it remains red)
//   - durations for red, green, yellow
//   - totalCycle = sum of red+green+yellow
// We store an internal “cycleTicks” that increments/decrements with each update,
// scaled by the speedFactor. Once cycleTicks >= totalCycle, wrap
// around. This yields a repeating cycle: RED → GREEN → YELLOW → RED.
// We also display a countdown (in whole seconds) for the current color.
// ---------------------------------------------------------------------
public class TrafficLight {
  private final Point location;
  private final int offsetTicks;    // how many ticks to remain red initially
  private final int redDuration;
  private final int greenDuration;
  private final int yellowDuration;
  private final int totalCycle;
  private List<String> associatedSegments;

  // We'll track partial time in a float or double (cycleTicks).
  // We can store as float to accumulate partial increments.
  private float cycleTicks = 0f;
  private boolean isFirstCycle = true;  // track if we're in the first cycle

  public TrafficLight(Point loc,
                      int offTicks,
                      int rDur,
                      int gDur,
                      int yDur) {
      location = loc;
      offsetTicks = offTicks;
      redDuration = rDur;
      greenDuration = gDur;
      yellowDuration = yDur;
      totalCycle = redDuration + greenDuration + yellowDuration;
      cycleTicks = -offsetTicks; // start offset
      associatedSegments = new ArrayList<>();
  }

  // Add one or more route labels that this traffic light governs
  public void addAssociatedSegment(String segmentLabel) {
      if (!associatedSegments.contains(segmentLabel)) {
          associatedSegments.add(segmentLabel);
      }
  }

  // Return all labels that pass through this traffic light
  public List<String> getAssociatedSegments() {
      return associatedSegments;
  }

  public Point getLocation() {
      return location;
  }

  public void updateLight(double speedFactor) {
      // Advance the cycle ticks by the speed factor
      cycleTicks += speedFactor;

      if (isFirstCycle) {
          // In the first cycle, consider offset + red duration
          if (cycleTicks >= (offsetTicks + redDuration)) {
              cycleTicks -= (offsetTicks + redDuration); // reset to start regular cycles
              isFirstCycle = false;
          }
      } else {
          // Regular cycles after the first cycle
          if (cycleTicks >= totalCycle) {
              cycleTicks -= totalCycle; // reset to start of the cycle
          }
      }
  }

  public LightState getLightState() {
      if (isFirstCycle) {
          // During the first cycle, everything is RED for offset + red duration
          return LightState.RED;
      }

      // Regular cycle behavior
      float t = cycleTicks;
      if (t < greenDuration) {
          return LightState.GREEN;
      }
      t -= greenDuration;
      if (t < yellowDuration) {
          return LightState.YELLOW;
      }
      t -= yellowDuration;
      if (t < redDuration) {
          return LightState.RED;
      }
      return LightState.GREEN;
  }

  public float getRemainingTicksInState() {
      if (isFirstCycle) {
          // In the first cycle, calculate remaining time in the initial extended RED period
          return (offsetTicks + redDuration) - cycleTicks;
      }

      // Regular cycle behavior
      float t = cycleTicks;
      if (t < greenDuration) {
          return greenDuration - t;
      }
      t -= greenDuration;
      if (t < yellowDuration) {
          return yellowDuration - t;
      }
      t -= yellowDuration;
      if (t < redDuration) {
          return redDuration - t;
      }

      return 0;
  }

  public void draw(Graphics2D g2d, int offsetX, int offsetY, double scale) {
      double xx = location.x * scale + offsetX;
      double yy = location.y * scale + offsetY;

      int rx = (int)(xx - 7);
      int ry = (int)(yy - 30);

      // small black rectangle
      g2d.setColor(Color.BLACK);
      g2d.fillRect(rx, ry, 14, 42);

      LightState st = getLightState();
      int circleSize = 10;
      int mx = rx + 2;
      int myRed = ry + 2;
      int myYellow = ry + 14;
      int myGreen = ry + 26;

      // draw red
      if (st == LightState.RED) g2d.setColor(Color.RED);
      else g2d.setColor(Color.DARK_GRAY);
      g2d.fillOval(mx, myRed, circleSize, circleSize);

      // draw yellow
      if (st == LightState.YELLOW) g2d.setColor(Color.YELLOW);
      else g2d.setColor(Color.DARK_GRAY);
      g2d.fillOval(mx, myYellow, circleSize, circleSize);

      // draw green
      if (st == LightState.GREEN) g2d.setColor(Color.GREEN);
      else g2d.setColor(Color.DARK_GRAY);
      g2d.fillOval(mx, myGreen, circleSize, circleSize);

      // draw countdown text (whole seconds plus "s")
      int remain = Math.round(getRemainingTicksInState());
      g2d.setColor(Color.BLACK);
      g2d.setFont(new Font("Arial", Font.PLAIN, 10));
      int tx = rx - 5;
      int ty = ry - 2;
      g2d.drawString(remain + "s", tx, ty);
  }
}