# TrafficLight-ALT

This repository contains a straightforward Java-based traffic simulation developed as a culminating project for our Automata and Language Theory course. Below is a concise overview of its purpose, core components, and guidance on compilation and execution in Visual Studio Code.

---

## Project Summary
• Main.java provides an initial user interface that transitions into the primary simulation window.  
• Other Java source code files govern the simulation logic, rendering traffic flow and controls for starting, stopping, and adjusting the simulation.  
• The traffic light logic uses a Finite State Machine (FSM) approach, where each light transitions through RED, YELLOW, and GREEN states, regulating vehicle movement.

This final project underscores practical concepts in Automata and Language Theory by integrating a discrete FSM for traffic lights, illustrating how states and transitions align with real-world safety constraints.

---

## Compiling & Running in VS Code

Below are two simple approaches to get the application running in Visual Studio Code. Be sure to install the “Java Extension Pack” beforehand.

### OPTION 1 (Structured Directories)
1. Place your .java files under a structured folder (for instance, src/main/java/com/simulation/traffic) and your generated .class files into bin.  
2. In the integrated terminal:
   ### • Compile:  
     `javac -d bin src/main/java/com/simulation/traffic/*.java`
   ### • Run the Main class:  
     `java -cp bin com.simulation.traffic.Main`  

### OPTION 2 (Draft Folder)
1. Keep the .java files in a single “draft” folder.  
2. In the integrated terminal:
   ### • Compile:  
     `javac *.java`  
   ### • Run the Main class:  
     `java Main`  

---