package org.fog.marina;

import java.io.*;
import java.util.*;
import java.nio.file.*;


public class ResourcePredictor {

    public static Map<String, Map<Integer,double[]>> loadPredictions(String csvPath, int currentTime) {
        Map<String, Map<Integer,double[]>> result = new HashMap<>();

        if (!Files.exists(Paths.get(csvPath))) return result;

        try (BufferedReader br = new BufferedReader(new FileReader(csvPath))) {
            String line;
            boolean first = true;
            while ((line = br.readLine()) != null) {
                if (first) { first = false; continue; } 
                String[] p = line.split(",");
                if (p.length < 6) continue;
                int t = (int) Double.parseDouble(p[0]);
                String vid = p[1];
                double x = Double.parseDouble(p[2]);
                double y = Double.parseDouble(p[3]);
                double speed = Double.parseDouble(p[4]);
                int flag = (int) Double.parseDouble(p[5]);
                if (flag == 1 && t >= currentTime) {
                    result.putIfAbsent(vid, new HashMap<>());
                    result.get(vid).put(t - currentTime, new double[]{x, y, speed});
                }
            }
        } catch (Exception e) {
            System.err.println("[Predictor] Error: " + e.getMessage());
        }

        return result;
    }
}
