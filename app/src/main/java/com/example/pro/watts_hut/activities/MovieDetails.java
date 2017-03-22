package com.example.pro.watts_hut.activities;

import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.example.pro.watts_hut.R;
import com.example.pro.watts_hut.data.FavoriteMoviesContract.favoriteMovies;
import com.example.pro.watts_hut.data.FavoriteMoviesHelper;
import com.example.pro.watts_hut.data.MovieObject;
import com.example.pro.watts_hut.data.ReviewObject;
import com.example.pro.watts_hut.databinding.MovieDetailBinding;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import static com.example.pro.watts_hut.data.FavoriteMoviesContract.favoriteMovies.*;


public class MovieDetails extends AppCompatActivity {
    String TAG = "MovieDetails";
    MovieObject movie;
    private String originalTitle;

    MovieDetailBinding binding;

    SQLiteDatabase mDb;

    int voteAverage = 0;
    int voteCount = 0;

    String movieDbId = null;
    boolean isFavorite = false;
    private String releaseDate;
    private String synopsis;
    boolean hasTrailer = false;
    private Context mContext;
    private Toast internalToast;
    private String[] mVideoUrls = null;
    private String mTrailerUrl = null;
    private ArrayList<ReviewObject> reviewsList = new ArrayList<ReviewObject>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.movie_detail);
        binding.watchTrailer.setVisibility(View.GONE);
        updateReviews();


        mContext = getApplicationContext();

        // Get the intent and the parcelable MovieObject to populate the fields.
        // I could have just passed the JSON string, but I wanted to learn parcelables
        Bundle data = getIntent().getExtras();
        movie = (MovieObject) data.getParcelable("movie");

        // Get the db
        FavoriteMoviesHelper helper = FavoriteMoviesHelper.getHelper(this);
        mDb = helper.getWritableDatabase();

        // Load the thumbnail, should be cached by picasso and load instantly
        Picasso.with(this)
                .load(movie.apiFormattedUrl)
                .into(binding.detailThumbnail);


        // Set the fields from the JSON
        try {
            originalTitle = movie.data.getString("original_title");
            releaseDate = movie.data.getString("release_date");
            synopsis = movie.data.getString("overview");
            movieDbId = movie.data.getString("id");

            binding.movieTitle.setText(originalTitle);
            binding.releaseDate.setText(releaseDate);

            voteAverage = movie.data.getInt("vote_average");
            voteCount = movie.data.getInt("vote_count");

            String ratingString = new String(new char[(int) Math.floor(voteAverage * 5 / 10)]).replace("\0", "* ")
                    + "\n" + String.valueOf(voteCount) + " votes";

            binding.ratings.setText(ratingString);
            binding.synopsis.setText(synopsis);

            loadDetails();



            // check if the movie is in the database and get its favorite status if it is
            String[] projection = {favoriteMovies.COLUMN_IS_FAVORITE};
            String selection = favoriteMovies.COLUMN_MOVIE_TITLE + " = ?";
            String[] selectionArgs = {originalTitle};

            Cursor cursor = mDb.query(
                    favoriteMovies.TABLE_NAME,                     // The table to query
                    projection,                               // The columns to return
                    selection,                                // The columns for the WHERE clause
                    selectionArgs,                            // The values for the WHERE clause
                    null,                                     // don't group the rows
                    null,                                     // don't filter by row groups
                    null                                 // The sort order
            );

            if (cursor.getCount() > 0) {
                cursor.moveToNext();
                isFavorite = (cursor.getInt(cursor.getColumnIndexOrThrow(favoriteMovies.COLUMN_IS_FAVORITE)) == 1); // getInt seems to be the way to go for bools
                boast(Toast.makeText(this, "Is favorite" + String.valueOf(isFavorite), Toast.LENGTH_SHORT));
                setFavoriteSrc();
            } else {
                isFavorite = false;
                boast(Toast.makeText(this, "Not found in db", Toast.LENGTH_SHORT));
                setFavoriteSrc();
            }
            cursor.close();



        } catch (JSONException e) {
            e.printStackTrace();
        }

