package fr.trinoma.myogesture.ui.detect;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class DetectViewModel extends ViewModel {

    private MutableLiveData<String> mText;

    public DetectViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("Detection not implemented yet");
    }

    public LiveData<String> getText() {
        return mText;
    }
}