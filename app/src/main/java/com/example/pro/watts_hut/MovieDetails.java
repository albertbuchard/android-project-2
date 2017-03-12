package com.example.pro.watts_hut;

import android.app.ActionBar;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.TextViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import org.json.JSONException;


public class MovieDetails extends AppCompatActivity {
    MainActivity.MovieObject movie;
    TextView movie_title;
    TextView release_date;
    TextView ratings;
    TextView synopsis;
    ImageView movieThumbnail;

    int voteAverage = 0;
    int voteCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.movie_detail);

        // Get the intent and the parcelable MovieObject to populate the fields.
        // I could have just passed the JSON string, but I wanted to learn parcelables
        Bundle data = getIntent().getExtras();
        movie = (MainActivity.MovieObject) data.getParcelable("movie");

        // Store the fields
        movie_title = (TextView) findViewById(R.id.movie_title);
        release_date = (TextView) findViewById(R.id.release_date);
        ratings = (TextView) findViewById(R.id.ratings);
        synopsis = (TextView) findViewById(R.id.synopsis);

        movieThumbnail = (ImageView) findViewById(R.id.detail_thumbnail);

        // Load the thumbnail, should be cached by picasso and load instantly
        Picasso.with(this)
                .load(movie.apiFormattedUrl)
                .into(movieThumbnail);

        // Set the fields from the JSON
        try {
            movie_title.setText(movie.data.getString("original_title"));
            release_date.setText(movie.data.getString("release_date"));
            voteAverage = movie.data.getInt("vote_average");
            voteCount = movie.data.getInt("vote_count");

            String ratingString = new String(new char[(int) Math.floor(voteAverage*5/10)]).replace("\0", "* ")
                    + "\n" + String.valueOf(voteCount) + " votes";

            ratings.setText(ratingString);
            synopsis.setText(movie.data.getString("overview"));
        } catch (JSONException e) {
            e.printStackTrace();
        }

//        android.support.v7.app.ActionBar ab = getSupportActionBar();
//        ab.setTitle("My Title");
    }

}
