import pandas as pd
import numpy as np
import os
from xy_predictor import XYPredictor
from speed_predictor import SpeedPredictor  # changed: use speed-only predictor

DATA_FILE = 'vehicle_trace_2.csv'

def main():
    if not os.path.exists(DATA_FILE):
        raise FileNotFoundError(f"Error: The input CSV file '{DATA_FILE}' was not found.")

    num_time_steps = 2
    num_predictions_to_make = 1  
    
    df = pd.read_csv(DATA_FILE)
    
    xy_predictors = {}
    speed_predictors = {}  # changed: separate dict for speed-only models
    
    all_predictions = []

    print(f"\n--- Training models for all vehicles from '{DATA_FILE}' ---")
    for vehicle_id in df['vehicle_id'].unique():
        print(f"Training models for vehicle {vehicle_id}...")
        
        history = df[df['vehicle_id'] == vehicle_id]
        
        # Train XY predictor (unchanged)
        xy_data = history[['x', 'y']].values
        xy_predictor = XYPredictor(num_time_steps)
        xy_predictors[vehicle_id] = xy_predictor
        if not xy_predictor.train(xy_data):
            print(f"  - Insufficient data for XY training for vehicle {vehicle_id}.")
        
        # Train Speed predictor ONLY (leave cpu_capacity untouched)
        speed_data = history[['speed']].values  # changed: single feature
        speed_predictor = SpeedPredictor(num_time_steps)  # changed
        speed_predictors[vehicle_id] = speed_predictor
        if not speed_predictor.train(speed_data):
            print(f"  - Insufficient data for Speed training for vehicle {vehicle_id}.")

    for t_interval in range(1, num_predictions_to_make + 1):
        
        for vehicle_id in df['vehicle_id'].unique():
            xy_predictor = xy_predictors.get(vehicle_id)
            speed_predictor = speed_predictors.get(vehicle_id)
            
            history = df[df['vehicle_id'] == vehicle_id]
            
            xy_predictions = []
            if xy_predictor and xy_predictor.is_trained:
                last_xy_data = history[['x', 'y']].tail(num_time_steps).values
                xy_predictions = xy_predictor.predict_next(last_xy_data, num_predictions=1)
            
            speed_predictions = []
            if speed_predictor and speed_predictor.is_trained:
                last_speed_data = history[['speed']].tail(num_time_steps).values
                speed_predictions = speed_predictor.predict_next(last_speed_data, num_predictions=1)
                
            if xy_predictions and speed_predictions:
                predicted_time = history['time'].max() + t_interval
                
                rounded_x = np.round(xy_predictions[0][0])
                rounded_y = np.round(xy_predictions[0][1])
                rounded_speed = np.round(speed_predictions[0])  

                prediction_dict = {
                    'vehicle_id': vehicle_id,
                    'time': predicted_time,
                    'x': rounded_x,
                    'y': rounded_y,
                    'speed': rounded_speed,
                }
                all_predictions.append(prediction_dict)

    if all_predictions:
        predictions_df = pd.DataFrame(all_predictions)
        combined_df = pd.concat([df, predictions_df], ignore_index=True)
        combined_df.to_csv(DATA_FILE, index=False)
    else:
        print("\nNo predictions were generated.")

if __name__ == "__main__":
    try:
        main()
    except Exception as e:
        print(f"An error occurred: {e}")
