package com.example.pro.watts_hut.data;

import android.content.Context;
import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;
import android.widget.ImageView;

import com.example.pro.watts_hut.R;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Holds the data of a movie from the JSONObject. Is also parcelable and sent between main activity and
 * detail activity.
 */
public class MovieObject implements Parcelable {

    public JSONObject data;
    public ImageView cachedImage;
    public String imageUrl = "null";
    public String apiFormattedUrl = "null";
    public String originalTitle = "null";
    public String releaseDate = "";
    public String synopsis = "";
    public String idMovieDb = "";
    public int voteCount = 0;
    public int voteAverage = 0;

    Context appContext = null;


    public MovieObject(Context context, JSONObject jsonObject) {
        // Store thumbnail path
        data = jsonObject;
        appContext = context;
        try {
            imageUrl = jsonObject.getString(appContext.getString(R.string.movie_api_results_image));
            originalTitle = jsonObject.getString("original_title");
            apiFormattedUrl = getThumbnailUrl();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public MovieObject(Context context, Cursor cursor) {
        // Store thumbnail path
        try {
            storeDataFromCursor(cursor);
            imageUrl = "";
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void storeDataFromCursor(Cursor cursor) throws JSONException {
        if (cursor.getPosition()  == -1) {
            cursor.moveToNext();
        }
        originalTitle = cursor.getString(cursor.getColumnIndexOrThrow(FavoriteMoviesContract.favoriteMovies.COLUMN_MOVIE_TITLE));
        releaseDate = cursor.getString(cursor.getColumnIndexOrThrow(FavoriteMoviesContract.favoriteMovies.COLUMN_RELEASE_DATE));
        synopsis = cursor.getString(cursor.getColumnIndexOrThrow(FavoriteMoviesContract.favoriteMovies.COLUMN_SYNOPSIS));
        idMovieDb = cursor.getString(cursor.getColumnIndexOrThrow(FavoriteMoviesContract.favoriteMovies.COLUMN_MOVIE_DB_ID));
        voteAverage = cursor.getInt(cursor.getColumnIndexOrThrow(FavoriteMoviesContract.favoriteMovies.COLUMN_RATINGS));
        voteCount = cursor.getInt(cursor.getColumnIndexOrThrow(FavoriteMoviesContract.favoriteMovies.COLUMN_VOTE_COUNT));
        apiFormattedUrl = cursor.getString(cursor.getColumnIndexOrThrow(FavoriteMoviesContract.favoriteMovies.COLUMN_THUMBNAIL));

        if (synopsis == null) {
            synopsis = "";
        }
        data = new JSONObject()
                .put("id", idMovieDb)
                .put("original_title", originalTitle)
                .put("release_date", releaseDate)
                .put("overview", synopsis)
                .put("vote_average", voteAverage)
                .put("vote_count", voteCount);



    }

    public static final Creator<MovieObject> CREATOR = new Creator<MovieObject>() {
        @Override
        public MovieObject createFromParcel(Parcel in) {
            return new MovieObject(in);
        }

        @Override
        public MovieObject[] newArray(int size) {
            return new MovieObject[size];
        }
    };

    public String getThumbnailUrl() {
        String url = apiFormattedUrl;
        if (url != "null")
            return url;

        if ((imageUrl != "null")&&(appContext != null))
            url = String.format(appContext.getString(R.string.image_api_url), "w185", imageUrl);

        return url;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.data.toString());
        dest.writeString(this.imageUrl);
        dest.writeString(this.apiFormattedUrl);
        //dest.writeByte((byte) (isFavorite ? 1 : 0));
    }

    public MovieObject(Parcel pc) {

        try {
            data = new JSONObject(pc.readString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        imageUrl =  pc.readString();
        apiFormattedUrl = pc.readString();
        //isFavorite = pc.readByte() != 0;
    }
}
