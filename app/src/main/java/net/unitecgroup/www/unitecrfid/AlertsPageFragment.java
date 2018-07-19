package net.unitecgroup.www.unitecrfid;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static net.unitecgroup.www.unitecrfid.AddAlertDialog.ALERT_DURATION;
import static net.unitecgroup.www.unitecrfid.AddAlertDialog.ALERT_ID;
import static net.unitecgroup.www.unitecrfid.AddAlertDialog.ALERT_POS;
import static net.unitecgroup.www.unitecrfid.AddAlertDialog.ALERT_TIME;
import static net.unitecgroup.www.unitecrfid.AddAlertDialog.ALERT_WEEKDAYS;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link AlertsPageFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link AlertsPageFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class AlertsPageFragment extends Fragment implements
        AlertListAdapter.OnItemDeletedListener {
    private static final String ALERT_LIST = "alertList";
    private static final String ALERT_DELETE_LIST = "alertDeleteList";

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private OnFragmentInteractionListener mListener;

    FragmentManager fm;
    FloatingActionButton fab;
    AddAlertDialog oAddAlert;

    static AlertListAdapter oAlertListAdapter;
    RecyclerView mRecyclerView;
    DatabaseTable mDB;


    public AlertsPageFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment AlertsPageFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static AlertsPageFragment newInstance(String param1, String param2) {
        AlertsPageFragment fragment = new AlertsPageFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }


//    @Override
//    public void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
        //mServer = new ServerCommunication(this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_addAlerts) {
            addAlerts();
            return true;
        } else if (id == R.id.action_removeAlerts) {
            removeAlerts();
            return true;
        } else if (id == R.id.action_updateAlerts) {
            oAlertListAdapter.notifyDataSetChanged();
            return true;
        } else if (id == R.id.action_sendAlerts) {
            sendAlerts();
            return true;
        } else if (id == R.id.action_getAlerts) {
            getAlerts();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_alerts_page, container, false);

        //fm = getSupportFragmentManager();
        fm = getFragmentManager();

        //Floating Action Button
        fab = (FloatingActionButton) rootView.findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                oAddAlert = new AddAlertDialog();
                oAddAlert.show(fm, "Dialog Fragment");
            }
        });

        mRecyclerView = (RecyclerView) rootView.findViewById(R.id.mainListView);

        mDB = Application.getDatabase();

        // Reading all contacts
        ArrayList<Alert> alerts = mDB.getAllAlerts();

        //Do not create more than one Adapter to avoid problems with screen rotation
        if (oAlertListAdapter == null) {
            //oAlertListAdapter = new AlertListAdapter( (AlertsPageActivity) this.getActivity(), alerts);
            oAlertListAdapter = new AlertListAdapter( this, alerts);
        }


        setUpRecyclerView();

        return rootView;
    }

    private void setUpRecyclerView() {

        oAlertListAdapter.setUndoOn(true);
        //mRecyclerView.setLayoutManager(new GridLayoutManager(this,2));
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this.getContext()));
        mRecyclerView.setAdapter(oAlertListAdapter);
        mRecyclerView.setHasFixedSize(true);

        //Create the Alert Edit Dialog, load the previous data.
        oAlertListAdapter.SetOnItemClickListener(
                new AlertListAdapter.OnItemClickListener() {
                    @Override
                    public void onItemClick(View view, int position, String id) {
                        Bundle args = new Bundle(); //Bundle containing data you are passing to the dialog

                        //Save alert data to pass it to Dialog
                        Alert oAlert = oAlertListAdapter.items.get(position);
                        args.putInt(ALERT_POS, position);
                        args.putInt(ALERT_ID, oAlert.get_id());
                        args.putString(ALERT_TIME, oAlert.get_time());
                        args.putString(ALERT_DURATION, oAlert.get_duration());

                        args.putIntegerArrayList(ALERT_WEEKDAYS, oAlert.get_weekdays());

                        oAddAlert = new AddAlertDialog();
                        oAddAlert.setArguments(args);
                        oAddAlert.show(fm, "Dialog Fragment");

                    }
                }
        );

        setUpItemTouchHelper();
        setUpAnimationDecoratorHelper();
    }

    /**
     * This is the standard support library way of implementing "swipe to delete" feature. You can do custom drawing in onChildDraw method
     * but whatever you draw will disappear once the swipe is over, and while the items are animating to their new position the recycler view
     * background will be visible. That is rarely an desired effect.
     */
    private void setUpItemTouchHelper() {

        ItemTouchHelper.SimpleCallback simpleItemTouchCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {

            // we want to cache these and not allocate anything repeatedly in the onChildDraw method
            Drawable background;
            Drawable xMark;
            int xMarkMargin;
            boolean initiated;

            private void init() {
                background = new ColorDrawable(Color.RED);
                //Context context = getParentFragment().getContext(); //.getActivity().getApplicationContext();
                Context context = Application.getContext();
                xMark = ContextCompat.getDrawable(context, R.drawable.ic_clear_24dp);
                xMark.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);
                xMarkMargin = (int) context.getResources().getDimension(R.dimen.fab_margin);
                initiated = true;
            }

            // not important, we don't want drag & drop
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            /*
            //This is creating problems trying to delete two consecutive rows.
            @Override
            public int getSwipeDirs(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
                int position = viewHolder.getAdapterPosition();
                AlertListAdapter testAdapter = (AlertListAdapter)recyclerView.getAdapter();
                if (testAdapter.isUndoOn() && testAdapter.isPendingRemoval(position)) {
                    return 0;
                }
                return super.getSwipeDirs(recyclerView, viewHolder);
            }
            */

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int swipeDir) {
                int swipedPosition = viewHolder.getAdapterPosition();
                AlertListAdapter adapter = (AlertListAdapter)mRecyclerView.getAdapter();
                boolean undoOn = adapter.isUndoOn();
                if (undoOn) {
                    adapter.pendingRemoval(swipedPosition);
                } else {
                    adapter.remove(swipedPosition);
                }
            }

            @Override
            public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
                View itemView = viewHolder.itemView;
                AlertListAdapter adapter = (AlertListAdapter)mRecyclerView.getAdapter();
                boolean undoOn = adapter.isUndoOn();

                // not sure why, but this method get's called for viewholder that are already swiped away
                if (viewHolder.getAdapterPosition() == -1) {
                    // not interested in those
                    return;
                }

                if (!initiated) {
                    init();
                }

                // draw red background
                background.setBounds(itemView.getRight() + (int) dX, itemView.getTop(), itemView.getRight(), itemView.getBottom());
                background.draw(c);

                if (!undoOn) {
                    // draw x mark
                    int itemHeight = itemView.getBottom() - itemView.getTop();
                    int intrinsicWidth = xMark.getIntrinsicWidth();
                    int intrinsicHeight = xMark.getIntrinsicWidth();

                    int xMarkLeft = itemView.getRight() - xMarkMargin - intrinsicWidth;
                    int xMarkRight = itemView.getRight() - xMarkMargin;
                    int xMarkTop = itemView.getTop() + (itemHeight - intrinsicHeight) / 2;
                    int xMarkBottom = xMarkTop + intrinsicHeight;
                    xMark.setBounds(xMarkLeft, xMarkTop, xMarkRight, xMarkBottom);

                    xMark.draw(c);
                }

                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }

        };
        ItemTouchHelper mItemTouchHelper = new ItemTouchHelper(simpleItemTouchCallback);
        mItemTouchHelper.attachToRecyclerView(mRecyclerView);
    }

    /**
     * We're gonna setup another ItemDecorator that will draw the red background in the empty space while the items are animating to thier new positions
     * after an item is removed.
     */
    private void setUpAnimationDecoratorHelper() {

        mRecyclerView.addItemDecoration(new RecyclerView.ItemDecoration() {

            // we want to cache this and not allocate anything repeatedly in the onDraw method
            Drawable background;
            boolean initiated;

            private void init() {
                background = new ColorDrawable(Color.RED);
                initiated = true;
            }

            @Override
            public void onDraw(Canvas c, RecyclerView parent, RecyclerView.State state) {

                if (!initiated) {
                    init();
                }

                // only if animation is in progress
                if (parent.getItemAnimator().isRunning()) {

                    // some items might be animating down and some items might be animating up to close the gap left by the removed item
                    // this is not exclusive, both movement can be happening at the same time
                    // to reproduce this leave just enough items so the first one and the last one would be just a little off screen
                    // then remove one from the middle

                    // find first child with translationY > 0
                    // and last one with translationY < 0
                    // we're after a rect that is not covered in recycler-view views at this point in time
                    View lastViewComingDown = null;
                    View firstViewComingUp = null;

                    // this is fixed
                    int left = 0;
                    int right = parent.getWidth();

                    // this we need to find out
                    int top = 0;
                    int bottom = 0;

                    // find relevant translating views
                    int childCount = parent.getLayoutManager().getChildCount();
                    for (int i = 0; i < childCount; i++) {
                        View child = parent.getLayoutManager().getChildAt(i);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                            if (child.getTranslationY() < 0) {
                                // view is coming down
                                lastViewComingDown = child;
                            } else if (child.getTranslationY() > 0) {
                                // view is coming up
                                if (firstViewComingUp == null) {
                                    firstViewComingUp = child;
                                }
                            }
                        } else {

                        }
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                        if (lastViewComingDown != null && firstViewComingUp != null) {
                            // views are coming down AND going up to fill the void
                            top = lastViewComingDown.getBottom() + (int) lastViewComingDown.getTranslationY();
                            bottom = firstViewComingUp.getTop() + (int) firstViewComingUp.getTranslationY();
                        } else if (lastViewComingDown != null) {
                            // views are going down to fill the void
                            top = lastViewComingDown.getBottom() + (int) lastViewComingDown.getTranslationY();
                            bottom = lastViewComingDown.getBottom();
                        } else if (firstViewComingUp != null) {
                            // views are coming up to fill the void
                            top = firstViewComingUp.getTop();
                            bottom = firstViewComingUp.getTop() + (int) firstViewComingUp.getTranslationY();
                        }
                    }

                    background.setBounds(left, top, right, bottom);
                    background.draw(c);

                }
                super.onDraw(c, parent, state);
            }

        });
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public boolean OnItemDeleted(int pos, Alert oAlert) {
        //This fixed the Activity to be frozen
        ArrayList<Alert> alAlerts = new ArrayList<Alert>();
        alAlerts.add(oAlert);
        sendDeleteAlerts(alAlerts);
        return false;
    }

    public void OnAlertSaved(int pos, Alert oAlert) {
        ArrayList<Alert> alAlerts = new ArrayList<Alert>();
        alAlerts.add(oAlert);

        if (oAlert.get_id() < 0) {
            //Adding new alert to DB
            if (mDB.addAlert(oAlert) >= 0)
                oAlertListAdapter.addAlert(oAlert);

        } else {
            //update row at DB
            if (mDB.updateAlert(oAlert))
                oAlertListAdapter.updateAlert(oAlert);
        }
        sendAlerts(alAlerts);
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }


    //region ADD/REMOVE ALERTS
    public void addAlerts() {
        ArrayList<Alert> aAlerts = new ArrayList<>();
        aAlerts.add(new Alert("06:30", "00:15", new ArrayList<Integer>() {{add(2); add(3); add(4); add(5);}}));
        aAlerts.add(new Alert("12:00", "00:30", new ArrayList<Integer>() {{add(2); add(3);}}));
        aAlerts.add(new Alert("18:30", "00:10", new ArrayList<Integer>() {{add(4); add(5); add(6);}}));

        for (Alert cn : aAlerts) {
            if (mDB.addAlert(cn) >= 0) {
                oAlertListAdapter.addAlert(cn);
            }
        }
        sendAlerts(aAlerts);
    }

    public void removeAlerts() {
        //This will erase the DB content
        sendDeleteAlerts();
        mDB.deleteAll();
        oAlertListAdapter.removeAll();
    }
    //endregion

    //region GET ALERTS
    private void callbackGetAlerts(JSONArray aJSAlerts) {
        mDB.deleteAll();
        oAlertListAdapter.removeAll();

        ArrayList<Alert> aAlerts = new ArrayList<>();
        for (int i = 0; i < aJSAlerts.length(); i++) {
            try {
                JSONObject JSAlert = aJSAlerts.getJSONObject(i);
                Alert oAlert = new Alert();
                oAlert.set_id(JSAlert.getInt("_id"));
                oAlert.set_time(JSAlert.getInt("_time"));
                oAlert.set_duration(JSAlert.getInt("_duration"));

                ArrayList<Integer> aWeekdays = new ArrayList<>();
                JSONArray aJSWeekdays = JSAlert.getJSONArray("Weekdays");
                for (int j = 0; j < aJSWeekdays.length(); j++) {
                    aWeekdays.add(aJSWeekdays.getInt(j));
                }
                oAlert.set_weekdays(aWeekdays);

                if (mDB.addAlert(oAlert) >= 0) {
                    oAlertListAdapter.addAlert(oAlert);
                }

                //aAlerts.add(oAlert);
                //Gson gson = new Gson();
                //Alert oAlert = (Alert) gson.fromJson(JSAlert, Alert.class);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }


    }

    private void getAlerts() {
        String requestPath = Application.loadServerPath();
        final Activity oParent = this.getActivity();

        JsonObjectRequest JsonRequest = new JsonObjectRequest(
                Request.Method.GET,
                requestPath,
                null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        int Status = -1;
                        try {
                            Status = response.getInt("Status");
                            if (Status == 1) {
                                JSONArray aJSAlerts = response.getJSONArray("alerts");
                                callbackGetAlerts(aJSAlerts);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        if (Status == 1) {
                            Toast.makeText(oParent, "Success on Loading Alerts", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(oParent, "No Alerts to be loaded", Toast.LENGTH_LONG).show();
                        }
                    }
                },

                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(oParent, "Error on Loading Alerts", Toast.LENGTH_LONG).show();
                        //master.addInventoryServerCallback(serverResponse);
                    }
                }
        );

        // Instantiate the RequestQueue.
        RequestQueue queue = Application.getVolleyRequestQueue();

        // Add the request to the RequestQueue.
        queue.add(JsonRequest);
    }
    //endregion

    //region SEND_ALERTS
    private boolean sendAlerts() {
        return sendAlerts(new ArrayList<Alert>());
    }

    private boolean sendAlerts(ArrayList<Alert> alerts) {

        final boolean[] bSuccess = {false};

        // Reading all contacts
        //ArrayList<Alert> alerts = mDB.getAllAlerts();
        if (alerts.size() == 0) {
            alerts = mDB.getAllAlerts();
        }

        String requestPath = Application.loadServerPath();

        GsonBuilder builder = new GsonBuilder();
        builder.excludeFieldsWithoutExposeAnnotation();
        Gson gson = builder.create();

        JSONObject json = null;
        String gsonString = gson.toJson(alerts);

        try {
            json = new JSONObject();
            json.put("alerts", new JSONArray(gsonString));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        final Activity oParent = this.getActivity();

        JsonObjectRequest JsonRequest = new JsonObjectRequest(
                Request.Method.POST,
                requestPath,
                json,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            int Status = response.getInt("Status");
                            bSuccess[0] = (Status == 1);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        if (bSuccess[0]) {
                            Toast.makeText(oParent, "Success on Updating Alerts", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(oParent, "Error on Updating Alerts", Toast.LENGTH_LONG).show();
                        }

                        //ServerResponse serverResponse = makeServerResponse(response);
                        //master.addInventoryServerCallback(serverResponse);
                    }
                },

                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(oParent, "Error on Updating Alerts", Toast.LENGTH_LONG).show();
                        //master.addInventoryServerCallback(serverResponse);
                    }
                }
        );

        // Instantiate the RequestQueue.
        RequestQueue queue = Application.getVolleyRequestQueue(); //Volley.newRequestQueue(this);

        // Add the request to the RequestQueue.
        queue.add(JsonRequest);

        return bSuccess[0];
    }
    //endregion

    //region DELETE_ALERTS
    public void returnDeleteAlerts(ArrayList<Alert> aAlerts, boolean bSuccess) {
        for (Alert oAlert : aAlerts) {
            if (bSuccess) {
                if (mDB.deleteAlert(oAlert)) {
                    oAlertListAdapter.deleteAlert(oAlert);
                }
            } else {
                oAlertListAdapter.cancelDelete(oAlert);
            }
        }
    }

    private boolean sendDeleteAlerts() {
        return sendDeleteAlerts(new ArrayList<Alert>());
    }

    private boolean sendDeleteAlerts(ArrayList<Alert> alerts) {
        // Reading all contacts
        //ArrayList<Alert> alerts = mDB.getAllAlerts();
        if (alerts.size() == 0) {
            alerts = mDB.getAllAlerts();
        }

        String requestPath = Application.loadServerPath();
        final boolean[] bSuccess = {false};

        GsonBuilder builder = new GsonBuilder();
        builder.excludeFieldsWithoutExposeAnnotation();
        Gson gson = builder.create();

        JSONObject json = null;
        String gsonString = gson.toJson(alerts);

        try {
            json = new JSONObject();
            json.put("alerts", new JSONArray(gsonString));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        final Activity oParent = this.getActivity();

        final JSONObject finalJson = json;
        final ArrayList<Alert> finalAlerts = alerts;

        JsonObjectRequest JsonRequest = new JsonObjectRequest(
                Request.Method.DELETE,
                requestPath,
                finalJson,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            int Status = response.getInt("Status");
                            bSuccess[0] = (Status == 1);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        if (bSuccess[0]) {
                            Toast.makeText(oParent, "Success on Removing Alerts", Toast.LENGTH_LONG).show();
                            returnDeleteAlerts(finalAlerts, true);
                        } else {
                            Toast.makeText(oParent, "Error on Removing Alerts", Toast.LENGTH_LONG).show();
                            returnDeleteAlerts(finalAlerts, false);
                        }
                        //master.addInventoryServerCallback(serverResponse);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(oParent, "Error on Removing Alerts", Toast.LENGTH_LONG).show();
                        returnDeleteAlerts(finalAlerts, false);
                        //ServerResponse serverResponse = makeServerResponse(error);
                        //master.addInventoryServerCallback(serverResponse);
                    }
                }
        ) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = super.getHeaders();

                if (headers == null
                        || headers.equals(Collections.emptyMap())) {
                    headers = new HashMap<String, String>();
                }

                //headers.put("access_token", "access_token");
                headers.put("plain", finalJson.toString());

                return headers;
            }

        };

        // Instantiate the RequestQueue.
        RequestQueue queue = Application.getVolleyRequestQueue(); //Volley.newRequestQueue(this);

        // Add the request to the RequestQueue.
        queue.add(JsonRequest);

        return bSuccess[0];
    }
    //endregion
}
