package com.example.pro.watts_hut.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import static com.example.pro.watts_hut.data.FavoriteMoviesContract.*;

/**
 * Created by Pro on 12.03.17.
 */

public class FavoriteMoviesHelper extends SQLiteOpenHelper {
    public static final int DATABASE_VERSION = 2;
    public static final String DATABASE_NAME = "favoriteMovies.db";
    private static final String SQL_DELETE_ENTRIES = "DROP TABLE IF EXISTS " + favoriteMovies.TABLE_NAME;

    public FavoriteMoviesHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    private static FavoriteMoviesHelper instance;

    public static synchronized FavoriteMoviesHelper getHelper(Context context)
    {
        if (instance == null)
            instance = new FavoriteMoviesHelper(context);

        return instance;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String SQL_CREATE_ENTRIES =  "CREATE TABLE " +
                favoriteMovies.TABLE_NAME + " (" +
                favoriteMovies._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                favoriteMovies.COLUMN_MOVIE_DB_ID + " INTEGER NOT NULL, " +
                favoriteMovies.COLUMN_USER_ID + " INTEGER NOT NULL, " +
                favoriteMovies.COLUMN_IS_FAVORITE + " BOOLEAN NOT NULL, " +
                favoriteMovies.COLUMN_DATE_ADDED + " TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                favoriteMovies.COLUMN_MOVIE_TITLE + " TEXT, " +
                favoriteMovies.COLUMN_RELEASE_DATE + " TEXT, " +
                favoriteMovies.COLUMN_RATINGS + " INTEGER, " +
                favoriteMovies.COLUMN_VOTE_COUNT + " INTEGER, " +
                favoriteMovies.COLUMN_SYNOPSIS + " TEXT, " +
                favoriteMovies.COLUMN_THUMBNAIL + " TEXT, " +
                favoriteMovies.COLUMN_TRAILER + " TEXT " +
                ")";

        db.execSQL(SQL_CREATE_ENTRIES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // This database is only a cache for online data, so its upgrade policy is
        // to simply to discard the data and start over
        db.execSQL(SQL_DELETE_ENTRIES);
        onCreate(db);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }
}
