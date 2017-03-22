package com.example.pro.watts_hut.fragments;

import android.content.Context;
import android.content.res.Configuration;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Toast;

import com.example.pro.watts_hut.R;
import com.example.pro.watts_hut.activities.MainActivityUsingFragment;
import com.example.pro.watts_hut.adapters.MovieRvAdapter;
import com.example.pro.watts_hut.data.MovieObject;

public class MainFragment extends Fragment {
    private static final String TAG = "MainActivity";

    private Context mContext;
    private Toast internalToast;

    // database
    private SQLiteDatabase mDb;

    // Recycler view
    public MovieRvAdapter internalMovieAdapter;
    private RecyclerView recyclerView;

    OnItemSelectedListener onItemSelectedListener = null;
    private int fragmentWidth;
    private int fragmentHeight;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        final View root = inflater.inflate(R.layout.fragment_main, container, false);

        root.post(new Runnable() {
            @Override
            public void run() {
                fragmentWidth = root.getMeasuredWidth();
                fragmentHeight = root.getMeasuredHeight();

                initLayout();
            }
        });

        return root;
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    private void initLayout() {

        mContext = getActivity();

        // Setup recycler view
        recyclerView = (RecyclerView) getView().findViewById(R.id.rv_movie_grid);

        // Set GridLayoutManager to the recyclerview, with two columns
        //GridLayoutManager layoutManager = new GridLayoutManager(this,2);
        //recyclerView.setLayoutManager(layoutManager);


        if((fragmentWidth>1500)&&(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)){
            recyclerView.setLayoutManager(new GridLayoutManager(mContext, 4));
        }
        else{
            recyclerView.setLayoutManager(new GridLayoutManager(mContext, 2));
        }

        // Set the adapter to the recycler view
        internalMovieAdapter = new MovieRvAdapter(mContext, recyclerView);
        internalMovieAdapter.shouldLoad = false;
        internalMovieAdapter.setOnLoadMoreListener(new MovieRvAdapter.OnLoadMoreListener() {
            @Override
            public void onLoadMore() {
                ((MainActivityUsingFragment) getActivity()).loadMovies();
            }
        });

        internalMovieAdapter.setOnSelectMovieListener(new MovieRvAdapter.OnSelectMovieListener() {
            @Override
            public void onSelectMovie(MovieObject movie) {
                onItemSelectedListener.onPickedMovie(movie);
            }
        });

        internalMovieAdapter.fragmentWidth = fragmentWidth;
        internalMovieAdapter.fragmentHeight = fragmentHeight;
        recyclerView.setAdapter(internalMovieAdapter);


        ((MainActivityUsingFragment) getActivity()).refreshList();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (internalMovieAdapter != null) {
            ((MainActivityUsingFragment) getActivity()).refreshList();
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

    public void setOnItemSelectedListener (OnItemSelectedListener onItemSelectedListener) {
        this.onItemSelectedListener = onItemSelectedListener;
    }

    public interface OnItemSelectedListener {
        public void onPickedMovie(MovieObject movieObject);
    }



}
