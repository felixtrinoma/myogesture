package fr.trinoma.myogesture.ui.detect;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import fr.trinoma.myogesture.R;

public class DetectFragment extends Fragment {

    private DetectViewModel detectViewModel;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        detectViewModel =
                ViewModelProviders.of(this).get(DetectViewModel.class);
        View root = inflater.inflate(R.layout.fragment_detect, container, false);
        final TextView textView = root.findViewById(R.id.text_notifications);
        detectViewModel.getText().observe(this, new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {
                textView.setText(s);
            }
        });
        return root;
    }
}