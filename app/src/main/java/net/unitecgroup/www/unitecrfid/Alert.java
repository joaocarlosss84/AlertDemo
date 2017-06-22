package net.unitecgroup.www.unitecrfid;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by 20006030 on 12/06/2017.
 * https://stackoverflow.com/questions/6681217/help-passing-an-arraylist-of-objects-to-a-new-activity
 */

public class Alert implements Parcelable {

    private int _id;
    private String _time;
    private String _duration;
    private String _weekdays;

    public Alert() {

    }

    public Alert(int id, String time, String duration, String weekdays) {
        this._id = id;
        this._time = time;
        this._duration = duration;
        this._weekdays = weekdays;
    }

    public Alert(String time, String duration, String weekdays) {
        this._time = time;
        this._duration = duration;
        this._weekdays = weekdays;
    }

    protected Alert(Parcel in) {
        _id = in.readInt();
        _time = in.readString();
        _duration = in.readString();
        _weekdays = in.readString();
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

    public String get_duration() {
        return _duration;
    }

    public void set_duration(String _duration) {
        this._duration = _duration;
    }

    public String get_weekdays() {
        return _weekdays;
    }

    public void set_weekdays(String _weekdays) {
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
        dest.writeString(_weekdays);
    }


}
