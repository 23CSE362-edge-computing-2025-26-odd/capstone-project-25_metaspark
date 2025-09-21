package org.fog.placement;

import org.fog.marina.*;
import java.util.*;

public class MARINAScheduler {

    private List<VehicularCloud> vcs;
    private List<BaseStation> baseStations;

    private double vehicleCost = 5.016;  
    private double baseCost = 11.444;    

    public MARINAScheduler(List<VehicularCloud> vcs, List<BaseStation> baseStations) {
        this.vcs = vcs;
        this.baseStations = baseStations;
    }
    private List<Task> paretoFilter(List<Task> tasks) {
        List<Task> paretoSet = new ArrayList<>();
        for (Task t1 : tasks) {
            boolean dominated = false;
            for (Task t2 : tasks) {
                if (t2.getDeadline() <= t1.getDeadline() && t2.getCpu() <= t1.getCpu()
                        && (t2.getDeadline() < t1.getDeadline() || t2.getCpu() < t1.getCpu())) {
                    dominated = true;
                    break;
                }
            }
            if (!dominated) paretoSet.add(t1);
        }
        return paretoSet;
    }

    private List<Task> binCovering(List<Task> tasks, double capacity) {
        tasks.sort((a, b) -> Double.compare(b.getCpu(), a.getCpu()));
        List<Task> selected = new ArrayList<>();
        double used = 0;
        for (Task task : tasks) {
            if (used + task.getCpu() <= capacity) {
                selected.add(task);
                used += task.getCpu();
            }
        }
        return selected;
    }

    private double computeCost(Task task, boolean isBaseStation) {
        return task.getCpu() * (isBaseStation ? baseCost : vehicleCost);
    }

    private boolean isVehicleStableForVC(VehicleState v, VehicularCloud vc) {

        if (v.getPredictedSpeed() > 30.0) return false;
        if (vc.getBaseStation() != null) {
            double dx = v.getPredictedX() - vc.getBaseStation().getX();
            double dy = v.getPredictedY() - vc.getBaseStation().getY();
            double dist = Math.sqrt(dx * dx + dy * dy);
            if (dist > vc.getBaseStation().getRange()) return false;
        }
        return true;
    }

    public void schedule(List<Task> tasks, Map<String, Map<Integer,double[]>> predicted, double currentTime) {
        List<Task> paretoSet = paretoFilter(tasks);
        System.out.println("[Pareto] Kept " + paretoSet.size() + " tasks.");

        vcs.sort((a, b) -> Double.compare(b.totalCpu(), a.totalCpu()));

        for (VehicularCloud vc : vcs) {
            if (paretoSet.isEmpty()) break;

            double vcCapacity = vc.totalCpu();
            if (vcCapacity <= 0) continue;

            List<Task> chosen = binCovering(new ArrayList<>(paretoSet), vcCapacity);

            for (Task t : new ArrayList<>(chosen)) {
                boolean assigned = false;
                vc.getVehicles().sort((v1, v2) -> Double.compare(v2.getCpuCapacity(), v1.getCpuCapacity()));
                for (VehicleState v : vc.getVehicles()) {
                    if (!v.isAvailable()) continue;
                    if (!isVehicleStableForVC(v, vc)) continue;
                    if (!v.canProcess(t)) continue;

                    double finishTime = currentTime + (t.getCpu() / v.getCpuCapacity());
                    if (finishTime <= t.getArrivalTime() + t.getDeadline()) {
                        v.assignTask(t, currentTime);
                        double cost = computeCost(t, false);
                        System.out.println("  [Cost] Task " + t.getId() + " cost = " + cost);
                        paretoSet.remove(t);
                        chosen.remove(t);
                        assigned = true;
                        break;
                    }
                }
                if (!assigned && vc.getBaseStation() != null) {
                    BaseStation bs = vc.getBaseStation();
                    if (bs.canProcess(t)) {
                        double finishTime = currentTime + (t.getCpu() / bs.getCpuCapacity());
                        if (finishTime <= t.getArrivalTime() + t.getDeadline()) {
                            bs.assignTask(t, currentTime);
                            double cost = computeCost(t, true);
                            System.out.println("  [Cost] Task " + t.getId() + " cost = " + cost);
                            paretoSet.remove(t);
                            chosen.remove(t);
                        }
                    }
                }
            }
        }
        for (Task t : paretoSet) {
            System.out.println("Task " + t.getId() + " sent to cloud.");
        }
    }
}
