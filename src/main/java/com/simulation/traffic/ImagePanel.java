package com.simulation.traffic;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.HashMap;
import javax.imageio.ImageIO;

// ---------------------------------------------------------------------
// CLASS: ImagePanel
//
// This class extends JPanel and:
//   - Displays the background map (img-map.png).
//   - Zoom with mouse wheel, pan with mouse drag (scale≥1.0).
//   - Coordinates displayed near the cursor.
//   - Mark points with 'Q' key for debugging.
//   - Traffic simulation (vehicles + traffic lights).
//   - Timers and collision avoidance, route logic, etc.
//   - Buttons for Start, Stop, Pause, Resume, Clear. 
//   - A field + “Enter” button to set the max number of vehicles.
//   - Speed factor control from a combo box (0.5x..5x).
//   - “Stop” = clear vehicles & halt timer, user must Start again.
//   - “Clear” = remove vehicles but keep timer going and lights cycling.
//   - If a route ends “NEAR TRAFFIC LIGHT,” vehicles must wait if
//     it’s red. 
//   - “END OF ROAD” means the vehicle is removed upon reaching that
//     route’s end. 
//   - Randomly choose next route if multiple continuations share
//     the same start coordinate as the current route’s end.
//   - On click of a vehicle’s circle, display a label or popup with
//     how many seconds it has been traveling since spawn.
// ---------------------------------------------------------------------
public class ImagePanel extends JPanel {
  private BufferedImage image;
  private double scale = 1.0;
  private int offsetX = 0, offsetY = 0;
  private int dragStartX, dragStartY;
  private JLabel coordinatesLabel;
  private List<Point> markedPoints;

  // Simulation timer
  private Timer simulationTimer;
  private boolean simulationRunning = false;

  // Base update interval (ms)
  private static final int SIM_UPDATE_MS = 40; // ~25 FPS

  // Keep track of "simTime" in seconds (for vehicle route timers)
  private double simulationTime = 0.0;

  // We’ll accumulate partial time as we scale with speedFactor:
  // each tick => simulationTime += (SIM_UPDATE_MS / 1000.0) * speedFactor

  // Vehicle storage:
  private List<Vehicle> vehicles = new LinkedList<>();
  private BookkeepingForStoppedVehicles waitList = new BookkeepingForStoppedVehicles();

  // Maximum vehicles allowed
  private int maxVehicles = 10;

  // All route segments
  private List<RouteSegment> routeSegments;
  private Map<String, RouteSegment> labelToRoute;
  private List<RouteSegment> spawnSegments;

  // Traffic lights
  private List<TrafficLight> trafficLights;

  // Random generator
  private Random random = new Random();

  // Collision gap distance
  private static final double COLLISION_GAP = 10.0;

  // Vehicle draw size
  private static final int VEHICLE_SIZE = 8;

  // The base speed for vehicles
  private static final double BASE_SPEED = 1.0;

  // Additional multiplier from the speed combo
  private double speedFactor = 1.0;

  // Allow user to click a vehicle to show its route time.
  private Vehicle selectedVehicle = null;
  private JLabel vehicleInfoLabel;

  private static final int SPECIAL_VEHICLE_INTERVAL = 120; // seconds
  private double nextSpecialVehicleTime = 0.0; // Tracks the next spawn time
  private int specialVehicleCount = 0; // Incremental count for labeling special vehicles
  private List<SpecialVehicleInfo> specialVehicleInfoList = new ArrayList<>(); // For the display

