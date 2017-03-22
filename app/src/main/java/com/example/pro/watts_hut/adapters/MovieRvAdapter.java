package com.example.pro.watts_hut.adapters;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Parcelable;
import android.support.v4.widget.ContentLoadingProgressBar;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.example.pro.watts_hut.activities.MovieDetails;
import com.example.pro.watts_hut.R;
import com.example.pro.watts_hut.data.MovieObject;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Created by Pro on 19.02.17.
 */

public class MovieRvAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final String TAG = MovieRvAdapter.class.getSimpleName();
    public Context appContext;
    private final int VIEW_ITEM = 1; // Loaded item
    private final int VIEW_PROG = 0; // Content is loading item
    private GridLayoutManager gridLayoutManager;
    public int fragmentWidth;
    public int fragmentHeight;

    public List<MovieObject> mDataset = new ArrayList<MovieObject>();
    private OnLoadMoreListener onLoadMoreListener; // Custom listener for handling data loading
    private OnSelectMovieListener onSelectMovieListener = null;

    private int visibleThreshold = 20; // threshold distance from the last item at which we start loading more movies
    private int lastVisibleItem, totalItemCount;
    private boolean loading;
    public boolean shouldLoad = true;


    public MovieRvAdapter (Context c, RecyclerView recyclerView) {
        appContext = c;
        if (recyclerView.getLayoutManager() instanceof GridLayoutManager) {

            gridLayoutManager = (GridLayoutManager) recyclerView.getLayoutManager();

            // on scroll check if we need to load more movies
            recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                    super.onScrolled(recyclerView, dx, dy);
                    shouldLoad = true;
                    checkVisibleAndLoad();
                }
            });
        } else {
            Log.e(TAG, "MovieRvAdapter: Wrong layout manager", new Exception());
        }
    }

    /**
     * Checks if the current scroll is close up to visibleThreshold from the lastVisibleItem.
     * If so, calls the onLoadMoreListener.onLoadMore().
     */
    public void checkVisibleAndLoad() {
        totalItemCount = gridLayoutManager.getItemCount();
        lastVisibleItem = gridLayoutManager.findLastVisibleItemPosition();
        if (shouldLoad && !loading && totalItemCount <= (lastVisibleItem + visibleThreshold)) {

            if (onLoadMoreListener != null) {
                onLoadMoreListener.onLoadMore();
            }

            loading = true;
        }
    }



    /**
     * Function called after movies have loaded in the background thread. Add MovieObjects to the
     * dataset, and notifyDataSetChanged().
     *
     * @param movieObjectList
     */
    public void addMovies (HashSet<MovieObject> movieObjectList) {
        mDataset.addAll(movieObjectList);
        this.notifyDataSetChanged();
        loading = false;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        Context context = viewGroup.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        int layoutIdForListItem = R.layout.movie_list_item;
        int layoutIdForProgressItem = R.layout.support_simple_spinner_dropdown_item;
        boolean shouldAttachToParentImmediately = false;

        RecyclerView.ViewHolder viewHolder;

        if (viewType == VIEW_ITEM) {
            View view = inflater.inflate(layoutIdForListItem, viewGroup, shouldAttachToParentImmediately);
            viewHolder = new MovieViewHolder(view);
        } else {
            // Item is loading - inflate the progress id - TODO not usefull in this version of the app
            View view = inflater.inflate(layoutIdForProgressItem, viewGroup, shouldAttachToParentImmediately);
            viewHolder = new ProgressViewHolder(view);
        }
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

        Log.d(TAG, "#" + position);

        // Progress view holder is not necessary in the current app, but maybe project 2 will need it
        if (holder instanceof MovieViewHolder) {
            MovieViewHolder cast = (MovieViewHolder) holder;
            cast.bind(position);
        } else if (holder instanceof ProgressViewHolder) {
            ProgressViewHolder cast = (ProgressViewHolder) holder;
            cast.bind();
        }

    }


    @Override
    public int getItemCount() {
        return (mDataset.size());
    }

    @Override
    /**
     * Selects between a loading and item view.
     */
    public int getItemViewType(int position) {
        return mDataset.get(position) != null ? VIEW_ITEM : VIEW_PROG;
    }



    /**
     * Allows to cache the whole list of movies and not reload them each time we change the sorting
     * criterion.
     *
     * @param cachedDataset
     * @return
     */
    public List<MovieObject> swapDataset(List<MovieObject> cachedDataset) {
        shouldLoad = false;
        List<MovieObject> temp = mDataset;
        mDataset = cachedDataset;
        notifyDataSetChanged();
        loading = false;
        return temp;
    }


    /**
     * Allows to set a callback when the recyclerview detects a scroll near the end of the list.
     * Also calls the onLoadMoreListener.onLoadMore() after setting it.
     *
     * @param onLoadMoreListener
     */
    public void setOnLoadMoreListener(OnLoadMoreListener onLoadMoreListener) {
        this.onLoadMoreListener = onLoadMoreListener;
        //onLoadMoreListener.onLoadMore();
        this.checkVisibleAndLoad();
    }

    public interface OnLoadMoreListener {
        void onLoadMore();
    }

    public void setOnSelectMovieListener(OnSelectMovieListener onSelectMovieListener) {
        this.onSelectMovieListener = onSelectMovieListener;
    }

    public interface OnSelectMovieListener {
        void onSelectMovie(MovieObject movie);
    }

    public void hasClickedOnMovie(int position) {
        // Start detail activity
        if (onSelectMovieListener != null) {
            onSelectMovieListener.onSelectMovie(mDataset.get(position));
        } else {
            // Create intent and add a Parcelable MovieObject in the extras
            Intent intentDetail = new Intent(appContext, MovieDetails.class);
            intentDetail.putExtra("movie", (Parcelable) mDataset.get(position));
            appContext.startActivity(intentDetail);
        }

    }

    //

    /**
     * Extends ViewHolder. Holds the thumbnails and produce intent on click for detailed view.
     */
    class MovieViewHolder extends RecyclerView.ViewHolder
        implements View.OnClickListener {
        ImageView movieThumbnail;
        private int position = -1;

        public MovieViewHolder (View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);
            movieThumbnail = (ImageView) itemView.findViewById(R.id.rv_movie_thumbnail);
        }

        void bind (int position) {
            this.position = position;
            // Get display size
            DisplayMetrics display = ((Activity) appContext).getResources().getDisplayMetrics();
            if ((fragmentHeight == 0)||((fragmentWidth == 0))) {
                fragmentHeight = display.heightPixels;
                fragmentWidth = display.widthPixels;
            }

            int imageWidth = (int) Math.round(fragmentWidth/2);
            int imageHeight = (int) Math.round(fragmentHeight/1.8);
            Log.i(TAG, "bind: width " + String.valueOf(fragmentWidth) + " Height " + fragmentHeight);
            if (appContext.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                imageWidth = (int) Math.round(fragmentWidth / 4);
                imageHeight = (int) Math.round(fragmentHeight/1.5);
                if (fragmentWidth<1500) { // TODO No time to make that work for real
                    imageWidth = (int) Math.round(fragmentWidth/2);
                    imageHeight = (int) Math.round(fragmentHeight/2.5);
                }
            }

            // Set layout parameter of the thumbnail to make sure it is fullbleed
            Log.i(TAG, "bind: parent is" + movieThumbnail.getParent().toString());

            movieThumbnail.setAdjustViewBounds(true);
            movieThumbnail.setScaleType(ImageView.ScaleType.CENTER_CROP);
            movieThumbnail.setPadding(0, 0, 0, 0);

            MovieObject movie = mDataset.get(position);
            if (movie != null) {
                Picasso.with(appContext)
                        .load(movie.getThumbnailUrl())
                        .resize(imageWidth,imageHeight)
                        .centerCrop()
                        .into(movieThumbnail);
            }
        }

        @Override
        public void onClick(View v) {
            hasClickedOnMovie(position);
        }
    }



    class ProgressViewHolder extends RecyclerView.ViewHolder {
        private ContentLoadingProgressBar spinner;

        public ProgressViewHolder(View itemView) {
            super(itemView);

            spinner = (ContentLoadingProgressBar) itemView.findViewById(R.id.progressBar);
        }

        void bind () {
            spinner.show();
        }
    }
}
