package com.simulation.traffic;

import java.util.ArrayList;

// ---------------------------------------------------------------------
// CLASS: BookkeepingForStoppedVehicles
//
// Manages a list that identifies vehicles forced to stop because
// the traffic light is red at the end of a “NEAR TRAFFIC LIGHT”
// route segment. 
// ---------------------------------------------------------------------
public class BookkeepingForStoppedVehicles {
  private ArrayList<Vehicle> stopped = new ArrayList<>();

  public void add(Vehicle v) {
      if (!stopped.contains(v)) {
          stopped.add(v);
      }
  }

  public void remove(Vehicle v) {
      stopped.remove(v);
  }

  public boolean isVehicleStopped(Vehicle v) {
      return stopped.contains(v);
  }

  public void clear() {
      stopped.clear();
  }
}