  public ImagePanel(String imagePath) {
      try {
          image = ImageIO.read(new File(imagePath));
      } catch (IOException e) {
          e.printStackTrace();
          JOptionPane.showMessageDialog(this,
              "Could not load image: " + e.getMessage(),
              "Error", JOptionPane.ERROR_MESSAGE);
          System.exit(1);
      }

      // Use absolute layout so we can place coordinate label freely
      setLayout(null);

      coordinatesLabel = new JLabel("Coordinates: ");
      coordinatesLabel.setFont(new Font("Arial", Font.BOLD, 12));
      coordinatesLabel.setForeground(Color.ORANGE);
      coordinatesLabel.setBounds(10, 10, 300, 20);
      add(coordinatesLabel);

      // Vehicle info label (when user clicks a vehicle)
      vehicleInfoLabel = new JLabel("");
      vehicleInfoLabel.setFont(new Font("Arial", Font.BOLD, 12));
      vehicleInfoLabel.setForeground(Color.getHSBColor(194.7f, 24.8f, 90.2f));
      vehicleInfoLabel.setBounds(10, 35, 300, 20);
      add(vehicleInfoLabel);

      markedPoints = new ArrayList<>();

      // Listener for mouse wheel (zoom)
      addMouseWheelListener(new MouseAdapter() {
          @Override
          public void mouseWheelMoved(MouseWheelEvent e) {
              if (image != null) {
                  int mouseX = e.getX();
                  int mouseY = e.getY();
                  double oldScale = scale;

                  if (e.getPreciseWheelRotation() < 0) {
                      scale *= 1.1;
                  } else if (scale > 1.0) {
                      scale /= 1.1;
                  }
                  scale = Math.max(1.0, scale);

                  offsetX = (int) (mouseX - (mouseX - offsetX) * (scale / oldScale));
                  offsetY = (int) (mouseY - (mouseY - offsetY) * (scale / oldScale));
                  constrainOffsets();
                  repaint();
              }
          }
      });

      // Mouse press/drag for panning
      addMouseListener(new MouseAdapter() {
          @Override
          public void mousePressed(MouseEvent e) {
              dragStartX = e.getX();
              dragStartY = e.getY();
              requestFocusInWindow();
          }

          @Override
          public void mouseReleased(MouseEvent e) {
              // We can check if it's a click, if minimal drag:
              int dx = e.getX() - dragStartX;
              int dy = e.getY() - dragStartY;
              if (Math.abs(dx) < 3 && Math.abs(dy) < 3) {
                  // This is effectively a click. Let's see if the user
                  // clicked on any vehicle:
                  handleVehicleClick(e.getX(), e.getY());
              }
          }
      });

      addMouseMotionListener(new MouseAdapter() {
          @Override
          public void mouseDragged(MouseEvent e) {
              if (image != null) {
                  int dx = e.getX() - dragStartX;
                  int dy = e.getY() - dragStartY;
                  offsetX += dx;
                  offsetY += dy;
                  dragStartX = e.getX();
                  dragStartY = e.getY();
                  constrainOffsets();
                  repaint();
              }
          }

          @Override
          public void mouseMoved(MouseEvent e) {
              updateCoordinateLabel(e.getX(), e.getY());
          }
      });

      // Key listener for marking points with 'Q'
      setFocusable(true);
      addKeyListener(new KeyAdapter() {
          @Override
          public void keyPressed(KeyEvent e) {
              if (e.getKeyCode() == KeyEvent.VK_Q) {
                  Point mousePos = getMousePosition();
                  if (mousePos != null && image != null) {
                      int ix = (int) ((mousePos.x - offsetX) / scale);
                      int iy = (int) ((mousePos.y - offsetY) / scale);
                      if (ix >= 0 && ix < image.getWidth() &&
                          iy >= 0 && iy < image.getHeight()) {
                          markedPoints.add(new Point(ix, iy));
                          repaint();
                      }
                  }
              }
          }
      });

      // Build routes and traffic lights
      buildRouteSegments();
      buildTrafficLights();

      // Create the simulation timer
      simulationTimer = new Timer(SIM_UPDATE_MS, _ -> updateSimulation());
  }

  private void handleVehicleClick(int mx, int my) {
      // Convert screen coords to image coords:
      double imgX = (mx - offsetX) / scale;
      double imgY = (my - offsetY) / scale;

      // Check each vehicle in reverse order (front is top)
      Vehicle clicked = null;
      for (int i = vehicles.size() - 1; i >= 0; i--) {
          Vehicle v = vehicles.get(i);
          double vx = v.getCurrentX();
          double vy = v.getCurrentY();
          double dx = vx - imgX;
          double dy = vy - imgY;
          double dist = Math.sqrt(dx*dx + dy*dy);
          if (dist <= VEHICLE_SIZE) {
              // user clicked within vehicle circle
              clicked = v;
              break;
          }
      }

      if (clicked != null) {
          selectedVehicle = clicked;
      } else {
          selectedVehicle = null;
          vehicleInfoLabel.setText("");
      }
  }

  private void updateCoordinateLabel(int mouseX, int mouseY) {
      if (image != null) {
          int ix = (int) ((mouseX - offsetX) / scale);
          int iy = (int) ((mouseY - offsetY) / scale);
          if (ix >= 0 && ix < image.getWidth() &&
              iy >= 0 && iy < image.getHeight()) {
              coordinatesLabel.setText("Coordinates: (" + ix + ", " + iy + ")");
          } else {
              coordinatesLabel.setText("Coordinates: Outside");
          }
      }
  }

  private void constrainOffsets() {
      int imgW = (int) (image.getWidth() * scale);
      int imgH = (int) (image.getHeight() * scale);

      int maxOffX = 0;
      int maxOffY = 0;
      int minOffX = getWidth() - imgW;
      int minOffY = getHeight() - imgH;

      offsetX = Math.min(maxOffX, Math.max(minOffX, offsetX));
      offsetY = Math.min(maxOffY, Math.max(minOffY, offsetY));
  }

