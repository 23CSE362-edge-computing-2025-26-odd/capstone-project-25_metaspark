package org.fog.marina;
import java.io.*;
import java.util.*;
public class TraceLoader {
	public static List<VehicleState> loadVehicleTrace(String filePath) {
	    List<VehicleState> vehicles = new ArrayList<>();
	    try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
	        String line = br.readLine(); 
	        while ((line = br.readLine()) != null) {
	            if (line.trim().isEmpty()) continue;
	            String[] parts = line.split(",");
	            if (parts.length < 5) continue;
	            if (parts.length > 5 && parts[5].trim().equals("1")) continue;

	            int time = Integer.parseInt(parts[0].trim());
	            String vehicleId = parts[1].trim();
	            double x = Double.parseDouble(parts[2].trim());
	            double y = Double.parseDouble(parts[3].trim());
	            double speed = Double.parseDouble(parts[4].trim());

	            vehicles.add(new VehicleState(time, vehicleId, x, y, speed));
	        }
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	    return vehicles;
	}
    public static List<VehicleState> getVehiclesAtTime(List<VehicleState> all, int time) {
        List<VehicleState> snapshot = new ArrayList<>();
        for (VehicleState v : all) {
            if (v.getTime() == time) snapshot.add(v);
        }
        return snapshot;
    }
}
