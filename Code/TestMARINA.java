package org.fog.test;

import org.fog.marina.*;
import org.fog.placement.MARINAScheduler;
import java.io.File;
import java.util.*;

public class TestMARINA {

    public static void main(String[] args) throws Exception {

        String traceFile = "dataset/vehicle_trace_2.csv";

        List<BaseStation> baseStations = new ArrayList<>();
        baseStations.add(new BaseStation("BS1", 12000, 10000, 9000, 2000, 100));
        baseStations.add(new BaseStation("BS2", 18000, 18000, 9000, 2500, 100));
        baseStations.add(new BaseStation("BS3", 24000, 7000, 9000, 2300, 100));

        Random rnd = new Random(42);
        System.out.println("Real-Time MARINA Scheduler");

        File f = new File(traceFile);
        while (!f.exists()) {
            System.out.println("Waiting for SUMO trace file...");
            Thread.sleep(1000);
        }

        Integer minT = null;
        while (minT == null) {
            minT = TraceLoader.getMinObservedTime(traceFile);
            if (minT == null) {
                System.out.println("Trace ready but no observed data...");
                Thread.sleep(1000);
            }
        }

        int time = minT;
        System.out.println("Starting scheduler");

        MARINAScheduler scheduler = new MARINAScheduler(new ArrayList<>(), baseStations);

        while (true) {

            List<VehicleState> allVehicles = TraceLoader.loadVehicleTrace(traceFile);
            List<VehicleState> vehicles = TraceLoader.getVehiclesAtTime(allVehicles, time);

            if (vehicles.isEmpty()) {
                System.out.println("[Tick " + time + "] No vehicles in range...");
                Thread.sleep(1000);
                continue;
            }
            Map<String, Map<Integer, double[]>> predicted = ResourcePredictor.loadPredictions(traceFile, time);
            for (VehicleState v : vehicles)
                if (predicted.containsKey(v.getId()))
                    v.updatePrediction(
                            predicted.get(v.getId()).getOrDefault(1, new double[]{v.getX(), v.getY(), v.getSpeed()})[0],
                            predicted.get(v.getId()).getOrDefault(1, new double[]{v.getX(), v.getY(), v.getSpeed()})[1],
                            predicted.get(v.getId()).getOrDefault(1, new double[]{v.getX(), v.getY(), v.getSpeed()})[2]);
            List<VehicularCloud> vcs = new ArrayList<>();
            for (BaseStation bs : baseStations)
                vcs.add(new VehicularCloud("VC_" + bs.getId(), bs, new ArrayList<>()));

            for (VehicleState v : vehicles) {
                BaseStation nearest = null;
                double bestDist = Double.MAX_VALUE;

                for (BaseStation bs : baseStations) {
                    double d = Math.hypot(v.getPredictedX() - bs.getX(), v.getPredictedY() - bs.getY());
                    if (d < bestDist && d <= bs.getRange()) {
                        bestDist = d;
                        nearest = bs;
                    }
                }

                if (nearest != null) {
                    for (VehicularCloud vc : vcs) {
                        if (vc.getBaseStation().getId().equals(nearest.getId())) {
                            vc.getVehicles().add(v);
                            break;
                        }
                    }
                } else {
                    VehicularCloud solo = new VehicularCloud("VC_vehicle_" + v.getId(), null, new ArrayList<>());
                    solo.getVehicles().add(v);
                    vcs.add(solo);
                }
            }

            List<Task> tasks = new ArrayList<>();
            double fixedSize = 5.0, fixedDeadline = 6.0;
            for (VehicleState v : vehicles) {
                String taskId = "Task_" + v.getId() + "_t" + time;
                double cpu = 2.0 + rnd.nextDouble() * 8.0;
                tasks.add(new Task(taskId, fixedSize, cpu, fixedDeadline, time));
            }

            scheduler.updateVcs(vcs);
            scheduler.releaseFinishedTasks(time);
            scheduler.schedule(tasks, predicted, time);

            time++;
            Thread.sleep(1000);
        }
    }
}