  // Simulation controls
  public void startSimulation() {
      if (!simulationRunning) {
          simulationRunning = true;
          simulationTimer.start();
      }
  }

  public void stopSimulation() {
      // “Stop” means: clear vehicles, stop the timer, user must press Start again
      simulationTimer.stop();
      simulationRunning = false;
      vehicles.clear();
      waitList.clear();
      selectedVehicle = null;
      vehicleInfoLabel.setText("");
      repaint();

      specialVehicleInfoList.clear();
      specialVehicleCount = 0;
      nextSpecialVehicleTime = 0.0;
      simulationTime = 0.0;
  }

  public void pauseSimulation() {
      simulationTimer.stop();
  }

  public void resumeSimulation() {
      if (simulationRunning) {
          simulationTimer.start();
      }
  }

  public void clearVehicles() {
      vehicles.clear();
      waitList.clear();
      selectedVehicle = null;
      vehicleInfoLabel.setText("");
      repaint();

      specialVehicleInfoList.clear();
  }

  public void setMaxVehicles(int max) {
      maxVehicles = Math.max(0, max); // if 0 => no new spawns
  }

  public void setSpeedFactor(double factor) {
      speedFactor = factor;
  }

  private void updateSimulation() {
      // Advance simulationTime by (SIM_UPDATE_MS / 1000 * speedFactor)
      simulationTime += (SIM_UPDATE_MS / 1000.0) * speedFactor;

      // Spawn special vehicle every 2 minutes
      if (simulationTime >= nextSpecialVehicleTime) {
          spawnSpecialVehicle();
          nextSpecialVehicleTime += SPECIAL_VEHICLE_INTERVAL;
      }

      // Update traffic lights (with speedFactor)
      for (TrafficLight tl : trafficLights) {
          tl.updateLight(speedFactor / 25);
      }

      // Possibly spawn a vehicle if under max
      if (vehicles.size() < maxVehicles) {
          // small random chance each tick
          if (random.nextDouble() < 0.35) {
              spawnRandomVehicle();
          }
      }

      // Now update the travel time label for the selectedVehicle, if any
      if (selectedVehicle != null) {
          // If the vehicle is still active (not at end of road)
          if (!selectedVehicle.isMarkedForRemoval()) {
              double traveled = simulationTime - selectedVehicle.getSpawnTime();
              String timeStr = String.format("%.1f", traveled);
              vehicleInfoLabel.setText("Vehicle travel time: " + timeStr + "s");
          } else {
              // If it reached the end and is removed, freeze time
              double finalTime = selectedVehicle.getFinalTravelTime(); 

              // Show that final time
              String timeStr = String.format("%.1f", finalTime);
              vehicleInfoLabel.setText("Vehicle travel time: " + timeStr + "s");
          }
      }

      // check red signals
      checkVehiclesAtRedBySegment();

      // Move vehicles
      double stepSpeed = BASE_SPEED * speedFactor;
      for (Vehicle v : vehicles) {
          if (!waitList.isVehicleStopped(v)) {
              // move
              v.updatePosition(stepSpeed);

              // if reached end
              if (v.hasReachedEnd()) {
                  // If it's a special vehicle, stop its timer now
                  if (v instanceof SpecialVehicle) {
                      ((SpecialVehicle) v).stopTimer(simulationTime);
                  }
                  RouteSegment seg = v.getRouteSegment();
                  if (seg.isEndOfRoad()) {
                      // remove
                      v.markForRemoval();
                      v.setFinalTravelTime(simulationTime - v.getSpawnTime());
                      if (selectedVehicle == v) {
                          selectedVehicle = null;
                          vehicleInfoLabel.setText("");
                      }
                  } else {
                      // pick a random next route
                      List<RouteSegment> continuations = seg.getContinuations();
                      if (continuations.isEmpty()) {
                          // no continuation => remove
                          v.markForRemoval();
                          if (selectedVehicle == v) {
                              selectedVehicle = null;
                              vehicleInfoLabel.setText("");
                          }
                      } else {
                          RouteSegment next = continuations.get(random.nextInt(continuations.size()));
                          v.setRouteSegment(next);
                      }
                  }
              }
          }
      }

      // Collision avoidance (applicable when they are on the same line)
      for (int i = 0; i < vehicles.size(); i++) {
          for (int j = i + 1; j < vehicles.size(); j++) {
              Vehicle va = vehicles.get(i);
              Vehicle vb = vehicles.get(j);
              double dist = distance(va.getCurrentX(), va.getCurrentY(),
                                     vb.getCurrentX(), vb.getCurrentY());
              if (dist < COLLISION_GAP) {
                  va.slowDown();
                  vb.slowDown();
              }
          }
      }

      // Remove stale
      vehicles.removeIf(Vehicle::isMarkedForRemoval);

      repaint();
  }

