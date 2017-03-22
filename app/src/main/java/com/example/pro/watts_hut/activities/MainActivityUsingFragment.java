package com.example.pro.watts_hut.activities;


import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.example.pro.watts_hut.R;
import com.example.pro.watts_hut.adapters.MovieRvAdapter;
import com.example.pro.watts_hut.data.FavoriteMoviesContract;
import com.example.pro.watts_hut.data.FavoriteMoviesHelper;
import com.example.pro.watts_hut.data.MovieObject;
import com.example.pro.watts_hut.data.ReviewObject;
import com.example.pro.watts_hut.fragments.EmptyFragment;
import com.example.pro.watts_hut.fragments.MainFragment;
import com.example.pro.watts_hut.fragments.MovieDetailsFragment;
import com.example.pro.watts_hut.fragments.MovieReviewsFragment;
import com.squareup.picasso.Picasso;

import org.apache.commons.lang3.ArrayUtils;
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
import java.util.HashSet;
import java.util.List;

public class MainActivityUsingFragment extends AppCompatActivity implements MainFragment.OnItemSelectedListener {
    private static final String TAG = "MainActivityFragment";

    private MovieRvAdapter internalMovieAdapter;

    private Context mContext;
    private MainFragment mainFragment;
    private MovieDetailsFragment displayFrag;

    final private int SORTING_POPULAR = R.string.movie_api_popular;
    final private int SORTING_TOP_RATED = R.string.movie_api_top_rate;
    private String CACHED_TOP_RATED = "cachedDatasetToprated";
    private String CACHED_POPULARE = "cachedDatasetPopular";
    private String CACHED_FAVORITE = "cachedFavorites";
    private String CURRENT_PAGE = "currentPage";
    private String CURRENT_PAGE_POPULAR = "currentPagePopular";
    private String CURRENT_PAGE_TOP_RATED = "currentPageTopRated";
    private String CURRENT_SORTING = "currentSorting";
    private int currentSorting = R.string.movie_api_top_rate;

    // Sorting fields and cached dataset for the recycler view
    private List<MovieObject> cachedDatasetPopular = new ArrayList<MovieObject>();
    private List<MovieObject> cachedDatasetToprated = new ArrayList<MovieObject>();
    private List<MovieObject> cachedFavorites = new ArrayList<MovieObject>();

    // Keeps a separate page count for the two API endpoints
    private int currentPage; // Used in LoadMovies()
    private int currentPagePopular = 0;
    private int currentPageTopRated = 0;
    private SQLiteDatabase mDb;
    private Toast internalToast;
    private int loadedSorting;
    private boolean isSeeingFavorites = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mContext = getApplicationContext();


