package net.unitecgroup.www.unitecrfid;

/**
 * Created by 20006030 on 12/06/2017.
 */

public class Alert {

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
        return this._time + " - " + this._duration + " - " + this._weekdays;
    }
}