  private void spawnRandomVehicle() {
      if (spawnSegments.isEmpty()) return;
      RouteSegment seg = spawnSegments.get(random.nextInt(spawnSegments.size()));
      // Create a new vehicle with the current simulationTime as spawn time
      Vehicle v = new Vehicle(seg, simulationTime);
      vehicles.add(v);
  }

  private void checkVehiclesAtRedBySegment() {
      // Group vehicles by their current route segment
      Map<RouteSegment, List<Vehicle>> segmentToVehicles = new HashMap<>();
      for (Vehicle v : vehicles) {
          RouteSegment seg = v.getRouteSegment();
          if (seg == null) continue;
          // Add v to the list for seg
          segmentToVehicles.computeIfAbsent(seg, _ -> new ArrayList<>()).add(v);
      }
  
      // For each segment, if it's a traffic-light lane, handle red logic:
      for (Map.Entry<RouteSegment, List<Vehicle>> entry : segmentToVehicles.entrySet()) {
          RouteSegment seg = entry.getKey();
          if (!seg.isTrafficLightLane()) {
              // Not traffic light lane, but if any vehicles were "stoppedByRed," un-stop them
              for (Vehicle v : entry.getValue()) {
                  if (v.isStoppedByRed()) {
                      v.setStoppedByRed(false);
                      v.setSpeedFactor(v.getSpeedFactor()); 
                  }
              }
              continue;
          }
  
          // Find the traffic light
          TrafficLight tl = findTrafficLightForSegment(seg);
          if (tl == null) {
              // No traffic light found for this "traffic light lane"?
              // Just resume if any vehicle was previously forced to stop:
              for (Vehicle v : entry.getValue()) {
                  if (v.isStoppedByRed()) {
                      v.setStoppedByRed(false);
                      v.setSpeedFactor(v.getSpeedFactor());
                  }
              }
              continue;
          }
  
          // Sort vehicles on this lane by distance to the lane's end
          Point endPt = getLastPoint(seg);
          if (endPt == null) continue;  // If no end point, skip
  
          List<Vehicle> list = entry.getValue();
          list.sort((v1, v2) -> {
              double d1 = distance(v1.getCurrentX(), v1.getCurrentY(), endPt.x, endPt.y);
              double d2 = distance(v2.getCurrentX(), v2.getCurrentY(), endPt.x, endPt.y);
              return Double.compare(d1, d2);
          });
  
          // If light is red, the front-most vehicle may stop at or near the end
          LightState state = tl.getLightState();
  
          // We define a threshold for "end reached" and a gap between vehicles
          double endThreshold = 10.0;
          double minVehicleGap = 5.0;  // Distance to keep between consecutive vehicles
  
          if (state == LightState.RED) {
              // We'll track the "stopping position" of the vehicle in front
              // so the next one lines up behind it.
              double prevVehicleX = Double.NaN;
              double prevVehicleY = Double.NaN;
              boolean frontVehicleForcedStop = false;
  
              for (int i = 0; i < list.size(); i++) {
                  Vehicle v = list.get(i);
                  double distToEnd = distance(
                      v.getCurrentX(), v.getCurrentY(),
                      endPt.x, endPt.y
                  );
  
                  // If front-most is near end, it must stop.
                  // If it's not near end, it can keep going.
                  if (distToEnd < endThreshold) {
                      // Force stop
                      v.setStoppedByRed(true);
                      v.setSpeedFactor(0.0);
  
                      // Save its location so the next vehicle can queue
                      prevVehicleX = v.getCurrentX();
                      prevVehicleY = v.getCurrentY();
                      frontVehicleForcedStop = true;
                  } else {
                      // If not at end, we see if there's already a vehicle in front that forced a stop
                      if (frontVehicleForcedStop) {
                          // Check gap to the previously stopped vehicle
                          double distToPrev = distance(v.getCurrentX(), v.getCurrentY(), 
                                                       prevVehicleX,         prevVehicleY);
                          if (distToPrev < minVehicleGap) {
                              // Force a stop behind it
                              v.setStoppedByRed(true);
                              v.setSpeedFactor(0.0);
  
                              // Update "prevVehicleX/Y" so subsequent vehicle also stops behind it
                              prevVehicleX = v.getCurrentX();
                              prevVehicleY = v.getCurrentY();
                          } else {
                              // Enough gap, can continue, but still red—so do you want it to keep moving?
                              // Typically, a vehicle not yet at the end can continue
                              // until it gets within endThreshold or behind a queue.
                              // We'll leave it "un-stopped" for now:
                              if (v.isStoppedByRed()) {
                                  v.setStoppedByRed(false);
                                  v.setSpeedFactor(1.0);
                              }
                          }
                      } else {
                          // No front vehicle forced a stop yet, so the lane might be free until the end
                          if (v.isStoppedByRed()) {
                              v.setStoppedByRed(false);
                              v.setSpeedFactor(1.0);
                          }
                      }
                  }
              }
          } 
          else { 
              // If the light is GREEN or YELLOW, un-stop everyone in this lane
              for (Vehicle v : list) {
                  if (v.isStoppedByRed()) {
                      v.setStoppedByRed(false);
                      v.setSpeedFactor(1.0);
                  }
              }
          }
      }
  }

