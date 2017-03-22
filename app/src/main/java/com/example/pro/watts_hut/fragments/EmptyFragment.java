package com.example.pro.watts_hut.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.pro.watts_hut.R;

/**
 * Created by Pro on 21.03.17.
 */

public class EmptyFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.empty, container, false);
    }

    @Override
    public void onStart() {
        super.onStart();
    }
}
