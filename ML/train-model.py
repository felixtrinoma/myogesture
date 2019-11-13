import matplotlib.pyplot as plt
import sys
import struct
import tensorflow as tf

from tensorflow import keras

from tensorflow.keras import layers
import scipy.io
import numpy as np
import random
import math

if len(sys.argv) != 3:
    sys.exit("Usage: python train.py xxx.dat xxx.labels.txt")

data_log = sys.argv[1]
manual_labels = sys.argv[2]

# Time interval in s between two EMG samples
EMG_SAMPLE_INTERVAL = 0.003
# Arround the maximum measured EMG, allowing to bring the interesting
# part of the signal between 0 and 1
EMG_NORMALISATION = 0.0002
# Duration of a gesture in s, should be the same than the one configured in the app
GESTURE_DURATION = 2.5
# For each training sample, generate x different training samples by shifting
# the window
AUGMENTATION_RATIO = 20
# Max window shift in s allowed while augmenting the training set
MAX_AUGMENTATION_OFFSET = 0.25

# Generate a model for categorical classification
model = tf.keras.Sequential()

# Adding an hidden layer like Trinoma Matlab
# Can be commented out to use the output layer only
# Dropout can be used to mitigate overfitting
model.add(layers.Dense(100,
        activation='relu',
        kernel_initializer='random_uniform',
        bias_initializer='zeros'))
# # model.add(layers.Dropout(0.25, input_shape=(100,)))
model.add(layers.Dense(50,
        activation='relu',
        kernel_initializer='random_uniform',
        bias_initializer='zeros'))
# model.add(layers.Dropout(0.25, input_shape=(50,)))

# Output layer, works pretty without hidden layers
model.add(layers.Dense(10,
        activation='relu',
        kernel_initializer='random_uniform',
        bias_initializer='zeros'))

# Output layer
model.add(layers.Dense(4, activation='softmax'))

model.compile(optimizer=tf.keras.optimizers.SGD(lr=0.1),
              loss=tf.keras.losses.CategoricalCrossentropy(),
              metrics=[tf.keras.metrics.CategoricalAccuracy()])

plt.style.use('ggplot')

is_first_frame = True
first_stamp = None
prev_stamp = None
frame_stamps = []
frame_intervals = []

stamps_emgrms = []
s1_emgrms = []
s2_emgrms = []

# Unpack 2 EMG RMS in EMG RMS mode
def unpack_raw_data(data):
    data = struct.unpack('>20d', data)
    emg1 = data[0:10]
    emg2 = data[10:20]
    return emg1, emg2

# Unpack 2 EMG RMS in EMG RMS + Acc mode
# def unpack_raw_data(data):
#     data = struct.unpack('>44d', data)
#     emg1 = data[0:10]
#     emg2 = data[22:32]
#     return emg1, emg2

with open(data_log,'rb') as f:
    while True:
        empty = f.readline()
        if empty != b'\n':
            break
        header = f.readline().decode("ascii")
        stamp, size = header.split('/')
        stamp = float(stamp) / 1e9
        size = int(size)
        if is_first_frame:
            first_stamp = stamp
            frame_stamps.append(0.0)
            frame_intervals.append(0.0)
        else:
            frame_stamps.append(stamp - first_stamp)
            frame_intervals.append(stamp - prev_stamp)

        data = f.read(size)
        if len(data) != size:
            break
        # First frame might be used to store metadata instead of actual data
        if not is_first_frame:
            emg1, emg2 = unpack_raw_data(data)
            s1_emgrms += emg1
            s2_emgrms += emg2
            for i in range(0, 10):
                stamps_emgrms.append(stamp - first_stamp + i * EMG_SAMPLE_INTERVAL)

        prev_stamp = stamp
        is_first_frame = False

s1_emgrms = [value if not math.isnan(value) else 0.0 for value in s1_emgrms]
s2_emgrms = [value if not math.isnan(value) else 0.0 for value in s2_emgrms]
max_emg = max(max(s1_emgrms), max(s2_emgrms))
max_emg = EMG_NORMALISATION
s1_emgrms = [value / max_emg for value in s1_emgrms]
s2_emgrms = [value / max_emg for value in s2_emgrms]

with open(manual_labels, 'r') as f:
    manual_labels = f.readlines()
manual_labels = [row.split(", ") for row in manual_labels if ", " in row]
manual_labels = [(int(row[0]), int(row[1])) for row in manual_labels]

