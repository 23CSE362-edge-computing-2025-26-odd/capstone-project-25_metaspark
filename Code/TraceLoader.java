package org.fog.marina;

import java.io.*;
import java.util.*;
import java.nio.file.*;

/**
 * Loads observed (flag=0) vehicle positions and speeds
 */
public class TraceLoader {

    public static List<VehicleState> loadVehicleTrace(String csvPath) {
        List<VehicleState> vehicles = new ArrayList<>();
        if (!Files.exists(Paths.get(csvPath))) return vehicles;

        try (BufferedReader br = new BufferedReader(new FileReader(csvPath))) {
            String line;
            boolean first = true;
            while ((line = br.readLine()) != null) {
                if (first) { first = false; continue; }
                String[] p = line.split(",");
                if (p.length < 6) continue;

                int t = (int) Double.parseDouble(p[0]);
                String id = p[1];
                double x = Double.parseDouble(p[2]);
                double y = Double.parseDouble(p[3]);
                double speed = Double.parseDouble(p[4]);
                int flag = (int) Double.parseDouble(p[5]);

                if (flag == 0) {
                    vehicles.add(new VehicleState(t, id, x, y, speed));
                }
            }
        } catch (Exception e) {
            System.err.println("[TraceLoader] Error: " + e.getMessage());
        }

        return vehicles;
    }

    public static List<VehicleState> getVehiclesAtTime(List<VehicleState> all, int time) {
        List<VehicleState> res = new ArrayList<>();
        for (VehicleState v : all)
            if (v.getTime() == time)
                res.add(v);
        return res;
    }

    public static Integer getMinObservedTime(String csvPath) {
        if (!Files.exists(Paths.get(csvPath))) return null;
        try (BufferedReader br = new BufferedReader(new FileReader(csvPath))) {
            String line = br.readLine(); // skip header
            line = br.readLine();
            if (line == null) return null;
            String[] p = line.split(",");
            if (p.length < 1) return null;
            return (int) Double.parseDouble(p[0]);
        } catch (Exception e) {
            return null;
        }
    }
}
