package com.example.pro.watts_hut.activities;

import android.content.Context;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.example.pro.watts_hut.R;
import com.example.pro.watts_hut.adapters.MovieRvAdapter;
import com.example.pro.watts_hut.data.FavoriteMoviesHelper;
import com.example.pro.watts_hut.data.MovieObject;
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

import static com.example.pro.watts_hut.data.FavoriteMoviesContract.*;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private Context mContext;
    private Toast internalToast;

    // database
    private SQLiteDatabase mDb;

    // Recycler view
    private MovieRvAdapter internalMovieAdapter;
    private RecyclerView recyclerView;

    // Sorting fields and cached dataset for the recycler view
    final private int SORTING_POPULAR = R.string.movie_api_popular;
    final private int SORTING_TOP_RATED = R.string.movie_api_top_rate;
    private int currentSorting = R.string.movie_api_top_rate;
    private List<MovieObject> cachedDatasetPopular = new ArrayList<MovieObject>();
    private List<MovieObject> cachedDatasetToprated = new ArrayList<MovieObject>();
    private List<MovieObject> cachedFavorites = new ArrayList<MovieObject>();

    // Keeps a separate page count for the two API endpoints
    private int currentPage; // Used in LoadMovies()
    private int currentPagePopular = 0;
    private int currentPageTopRated = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_no_fragment);

        mContext = getApplicationContext();

        // Setup recycler view
        recyclerView = (RecyclerView) findViewById(R.id.rv_movie_grid);

        // Set GridLayoutManager to the recyclerview, with two columns
        //GridLayoutManager layoutManager = new GridLayoutManager(this,2);
        //recyclerView.setLayoutManager(layoutManager);


        if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT){
            recyclerView.setLayoutManager(new GridLayoutManager(mContext, 2));
        }
        else{
            recyclerView.setLayoutManager(new GridLayoutManager(mContext, 4));
        }

        // Set the adapter to the recycler view
        internalMovieAdapter = new MovieRvAdapter(this, recyclerView);
        internalMovieAdapter.setOnLoadMoreListener(new MovieRvAdapter.OnLoadMoreListener() {
            @Override
            public void onLoadMore() {
                loadMovies();
            }
        });
        recyclerView.setAdapter(internalMovieAdapter);


        // access the dababase
        FavoriteMoviesHelper mDbHelper = FavoriteMoviesHelper.getHelper(mContext);
        mDb = mDbHelper.getWritableDatabase();

        refreshFavorites();
    }

    @Override
    protected void onRestart() {
        super.onRestart();

        refreshFavorites();
    }

    // cmd shift a
    // ctrl o

    public void refreshFavorites() {
        Cursor cursor = getAllFavorite();
        Log.i(TAG, "onCreate: " + cursor.getCount());

        cachedFavorites = new ArrayList<MovieObject>();
        while (cursor.moveToNext()) {
            cachedFavorites.add(new MovieObject(this, cursor));
        }
    }


    private Cursor getAllFavorite () {
        String[] projection = {
                favoriteMovies._ID,
                favoriteMovies.COLUMN_MOVIE_DB_ID,
                favoriteMovies.COLUMN_RELEASE_DATE,
                favoriteMovies.COLUMN_MOVIE_TITLE,
                favoriteMovies.COLUMN_THUMBNAIL,
                favoriteMovies.COLUMN_IS_FAVORITE,
                favoriteMovies.COLUMN_RATINGS,
                favoriteMovies.COLUMN_VOTE_COUNT,
                favoriteMovies.COLUMN_SYNOPSIS,

        };

        // Filter results WHERE "title" = 'My Title'
        String selection = favoriteMovies.COLUMN_IS_FAVORITE + " = ?";
        String[] selectionArgs = { "1" };

        // How you want the results sorted in the resulting Cursor
        String sortOrder =
                favoriteMovies.COLUMN_DATE_ADDED + " DESC";

        return mDb.query(favoriteMovies.TABLE_NAME,
                projection,
                selection,
                selectionArgs,
                null,
                null,
                sortOrder);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate custom menu
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
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
            Context context = this;
            String message = "Refresh!";
            boast(Toast.makeText(context, message, Toast.LENGTH_LONG));
            internalMovieAdapter.shouldLoad = true;
            if ( currentSorting == SORTING_POPULAR ) {
                currentSorting = SORTING_TOP_RATED;
                currentPagePopular = currentPage;
                cachedDatasetPopular = internalMovieAdapter.swapDataset(cachedDatasetToprated);
                currentPage = currentPageTopRated;
                item.setTitle(R.string.sort_by_popular);
            } else {
                currentSorting = SORTING_POPULAR;

                currentPageTopRated = currentPage;
                cachedDatasetToprated = internalMovieAdapter.swapDataset(cachedDatasetPopular);
                currentPage = currentPagePopular;
                item.setTitle(R.string.sort_by_top);
            }

            loadMovies();
        } else if ((menuItemId == R.id.action_show_favorite)&&(cachedFavorites.size() > 0)) {
            if (currentSorting == SORTING_POPULAR) {
                currentPagePopular = currentPage;
                cachedDatasetPopular = internalMovieAdapter.swapDataset(cachedFavorites);
                internalMovieAdapter.shouldLoad = false;
            } else {
                currentPageTopRated = currentPage;
                cachedDatasetToprated = internalMovieAdapter.swapDataset(cachedFavorites);
                internalMovieAdapter.shouldLoad = false;
            }

            // TODO set dataset to cached favorite if exists
            // TODO start a query on background thread for db
            // TODO once query is fetch compare each result to the cache data, add if new
            // TODO refresh with new data.set
            String message = "En attendant";

            boast(Toast.makeText(this, message, Toast.LENGTH_SHORT));

        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Sets up the URI for the API call, and call the async task to load the data.
     */
    public void loadMovies() {

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
            Context context = getApplicationContext();
            String message = "URL:"+url;


            Log.d(TAG, String.valueOf(internalToast == null));
            boast(Toast.makeText(context, message, Toast.LENGTH_SHORT));

            Log.d(TAG, "loadMovies: "+url);

            // The URL is built, calls the async task
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
                            MovieObject movieObject = new MovieObject(MainActivity.this, movie);
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
                internalMovieAdapter.addMovies(movieList);
            }
        }

    }



}