  private TrafficLight findTrafficLightForSegment(RouteSegment seg) {
      for (TrafficLight tl : trafficLights) {
          // If seg.getLabel() is in tl’s associatedSegments, we have a match
          if (tl.getAssociatedSegments().contains(seg.getLabel())) {
              return tl;
          }
      }
      return null; // If none matched
  }

  private Point getLastPoint(RouteSegment seg) {
      List<Point> pts = seg.getPoints();  
      if (pts == null || pts.isEmpty()) return null;
      return pts.get(pts.size() - 1);
  }

  private void spawnSpecialVehicle() {
      specialVehicleCount++;
      String vehicleLabel = "Special Vehicle #" + specialVehicleCount + " (From Old Sta. Mesa St. to SM Sta. Mesa)";
  
      // Starting segment
      RouteSegment startSegment = labelToRoute.get("SPAWN OldStaMesaRightFront1noSantol");
  
      // Special vehicle creation
      SpecialVehicle specialVehicle = new SpecialVehicle(startSegment, simulationTime, vehicleLabel);
  
      // Define the sequence of segments explicitly
      List<RouteSegment> routeSequence = List.of(
          labelToRoute.get("SPAWN OldStaMesaRightFront1noSantol"),
          labelToRoute.get("NEAR TL OldStaMesaRightFront1beforeMagsaysay"),
          labelToRoute.get("OldStaMesaRightFront1toMagsMidDownFront1"),
          labelToRoute.get("MagsMidDownFront1before2nd"),
          labelToRoute.get("MagsMidDownFront1pass2nd"),
          labelToRoute.get("Mags2ndMidDownFront1before3rd"),
          labelToRoute.get("Mags2ndMidDownFront1toEndMidDownFront1"),
          labelToRoute.get("END MagsEndMidDownFront1after3rd")
      );
  
      // Assign the route sequence to the special vehicle
      specialVehicle.setRouteSequence(routeSequence);
  
      // Track for GUI display
      specialVehicleInfoList.add(new SpecialVehicleInfo(vehicleLabel, specialVehicle));
      vehicles.add(specialVehicle);
  }
  
  @Override
  protected void paintComponent(Graphics g) {
      super.paintComponent(g);
      if (image == null) return;

      Graphics2D g2d = (Graphics2D) g;

      // background
      g2d.setColor(Color.WHITE);
      g2d.fillRect(0, 0, getWidth(), getHeight());

      // draw scaled image
      int w = (int) (image.getWidth() * scale);
      int h = (int) (image.getHeight() * scale);
      g2d.drawImage(image, offsetX, offsetY, w, h, null);

      // draw marked points (for debugging purposes)
      g2d.setFont(new Font("Arial", Font.PLAIN, 12));
      for (Point mp : markedPoints) {
          int px = (int) (mp.x * scale + offsetX);
          int py = (int) (mp.y * scale + offsetY);
          g2d.setColor(Color.RED);
          g2d.fillOval(px - 5, py - 5, 10, 10);
          g2d.drawString("(" + mp.x + ", " + mp.y + ")", px + 10, py - 10);
      }

      // draw traffic lights
      for (TrafficLight tl : trafficLights) {
          tl.draw(g2d, offsetX, offsetY, scale);
      }

      // draw vehicles
      for (Vehicle v : vehicles) {
          double vx = v.getCurrentX() * scale + offsetX;
          double vy = v.getCurrentY() * scale + offsetY;
          int xInt = (int) (vx - VEHICLE_SIZE/2.0);
          int yInt = (int) (vy - VEHICLE_SIZE/2.0);

          // if vehicle is selected, highlight with a thicker border
          if (v == selectedVehicle) {
              g2d.setColor(Color.BLACK);
              g2d.fillOval(xInt - 2, yInt - 2, VEHICLE_SIZE + 4, VEHICLE_SIZE + 4);
          }

          // Color logic~
          if (v instanceof SpecialVehicle) {
              g2d.setColor(Color.RED); // Special vehicle is pink
          } else {
              g2d.setColor(Color.BLUE); // Regular vehicle is blue
          }
          g2d.fillOval(xInt, yInt, VEHICLE_SIZE, VEHICLE_SIZE);
      }

      // Render special vehicle info in the upper middle
      int panelWidth = getWidth();
      int yPosition = 20; // Vertical position for the first line
      g2d.setColor(Color.RED);
      g2d.setFont(new Font("Arial", Font.BOLD, 14));

      // Counter to limit displayed phrases (OPTIONAL: if you want to uncomment them)
      // int displayCount = 0;
      for (SpecialVehicleInfo info : specialVehicleInfoList) {
          // if (displayCount >= 5) break; // Stop after 5 items

          String displayText = info.getLabel() + ": " + String.format("%.1fs", info.getElapsedTime(simulationTime));
          // Center the text horizontally
          int textWidth = g2d.getFontMetrics().stringWidth(displayText);
          int xPosition = (panelWidth - textWidth) / 2; // Center horizontally
          g2d.drawString(displayText, xPosition, yPosition);
          yPosition += 20; // Move to the next line
          // displayCount++;
      }
  }

