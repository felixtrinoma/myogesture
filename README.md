# Myo Gesture

Gesture recognition library for Android.
Work in progress…

# Setup

Licence files are needed to access Delsys API. Before using the app, you need to place yours in the app assets:

- `MyoGesture/app/src/main/assets/License.lic`
- `MyoGesture/app/src/main/assets/PublicKey.lic`

Check that your bluetooth is enabled. You might need to restart the app after granting the permissions.

# Logging data

The train mode logs every data it acquires to the sdcard. Those logs should be saved in a location like `/storage/0123-4567/Android/data/fr.trinoma.myogesture/files/`.

When the capture is done, you can labelize gesture samples by taping on the chart. Those labels are saved in text file with the data log.

