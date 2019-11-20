import sys
import argparse
import random
import math
import json
import struct

import matplotlib.pyplot as plt
import numpy as np

import tensorflow as tf
from tensorflow import keras
from tensorflow.keras import layers

import rawdata

parser = argparse.ArgumentParser()
parser.add_argument("data", help="Path of the file containing the logged data (.dat)", type=str)
parser.add_argument("labels", help="Path of the file containing the labels (.labels.txt)", type=str)
parser.add_argument("--model-specs",
                    help="Path of the file specifying the model inputs and outputs",
                    default='shifumi.specs.json',
                    type=str)
parser.add_argument("--out",
                    help="Ouput model path",
                    default='model.tflite',
                    type=str)
parser.add_argument("--frame-layout",
                    help=("Layout of the raw data frames, "
                          "consisting of concatenated samples of each channels"),
                    nargs='+', default=[10, 10], type=int)
parser.add_argument("--input-channels",
                    help=("List of channels from the raw data to use for model input. "
                          "Channels specified as ids starting from 0. "
                          "ie. 0 3 to use the first and fourth."),
                    nargs='+', default=[0, 1], type=int)
parser.add_argument("--augmentation-ratio",
                    help=("For each training sample, "
                          "generate x different training samples by shifting the window"),
                    default=20, type=int)
parser.add_argument("--augmentation-offset",
                    help="Max window shift in s allowed while augmenting the training set",
                    default=0.25, type=float)
parser.add_argument("--epochs",
                    help="Number of times the training set will be used to train the model",
                    default=50, type=int)
parser.add_argument("--validation-size",
                    help="Percentage of samples to keep for validation",
                    default=25, type=int)
parser.add_argument("--end-marked",
                    help="End of gesture is marked in labels file, instead of start",
                    action='store_true')

args = parser.parse_args()

with open(args.model_specs, 'r') as json_file:
    model_specs = json.load(json_file)

GESTURE_DURATION = model_specs["model"]["input"]["duration"]
GESTURE_NAMES = model_specs["model"]["output"]["labels"]

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
model.add(layers.Dense(len(GESTURE_NAMES), activation='softmax'))

model.compile(optimizer=tf.keras.optimizers.SGD(lr=0.1),
              loss=tf.keras.losses.CategoricalCrossentropy(),
              metrics=[tf.keras.metrics.CategoricalAccuracy()])

# Loads channels samples from raw data

try:
    with open(args.data, 'rb') as f:
        timestamps, channels_samples = rawdata.read(f, args.frame_layout)
except struct.error as e:
    sys.stderr.write(str(e) + "\n")
    sys.exit("Unable to unpack raw data, did you specify the correct --frame-layout?")

if len(args.input_channels) != len(model_specs["model"]["input"]["signals"]):
    sys.exit(("Model specifies {} channels as input but "
              "{} have been specified with --input-channels"
             ).format(len(model_specs["model"]["input"]["signals"]),
                      len(args.input_channels)))

class Channel:
    def __init__(self, name, samples, sample_interval):
        self.name = name
        self.samples = samples
        self.sample_interval = sample_interval
        self.time = [sample_interval * i for i in range(len(samples))]

    def extract_feature(self, start_time, duration):
        start_id = int(round(start_time / self.sample_interval))
        size = int(round(duration / self.sample_interval))
        return self.samples[start_id:start_id+size]

    def replace_nans(self, replace_with):
        self.samples = [v if not math.isnan(v) else replace_with for v in self.samples]

    def scale(self, from_min, from_max, to_min, to_max):
        from_range = from_max - from_min
        to_range = to_max - to_min
        scale = lambda v: (((v - from_min) * to_range) / from_range) + to_min
        self.samples = [scale(value) for value in channel.samples]

channels = []
for channels_input_idx, name in enumerate(model_specs["model"]["input"]["signals"]):
    # Get samples from raw data
    channels_samples_idx = args.input_channels[channels_input_idx]
    samples = channels_samples[channels_samples_idx]

    if not "sample_interval" in model_specs["signals"][name]:
        sys.exit("No sample interval specified in model specs for channel {}".format(name))
    sample_interval = model_specs["signals"][name]["sample_interval"]

    channel = Channel(name, samples, sample_interval)

    # Normalize channels samples
    channel.replace_nans(0.0)
    if "scale" in model_specs["signals"][name]:
        scale = model_specs["signals"][name]["scale"]
        channel.scale(scale["from"][0], scale["from"][1], scale["to"][0], scale["to"][1])
        channel.scaled_to = scale["to"]
    else:
        channel.scaled_to = None

    channels.append(channel)