  private double distance(double x1, double y1, double x2, double y2) {
      double dx = x2 - x1;
      double dy = y2 - y1;
      return Math.sqrt(dx*dx + dy*dy);
  }

  public int getImageWidth() {
      if (image != null) {
          return image.getWidth();
      }
      return 640;
  }

  public int getImageHeight() {
      if (image != null) {
          return image.getHeight();
      }
      return 480;
  }

  // Build route segments from the user’s data
  private void buildRouteSegments() {
      routeSegments = new ArrayList<>();
      labelToRoute = new HashMap<>();
      spawnSegments = new ArrayList<>();

      // EXACT COORDINATES (plus “SPAWN VEHICLE,” "END OF ROAD", “NEAR TRAFFIC LIGHT,” etc.):

      // 1ST INTERSECTION
      // 1st Traffic Light
      addRoute("SPAWN MagsaysayDownFront1before1st", true, false,
          true, 0,505,   26,502);
      addRoute("SPAWN MagsaysayDownFront2before1st", true, false,
          true, 1,513,   28,510);
      addRoute("MagsaysayDownFront1toOldStaMesa", false, false,
          false, 26,502, 44,501, 54,514);
      addRoute("MagsaysayDownFront2toOldStaMesa", false, false,
          false, 26,502, 44,501, 42,516);
      addRoute("MagsaysayDownFront1pass1st", false, false,
          false, 26,502, 84,493);
      addRoute("MagsaysayDownFront2pass1st", false, false,
          false, 28,510, 87,502);

      // 2nd Traffic Light (93,511)
      addRoute("NEAR TL OldStaMesaRightFront1beforeMagsaysay", false, false, true, 
          92,538, 69,513);
      addRoute("NEAR TL OldStaMesaRightFront2beforeMagsaysay", false, false, true, 
          103,536, 80,511);
      addRoute("NEAR TL OldStaMesaRightFront1toMagsUpBack2", false, false, false, 
          69,513, 62,505, 44,501, 32,491, 23,492);
      addRoute("NEAR TL OldStaMesaRightFront2toMagsUpBack1", false, false, false, 
          80,511, 76,504, 59,496, 41,490, 40,483, 21,483);
      addRoute("OldStaMesaRightFront1toMagsMidDownFront1", false, false, false, 
          69,513, 60,496, 84,493);
      addRoute("OldStaMesaRightFront2toMagsMidDownFront2", false, false, false, 
          80,511, 75,505, 87,502);

      // 3rd Traffic Light (79,467) related
      addRoute("MagsMidUpBack1toOldStaMesaLeftBack1", false, false, false, 
          79,475, 68,477, 58,487, 44,501, 37,509, 42,516);
      addRoute("MagsMidUpBack2toOldStaMesaLeftBack2", false, false, false, 
          81,484, 70,486, 59,496, 49,507, 54,514);
      addRoute("MagsMidUpBack1pass1st", false, false, false, 
          79,475, 21,483);
      addRoute("MagsMidUpBack2pass1st", false, false, false, 
          81,484, 23,492);
      addRoute("MagsMidUpBack1before1st", false, false, true, 
          252,451, 79,475);
      addRoute("MagsMidUpBack2before1st", false, false, true, 
          253,461, 81,484);

      // some spawns from old santa mesa
      addRoute("SPAWN OldStaMesaRightFront1noSantol", true, false, false, 
          328,765, 92,538);
      addRoute("SPAWN OldStaMesaRightFront2noSantol", true, false, false, 
          342,765, 103,536);
      addRoute("OldStaMesaRightFront1beforeSantol", false, false, false, 
          92,538, 79,524, 96,521);
      addRoute("OldStaMesaRightFront2beforeSantol", false, false, false, 
          103,536, 98,530, 105,530);

      // End of road
      addRoute("END OldStaMesaLeftBack1", false, true,
          false, 42, 516, 301,765);
      addRoute("END OldStaMesaLeftBack2", false, true,
          false, 54, 514, 314,765);
      addRoute("END MagsUpBack1", false, true,
          false, 21, 483, 0,486);
      addRoute("END MagsUpBack2", false, true,
          false, 23, 492, 0,494);

      // 2ND INTERSECTION
      // 4th traffic light
      addRoute("SantolExtMidFront1before2nd", false, false,
          true, 96,521, 256,506, 267,486);
      addRoute("SantolExtMidFront2before2nd", false, false,
          true, 105,530, 269,515, 284,484);
      addRoute("SantolExtMidFront1toMagsMidUpBack2", false, false, false, 
          267,486, 269,478, 267,460, 253,461);
      addRoute("SantolExtMidFront2toMags2ndMidDownFront2", false, false, false, 
          284,484, 289,474, 297,474);
      addRoute("SantolExtMidFront2toSantolStreetUpperRightBack", false, false, false, 
          284,484, 281,475, 282,439);

      // 5th traffic light
      addRoute("MagsMidDownFront1before2nd", false, false,
          true, 84,493, 254,470);
      addRoute("MagsMidDownFront2before2nd", false, false,
          true, 87,502, 255,479);
      addRoute("MagsMidDownFront1toUpperRightBack", false, false,
          false, 254,470, 264,468, 271,458, 277,449, 282,439);
      addRoute("MagsMidDownFront1pass2nd", false, false,
          false, 254,470, 296,465);
      addRoute("MagsMidDownFront2pass2nd", false, false,
          false, 255,479, 297,474);

      // 6th traffic light
      addRoute("SPAWN SantolStreetUpperLeftFrontbefore2nd", true, false, true, 
          363,0, 373,187, 267,440);
      addRoute("SantolStreetUpperLeftFronttoMidUpBack1", false, false, false, 
          267,440, 262,451, 252,451);
      addRoute("SantolStreetUpperLeftFrontto2ndMidDownFront1", false, false, false, 
          267,440, 269,450, 279,456, 288,465, 296,465);

      // 7th traffic light
      addRoute("Mags2ndMidUpBack1toSantolStreetUpperRightBack", false, false, false, 
          293,446, 285,448, 282,439);
      addRoute("Mags2ndMidUpBack1pass2nd", false, false, false, 
          293,446, 252,451);
      addRoute("Mags2ndMidUpBack2pass2nd", false, false, false, 
          295,455, 253,461);
      addRoute("Mags2ndMidUpBack1before2nd", false, false, true, 
          610,402, 293,446);
      addRoute("Mags2ndMidUpBack2before2nd", false, false, true, 
          608,414, 295,455);

      // not affected to any traffic lights
      addRoute("END SantolStreetUpperRightBackpass2nd", false, true, false, 
          282,439, 388,185, 378,0);

      // 3RD INTERSECTION
      // 8th traffic light
      addRoute("Mags2ndMidDownFront1before3rd", false, false, true, 
          296,465, 604,423);
      addRoute("Mags2ndMidDownFront2before3rd", false, false, true, 
          297,474, 602,434);
      addRoute("Mags2ndMidDownFront1toEndMidDownFront1", false, false, false, 
          604,423, 613,422, 630,414, 635,410);
      addRoute("Mags2ndMidDownFront2toVictorinaEndLeftBack1", false, false, false, 
          602,434, 610,433, 612,440);

      // 9th traffic light
      addRoute("SPAWN VictorinoMapaStreetEndRightFront1before3rd", true, false, true, 
          742,765, 630,432);
      addRoute("SPAWN VictorinoMapaStreetEndRightFront2before3rd", true, false, true, 
          751,765, 638,427);
      addRoute("VictorinoMapaStreetEndRightFront1toMags2ndMidUpBack2", false, false, false, 
          630,432, 626,424, 615,414, 608,414);
      addRoute("VictorinoMapaStreetEndRightFront2toMagsEndMidDownFront2", false, false, false, 
          638,427, 635,420, 641,416);

      // 10th traffic light
      addRoute("SPAWN MagsEndMidUpBack1before3rd", true, false, true, 
          1003,10, 622,398);
      addRoute("SPAWN MagsEndMidUpBack2before3rd", true, false, true, 
          1003,22, 628,404);
      addRoute("MagsEndMidUpBack1to2ndMidUpBack1", false, false, false, 
          622,398, 617,403, 610,402);
      addRoute("MagsEndMidUpBack2toVictorinoEndLeftBack2", false, false, false, 
          628,404, 623,409, 618,428, 621,435);

      // not affected to any traffic lights
      addRoute("END MagsEndMidDownFront1after3rd", false, true, false, 
          635,410, 1004,33);
      addRoute("END MagsEndMidDownFront2after3rd", false, true, false, 
          641,416, 1004,47);
      addRoute("END VictorinoMapaStreetEndLeftBack1", false, true, false, 
          612,440, 721,765);
      addRoute("END VictorinoMapaStreetEndLeftBack2", false, true, false, 
          621,435, 731,765);

      // Build continuations (link routes sharing end/start coords)
      linkContinuations();

      // Identify spawn segments
      for (RouteSegment rs : routeSegments) {
          if (rs.isSpawnSegment()) {
              spawnSegments.add(rs);
          }
      }
  }

