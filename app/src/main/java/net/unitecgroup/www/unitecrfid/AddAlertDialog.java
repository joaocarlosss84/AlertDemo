package net.unitecgroup.www.unitecrfid;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.icu.util.Calendar;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TimePicker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.TreeSet;

import static java.util.Calendar.FRIDAY;
import static java.util.Calendar.MONDAY;
import static java.util.Calendar.SATURDAY;
import static java.util.Calendar.SUNDAY;
import static java.util.Calendar.THURSDAY;
import static java.util.Calendar.TUESDAY;
import static java.util.Calendar.WEDNESDAY;

//import android.app.DialogFragment;


/**
 * Created by 20006030 on 30/05/2017.
 */

public class AddAlertDialog extends DialogFragment {

    public static final String ALERT_ID = "alertID";
    public static final String ALERT_TIME = "alertTime";
    public static final String ALERT_DURATION = "alertDuration";
    public static final String ALERT_WEEKDAYS = "alertWeekdays";

    private TextView oTextViewTime;
    private TextView oTextViewDuration;
    //TreeSet will insert integers in order
    private TreeSet oSelectedWeekdays = new TreeSet<>();
    private String sTitle = "Add Alert";
    public  Boolean bAdd = true;

    private int _id = -1;

    OnAlertSavedListener oAlertListener;
    private int iTimerPickerTheme = 0;
    //private int iTimerPickerTheme = android.R.style.Theme_Holo_Dialog; //Theme_Holo_Dialog_NoActionBar_MinWidth;

    public void AddAlertDialog(String sTitle) {
        this.sTitle = sTitle;
    }

