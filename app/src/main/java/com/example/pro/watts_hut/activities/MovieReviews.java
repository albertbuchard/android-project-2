package com.example.pro.watts_hut.activities;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;

import com.example.pro.watts_hut.R;
import com.example.pro.watts_hut.adapters.ReviewRvAdapter;
import com.example.pro.watts_hut.data.ReviewObject;

import java.util.ArrayList;

public class MovieReviews extends AppCompatActivity {

    private static final String TAG = "MovieReviews";
    private ArrayList<ReviewObject> reviews;
    private String movieDbId;
    private Context mContext;

    // Recycler view
    private ReviewRvAdapter internalReviewAdapter;
    private RecyclerView recyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_movie_reviews);

        Bundle data = getIntent().getExtras();
        reviews = data.getParcelableArrayList("reviews");

        Log.i(TAG, "onCreate: " + String.valueOf(reviews.size()));

        mContext = getApplicationContext();

        // Setup recycler view
        recyclerView = (RecyclerView) findViewById(R.id.rv_review_grid);

        // Set GridLayoutManager to the recyclerview, with two columns
        //GridLayoutManager layoutManager = new GridLayoutManager(this,2);
        //recyclerView.setLayoutManager(layoutManager);
            recyclerView.setLayoutManager(new GridLayoutManager(mContext, 1));

        // Set the adapter to the recycler view
        internalReviewAdapter = new ReviewRvAdapter(this, recyclerView);
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
