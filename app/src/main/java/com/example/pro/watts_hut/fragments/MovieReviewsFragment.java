package com.example.pro.watts_hut.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.pro.watts_hut.R;
import com.example.pro.watts_hut.data.ReviewObject;
import com.example.pro.watts_hut.adapters.ReviewRvAdapter;

import java.util.ArrayList;

public class MovieReviewsFragment extends Fragment {

    private static final String TAG = "MovieReviews";
    private ArrayList<ReviewObject> reviews;
    private Context mContext;
    private String movieDbId;

    // Recycler view
    private ReviewRvAdapter internalReviewAdapter;
    private RecyclerView recyclerView;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        reviews = getArguments().getParcelableArrayList("reviews");
        movieDbId = getArguments().getString("movieDbId");
        return inflater.inflate(R.layout.activity_movie_reviews, null);
    }

    @Override
    public void onStart() {
        super.onStart();
        fillData();
    }

    protected void fillData() {

        Bundle data = getArguments();
        reviews = data.getParcelableArrayList("reviews");

        Log.i(TAG, "onCreate: " + String.valueOf(reviews.size()));

        mContext = getActivity();

        // Setup recycler view
        recyclerView = (RecyclerView) getView().findViewById(R.id.rv_review_grid);

        // Set GridLayoutManager to the recyclerview, with two columns
        //GridLayoutManager layoutManager = new GridLayoutManager(this,2);
        //recyclerView.setLayoutManager(layoutManager);
            recyclerView.setLayoutManager(new GridLayoutManager(mContext, 1));

        // Set the adapter to the recycler view
        internalReviewAdapter = new ReviewRvAdapter(mContext, recyclerView);
        internalReviewAdapter.setOnLoadMoreListener(new ReviewRvAdapter.OnLoadMoreListener() {
            @Override
            public void onLoadMore() {

            }
        });
        recyclerView.setAdapter(internalReviewAdapter);

        internalReviewAdapter.addReviews(reviews);


    }

    private void loadReviews() {
    }
}
