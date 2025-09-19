package org.fog.marina;

public class BaseStation {
    private String id;
    private double x, y;
    private double cpuCapacity;
    private double storage;
    private double range;

    private static final double COST_FACTOR = 11.444;

    public BaseStation(String id, double x, double y, double cpuCapacity, double storage, double range) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.cpuCapacity = cpuCapacity; 
        this.storage = storage;
        this.range = range;
    }

    public String getId() { return id; }
    public double getCpuCapacity() { return cpuCapacity; }
    public double getStorage() { return storage; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getRange() { return range; }

    public boolean canProcess(Task task) {
        return task.getCpu() <= cpuCapacity && task.getSize() <= storage;
    }

    public void assignTask(Task task, double currentTime) {
        double procTime = task.computeProcessingTime(cpuCapacity);
        double finishTime = currentTime + procTime;
        task.setFinishTime(finishTime);

        double cost = task.getCpu() * COST_FACTOR;
        task.setCost(cost);

        System.out.println("Task " + task.getId() + " assigned to Base Station " + id +
                           " [Finish=" + finishTime + ", Deadline=" + (task.getArrivalTime() + task.getDeadline()) +
                           ", Cost=" + cost + "]");
    }
}
