package com.simulation.traffic;

public class SpecialVehicleInfo {
  private final String label;
  private final SpecialVehicle vehicle;

  public SpecialVehicleInfo(String label, SpecialVehicle vehicle) {
      this.label = label;
      this.vehicle = vehicle;
  }

  public String getLabel() {
      return label;
  }

  public double getElapsedTime(double currentTime) {
      return vehicle.getStopTime() >= 0 ? vehicle.getStopTime() : currentTime - vehicle.getSpawnTime();
  }
}