# Plot channels to check that the scaled data lies in the intended range

plt.style.use('ggplot')
fig = plt.figure()
fig.suptitle('Signals that will used to train the model')
for i, channel in enumerate(channels):
    ax = fig.add_subplot(len(channels), 1, i+1)
    ax.plot(channel.time, channel.samples, label=channel.name)
    ax.legend()
    if channel.scaled_to is not None:
        ax.axhline(channel.scaled_to[0], color='k')
        ax.axhline(channel.scaled_to[1], color='k')

plt.show()

# For now the label files contains rows like
# marker, gesture
# where marker is the id of the sample that have been marked in the first channel
#       gesture is a string naming the label

def marker_to_start_time(marker):
    start_time = channels[0].time[marker]
    if args.end_marked:
        start_time = start_time - GESTURE_DURATION
    return start_time

with open(args.labels, 'r') as f:
    manual_labels = f.readlines()
manual_labels = [row.split(", ") for row in manual_labels if ", " in row]
manual_labels = [(marker_to_start_time(int(row[0])), row[1].strip()) for row in manual_labels]

sorted_manual_labels = {}
for label in GESTURE_NAMES:
    sorted_manual_labels[label] = []

for time, label in manual_labels:
    if not label in sorted_manual_labels:
        sys.exit("Unable to find label {} in model output specs".format(label))
    sorted_manual_labels[label].append(time)

fig = plt.figure()
fig.suptitle('Samples that will used to train the model')
rows = len(sorted_manual_labels)
cols = max([len(l) for l in sorted_manual_labels.values()])

for row, label in enumerate(sorted_manual_labels.keys()):
    for col in range(0, cols):
        if col >= len(sorted_manual_labels[label]):
            break
        start_time = sorted_manual_labels[label][col]
        ax = fig.add_subplot(rows, cols, row * cols + col + 1)
        ax.set_ylim([0.0, 1.0])
        for channel in channels:
            samples = channel.extract_feature(start_time, GESTURE_DURATION)
            ax.plot(samples)

plt.show()

# Make training and validation sets

random.shuffle(manual_labels)

validation_count = round(len(manual_labels) * args.validation_size / 100.0)

print("Got {} samples, taking {} for validation".format(len(manual_labels), validation_count))

manual_labels_val = manual_labels[:validation_count]
manual_labels_train = manual_labels[validation_count:]

def one_hot_encode(label_name):
    return np.array([1.0 if label_name == name else 0.0 for name in GESTURE_NAMES])

def make_data_and_labels(manual_labels, augment=1, max_offset=None):
    inputs = []
    outputs = []
    for i in range(augment):
        for i in range(len(manual_labels)):
            start_time, label = manual_labels[i]
            if max_offset is None:
                offset = 0.0
            else:
                offset = random.uniform(-max_offset, max_offset)
            input = []
            for channel in channels:
                input += channel.extract_feature(start_time + offset, GESTURE_DURATION)
            inputs.append(np.array(input))
            outputs.append(one_hot_encode(label))
    return np.vstack(inputs), np.vstack(outputs)

data_val, labels_val = make_data_and_labels(manual_labels_val)
data_train, labels_train = make_data_and_labels(manual_labels_train,
                                                augment=args.augmentation_ratio,
                                                max_offset=args.augmentation_offset)

class PlotHistory(keras.callbacks.Callback):

    def __init__(self):
        fig = plt.figure()
        self.ax_acc = fig.add_subplot(1, 2, 1)
        self.ax_loss = fig.add_subplot(1, 2, 2)

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

history = model.fit(data_train, labels_train,
                    epochs=args.epochs, batch_size=32,
                    validation_data=(data_val, labels_val),
                    callbacks=[PlotHistory()])

converter = tf.lite.TFLiteConverter.from_keras_model(model)
tflite_model = converter.convert()
open(args.out, "wb").write(tflite_model)

plt.show()