  private void addRoute(String label, boolean spawn, boolean end, boolean trafficLight, int... coords) {
      List<Point> pts = new ArrayList<>();
      for (int i = 0; i < coords.length; i += 2) {
          pts.add(new Point(coords[i], coords[i + 1]));
      }
      RouteSegment seg = new RouteSegment(label, pts, spawn, end, trafficLight);
      routeSegments.add(seg);
      labelToRoute.put(label, seg);
  }

  private void linkContinuations() {
      for (RouteSegment a : routeSegments) {
          if (a.isEndOfRoad()) continue;
          Point endA = a.getLastPoint();
          for (RouteSegment b : routeSegments) {
              if (b == a) continue;
              Point startB = b.getFirstPoint();
              if (startB.equals(endA)) {
                  a.addContinuation(b);
              }
          }
      }
  }

  private void buildTrafficLights() {
      // Each traffic light has (Point location, offsetTicks, redDur, greenDur, yellowDur).
      trafficLights = new ArrayList<>();

      // first intersection
      trafficLights.add(new TrafficLight( new Point(18,522),
          -60, 120, 55, 5 ));
      trafficLights.add(new TrafficLight( new Point(93,511),
          -25, 120, 55, 5 ));
      trafficLights.add(new TrafficLight( new Point(79,467),
          5, 120, 55, 5 ));

      // second intersection
      trafficLights.add(new TrafficLight( new Point(295,491),
          -42, 120, 55, 5 ));
      trafficLights.add(new TrafficLight( new Point(242,491),
          -8, 120, 55, 5 ));
      trafficLights.add(new TrafficLight( new Point(256,430),
          -55, 120, 55, 5 ));
      trafficLights.add(new TrafficLight( new Point(316,432),
          -27, 120, 55, 5 ));
      
      // third intersection
      trafficLights.add(new TrafficLight( new Point(591,448),
          5, 120, 55, 5 ));
      trafficLights.add(new TrafficLight( new Point(657,442),
          -25, 120, 55, 5 ));
      trafficLights.add(new TrafficLight( new Point(624,379),
          -60, 120, 55, 5 ));

      trafficLights.get(0).addAssociatedSegment("SPAWN MagsaysayDownFront1before1st");
      trafficLights.get(0).addAssociatedSegment("SPAWN MagsaysayDownFront2before1st");

      trafficLights.get(1).addAssociatedSegment("NEAR TL OldStaMesaRightFront1beforeMagsaysay");
      trafficLights.get(1).addAssociatedSegment("NEAR TL OldStaMesaRightFront2beforeMagsaysay");
      
      trafficLights.get(2).addAssociatedSegment("MagsMidUpBack1before1st");
      trafficLights.get(2).addAssociatedSegment("MagsMidUpBack2before1st");

      trafficLights.get(3).addAssociatedSegment("SantolExtMidFront1before2nd");
      trafficLights.get(3).addAssociatedSegment("SantolExtMidFront2before2nd");

      trafficLights.get(4).addAssociatedSegment("MagsMidDownFront1before2nd");
      trafficLights.get(4).addAssociatedSegment("MagsMidDownFront2before2nd");

      trafficLights.get(5).addAssociatedSegment("SPAWN SantolStreetUpperLeftFrontbefore2nd");

      trafficLights.get(6).addAssociatedSegment("Mags2ndMidUpBack1before2nd");
      trafficLights.get(6).addAssociatedSegment("Mags2ndMidUpBack2before2nd");

      trafficLights.get(7).addAssociatedSegment("Mags2ndMidDownFront1before3rd");
      trafficLights.get(7).addAssociatedSegment("Mags2ndMidDownFront2before3rd");

      trafficLights.get(8).addAssociatedSegment("SPAWN VictorinoMapaStreetEndRightFront1before3rd");
      trafficLights.get(8).addAssociatedSegment("SPAWN VictorinoMapaStreetEndRightFront2before3rd");

      trafficLights.get(9).addAssociatedSegment("SPAWN MagsEndMidUpBack1before3rd");
      trafficLights.get(9).addAssociatedSegment("SPAWN MagsEndMidUpBack2before3rd");
  }
}