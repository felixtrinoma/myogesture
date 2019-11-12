package fr.trinoma.myogesture.ui.train;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class TrainViewModel extends ViewModel {

    private MutableLiveData<RecordingState> state;

    public TrainViewModel() {
        state = new MutableLiveData<>();
    }

    public LiveData<RecordingState> getState() {
        return state;
    }

    public void postState(RecordingState newState) {
        state.postValue(newState);
    }
}