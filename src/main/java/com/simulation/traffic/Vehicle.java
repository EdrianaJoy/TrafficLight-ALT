package com.simulation.traffic;

import java.awt.*;
import java.util.List;

// ---------------------------------------------------------------------
// CLASS: Vehicle
//
// Each Vehicle references a RouteSegment and an index that tracks the
// current sub-segment along that route. The vehicle moves from one
// route coordinate to the next in sequence. If the route is an
// “END OF ROAD,” the vehicle is removed at the end. Otherwise, it
// transitions to a next route if one (or multiple) matches the final
// coordinate. In collisions (less than 10 px), we slow them to half.
//
// We also store a "spawnTime" so we can compute how many seconds the
// vehicle has traveled when clicked by the user.
// ---------------------------------------------------------------------
public class Vehicle {
  private RouteSegment routeSegment;
  private int currentLeg = 0;
  private double x, y;
  private boolean removeFlag = false;
  private double speedFactor = 1.0; // 1.0 normal, 0.5 if slowed
  private final double spawnTime;   // time at which it was created
  private boolean stoppedByRed = false;
  private double finalTravelTime;
  private Color color = Color.BLUE; // Default to blue

  public Vehicle(RouteSegment seg, double spawnT) {
      spawnTime = spawnT;
      setRouteSegment(seg);
  }

  public void setFinalTravelTime(double t) {
      finalTravelTime = t;
  }
  
  public double getFinalTravelTime() {
      return finalTravelTime;
  }

  public void setRouteSegment(RouteSegment seg) {
      routeSegment = seg;
      currentLeg = 0;
      if (!seg.getPoints().isEmpty()) {
          Point p0 = seg.getPoints().get(0);
          x = p0.x;
          y = p0.y;
      }
  }

  public RouteSegment getRouteSegment() {
      return routeSegment;
  }

  public void updatePosition(double baseSpeed) {
      if (routeSegment == null) return;
      List<Point> pts = routeSegment.getPoints();
      if (pts.size() < 2) return;

      if (currentLeg >= pts.size() - 1) return;

      Point A = pts.get(currentLeg);
      Point B = pts.get(currentLeg + 1);
      double dx = B.x - A.x;
      double dy = B.y - A.y;
      double dist = Math.sqrt(dx*dx + dy*dy);
      double step = baseSpeed * speedFactor;

      if (dist > 0) {
          double mx = (dx / dist) * step;
          double my = (dy / dist) * step;
          x += mx;
          y += my;
      }

      double distNext = Math.sqrt((x - B.x)*(x - B.x) + (y - B.y)*(y - B.y));
      if (distNext < step) {
          x = B.x;
          y = B.y;
          currentLeg++;
          // reset slow-down after each segment
          speedFactor = 1.0;
      }

  }

  public boolean isStoppedByRed() { return stoppedByRed; }
  public void setStoppedByRed(boolean val) { this.stoppedByRed = val; }

  public double getCurrentX() { return x; }
  public double getCurrentY() { return y; }

  public boolean hasReachedEnd() {
      if (routeSegment == null) return true;
      return (currentLeg >= routeSegment.getPoints().size() - 1);
  }

  public void markForRemoval() {
      removeFlag = true;
  }

  public boolean isMarkedForRemoval() {
      return removeFlag;
  }

  public void slowDown() {
      speedFactor = 0.5;
  }

  public double getSpawnTime() {
      return spawnTime;
  }

  public void setSpeedFactor(double givenSpeedFactor) {
      speedFactor = givenSpeedFactor;
  }

  public double getSpeedFactor() {
      return speedFactor;
  }

  public void setColor(Color color) { this.color = color; }
  public Color getColor() { return color; }
}