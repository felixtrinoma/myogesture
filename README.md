# Myo Gesture

Gesture recognition library for Android.
Work in progress…

## Setup

Licence files are needed to access Delsys API. Before using the app, you need to place yours in the app assets:

- `MyoGesture/app/src/main/assets/License.lic`
- `MyoGesture/app/src/main/assets/PublicKey.lic`

Check that your Bluetooth is enabled. You might need to restart the app after granting the permissions.

## Training

This part is still experimental, done on a computer for now.

### Create a Python3 environment with Tensorflow

```bash
python3 -m venv myogesture-py3
source myogesture-py3/bin/activate
pip install numpy matplotlib scipy tensorflow
```

### Get your data

Record 2 EMG signals in the train mode, including about 20 times each gesture and periods with noise (nothing, or other gestures that should not be detected).

Labelize those gestures when the data have been recorded by taping on the chart, selecting the correct label and saving. Label noise like gestures (with the "Unknown" label for instance).

The train mode logs every data it acquires to the sdcard. Those logs should be saved in a location like: `/sdcard/Android/data/fr.trinoma.myogesture/files/` or `/storage/0123-4567/Android/data/fr.trinoma.myogesture/files/`.

```bash
adb pull /storage/0123-4567/Android/data/fr.trinoma.myogesture/files/
```

### Run the train script

```bash
python ML/train-model.py files/2019-11-12-13-37-14.dat files /2019-11-12-13-37-14.labels.txt
```

The first graph shows the labeled data samples. Their values should lie between 0.0 and 1.0. If those samples looks hard to distinguish, the model might not be able to classify them correctly:

![alt text](https://cyril.lugan.fr/assets/stash/2019-11-13-myogesture-samples.png)

The following plot allows to monitor the model training:

![alt text](https://cyril.lugan.fr/assets/stash/2019-11-13-myogesture-deeptraining.png)

The test "Model accuracy" should tend toward 1.0, which indicates that all the validation data have been classified correctly. If the difference between the train and test "Model loss" increases over time, the network is overfitting the training data. You can try to add dropout layers or remove the hidden layers.

### Put the model in your app assets

The script has generated a `.tflite` file, copy to the assets of the app ` MyoGesture/app/src/main/assets/model.tflite`. Recompile the app and run it, the detection mode, when used with two sensors should display the currently detected gesture.