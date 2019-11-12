package fr.trinoma.myogesture.ui.detect;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import fr.trinoma.myogesture.ui.train.RecordingState;

public class DetectViewModel extends ViewModel {

    private MutableLiveData<String> detectedGesture;

    public DetectViewModel() {
        detectedGesture = new MutableLiveData<>();
        state = new MutableLiveData<>();
    }

    public LiveData<String> getDetectedGesture() {
        return detectedGesture;
    }

    public void postDetectedGesture(String gesture) {
        detectedGesture.postValue(gesture);
    }

    private MutableLiveData<DetectionState> state;

    public LiveData<DetectionState> getState() {
        return state;
    }

    public void postState(DetectionState newState) {
        state.postValue(newState);

    }
}