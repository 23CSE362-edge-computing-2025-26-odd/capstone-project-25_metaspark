package org.fog.placement;

import org.fog.marina.*;
import java.util.*;
import java.util.stream.Collectors;

public class MARINAScheduler {

    private List<VehicularCloud> vcs;
    private List<BaseStation> baseStations;

    private double vehicleCost = 5.016;
    private double baseCost = 11.444;
    private final Random rand = new Random();

    public MARINAScheduler(List<VehicularCloud> vcs, List<BaseStation> baseStations) {
        this.vcs = vcs;
        this.baseStations = baseStations;
    }

    public void updateVcs(List<VehicularCloud> newVcs) {
        this.vcs = newVcs;
    }

    public void releaseFinishedTasks(double currentTime) {
        for (VehicularCloud vc : vcs) {
            for (VehicleState v : vc.getVehicles())
                v.releaseFinished(currentTime);
            if (vc.getBaseStation() != null)
                vc.getBaseStation().releaseFinished(currentTime);
        }
    }

    private List<Task> paretoFilter(List<Task> tasks, List<VehicleState> vehicles) {
        List<Task> pareto = new ArrayList<>();
        for (Task t1 : tasks) {
            boolean dominated = false;
            for (Task t2 : tasks) {
                if (t2 == t1) continue;
                if (t2.getDeadline() <= t1.getDeadline() && t2.getCpu() <= t1.getCpu()
                        && (t2.getDeadline() < t1.getDeadline() || t2.getCpu() < t1.getCpu())) {
                    dominated = true;
                    break;
                }
            }
            if (!dominated) pareto.add(t1);
        }

        int minKeep = adaptiveKeepCount(tasks.size(), vehicles.size(), baseStations.size());

        if (pareto.size() < minKeep) {
            List<Task> fallback = new ArrayList<>(pareto);
            List<Task> remaining = new ArrayList<>(tasks);
            remaining.removeAll(pareto);
            Collections.shuffle(remaining, rand);
            while (fallback.size() < minKeep && !remaining.isEmpty()) {
                fallback.add(remaining.remove(0));
            }
            System.out.println("[Pareto] Kept " + fallback.size() + " tasks (with fallback).");
            return fallback;
        }

        System.out.println("[Pareto] Kept " + pareto.size() + " tasks.");
        return pareto;
    }

    private int adaptiveKeepCount(int taskCount, int vehicleCount, int bsCount) {
        int totalResources = Math.max(1, vehicleCount + bsCount);
        int baseKeep = Math.max(5, (int) Math.round(taskCount * 0.1));
        int randomBoost = rand.nextInt(Math.min(6, totalResources));
        return Math.min(taskCount, baseKeep + randomBoost);
    }

    private boolean isVehicleStableForVC(VehicleState v, VehicularCloud vc) {
        if (v.getPredictedSpeed() > 70.0) return false;
        if (vc.getBaseStation() != null) {
            double dx = v.getPredictedX() - vc.getBaseStation().getX();
            double dy = v.getPredictedY() - vc.getBaseStation().getY();
            double dist = Math.sqrt(dx * dx + dy * dy);
            return dist <= vc.getBaseStation().getRange() * 1.5;
        }
        return true;
    }

    private double computeCost(Task task, boolean isBaseStation) {
        double processTime = 0.1; 
        double price = isBaseStation ? baseCost : vehicleCost;
        return processTime * task.getCpu() * price;
    }

    public void schedule(List<Task> tasks, Map<String, Map<Integer,double[]>> predicted, double currentTime) {
        if (tasks == null || tasks.isEmpty()) return;

        int vehicleCount = vcs.stream().mapToInt(vc -> vc.getVehicles().size()).sum();
        System.out.println("[Tick " + (int) currentTime + "] Vehicles: " + vehicleCount + " | Tasks: " + tasks.size());

        List<Task> paretoSet = paretoFilter(tasks,
                vcs.stream().flatMap(vc -> vc.getVehicles().stream()).collect(Collectors.toList()));

        List<String> assignedLogs = new ArrayList<>();

        for (VehicularCloud vc : vcs) {
            if (paretoSet.isEmpty()) break;

            vc.getVehicles().sort((v1, v2) -> Double.compare(v2.getAvailableCpu(), v1.getAvailableCpu()));

            for (Task t : new ArrayList<>(paretoSet)) {
                boolean assigned = false;

                if (rand.nextDouble() < 0.7) {
                    for (VehicleState v : vc.getVehicles()) {
                        if (!isVehicleStableForVC(v, vc)) continue;
                        if (!v.canProcess(t)) continue;

                        double finishTime = currentTime + (t.getCpu() / v.getCpuCapacity());
                        double deadline = t.getArrivalTime() + t.getDeadline();

                        if (finishTime <= deadline) {
                            v.assignTask(t, currentTime);
                            double cost = computeCost(t, false);
                            assignedLogs.add(String.format(
                                "  [Assigned->Vehicle] %-22s -> %-12s [Finish=%.3f, Deadline=%.3f, CPU=%.3f, Cost=%.3f]",
                                t.getId(), v.getId(), finishTime, deadline, t.getCpu(), cost
                            ));
                            paretoSet.remove(t);
                            assigned = true;
                            break;
                        }
                    }
                }

                if (!assigned && vc.getBaseStation() != null && vc.getBaseStation().canProcess(t)) {
                    double finishTime = currentTime + (t.getCpu() / vc.getBaseStation().getCpuCapacity());
                    double deadline = t.getArrivalTime() + t.getDeadline();

                    if (finishTime <= deadline) {
                        vc.getBaseStation().assignTask(t, currentTime);
                        double cost = computeCost(t, true);
                        assignedLogs.add(String.format(
                            "  [Assigned->BS]      %-22s -> %-4s [Finish=%.3f, Deadline=%.3f, CPU=%.3f, Cost=%.3f]",
                            t.getId(), vc.getBaseStation().getId(), finishTime, deadline, t.getCpu(), cost
                        ));
                        paretoSet.remove(t);
                        assigned = true;
                    }
                }

                if (!assigned) {
                    for (VehicularCloud otherVc : vcs) {
                        for (VehicleState v2 : otherVc.getVehicles()) {
                            if (v2.canProcess(t)) {
                                double finishTime = currentTime + (t.getCpu() / v2.getCpuCapacity());
                                double deadline = t.getArrivalTime() + t.getDeadline();

                                if (finishTime <= deadline) {
                                    v2.assignTask(t, currentTime);
                                    double cost = computeCost(t, false);
                                    assignedLogs.add(String.format(
                                        "  [Assigned->Vehicle] %-22s -> %-12s [Finish=%.3f, Deadline=%.3f, CPU=%.3f, Cost=%.3f]",
                                        t.getId(), v2.getId(), finishTime, deadline, t.getCpu(), cost
                                    ));
                                    paretoSet.remove(t);
                                    assigned = true;
                                    break;
                                }
                            }
                        }
                        if (assigned) break;
                    }
                }

                if (!assigned) {
                    assignedLogs.add(String.format("  [CloudOffload] %-25s (no local fit)", t.getId()));
                    paretoSet.remove(t);
                }
            }
        }

        assignedLogs.forEach(System.out::println);
        System.out.println("-----------------------------------");
    }
}
