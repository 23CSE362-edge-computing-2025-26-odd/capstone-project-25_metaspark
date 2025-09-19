package org.fog.test;

import org.fog.marina.*;
import org.fog.placement.MARINAScheduler;

import java.util.*;

public class TestMARINA {
    public static void main(String[] args) {
        String vehicleTraceFile = "dataset/sample_vehicle_trace.csv"; 
        String predictedFile = "dataset/predict_vehicle_metrics.csv";

        List<VehicleState> allVehicles = TraceLoader.loadVehicleTrace(vehicleTraceFile);

        List<BaseStation> baseStations = new ArrayList<>();
        baseStations.add(new BaseStation("BS1", 100.0, 100.0, 50.0, 200.0, 250.0));
        baseStations.add(new BaseStation("BS2", 400.0, 300.0, 70.0, 300.0, 250.0));

        Map<String, Map<Integer,double[]>> predicted = ResourcePredictor.loadPredictions(predictedFile);

        int maxTime = allVehicles.stream().mapToInt(VehicleState::getTime).max().orElse(0);
        double poissonLambda = 3.0;

        Random rnd = new Random(42);
        List<VehicleState> activeRandomVehicles = new ArrayList<>();

        for (int time = 0; time <= maxTime; time++) {
            System.out.println("\nScheduling at time " + time);

            List<VehicleState> vehicles = TraceLoader.getVehiclesAtTime(allVehicles, time);

        
            for (VehicleState rv : activeRandomVehicles) {
                rv.moveOneTick();
                vehicles.add(rv);
            }

        
            if (rnd.nextDouble() < 0.2) {
                String newId = "RandVeh_" + time + "_" + rnd.nextInt(1000);
                double x = rnd.nextDouble() * 500;
                double y = rnd.nextDouble() * 500;
                double speed = 5 + rnd.nextDouble() * 20;
                double cpu = 50 + rnd.nextDouble() * 50;
                VehicleState newVeh = new VehicleState(time, newId, x, y, speed);

                vehicles.add(newVeh);
                activeRandomVehicles.add(newVeh);
                System.out.println("New persistent random vehicle added: " + newId);
            }

            for (VehicleState v : vehicles) {
            	if (predicted.containsKey(v.getId())) {
            	    Map<Integer,double[]> preds = predicted.get(v.getId());
            	    if (preds.containsKey(1)) {
            	        double[] pr = preds.get(1);
            	        v.updatePrediction(pr[0], pr[1], pr[2]);
            	    }
            	}

            }

            List<VehicularCloud> vcs = new ArrayList<>();
            for (BaseStation bs : baseStations) {
                VehicularCloud vc = new VehicularCloud("VC_" + bs.getId(), bs, new ArrayList<>());
                vcs.add(vc);
            }
            for (VehicleState v : vehicles) {
                BaseStation nearest = null;
                double bestDist = Double.MAX_VALUE;
                for (BaseStation bs : baseStations) {
                    double d = distance(v.getPredictedX(), v.getPredictedY(), bs.getX(), bs.getY());
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
                    VehicularCloud adHoc = new VehicularCloud("VC_vehicle_" + v.getId(), null, new ArrayList<>());
                    adHoc.getVehicles().add(v);
                    vcs.add(adHoc);
                }
            }

            List<Task> tasks = new ArrayList<>();
            int arrivals = poissonRandom(poissonLambda, rnd);
            for (int a = 0; a < arrivals; a++) {
                if (vehicles.isEmpty()) break;
                VehicleState origin = vehicles.get(rnd.nextInt(vehicles.size()));
                String taskId = "Task_" + origin.getId() + "_t" + time + "_a" + a;
                double size = 1 + rnd.nextDouble() * 9;
                double cpu = Math.min(origin.getCpuCapacity() * 0.2, 100.0);
                double deadline = 3 + rnd.nextInt(5);
                Task t = new Task(taskId, size, cpu, deadline, time);
                tasks.add(t);
            }
            System.out.println("Generated " + tasks.size() + " tasks at time " + time);

            MARINAScheduler scheduler = new MARINAScheduler(vcs, baseStations);
            scheduler.schedule(tasks, predicted, time);
        }
    }

    private static double distance(double x1, double y1, double x2, double y2) {
        double dx = x1 - x2;
        double dy = y1 - y2;
        return Math.sqrt(dx * dx + dy * dy);
    }

    private static int poissonRandom(double lambda, Random rnd) {
        double L = Math.exp(-lambda);
        int k = 0;
        double p = 1.0;
        do {
            k++;
            p *= rnd.nextDouble();
        } while (p > L);
        return k - 1;
    }
}
