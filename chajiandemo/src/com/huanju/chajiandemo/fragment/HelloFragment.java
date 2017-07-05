package com.huanju.chajiandemo.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.huanju.chajiandemo.R;

/**
 * Created by jiangjingbo on 2017/6/27.
 */

public class HelloFragment extends Fragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_hello, null);  // View android.view.LayoutInflater.inflate(int resource, ViewGroup root)
        return view;
    }

    @Override
    public void onPause() {
        super.onPause();
    }
}