def extract_feature(samples, sample_id, duration, offset):
    duration = round(duration / EMG_SAMPLE_INTERVAL)
    offset = round(offset / EMG_SAMPLE_INTERVAL)
    start_id = sample_id + offset
    end_id = start_id + duration
    return samples[start_id:end_id]

def plot_emg_sample(axis, samples):
    stamps = [t * EMG_SAMPLE_INTERVAL for t in range(0, len(samples))]
    axis.plot(stamps, samples)

# Display manual labels,
# each row containing all the samples

sorted_manual_labels = [[], [], [], []]
for row in manual_labels:
    sorted_manual_labels[row[1]].append(row[0])

fig = plt.figure()
rows = len(sorted_manual_labels)
cols = max([len(l) for l in sorted_manual_labels])
for r in range(0,rows):
    for c in range(0,cols):
        i = r*cols+c
        if c >= len(sorted_manual_labels[r]):
            break
        index = sorted_manual_labels[r][c]
        ax = fig.add_subplot(rows,cols,i+1)
        ax.set_ylim([0.0, 1.0])
        s1 = extract_feature(s1_emgrms, index, GESTURE_DURATION, 0)
        s2 = extract_feature(s2_emgrms, index, GESTURE_DURATION, 0)
        plot_emg_sample(ax, s1)
        plot_emg_sample(ax, s2)

fig.show()

random.shuffle(manual_labels)

validation_count = len(manual_labels) // 4

print("Got {} samples, taking {} for validation".format(len(manual_labels), validation_count))

manual_labels_val = manual_labels[:validation_count]
manual_labels_train = manual_labels[validation_count:]

def make_data_and_labels(manual_labels, augment = 1, max_offset = None):
    data = []
    labels = []
    for i in range(augment):
        for i in range(len(manual_labels)):
            sample_id, label = manual_labels[i]
            if max_offset is None:
                offset = 0
            else:
                offset = random.uniform(-max_offset, max_offset)
            s1 = extract_feature(s1_emgrms, sample_id, GESTURE_DURATION, offset)
            s2 = extract_feature(s2_emgrms, sample_id, GESTURE_DURATION, offset)
            data.append(s1 + s2)
            one_hot_label = [0.0, 0.0, 0.0, 0.0]
            one_hot_label[label] = 1.0
            labels.append(one_hot_label)

    data = np.array(data)
    labels = np.array(labels)
    return data, labels

data_val, labels_val = make_data_and_labels(manual_labels_val)
data_train, labels_train = make_data_and_labels(
    manual_labels_train,
    augment = AUGMENTATION_RATIO, max_offset = MAX_AUGMENTATION_OFFSET)

class PlotHistory(keras.callbacks.Callback):

    def __init__(self):
        fig = plt.figure()
        self.ax_acc = fig.add_subplot(1,2,1)
        self.ax_loss = fig.add_subplot(1,2,2)

    def on_train_begin(self, logs={}):
        self.acc = []
        self.val_acc = []
        self.losses = []
        self.val_losses = []

    def on_epoch_end(self, batch, logs={}):
        self.acc.append(logs.get('categorical_accuracy'))
        self.val_acc.append(logs.get('val_categorical_accuracy'))
        self.losses.append(logs.get('loss'))
        self.val_losses.append(logs.get('val_loss'))

        self.ax_acc.clear()
        self.ax_acc.plot(self.acc)
        self.ax_acc.plot(self.val_acc)
        self.ax_acc.set_title('Model accuracy')
        self.ax_acc.set_ylabel('Accuracy')
        self.ax_acc.set_xlabel('Epoch')
        self.ax_acc.legend(['Train', 'Test'], loc='upper left')

        self.ax_loss.clear()
        self.ax_loss.plot(self.losses)
        self.ax_loss.plot(self.val_losses)
        self.ax_loss.set_title('Model loss')
        self.ax_loss.set_ylabel('Loss')
        self.ax_loss.set_xlabel('Epoch')
        self.ax_loss.legend(['Train', 'Test'], loc='upper left')

        plt.draw()
        plt.pause(0.01)

history = model.fit(
    data_train, labels_train,
    epochs=250, batch_size=32,
    validation_data=(data_val, labels_val),
    callbacks=[PlotHistory()])

converter = tf.lite.TFLiteConverter.from_keras_model(model)
tflite_model = converter.convert()
open("model.tflite", "wb").write(tflite_model)

plt.show()
