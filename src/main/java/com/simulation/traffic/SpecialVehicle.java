package com.simulation.traffic;

import java.util.List;

public class SpecialVehicle extends Vehicle {
  private final String label;
  private List<RouteSegment> routeSequence;
  private int currentRouteIndex = 0;
  private double stopTime = -1.0;

  public SpecialVehicle(RouteSegment startSegment, double spawnTime, String label) {
      super(startSegment, spawnTime);
      this.label = label;
  }

  public void setRouteSequence(List<RouteSegment> routeSequence) {
      this.routeSequence = routeSequence;
  }

  @Override
  public boolean hasReachedEnd() {
      if (super.hasReachedEnd()) {
          if (currentRouteIndex < routeSequence.size() - 1) {
              // Move to the next segment in the sequence
              currentRouteIndex++;
              setRouteSegment(routeSequence.get(currentRouteIndex));
              return false;
          }
          return true; // End of sequence
      }
      return false;
  }

  public void stopTimer(double currentTime) {
      if (stopTime < 0) {
          stopTime = currentTime - getSpawnTime();
      }
  }

  public double getStopTime() {
      return stopTime;
  }

  public String getLabel() {
      return label;
  }
}