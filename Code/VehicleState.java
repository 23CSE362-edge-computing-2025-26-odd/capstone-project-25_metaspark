package org.fog.marina;

public class VehicleState {
    private static final double DEFAULT_CPU = 5.0; 
    private static final double DEFAULT_STORAGE = 100.0;

    private String id;
    private int time;
    private double x, y;
    private double speed;
    private double cpuCapacity;
    private double storage;
    private boolean available;

    private double vx, vy;

    private double predictedX, predictedY, predictedSpeed;

    public VehicleState(int time, String id, double x, double y, double speed) {
        this.time = time;
        this.id = id;
        this.x = x; this.y = y;
        this.speed = speed;
        this.cpuCapacity = DEFAULT_CPU;
        this.storage = DEFAULT_STORAGE;
        this.available = true;

        this.predictedX = x;
        this.predictedY = y;
        this.predictedSpeed = speed;

        double angle = Math.random() * 2 * Math.PI;
        this.vx = speed * Math.cos(angle);
        this.vy = speed * Math.sin(angle);
    }

    public void moveOneTick() {
        this.x += vx;
        this.y += vy;
    }

    public int getTime() { return time; }
    public String getId() { return id; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getSpeed() { return speed; }
    public double getCpuCapacity() { return cpuCapacity; }
    public double getStorage() { return storage; }
    public boolean isAvailable() { return available; }

    public void setAvailable(boolean available) { this.available = available; }

    public void updatePrediction(double predictedX, double predictedY, double predictedSpeed) {
        this.predictedX = predictedX;
        this.predictedY = predictedY;
        this.predictedSpeed = predictedSpeed;
    }

    public double getPredictedX() { return predictedX; }
    public double getPredictedY() { return predictedY; }
    public double getPredictedSpeed() { return predictedSpeed; }

    public boolean canProcess(Task task) {
        return available && task.getCpu() <= cpuCapacity && task.getSize() <= storage;
    }

    public void assignTask(Task task, double currentTime) {
        this.available = false;
        double procTime = task.computeProcessingTime(cpuCapacity);
        double finishTime = currentTime + procTime;
        task.setFinishTime(finishTime);

        double cost = task.getCpu() * 1.0; 
        task.setCost(cost);

        System.out.println("Task " + task.getId() + " assigned to Vehicle " + id
                           + " [Finish=" + finishTime + ", Deadline=" + (task.getArrivalTime() + task.getDeadline())
                           + ", Cost=" + cost + "]");
    }
}

