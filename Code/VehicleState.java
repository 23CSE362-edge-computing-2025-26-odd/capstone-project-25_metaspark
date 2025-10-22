package org.fog.marina;

import java.util.*;

public class VehicleState {
    private static final double DEFAULT_CPU = 40.0;      
    private static final double DEFAULT_STORAGE = 100.0;

    private String id;
    private int time;
    private double x, y, speed;
    private double cpuCapacity;
    private double usedCpu;
    private double storage;
    private double predictedX, predictedY, predictedSpeed;

    private final List<Task> activeTasks = new ArrayList<>();

    public VehicleState(int time, String id, double x, double y, double speed) {
        this.time = time;
        this.id = id;
        this.x = x;
        this.y = y;
        this.speed = speed;
        this.cpuCapacity = DEFAULT_CPU;
        this.usedCpu = 0.0;
        this.storage = DEFAULT_STORAGE;
        this.predictedX = x;
        this.predictedY = y;
        this.predictedSpeed = speed;
    }

    public int getTime() { return time; }
    public String getId() { return id; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getSpeed() { return speed; }
    public double getCpuCapacity() { return cpuCapacity; }
    public double getUsedCpu() { return usedCpu; }
    public double getAvailableCpu() { return Math.max(0.0, cpuCapacity - usedCpu); }
    public double getStorage() { return storage; }

    public void updatePrediction(double px, double py, double ps) {
        this.predictedX = px; this.predictedY = py; this.predictedSpeed = ps;
    }

    public double getPredictedX() { return predictedX; }
    public double getPredictedY() { return predictedY; }
    public double getPredictedSpeed() { return predictedSpeed; }

    public boolean canProcess(Task t) {
        return (t.getCpu() <= getAvailableCpu()) && (t.getSize() <= storage);
    }

    public void assignTask(Task t, double currentTime) {
        double procTime = t.computeProcessingTime(cpuCapacity);
        double finishTime = currentTime + procTime;
        t.setFinishTime(finishTime);
        usedCpu += t.getCpu();
        activeTasks.add(t);

        double cost = t.getCpu() * 5.016;
        t.setCost(cost);

    }

    public void releaseFinished(double currentTime) {
        Iterator<Task> it = activeTasks.iterator();
        while (it.hasNext()) {
            Task t = it.next();
            if (currentTime >= t.getFinishTime()) {
                usedCpu = Math.max(0.0, usedCpu - t.getCpu());
                it.remove();
                System.out.println("  [Released->Vehicle] " + t.getId() + " finished on " + id);
            }
        }
    }

    public boolean isAvailable() {
        return getAvailableCpu() > 0.0;
    }
}
