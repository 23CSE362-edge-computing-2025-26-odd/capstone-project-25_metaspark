# speed_cpu_predictor.py
import numpy as np
import tensorflow as tf
from sklearn.preprocessing import MinMaxScaler
from tensorflow.keras.models import Sequential, load_model
from tensorflow.keras.layers import LSTM, Dense, Dropout

class SpeedCPU_Predictor:
    def __init__(self, time_steps):
        self.time_steps = time_steps
        self.num_features = 2  # Only 'speed' and 'cpu_capacity'
        self.scaler = MinMaxScaler(feature_range=(0, 1))
        self.model = self._create_lstm_model()
        self.is_trained = False

    def _create_lstm_model(self):
        model = Sequential()
        model.add(LSTM(units=128, input_shape=(self.time_steps, self.num_features), return_sequences=True))
        model.add(Dropout(0.2))
        model.add(LSTM(units=64))
        model.add(Dropout(0.2))
        model.add(Dense(units=self.num_features, activation='linear'))
        model.compile(optimizer='adam', loss='mean_squared_error')
        return model

    def train(self, data, epochs=15, batch_size=1, val_steps=5, verbose=1):
        T = len(data)
        if T <= self.time_steps + val_steps:
            return False

        train_fit_upto = T - val_steps
        self.scaler.fit(data[:train_fit_upto])
        scaled_data = self.scaler.transform(data)

        X_all, y_all = self._create_dataset(scaled_data)
        y_all = y_all.reshape(y_all.shape[0], self.num_features)

        if len(X_all) <= val_steps:
            return False

        X_train, y_train = X_all[:-val_steps], y_all[:-val_steps]
        X_val, y_val = X_all[-val_steps:], y_all[-val_steps:]

        # Compute feature range on training data (original units) for NMAE
        train_data = data[:train_fit_upto]
        feature_range = np.maximum(train_data.max(axis=0) - train_data.min(axis=0), 1e-9)

        # Callback to print only percentage accuracy per epoch on validation set
        class ValPctAccCallback(tf.keras.callbacks.Callback):
            def __init__(self, X_val, y_val, scaler, feature_range, val_steps):
                super().__init__()
                self.X_val = X_val
                self.y_val = y_val
                self.scaler = scaler
                self.feature_range = feature_range
                self.val_steps = val_steps

            def on_epoch_end(self, epoch, logs=None):
                y_pred_scaled = self.model.predict(self.X_val, verbose=0)
                y_pred = self.scaler.inverse_transform(y_pred_scaled)
                y_true = self.scaler.inverse_transform(self.y_val)
                mae_per_feature = np.mean(np.abs(y_pred - y_true), axis=0)
                nmae = float(np.mean(mae_per_feature / self.feature_range))
                acc_pct = max(0.0, 100.0 * (1.0 - nmae))
                print(f"{acc_pct:.2f}%")

        history = self.model.fit(
            X_train,
            y_train,
            validation_data=(X_val, y_val),
            epochs=epochs,
            batch_size=batch_size,
            verbose=0,
            callbacks=[ValPctAccCallback(X_val, y_val, self.scaler, feature_range, val_steps)],
        )
        self.is_trained = True
        return True

    def _create_dataset(self, data):
        X, y = [], []
        for i in range(len(data) - self.time_steps):
            X.append(data[i:(i + self.time_steps)])
            y.append(data[i + self.time_steps])
        return np.array(X), np.array(y)

    def predict_next(self, last_known_data, num_predictions=1):
        if not self.is_trained:
            return []
        predictions = []
        current_input_sequence = self.scaler.transform(last_known_data).reshape(1, self.time_steps, self.num_features)
        for _ in range(num_predictions):
            predicted_scaled = self.model.predict(current_input_sequence, verbose=0)
            predicted_values = self.scaler.inverse_transform(predicted_scaled)
            predictions.append(predicted_values[0])
            current_input_sequence = np.vstack([current_input_sequence[0][1:], predicted_scaled[0].reshape(1, -1)]).reshape(1, self.time_steps, self.num_features)
        return predictions