    public void AddAlertDialog() {
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState){

        AlertDialog.Builder oDialogBuilder = new AlertDialog.Builder(getActivity());
        final Bundle args = getArguments();

        //Use inflater to create a new instance of the Dialog and set the view objects attributes
        LayoutInflater inflater = (LayoutInflater) getActivity().getBaseContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View oDialogView = inflater.inflate(R.layout.add_alert_dialog, null);

        //Configure TextViews:
        oTextViewTime = (TextView) oDialogView.findViewById(R.id.textViewTime);
        oTextViewDuration = (TextView) oDialogView.findViewById(R.id.textViewDuration);

        if (args != null) {
            _id = args.getInt(ALERT_ID);
            oTextViewTime.setText(args.getString(ALERT_TIME));
            oTextViewDuration.setText(args.getString(ALERT_DURATION));
            ArrayList<Integer> aWeekdays = args.getIntegerArrayList(ALERT_WEEKDAYS);
            oSelectedWeekdays.addAll(aWeekdays);
        }

        //Configure Dialog with Save and Cancel buttons
        oDialogBuilder
                .setView(oDialogView)
                //.setIcon(R.drawable.ic_menu_camera)
                .setTitle(sTitle)
                .setPositiveButton("Save", null)
/*
                .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {


                        Snackbar.make(oTextViewTime.getRootView(), "Replace with your own action", Snackbar.LENGTH_LONG)
                                .setAction("Action", null).show();

                        // Send the event to the host activity
                        oAlertListener.OnAlertSavedListener(_id,
                                oTextViewTime.getText() + " - "
                                + oTextViewDuration.getText() + " - "
                                + oSelectedWeekdays.toString());

                    }
                })
*/
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });


        //Configure timePicker
        //TimePicker oTimePicker = (TimePicker) oDialogView.findViewById(R.id.timePicker);
        //oTimePicker.setIs24HourView(true);

        //Configure Weekday Checkboxes
        setupWeekdays(oDialogView);


        //Setting the Time Button OnClick
        Button setTime;
        setTime = (Button) oDialogView.findViewById(R.id.buttonSetTime);
        setTime.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                //TimePickerDialog tp1 = new TimePickerDialog(getContext(), TimePickerListener, Calendar.HOUR_OF_DAY, Calendar.MINUTE, true);
                int theme = 0;
                // Get a Calendar instance
                //final Calendar calendar = Calendar.getInstance();

                TimePickerDialog tp1 = new TimePickerDialog(getContext(), iTimerPickerTheme, TimePickerListener, Calendar.HOUR, Calendar.MINUTE, true);

                //not a new alert
                if (!oTextViewTime.getText().toString().equals("00:00")) {
                    String[] aTime = oTextViewTime.getText().toString().split(":");

                    try {
                        tp1.updateTime(Integer.parseInt(aTime[0]), Integer.parseInt(aTime[1]));
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }
                }
                tp1.show();
            }
        });

        //Setting the Duration Button OnClick
        Button setDuration;
        setDuration = (Button) oDialogView.findViewById(R.id.buttonSetDuration);
        setDuration.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //TimePickerDialog tp1 = new TimePickerDialog(getContext(), DurationPickerListener, 0, 0, true);
                TimePickerDialog tp1 = new TimePickerDialog(getContext(), iTimerPickerTheme, DurationPickerListener, 0, 0, true);

                if (!oTextViewDuration.getText().toString().equals("00:00")) {
                    String[] aTime = oTextViewDuration.getText().toString().split(":");

                    try {
                        tp1.updateTime(Integer.parseInt(aTime[0]), Integer.parseInt(aTime[1]));
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }
                }

                tp1.show();
            }
        });

        AlertDialog oAlertDialog = oDialogBuilder.create();


        //Overwrite the Save button to add verification
        oAlertDialog.setOnShowListener(new DialogInterface.OnShowListener() {

            @Override
            public void onShow(DialogInterface dialog) {

                Button button = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE);
                button.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View view) {
                        // Alert Validation
                        if (oSelectedWeekdays.size() == 0) {
                            Snackbar.make(view, "Please select weekdays", Snackbar.LENGTH_LONG)
                                    .setAction("Action", null).show();

                        } else if (oTextViewDuration.getText().equals("00:00")) {
                            Snackbar.make(view, "Please define a duration", Snackbar.LENGTH_LONG)
                                    .setAction("Action", null).show();
                        } else {
                            // Send the event to the host activity
                            //Collections.sort(oSelectedWeekdays);
                            Alert oAlert = new Alert();
                            oAlert.set_id(_id);
                            oAlert.set_time(oTextViewTime.getText().toString());
                            oAlert.set_duration(oTextViewDuration.getText().toString());
                            oAlert.set_weekdays(oSelectedWeekdays.toString());

                            oAlertListener.OnAlertSavedListener(oAlert);

                            /*
                            oAlertListener.OnAlertSavedListener(_id,
                                    String.valueOf(_id) + " - " +
                                    oTextViewTime.getText() + " - " +
                                    oTextViewDuration.getText() + " - " +
                                    oSelectedWeekdays.toString());
                            */
                            //Dismiss once everything is OK.
                            getDialog().dismiss();
                        }
                    }
                });
            }
        });


        return oAlertDialog;
    }



    private void setupWeekdays(View oDialogView) {
        LinearLayout oWeekdayGroup = (LinearLayout) oDialogView.findViewById(R.id.weekdayGroup);
        View v = null;
        for(int i=0; i< oWeekdayGroup.getChildCount(); i++) {
            v = oWeekdayGroup.getChildAt(i);
            if (v instanceof CheckBox) {
                switch (v.getId()) {
                    case R.id.cb_sun:
                        v.setTag(SUNDAY);
                        break;
                    case R.id.cb_mon:
                        v.setTag(MONDAY);
                        break;
                    case R.id.cb_tue:
                        v.setTag(TUESDAY);
                        break;
                    case R.id.cb_wed:
                        v.setTag(WEDNESDAY);
                        break;
                    case R.id.cb_thu:
                        v.setTag(THURSDAY);
                        break;
                    case R.id.cb_fri:
                        v.setTag(FRIDAY);
                        break;
                    case R.id.cb_sat:
                        v.setTag(SATURDAY);
                        break;
                }

                //Check the checkbox if it was selected previously
                if (oSelectedWeekdays.contains(v.getTag())) {
                    ((CheckBox) v).setChecked(true);
                }

                //Bind our SelectedWeekdays with checkboxes
                ((CheckBox) v).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        // update your model (or other business logic) based on isChecked
                        if (isChecked) {
                            oSelectedWeekdays.add(buttonView.getTag());
                        } else {
                            oSelectedWeekdays.remove(buttonView.getTag());
                        }
                    }
                });

            }
        }
    }

    //Restores the DialogFragment values
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (savedInstanceState != null) {
            //Restore the fragment's state here
            oTextViewTime.setText(savedInstanceState.getCharSequence(ALERT_TIME));
            oTextViewDuration.setText(savedInstanceState.getCharSequence(ALERT_DURATION));

            ArrayList<Integer> aWeekdays = savedInstanceState.getIntegerArrayList(ALERT_WEEKDAYS);
            oSelectedWeekdays.addAll(aWeekdays);
        }
    }

    //Save the DialogFrament values
    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putCharSequence(ALERT_TIME, oTextViewTime.getText());
        outState.putCharSequence(ALERT_DURATION, oTextViewDuration.getText());

        //Convert HashSet to ArrayList and save it to State
        Integer[] aInt = (Integer[]) oSelectedWeekdays.toArray(new Integer[oSelectedWeekdays.size()]);
        ArrayList<Integer> arr = new ArrayList<Integer>(Arrays.asList(aInt));
        outState.putIntegerArrayList(ALERT_WEEKDAYS, arr);

        super.onSaveInstanceState(outState);
    }

    // Container Activity must implement this interface
    public interface OnAlertSavedListener {
        //public void OnAlertSavedListener(int id, String alertTime);
        public void OnAlertSavedListener(Alert oAlert);
    }

    //Attach the activy that call AddAlertDialog to callback
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        try {
            oAlertListener = (OnAlertSavedListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement OnAlertSavedListener");
        }
    }

    private TimePickerDialog.OnTimeSetListener TimePickerListener =
            new TimePickerDialog.OnTimeSetListener() {
                @Override
                public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                    oTextViewTime.setText( String.format("%02d:%02d", hourOfDay, minute) );
                    return;
                }
            };

    private TimePickerDialog.OnTimeSetListener DurationPickerListener =
            new TimePickerDialog.OnTimeSetListener() {
                @Override
                public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                    oTextViewDuration.setText( String.format("%02d:%02d", hourOfDay, minute) );
                    return;
                }
            };


}