//        android.support.v7.app.ActionBar ab = getSupportActionBar();
//        ab.setTitle("My Title");
    }

    /**
     * Click action on the trailer button generates an ACTION_VIEW intent for the video
     *
     * @param view
     */
    public void goToTrailer(View view) {
        if (mTrailerUrl != null) {
            Log.i(TAG, "goToTrailer: " + mTrailerUrl);
            Intent appIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(mTrailerUrl));
            Intent webIntent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse(mTrailerUrl));
            try {
                startActivity(appIntent);
            } catch (ActivityNotFoundException ex) {
                startActivity(webIntent);
            }
        }
    }

    /**
     * Click action on the review button. Generates an intent with review data and call the MovieReview
     * activity.
     *
     * @param view
     */
    public void goToReviews(View view) {
        // Create intent and add a Parcelable MovieObject in the extras
        Intent intentDetail = new Intent(mContext, MovieReviews.class);
        intentDetail.putParcelableArrayListExtra("reviews", (ArrayList<ReviewObject>) reviewsList);
        intentDetail.putExtra("movieDbId", movieDbId);
        // Start detail activity
        mContext.startActivity(intentDetail);
    }

    /**
     * Switches the isFavorite state variable and update the Database
     * @param view
     * @throws Exception
     */
    public void switchFavorite(View view) throws Exception {
        boolean isFavoriteTemp = !isFavorite;

        // DB update / insert

        // Check if the favorite movie is in the database already
        String[] projection = {
                _ID,
                COLUMN_MOVIE_TITLE,
                COLUMN_IS_FAVORITE
        };

        // Filter results WHERE "title" = 'My Title'
        String selection = COLUMN_MOVIE_TITLE + " = ?";
        String[] selectionArgs = {originalTitle}; // TODO use match string LIKE %string%

        Cursor cursor = mDb.query(
                TABLE_NAME,                     // The table to query
                projection,                               // The columns to return
                selection,                                // The columns for the WHERE clause
                selectionArgs,                            // The values for the WHERE clause
                null,                                     // don't group the rows
                null,                                     // don't filter by row groups
                null                                 // The sort order
        );

        if (cursor.getCount() > 0) {
            // Update
            cursor.moveToNext();
            long mId = cursor.getLong(cursor.getColumnIndexOrThrow(_ID));
            cursor.close();

            // New value for one column
            ContentValues values = new ContentValues();
            values.put(COLUMN_IS_FAVORITE, isFavoriteTemp);

            // Which row to update, based on the title
            selection = _ID + " = ?";
            selectionArgs = new String[]{String.valueOf(mId)};

            int count = mDb.update(
                    TABLE_NAME,
                    values,
                    selection,
                    selectionArgs);

            // Catch error
            if (count != 1) {
                // TODO look for a android specific error
                throw new Exception("switchFavorite: could not update the database appropriately. " + String.valueOf(count) + " columns updated");
            } else {
                isFavorite = isFavoriteTemp;
                setFavoriteSrc();
            }
        } else {
            // Insert new movie in the DB
            ContentValues values = new ContentValues();
            values.put(COLUMN_MOVIE_DB_ID, movieDbId);
            values.put(COLUMN_MOVIE_TITLE, originalTitle);
            values.put(COLUMN_USER_ID, Settings.Secure.ANDROID_ID);
            values.put(COLUMN_IS_FAVORITE, (isFavoriteTemp ? "1" : "0"));
            values.put(COLUMN_RELEASE_DATE, releaseDate);
            values.put(COLUMN_RATINGS, voteAverage);
            values.put(COLUMN_VOTE_COUNT, voteCount);
            values.put(COLUMN_THUMBNAIL, movie.apiFormattedUrl);
            values.put(COLUMN_SYNOPSIS, synopsis);

            // Insert the new row, returning the primary key value of the new row
            long newRowId = mDb.insert(TABLE_NAME, null, values);

            if (newRowId == -1) {
                throw new Exception("switchFavorite: could not insert a new row in the database.");
            } else {
                isFavorite = isFavoriteTemp;
                setFavoriteSrc();
            }
        }

    }

    /**
     * Changes the favorite icon based on isFavorite value
     */
    public void setFavoriteSrc() {
        if (isFavorite) {
            binding.starButton.setImageResource(R.drawable.ic_star);
        } else {
            binding.starButton.setImageResource(R.drawable.ic_star_outline);
        }
    }

    /**
     * Changes the visibility of the review button based on w
     */
    private void updateReviews() {
        if (reviewsList.size() > 0) {
            binding.reviewButton.setVisibility(View.VISIBLE);
        } else {
            binding.reviewButton.setVisibility(View.GONE);
        }
    }

    /**
     * Sets the visibility of the trailer button based on whether a trailer was found in the data.
     * Sets the mTrailerUrl and mVideoUrls.
     * @param videoUrls
     */
    private void updateVideoView(String[] videoUrls) {

        if (videoUrls.length > 0) {
            // set the url field
            mTrailerUrl = videoUrls[0];
            mVideoUrls = videoUrls;

            // Show the trailer button
            binding.watchTrailer.setVisibility(View.VISIBLE);
            binding.watchTrailer.setText("Trailer");

            // pick the first video as a trailer
            //boast(Toast.makeText(this, mTrailerUrl, Toast.LENGTH_SHORT));

        } else {
            // hide the button
            binding.watchTrailer.setVisibility(View.GONE);
        }
    }


    /* ==== Async task ==== */

    /**
     * Sets up the URI for the API call, and call the async task to load the data.
     */
    public void loadDetails() {

        if (movieDbId == null) {
            return;
        }

        String apiKey = getString(R.string.movie_api_key);
        String baseUrl = String.format(mContext.getString(R.string.movie_api_detail), movieDbId);
        String reviewUrl = String.format(mContext.getString(R.string.movie_api_reviews), movieDbId);

        Uri builtURI = Uri.parse(baseUrl).buildUpon()
                .appendQueryParameter(getString(R.string.movie_api_param_key), apiKey)
                .build();

        URL url = null;

        Uri reviewUri = Uri.parse(reviewUrl).buildUpon()
                .appendQueryParameter(getString(R.string.movie_api_param_key), apiKey)
                .build();

        URL urlReviews = null;
        try {
            url = new URL(builtURI.toString());
            urlReviews = new URL(reviewUri.toString());

            Log.d(TAG, "Movie Details: " + urlReviews);

            // The URL is built, calls the async task
            new MovieDetails.LoadDetailTask().execute(url);
            new MovieDetails.LoadReviewsTask().execute(urlReviews);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

    }


    /**
     * Loads the movie information in the background to extract the videos
     */
    class LoadDetailTask extends AsyncTask<URL, Void, JSONObject> {

        @Override
        protected JSONObject doInBackground(URL... urls) {
            URL url = urls[0];
            JSONObject movieDetail = null;

            try {
                // Create connection to the API
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.setDoOutput(false);
                urlConnection.setDoInput(true);
                urlConnection.setUseCaches(true);
                urlConnection.connect();
                urlConnection.getErrorStream();

                try {
                    // Start reading
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream(), "utf-8"),8);
                    StringBuilder stringBuilder = new StringBuilder();
                    String line;
                    while ((line = bufferedReader.readLine()) != null) {
                        stringBuilder.append(line).append("\n");
                    }
                    bufferedReader.close();
                    Log.i(TAG, "doInBackground:" + stringBuilder.toString());
                    // Get JSONObject from read string
                    movieDetail = new JSONObject(stringBuilder.toString());

                    if (movieDetail.has("status_code")) {
                        return movieDetail;
                    }


                    // We only return the movieList to be added to the adapter
                    return movieDetail;
                } catch (JSONException e) {
                    e.printStackTrace();
                } finally {
                    urlConnection.disconnect();
                }

            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(JSONObject movieDetail) {
            if ((movieDetail == null) || (movieDetail.has("status_code"))) {
                Log.i("INFO", "THERE WAS AN ERROR");
            } else {
                try {
                    // Get the videos
                    JSONObject videos = movieDetail.getJSONObject("videos");
                    JSONArray videosResults = videos.getJSONArray("results");


                    String[] videoYoutubeIds = new String[videosResults.length()];

                    // Start looping over the results to create the movie list and preload thumbnails
                    for (int n = 0; n < videosResults.length(); n++) {
                        JSONObject video = videosResults.getJSONObject(n);
                        String id = video.getString("key");
                        videoYoutubeIds[n] = String.format(mContext.getString(R.string.youtube_video_url), id);


                        Log.i(TAG, "doInBackground: " + videoYoutubeIds[n]);
                    }

                    updateVideoView(videoYoutubeIds);

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

        }
    }

    /**
     * Loads the reviews in the background from the API
     */
    class LoadReviewsTask extends AsyncTask<URL, Void, JSONObject> {

        @Override
        protected JSONObject doInBackground(URL... urls) {
            URL url = urls[0];
            JSONObject movieDetail = null;

            try {
                // Create connection to the API
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.setDoOutput(false);
                urlConnection.setDoInput(true);
                urlConnection.setUseCaches(true);
                urlConnection.connect();
                urlConnection.getErrorStream();

                try {
                    // Start reading
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream(), "utf-8"),8);
                    StringBuilder stringBuilder = new StringBuilder();
                    String line;
                    while ((line = bufferedReader.readLine()) != null) {
                        stringBuilder.append(line).append("\n");
                    }
                    bufferedReader.close();
                    Log.i(TAG, "doInBackground:" + stringBuilder.toString());
                    // Get JSONObject from read string
                    movieDetail = new JSONObject(stringBuilder.toString());

                    if (movieDetail.has("status_code")) {
                        return movieDetail;
                    }


                    // We only return the movieList to be added to the adapter
                    return movieDetail;
                } catch (JSONException e) {
                    e.printStackTrace();
                } finally {
                    urlConnection.disconnect();
                }

            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(JSONObject movieReviews) {
            if ((movieReviews == null) || (movieReviews.has("status_code"))) {
                Log.i("INFO", "THERE WAS AN ERROR");
            } else {
                try {
                    // Get the videos
                    int totalResults = movieReviews.getInt("total_results");
                    if (totalResults>0) {
                        JSONArray results = movieReviews.getJSONArray("results");




                        // Start looping over the results to create the movie list and preload thumbnails
                        for (int n = 0; n < results.length(); n++) {
                            JSONObject reviewJson = results.getJSONObject(n);
                            ReviewObject review = new ReviewObject(reviewJson);
                            reviewsList.add(review);


                            Log.i(TAG, "doInBackground.LoadReviewTask.onPost: " + review.author);
                        }

                        updateReviews();
                    }


                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

        }
    }


    /* ==== Utilities ==== */
    /**
     * Allows to use only one toast
     *
     * @param toast
     */
    public void boast(Toast toast) {
        // Toast helper function
        if (internalToast != null) {
            internalToast.cancel();
        }

        internalToast = toast;
        internalToast.show();
    }
}
