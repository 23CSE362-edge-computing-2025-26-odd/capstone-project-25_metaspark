import os
import pandas as pd
import traci
import time
from xy_predictor import XYPredictor
from speed_predictor import SpeedPredictor
import tempfile

SUMO_CMD = [
    "sumo",
    "-c", r"C:\Users\PRAVEEN\Downloads\Code\Code\cologne6to8.sumocfg"
]

OUTPUT_FILE = r"C:\Users\PRAVEEN\eclipse-workspace\iFogSim22\dataset\vehicle_trace_2.csv"
PREDICT_EVERY = 3
NUM_TIME_STEPS = 2
MAX_EMPTY_TICKS = 50

os.makedirs(os.path.dirname(OUTPUT_FILE), exist_ok=True)

print("Starting SUMO...")
traci.start(SUMO_CMD)

history_df = pd.DataFrame(columns=["time","vehicle_id","x","y","speed","flag"])
xy_models = {}
speed_models = {}

def collect_current(sim_time):
    rows = []
    for vid in traci.vehicle.getIDList():
        x, y = traci.vehicle.getPosition(vid)
        speed = traci.vehicle.getSpeed(vid)
        rows.append([int(sim_time), vid, x, y, speed, 0])
    if len(rows) == 0:
        return pd.DataFrame(columns=history_df.columns)
    return pd.DataFrame(rows, columns=history_df.columns)

def train_predict_and_build(sim_time, df):
    all_preds = []
    for vid in df['vehicle_id'].unique():
        hist = df[df['vehicle_id'] == vid].sort_values('time')
        if len(hist) < NUM_TIME_STEPS + 5:
            continue
        if vid not in xy_models:
            m = XYPredictor(NUM_TIME_STEPS)
            if m.train(hist[['x','y']].values):
                xy_models[vid] = m
        if vid not in speed_models:
            s = SpeedPredictor(NUM_TIME_STEPS)
            if s.train(hist[['speed']].values):
                speed_models[vid] = s
        if vid in xy_models and vid in speed_models:
            xy_pred = xy_models[vid].predict_next(hist[['x','y']].tail(NUM_TIME_STEPS).values, num_predictions=1)
            sp_pred = speed_models[vid].predict_next(hist[['speed']].tail(NUM_TIME_STEPS).values, num_predictions=1)
            if xy_pred and sp_pred:
                pred_time = int(sim_time) + 1
                x_pred = float(xy_pred[0][0])
                y_pred = float(xy_pred[0][1])
                spred = float(sp_pred[0])
                all_preds.append([pred_time, vid, x_pred, y_pred, spred, 1])
                print(f"Predicted vehicle {vid} at time {pred_time}: x={x_pred:.2f}, y={y_pred:.2f}, speed={spred:.2f}")
    if len(all_preds) > 0:
        return pd.DataFrame(all_preds, columns=history_df.columns)
    return pd.DataFrame(columns=history_df.columns)

def atomic_write(df, out_path):
    dirn = os.path.dirname(out_path)
    fd, tmp = tempfile.mkstemp(dir=dirn, suffix=".tmp")
    os.close(fd)
    df.to_csv(tmp, index=False)
    os.replace(tmp, out_path)

print("Running simulation loop (live trace)...")
empty_ticks = 0
tick = 0

while True:
    traci.simulationStep()
    sim_time = traci.simulation.getTime()
    sim_time = int(sim_time)
    vehicle_ids = traci.vehicle.getIDList()
    print(f"Tick counter: {tick}, sim_time: {sim_time}, Vehicles: {len(vehicle_ids)}")
    step_df = collect_current(sim_time)
    if not step_df.empty:
        history_df = pd.concat([history_df, step_df], ignore_index=True)
        empty_ticks = 0
    else:
        empty_ticks += 1
    if tick % PREDICT_EVERY == 0 and len(history_df) > NUM_TIME_STEPS:
        preds_df = train_predict_and_build(sim_time, history_df)
        if not preds_df.empty:
            history_df = pd.concat([history_df, preds_df], ignore_index=True)
            atomic_write(history_df, OUTPUT_FILE)
            print("Wrote predictions to", OUTPUT_FILE)
    if tick % 5 == 0:
        atomic_write(history_df, OUTPUT_FILE)
    tick += 1
    if empty_ticks >= MAX_EMPTY_TICKS:
        print(f"No vehicles for {MAX_EMPTY_TICKS} ticks. Ending simulation.")
        break

traci.close()
print("SUMO closed. Final trace saved at:", OUTPUT_FILE)
