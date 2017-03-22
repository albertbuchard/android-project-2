package com.example.pro.watts_hut.data;

import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Class that stores the review information. Parcelable and able to be passed as extra
 * to the MovieReview activity.
 */
public class ReviewObject implements Parcelable {
    public String id;
    public String author;
    public String content;

    public ReviewObject(JSONObject data) {
        try {
            id = data.getString("id");
            author = data.getString("author");
            content = data.getString("content");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }



    public ReviewObject(String anId, String anAuthor, String aContent) {
        id = anId;
        author = anAuthor;
        content = aContent;
    }

    protected ReviewObject(Parcel in) {
        id = in.readString();
        author = in.readString();
        content = in.readString();
    }

    public static final Creator<ReviewObject> CREATOR = new Creator<ReviewObject>() {
        @Override
        public ReviewObject createFromParcel(Parcel in) {
            return new ReviewObject(in);
        }

        @Override
        public ReviewObject[] newArray(int size) {
            return new ReviewObject[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(author);
        dest.writeString(content);
    }
}