        mainFragment = new MainFragment();
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.list_fragment, mainFragment)
                .commit();

        mainFragment.setOnItemSelectedListener(this);


        View displayView = (View) findViewById(R.id.detail_fragment);

        if (displayView != null) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

            fragmentManager.beginTransaction()
                    .add(R.id.detail_fragment, new EmptyFragment())
                    .commit();
        }

        // access the dababase
        FavoriteMoviesHelper mDbHelper = FavoriteMoviesHelper.getHelper(mContext);
        mDb = mDbHelper.getWritableDatabase();

        if (savedInstanceState != null) {
            cachedDatasetToprated = savedInstanceState.getParcelableArrayList(CACHED_TOP_RATED);
            cachedDatasetPopular = savedInstanceState.getParcelableArrayList(CACHED_POPULARE);
            cachedFavorites = savedInstanceState.getParcelableArrayList(CACHED_FAVORITE);
            currentPage = savedInstanceState.getInt(CURRENT_PAGE);
            currentPagePopular = savedInstanceState.getInt(CURRENT_PAGE_POPULAR);
            currentPageTopRated = savedInstanceState.getInt(CURRENT_PAGE_TOP_RATED);
            currentSorting = savedInstanceState.getInt(CURRENT_SORTING);


        }

        refreshFavorites();

    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        // Save the user's current game state
        savedInstanceState.putParcelableArrayList(CACHED_POPULARE, (ArrayList<? extends Parcelable>) cachedDatasetPopular);
        savedInstanceState.putParcelableArrayList(CACHED_TOP_RATED, (ArrayList<? extends Parcelable>) cachedDatasetToprated);
        savedInstanceState.putParcelableArrayList(CACHED_FAVORITE, (ArrayList<? extends Parcelable>) cachedFavorites);
        savedInstanceState.putInt(CURRENT_PAGE, currentPage);
        savedInstanceState.putInt(CURRENT_PAGE_POPULAR, currentPagePopular);
        savedInstanceState.putInt(CURRENT_PAGE_TOP_RATED, currentPageTopRated);
        savedInstanceState.putInt(CURRENT_SORTING, currentSorting);

        // Always call the superclass so it can save the view hierarchy state
        super.onSaveInstanceState(savedInstanceState);
    }

    public void refreshList() {
        if (mainFragment != null) {
            List<MovieObject> data = cachedFavorites;

            // We add the cached movies to the adapter
            if (!isSeeingFavorites) {
                if (currentSorting == SORTING_POPULAR) {
                    data = cachedDatasetPopular;
                } else {
                    data = cachedDatasetToprated;
                }
            }

            if (data.size() == 0) {
                loadMovies();
            } else {
                mainFragment.internalMovieAdapter.swapDataset(data);
            }


        }
    }


    public void refreshFavorites() {
        Cursor cursor = getAllFavorite();
        Log.i(TAG, "onCreate: " + cursor.getCount());

        cachedFavorites = new ArrayList<MovieObject>();
        while (cursor.moveToNext()) {
            cachedFavorites.add(new MovieObject(mContext, cursor));
        }

        if (isSeeingFavorites) {
            refreshList();
        }
    }


    private Cursor getAllFavorite () {
        String[] projection = {
                FavoriteMoviesContract.favoriteMovies._ID,
                FavoriteMoviesContract.favoriteMovies.COLUMN_MOVIE_DB_ID,
                FavoriteMoviesContract.favoriteMovies.COLUMN_RELEASE_DATE,
                FavoriteMoviesContract.favoriteMovies.COLUMN_MOVIE_TITLE,
                FavoriteMoviesContract.favoriteMovies.COLUMN_THUMBNAIL,
                FavoriteMoviesContract.favoriteMovies.COLUMN_IS_FAVORITE,
                FavoriteMoviesContract.favoriteMovies.COLUMN_RATINGS,
                FavoriteMoviesContract.favoriteMovies.COLUMN_VOTE_COUNT,
                FavoriteMoviesContract.favoriteMovies.COLUMN_SYNOPSIS,

        };

        // Filter results WHERE "title" = 'My Title'
        String selection = FavoriteMoviesContract.favoriteMovies.COLUMN_IS_FAVORITE + " = ?";
        String[] selectionArgs = { "1" };

        // How you want the results sorted in the resulting Cursor
        String sortOrder =
                FavoriteMoviesContract.favoriteMovies.COLUMN_DATE_ADDED + " DESC";

        return mDb.query(FavoriteMoviesContract.favoriteMovies.TABLE_NAME,
                projection,
                selection,
                selectionArgs,
                null,
                null,
                sortOrder);
    }


    public void showFavorites() {
        if (cachedFavorites.size() > 0) {
            isSeeingFavorites = true;
            if (currentSorting == SORTING_POPULAR) {
                currentPagePopular = currentPage;
            } else {
                currentPageTopRated = currentPage;
            }

            refreshList();

            String message = "Favorites";
            boast(Toast.makeText(this, message, Toast.LENGTH_SHORT));

        }
    }



    /**
     * Sets up the URI for the API call, and call the async task to load the data.
     */
    public void loadMovies() {
        // Only load if does not look at his favorite TODO might be better to load also
        if (isSeeingFavorites) {
            return;
        }

        // Get sorting options and api key to build request url
        String[] sortingArray = getResources().getStringArray(R.array.movie_api_sorting);
        String apiKey = getString(R.string.movie_api_key);

        // Keeps track of the page it is currently loading (for each type of sorting)
        currentPage += 1;

        Uri builtURI = Uri.parse(getString(currentSorting)).buildUpon()
                .appendQueryParameter(getString(R.string.movie_api_param_key), apiKey)
                .appendQueryParameter(getString(R.string.movie_api_param_sort), sortingArray[0])
                .appendQueryParameter(getString(R.string.movie_api_param_page), Integer.toString(currentPage))
                .build();

        URL url = null;
        try {
            url = new URL(builtURI.toString());
            Context context = mContext.getApplicationContext();
            String message = "URL:"+url;


            Log.d(TAG, String.valueOf(internalToast == null));
            boast(Toast.makeText(context, message, Toast.LENGTH_SHORT));

            Log.d(TAG, "loadMovies: "+url);

            // The URL is built, calls the async task
            loadedSorting = currentSorting;
            new loadDataTask().execute(url);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

    }

    /**
     * Loads the movies in the back thread, creates a HashSet of MovieObjects out of the JSON and
     * prefetch the thumbnails images. Returns the movieList that is to be added to the recycler view
     * in onPostExecute()
     *
     */
    class loadDataTask extends AsyncTask<URL, Void, HashSet<MovieObject>> {

        @Override
        protected HashSet<MovieObject> doInBackground(URL... urls) {
            URL url = urls[0];
            String[] imageUrls = new String[0];
            HashSet<MovieObject> movieList = new HashSet<MovieObject>();

            try {
                // Create connection to the API
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.setDoOutput(true);
                urlConnection.setDoInput(true);
                urlConnection.setUseCaches(true);
                urlConnection.connect();

                try {
                    // Start reading
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                    StringBuilder stringBuilder = new StringBuilder();
                    String line;
                    while ((line = bufferedReader.readLine()) != null) {
                        stringBuilder.append(line).append("\n");
                    }
                    bufferedReader.close();

                    // Get JSONObject from read string
                    JSONObject data = new JSONObject(stringBuilder.toString());

                    try {
                        JSONArray results = data.getJSONArray("results");
                        currentPage = data.getInt("page")+1;

                        // Declare an array of string of the size of the number of movies
                        imageUrls = new String[results.length()];

                        // Start looping over the results to create the movie list and preload thumbnails
                        for(int n = 0; n < results.length(); n++)
                        {
                            JSONObject movie = results.getJSONObject(n);


                            imageUrls[n] = movie.getString(getString(R.string.movie_api_results_image));

                            if (imageUrls[n] == "null")
                                continue;

                            // If thumbnail url is not null we create a movie object
                            // We exclude the other movies because the UI is mainly graphic through the thumbnail
                            MovieObject movieObject = new MovieObject(mContext, movie);
                            movieList.add(movieObject);

                            imageUrls[n] = String.format(getString(R.string.image_api_url), "w185", imageUrls[n]);

                            // We will try to preload and cache the images in the background thread for smoother scrolling
                            try {
                                Picasso.with(mContext)
                                        .load(imageUrls[n])
                                        .get();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                            Log.i(TAG, "doInBackground: "+imageUrls[n]);
                        }

                        imageUrls = ArrayUtils.removeAllOccurences(imageUrls, "null");

                        // We only return the movieList to be added to the adapter
                        return movieList;

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                } finally{
                    urlConnection.disconnect();
                }

            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(HashSet<MovieObject> movieList) {
            if((movieList == null)||(movieList.size() == 0)) {
                Log.i("INFO", "THERE WAS AN ERROR");
            } else {
                // We add the movies just loaded to the adapter
                if (loadedSorting == SORTING_POPULAR) {
                    cachedDatasetPopular.addAll(movieList);
                } else {
                    cachedDatasetToprated.addAll(movieList);
                }
                refreshList();
            }
        }

    }

    @Override
    public void onPickedMovie(MovieObject movieObject) {
        Toast.makeText(this, "Clicked "+ movieObject.idMovieDb, Toast.LENGTH_LONG).show();
        Bundle bundle = new Bundle();
        bundle.putParcelable("movie", movieObject );
        MovieDetailsFragment details = new MovieDetailsFragment();
        details.setArguments(bundle);
        FragmentManager fragmentManager = getSupportFragmentManager();

        View displayView = (View) findViewById(R.id.detail_fragment);

        if (displayView != null) {

            fragmentManager.beginTransaction()
                    .replace(R.id.detail_fragment, details)
                    .addToBackStack(null)
                    .commit();
        } else {

            fragmentManager.beginTransaction()
                    .replace(R.id.list_fragment, details)
                    .addToBackStack(null)
                    .commit();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate custom menu
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    /**
     * Changes the sorting. Caching the dataset and the current page for quick swapping.
     */
    public boolean onOptionsItemSelected(MenuItem item) {
        /**
         * onOptionsItemSelected - When user click the menu button for sorting, the sorting type is swapped either Popular or top rated.
         * We cache the current dataset, restore the previously cached data for this sorting, change the button title
         * And finally load a new page of data by calling LoadMovies()
         */
        int menuItemId = item.getItemId();
        if (menuItemId == R.id.action_sort) {
            isSeeingFavorites = false;

            if ( currentSorting == SORTING_POPULAR ) {
                currentSorting = SORTING_TOP_RATED;
                currentPagePopular = currentPage;
                currentPage = currentPageTopRated;

                item.setTitle(R.string.sort_by_popular);
            } else {
                currentSorting = SORTING_POPULAR;
                currentPageTopRated = currentPage;
                currentPage = currentPagePopular;

                item.setTitle(R.string.sort_by_top);
            }

            refreshList();
        } else if (menuItemId == R.id.action_show_favorite) {
            isSeeingFavorites = true;
            showFavorites();
            String message = "En attendant";

            Toast.makeText(mContext, message, Toast.LENGTH_SHORT).show();

        }
        return super.onOptionsItemSelected(item);
    }


    public void goToReview(String movieDbId, ArrayList<ReviewObject> reviewsList) {
        Bundle bundle = new Bundle();
        bundle.putParcelableArrayList("reviews", (ArrayList<ReviewObject>) reviewsList);
        bundle.putString("movieDbId", movieDbId);

        MovieReviewsFragment reviewFragment = new MovieReviewsFragment();
        reviewFragment.setArguments(bundle);
        FragmentManager fragmentManager = getSupportFragmentManager();

        View displayView = (View) findViewById(R.id.detail_fragment);

        if (displayView != null) {

            fragmentManager.beginTransaction()
                    .replace(R.id.detail_fragment, reviewFragment).addToBackStack(null)
                    .commit();
        } else {
            fragmentManager.beginTransaction()
                    .replace(R.id.list_fragment, reviewFragment).addToBackStack(null)
                    .commit();
        }
    }


    /**
     * Allows to use only one toast
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
