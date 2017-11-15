package net.unitecgroup.www.unitecrfid;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;

/**
 * Created by 20006030 on 12/06/2017.
 * https://stackoverflow.com/questions/6681217/help-passing-an-arraylist-of-objects-to-a-new-activity
 */

public class Alert implements Parcelable {

    @Expose(serialize = true)
    private int _id;
    @Expose(serialize = true)
    private String _time;
    @Expose(serialize = true)
    private String _duration;
    @Expose(serialize = true)
    @SerializedName("Weekdays")
    private ArrayList<Integer> _weekdays;

    public Alert() {

    }

    public Alert(int id, String time, String duration, ArrayList<Integer> weekdays) {
        this._id = id;
        this._time = time;
        this._duration = duration;
        this._weekdays = weekdays;
    }

    public Alert(String time, String duration, ArrayList<Integer> weekdays) {
        this._time = time;
        this._duration = duration;
        this._weekdays = weekdays;
    }

    public int get_id() {
        return _id;
    }

    public void set_id(int _id) {
        this._id = _id;
    }

    public String get_time() {
        return _time;
    }

    public void set_time(String _time) {
        this._time = _time;
    }

    public void set_time(int time) {
        String hour = Integer.toString(time/60);
        String minutes = Integer.toString(time%60);
        this._time = hour + ":" + minutes;
    }

    public String get_duration() {
        return _duration;
    }

    public void set_duration(String _duration) {
        this._duration = _duration;
    }

    public void set_duration(int duration) {
        String hour = Integer.toString(duration/60);
        String minutes = Integer.toString(duration%60);
        this._duration = hour + ":" + minutes;
    }

    public ArrayList<Integer> get_weekdays() {
        return _weekdays;
    }

    public void set_weekdays(ArrayList<Integer> _weekdays) {
        this._weekdays = _weekdays;
    }

    @Override
    public String toString() {
        return this._id + " - " + this._time + " - " + this._duration + " - " + this._weekdays;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(_id);
        dest.writeString(_time);
        dest.writeString(_duration);
        dest.writeList(_weekdays);
    }

    protected Alert(Parcel in) {
        _id = in.readInt();
        _time = in.readString();
        _duration = in.readString();
        _weekdays = in.readArrayList(ArrayList.class.getClassLoader());
    }

    public static final Creator<Alert> CREATOR = new Creator<Alert>() {
        @Override
        public Alert createFromParcel(Parcel in) {
            return new Alert(in);
        }

        @Override
        public Alert[] newArray(int size) {
            return new Alert[size];
        }
    };



}
