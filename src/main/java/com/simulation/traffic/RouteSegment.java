package com.simulation.traffic;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

// ---------------------------------------------------------------------
// CLASS: RouteSegment
//
// A route is a sequence of points. The first coordinate is the
// start, the last coordinate is the end. If isSpawnSegment == true,
// we can spawn vehicles at its first point. If isEndOfRoad == true,
// vehicles that reach the final coordinate are removed. Otherwise,
// if the final coordinate matches the start of other routes, those
// are possible continuations. 
// ---------------------------------------------------------------------
public class RouteSegment {
  private final String label;
  private final List<Point> points;
  private final boolean spawnSegment;
  private final boolean endOfRoad;
  private final List<RouteSegment> continuations;
  private boolean trafficLightLane;

  public RouteSegment(String lbl, List<Point> pts,
                      boolean spawn, boolean end, boolean trafficLight) {
      label = lbl;
      points = pts;
      spawnSegment = spawn;
      endOfRoad = end;
      continuations = new ArrayList<>();
      trafficLightLane = trafficLight;
  }

  public String getLabel() { return label; }
  public boolean isSpawnSegment() { return spawnSegment; }
  public boolean isEndOfRoad() { return endOfRoad; }
  public List<Point> getPoints() { return points; }

  public void addContinuation(RouteSegment next) {
      continuations.add(next);
  }
  public List<RouteSegment> getContinuations() { return continuations; }

  public Point getFirstPoint() {
      if (points.isEmpty()) return new Point(0,0);
      return points.get(0);
  }
  public Point getLastPoint() {
      if (points.isEmpty()) return new Point(0,0);
      return points.get(points.size() - 1);
  }

  public boolean isTrafficLightLane() {
      return trafficLightLane;
  }
}