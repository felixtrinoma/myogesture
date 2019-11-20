import sys
import struct

# Modes and the number of samples of each channel present in a frame
FRAME_LAYOUTS = {
    "EMG RMS": [10],
    "EMG RMS+ACC": [10, 4, 4, 4],
    "EMG RMS+ACC, EMG RMS+ACC": [10, 4, 4, 4, 10, 4, 4, 4],
    "EMG RMS x4 plus IMU": [2, 2, 2, 2, 4, 4, 4, 4, 4, 4],
}

# Binary files are made of frames encoded like
# \n<stamp>/<size>\n<data>
# stamp is the ASCII encoded frame reception timestamp in nanoseconds
# size is the ASCII encoded size of the frame in bytes
# data is binary data
#
# ie.
# 203201612622753/352
# ...
# 203201624637544/352
# ...

def read_frame(file):
    empty = file.readline()
    if empty != b'\n':
        return None, None
    header = file.readline().decode("ascii")
    stamp, size = header.split('/')
    stamp = float(stamp) / 1e9
    size = int(size)
    data = file.read(size)
    return stamp, data

# Binary data consists of packed double values
# of one or more channels
# ie. in EMG RMS+ACC mode, the frame will contain
# - 10 samples of the EMG RMS channel
# - 4 samples of the ACC X channel
# - 4 samples of the ACC Y channel
# - 4 samples of the ACC Z channel
#
# This pattern is repeated for each devices in the capture

def unpack_channels(data, frame_layout):
    channels = [[] for _ in range(len(frame_layout))]
    total_size = sum(frame_layout)
    data = struct.unpack('>{}d'.format(total_size), data)
    offset = 0
    for i, _ in enumerate(channels):
        size = frame_layout[i]
        channels[i] += data[offset:offset+size]
        offset += size
    return channels

# Read channels contained in a raw data file
# returns a list of the timestamps of each received frames and
# a list of values for each channel
# If no frame layout is specifier, the first compatible with the
# data frame size will be used.

def read(file, frame_layout=None):
    timestamps = []
    channels = None
    is_first_frame = True
    while True:
        stamp, data = read_frame(file)
        if stamp is None:
            return timestamps, channels
        if is_first_frame:
            # Skip first frame that might have been used to store metadata
            is_first_frame = False
            continue
        timestamps.append(stamp)
        if frame_layout is None:
            mode, frame_layout = guess_mode(len(data))
            if frame_layout is None:
                sys.exit("Unable to find mode for data frame of size {}".format(
                    len(data)))
            else:
                print("No mode was specified, assuming", mode,
                      "with layout", " ".join([str(i) for i in frame_layout]))
        if channels is None:
            channels = [[] for _ in range(len(frame_layout))]
        frame_channels = unpack_channels(data, frame_layout)
        for i, _ in enumerate(channels):
            channels[i] += frame_channels[i]

# No information about the file content in the metadata
# Guessing it for now
def guess_mode(data_size):
    for mode, layout in FRAME_LAYOUTS.items():
        if sum(layout) * 8 == data_size:
            return mode, layout
    return None, None

if __name__ == '__main__':
    import argparse

    parser = argparse.ArgumentParser()
    parser.add_argument("data", help="Path of the file containing the logged data (.dat)", type=str)
    parser.add_argument("--frame-layout",
                        help=("Layout of the raw data frames, "
                              "consisting of concatenated samples of each channels"),
                        nargs='+', default=None, type=int)
    parser.add_argument("--plot",
                        help="Plots channels form data",
                        action='store_true')
    parser.add_argument("--export-csv",
                        help="Export channels as csv files",
                        action='store_true')

    args = parser.parse_args()

    with open(args.data, 'rb') as f:
        timestamps, channels = read(f, args.frame_layout)

    if args.plot:
        import matplotlib.pyplot as plt
        import numpy as np

        fig = plt.figure()
        frame_intervals = np.diff(np.array(timestamps))
        plt.plot(range(len(frame_intervals)), frame_intervals)

        fig = plt.figure()
        for i, channel in enumerate(channels):
            ax = fig.add_subplot(len(channels), 1, i+1)
            ax.plot(range(len(channel)), channel)

        plt.show()

    if args.export_csv:
        import csv

        for i, channel in enumerate(channels):
            with open(args.data + '.channel' + str(i) + '.csv', 'w') as csvfile:
                writer = csv.writer(csvfile)
                for sample in channel:
                    writer.writerow([sample])

        with open(args.data + '.stamps.csv', 'w') as csvfile:
            writer = csv.writer(csvfile)
            for stamp in timestamps:
                writer.writerow([stamp])
