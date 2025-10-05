package org.fog.marina;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import org.cloudbus.cloudsim.Datacenter;
import org.cloudbus.cloudsim.core.CloudSim;

import out.production.iFogSim.org.fog.entities.Actuator;
import out.production.iFogSim.org.fog.entities.Sensor;

public class MARINAiFogSim {
    private static List<Datacenter> fogDevices = new ArrayList<>();
    private static List<Sensor> sensors = new ArrayList<>();
    private static List<Actuator> actuators = new ArrayList<>();
    private static MARINAScheduler marinaScheduler;
    
    public static void main(String[] args) {
        System.out.println("Starting MARINA iFogSim2 Simulation...");
        
        try {
            CloudSim cloudsim = new CloudSim();
            createFogDevices();
            createMARINAScheduler();
            generateDynamicTasks(cloudsim);
            cloudsim.runStart();
            System.out.println("MARINA Simulation finished!");
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private static void createFogDevices() {
        System.out.println("Creating Fog Devices...");
        System.out.println("Using MARINA components directly (bypassing CloudSim devices)");
    }
    
    private static void createMARINAScheduler() {
        System.out.println("Initializing MARINA Scheduler...");
        
        List<BaseStation> baseStations = new ArrayList<>();
        List<VehicleState> vehicles = new ArrayList<>();
        Random random = new Random(42);
        
        baseStations.add(new BaseStation("BS1", 100.0, 100.0, 10000.0, 1000.0, 250.0));
        baseStations.add(new BaseStation("BS2", 400.0, 300.0, 15000.0, 1500.0, 250.0));
        
        for (int i = 1; i <= 8; i++) {
            double x = 50 + random.nextDouble() * 400;
            double y = 50 + random.nextDouble() * 400;
            double speed = 5 + random.nextDouble() * 20;
            double cpu = 2000 + random.nextDouble() * 3000;
            double storage = 500;
            
            vehicles.add(new VehicleState(0, "V" + i, x, y, speed, cpu, storage));
        }
        
        List<VehicularCloud> vcs = new ArrayList<>();
        for (BaseStation bs : baseStations) {
            List<VehicleState> nearbyVehicles = getVehiclesInRange(bs, vehicles);
            vcs.add(new VehicularCloud("VC_" + bs.getId(), bs, nearbyVehicles));
            System.out.println("VC " + bs.getId() + " has " + nearbyVehicles.size() + " vehicles");
        }
        
        marinaScheduler = new MARINAScheduler(vcs, baseStations);
        System.out.println("MARINA Scheduler ready with " + vcs.size() + " vehicular clouds");
    }
    
    private static void generateDynamicTasks(CloudSim cloudsim) {
        System.out.println("Setting up dynamic task generation...");
        
        Timer timer = new Timer();
        Random random = new Random(42);
        final int[] taskCounter = {0};
        
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                double currentTime = cloudsim.clock();
                
                if (currentTime > 60) {
                    timer.cancel();
                    System.out.println("Task generation stopped at time " + currentTime);
                    return;
                }
                
                int numTasks = random.nextInt(4) + 1;
                List<Task> tasks = new ArrayList<>();
                
                for (int i = 0; i < numTasks; i++) {
                    String taskId = "Task_" + (taskCounter[0]++);
                    double size = 2 + random.nextDouble() * 8;
                    double cpu = 1 + random.nextDouble() * 4;
                    double deadline = 3 + random.nextDouble() * 7;
                    
                    Task task = new Task(taskId, size, cpu, deadline, currentTime);
                    tasks.add(task);
                }
                
                System.out.println("Time " + currentTime + ": Generated " + tasks.size() + " tasks");
                
                Map<String, Map<Integer, double[]>> predictions = new HashMap<>();
                marinaScheduler.schedule(tasks, predictions, currentTime);
            }
        }, 0, 2000);
    }
    
    private static List<VehicleState> getVehiclesInRange(BaseStation bs, List<VehicleState> allVehicles) {
        List<VehicleState> inRange = new ArrayList<>();
        for (VehicleState vehicle : allVehicles) {
            double distance = calculateDistance(
                vehicle.getX(), vehicle.getY(),
                bs.getX(), bs.getY()
            );
            if (distance <= bs.getRange()) {
                inRange.add(vehicle);
            }
        }
        return inRange;
    }
    
    private static double calculateDistance(double x1, double y1, double x2, double y2) {
        return Math.sqrt(Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2));
    }
}
