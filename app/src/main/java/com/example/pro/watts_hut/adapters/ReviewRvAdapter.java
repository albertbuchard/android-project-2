package com.example.pro.watts_hut.adapters;

import android.content.Context;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.pro.watts_hut.R;
import com.example.pro.watts_hut.data.ReviewObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by Pro on 21.03.17.
 */

public class ReviewRvAdapter extends RecyclerView.Adapter {

    private static final String TAG = MovieRvAdapter.class.getSimpleName();
    public Context appContext;
    private final int VIEW_ITEM = 1; // Loaded item
    private final int VIEW_PROG = 0; // Content is loading item
    private GridLayoutManager gridLayoutManager;

    public List<ReviewObject> mDataset = new ArrayList<ReviewObject>();
    private ReviewRvAdapter.OnLoadMoreListener onLoadMoreListener; // Custom listener for handling data loading

    private int visibleThreshold = 20; // threshold distance from the last item at which we start loading more movies
    private int lastVisibleItem, totalItemCount;
    private boolean loading;
    public boolean shouldLoad = true;

    public ReviewRvAdapter(Context c, RecyclerView recyclerView) {
        this.appContext = appContext;
        appContext = c;
        if (recyclerView.getLayoutManager() instanceof GridLayoutManager) {

            gridLayoutManager = (GridLayoutManager) recyclerView.getLayoutManager();

            // on scroll check if we need to load more movies
            recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                    super.onScrolled(recyclerView, dx, dy);

                    checkVisibleAndLoad();
                }
            });
        } else {
            Log.e(TAG, "ReviewRvAdapter: Wrong layout manager", new Exception());
        }
    }

    /**
     * Checks if the current scroll is close up to visibleThreshold from the lastVisibleItem.
     * If so, calls the onLoadMoreListener.onLoadMore().
     */
    public void checkVisibleAndLoad() {
        // TODO implement
//        totalItemCount = gridLayoutManager.getItemCount();
//        lastVisibleItem = gridLayoutManager.findLastVisibleItemPosition();
//        if (shouldLoad && !loading && totalItemCount <= (lastVisibleItem + visibleThreshold)) {
//
//            if (onLoadMoreListener != null) {
//                onLoadMoreListener.onLoadMore();
//            }
//
//            loading = true;
//        }
    }

    /**
     * Function called after movies have loaded in the background thread. Add MovieObjects to the
     * dataset, and notifyDataSetChanged().
     *
     * @param reviewList
     */
    public void addReviews (Collection<ReviewObject> reviewList) {
        mDataset.addAll(reviewList);
        this.notifyDataSetChanged();
        loading = false;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        int layoutIdForListItem = R.layout.movie_review_item;
        boolean shouldAttachToParentImmediately = false;

        RecyclerView.ViewHolder viewHolder;

        View view = inflater.inflate(layoutIdForListItem, parent, shouldAttachToParentImmediately);
        viewHolder = new ReviewViewHolder(view);

        return viewHolder;
    }

    @Override
    public int getItemViewType(int position) {
        return super.getItemViewType(position);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        // Progress view holder is not necessary in the current app, but maybe project 2 will need it
        if (holder instanceof ReviewRvAdapter.ReviewViewHolder) {
            ReviewRvAdapter.ReviewViewHolder cast = (ReviewRvAdapter.ReviewViewHolder) holder;
            cast.bind(position);
        }
    }

    @Override
    public int getItemCount() {
        return mDataset.size();
    }

    /**
     * Allows to set a callback when the recyclerview detects a scroll near the end of the list.
     * Also calls the onLoadMoreListener.onLoadMore() after setting it.
     *
     * @param onLoadMoreListener
     */
    public void setOnLoadMoreListener(ReviewRvAdapter.OnLoadMoreListener onLoadMoreListener) {
        this.onLoadMoreListener = onLoadMoreListener;
        onLoadMoreListener.onLoadMore();
        this.checkVisibleAndLoad();
    }


    public interface OnLoadMoreListener {
        void onLoadMore();
    }

    /**
     * Extends ViewHolder. Holds the thumbnails and produce intent on click for detailed view.
     */
    class ReviewViewHolder extends RecyclerView.ViewHolder {
        TextView author;
        TextView content;
        ReviewObject review;
        private int position = -1;

        public ReviewViewHolder(View itemView) {
            super(itemView);
            author = (TextView) itemView.findViewById(R.id.review_author);
            content = (TextView) itemView.findViewById(R.id.review_content);
        }

        void bind (int position) {
            this.position = position;
            // Get display size

            review = mDataset.get(position);

            author.setText(review.author + " says:");
            content.setText(review.content);

        }


    }
}
