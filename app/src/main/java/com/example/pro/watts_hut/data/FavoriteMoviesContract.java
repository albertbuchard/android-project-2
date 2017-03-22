package com.example.pro.watts_hut.data;

import android.provider.BaseColumns;

/**
 * Created by Pro on 12.03.17.
 */

public class FavoriteMoviesContract {
    private FavoriteMoviesContract() {}

    public static class favoriteMovies implements BaseColumns {
        public static  final String TABLE_NAME = "favoriteMovies";
        public static  final String COLUMN_MOVIE_DB_ID = "movieDbId";
        public static  final String COLUMN_USER_ID = "userId";
        public static final String COLUMN_IS_FAVORITE = "isFavorite";
        public static  final String COLUMN_DATE_ADDED = "addedAt";
        public static  final String COLUMN_MOVIE_TITLE = "title";
        public static  final String COLUMN_RELEASE_DATE = "releaseDate";
        public static  final String COLUMN_RATINGS = "rating";
        public static  final String COLUMN_VOTE_COUNT = "voteCount";
        public static  final String COLUMN_SYNOPSIS = "synopsis";
        public static  final String COLUMN_THUMBNAIL = "thumbnail";
        public static  final String COLUMN_TRAILER = "trailer";


    }
}
