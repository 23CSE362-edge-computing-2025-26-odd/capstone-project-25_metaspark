package org.fog.marina;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class BaseStation {
    private String id;
    private double x, y;
    private double range;
    private double cpuCapacity;
    private double usedCpu;
    private double storage;
    private List<Task> runningTasks;

    public BaseStation(String id, double x, double y, double range, double cpuCapacity, double storage) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.range = range;
        this.cpuCapacity = cpuCapacity;
        this.usedCpu = 0.0;
        this.storage = storage;
        this.runningTasks = new ArrayList<>();
    }

    public String getId() { return id; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getRange() { return range; }
    public double getCpuCapacity() { return cpuCapacity; }
    public double getUsedCpu() { return usedCpu; }
    public double getAvailableCpu() { return Math.max(0.0, cpuCapacity - usedCpu); }
    public double getStorage() { return storage; }

    public boolean canProcess(Task t) {
        return (t.getCpu() <= getAvailableCpu()) && (t.getSize() <= storage);
    }

    public void assignTask(Task t, double currentTime) {
        double procTime = t.computeProcessingTime(cpuCapacity);
        double finishTime = currentTime + procTime;
        t.setFinishTime(finishTime);
        usedCpu += t.getCpu();
        runningTasks.add(t);
        double cost = t.getCpu() * 11.444;
        t.setCost(cost);
        System.out.println(t.getId() +
                " -> BaseStation " + id +
                " [Finish=" + String.format("%.3f", finishTime) +
                ", Deadline=" + (t.getArrivalTime() + t.getDeadline()) +
                ", Cost=" + String.format("%.3f", cost) +
                ", BS_usedCpu=" + String.format("%.3f", usedCpu) + "]");
    }

    public void releaseTask(Task t) {
        usedCpu = Math.max(0.0, usedCpu - t.getCpu());
        runningTasks.remove(t);
    }

    public void releaseFinished(double currentTime) {
        Iterator<Task> it = runningTasks.iterator();
        while (it.hasNext()) {
            Task t = it.next();
            if (t.getFinishTime() <= currentTime) {
                usedCpu = Math.max(0.0, usedCpu - t.getCpu());
                it.remove();
            }
        }
    }

    @Override
    public String toString() {
        return "BaseStation{" +
                "id='" + id + '\'' +
                ", CPU=" + usedCpu + "/" + cpuCapacity +
                ", Storage=" + storage +
                ", Range=" + range +
                ", RunningTasks=" + runningTasks.size() +
                '}';
    }
}

