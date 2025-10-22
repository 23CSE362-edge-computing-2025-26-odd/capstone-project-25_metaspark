# xy_predictor.py
import numpy as np
import tensorflow as tf
from sklearn.preprocessing import MinMaxScaler
from tensorflow.keras.models import Sequential
from tensorflow.keras.layers import LSTM, Dense, Dropout

class XYPredictor:
    def __init__(self, time_steps):
        self.time_steps = time_steps
        self.num_features = 2
        self.scaler = MinMaxScaler(feature_range=(0,1))
        self.model = self._create_lstm_model()
        self.is_trained = False

    def _create_lstm_model(self):
        model = Sequential()
        model.add(LSTM(128, input_shape=(self.time_steps, self.num_features), return_sequences=True))
        model.add(Dropout(0.2))
        model.add(LSTM(64))
        model.add(Dropout(0.2))
        model.add(Dense(self.num_features, activation='linear'))
        model.compile(optimizer='adam', loss='mean_squared_error')
        return model

    def train(self, data, epochs=10, batch_size=8, val_steps=5):
        T = len(data)
        if T <= self.time_steps + val_steps:
            return False
        train_upto = T - val_steps
        self.scaler.fit(data[:train_upto])
        scaled = self.scaler.transform(data)

        X, y = self._create_dataset(scaled)
        if len(X) <= val_steps:
            return False

        X_train, y_train = X[:-val_steps], y[:-val_steps]
        X_val, y_val = X[-val_steps:], y[-val_steps:]

        self.model.fit(X_train, y_train, validation_data=(X_val, y_val),
                       epochs=epochs, batch_size=batch_size, verbose=0)
        self.is_trained = True
        return True

    def _create_dataset(self, data):
        X, y = [], []
        for i in range(len(data) - self.time_steps):
            X.append(data[i:(i + self.time_steps)])
            y.append(data[i + self.time_steps])
        return np.array(X), np.array(y)

    def predict_next(self, last_known_data, num_predictions=1):
        if not self.is_trained: return []
        seq = self.scaler.transform(last_known_data).reshape(1, self.time_steps, self.num_features)
        preds = []
        for _ in range(num_predictions):
            p_scaled = self.model.predict(seq, verbose=0)
            p = self.scaler.inverse_transform(p_scaled)
            preds.append(p[0])
            seq = np.vstack([seq[0][1:], p_scaled[0].reshape(1, -1)]).reshape(1, self.time_steps, self.num_features)
        return preds
