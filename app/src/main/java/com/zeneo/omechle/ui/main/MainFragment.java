package com.zeneo.omechle.ui.main;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.zeneo.omechle.R;

public class MainFragment extends Fragment {

    private ChipGroup chipGroup;
    private Chip chip;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_main, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        chipGroup = view.findViewById(R.id.chipGroup);
        chipGroup.addView(getChip("New"));

        view.findViewById(R.id.text_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NavController navController = NavHostFragment.findNavController(MainFragment.this);
                navController.navigate(R.id.action_mainFragment_to_textFragment);
            }
        });
        view.findViewById(R.id.video_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NavController navController = NavHostFragment.findNavController(MainFragment.this);
                navController.navigate(R.id.action_mainFragment_to_videoFragment);
            }
        });
    }

    private Chip getChip(String text){
        Chip chip = new Chip(getActivity());
        chip.setTextColor(getResources().getColor(R.color.colorAccent));
        chip.setCheckable(false);
        chip.setCloseIconTintResource(R.color.colorAccent);
        chip.setCloseIcon(getResources().getDrawable(R.drawable.ic_baseline_close_24));
        chip.setChipBackgroundColorResource(R.color.colorPrimary);
        chip.setChipStrokeWidth(1f);
        chip.setChipStrokeColorResource(R.color.colorAccent);
        chip.setText(text);
        return chip;
    }
}