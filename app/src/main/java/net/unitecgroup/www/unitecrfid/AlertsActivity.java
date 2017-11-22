package net.unitecgroup.www.unitecrfid;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
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
 *
 * http://nemanjakovacevic.net/blog/english/2016/01/12/recyclerview-swipe-to-delete-no-3rd-party-lib-necessary/
 * https://github.com/ashrithks/SwipeRecyclerView
 */
public class AlertsActivity extends BaseActivity implements
        AddAlertDialog.OnAlertSavedListener,
        AlertListAdapter.OnItemDeletedListener {

    private static final String ALERT_LIST = "alertList";
    private static final String ALERT_DELETE_LIST = "alertDeleteList";
    static AlertListAdapter oAlertListAdapter;
    RecyclerView mRecyclerView;

    FragmentManager fm;
    AddAlertDialog oAddAlert;

    DatabaseTable mDB;

    ServerCommunication mServer;
    FloatingActionButton fab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alerts);

        fm = getSupportFragmentManager();

        //Floating Action Button
        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                oAddAlert = new AddAlertDialog();
                oAddAlert.show(fm, "Dialog Fragment");
            }
        });

        //Receiving query from SEARCH
        // Get the intent, verify the action and get the query
        /*
        Intent intent = getIntent();
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            //doMySearch(query);
        }
        */

        mRecyclerView = (RecyclerView) findViewById(R.id.mainListView);

        mDB = Application.getDatabase();

        //mServer = new ServerCommunication(this);

        // Reading all contacts
        ArrayList<Alert> alerts = mDB.getAllAlerts();

        //Do not create more than one Adapter to avoid problems with screen rotation
        if (oAlertListAdapter == null)
            oAlertListAdapter = new AlertListAdapter(this, alerts);

        setUpRecyclerView();

    }

    private void setUpRecyclerView() {

        oAlertListAdapter.setUndoOn(true);
        //mRecyclerView.setLayoutManager(new GridLayoutManager(this,2));
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this.getBaseContext()));
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
                xMark = ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_clear_24dp);
                xMark.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);
                xMarkMargin = (int) getApplicationContext().getResources().getDimension(R.dimen.fab_margin);
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

    /**
     * Creates Activity menu
     *
     * https://developer.android.com/training/appbar/action-views.html
     * https://developer.android.com/guide/topics/search/search-dialog.html#SearchableConfiguration
     * @param menu
     * @return
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.alerts, menu);

        // Get the SearchView and set the searchable configuration
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        //TODO: NOT WORKING

        //SearchView searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
        //if (searchView != null) {
            // Assumes current activity is the searchable activity
        //    searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        //    searchView.setIconifiedByDefault(false); // Do not iconify the widget; expand it by default
        //}
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
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
        final Activity oParent = this;

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

        final Activity oParent = this;

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

    public void returnDeleteAlerts(ArrayList<Alert> aAlerts, boolean bSucess) {
        for (Alert oAlert : aAlerts) {
            if (bSucess) {
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

        final Activity oParent = this;

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



    @Override
    protected int getNavigationDrawerID() {
        return R.id.nav_alerts;
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putParcelableArrayList(ALERT_LIST, oAlertListAdapter.items);
        outState.putIntegerArrayList(ALERT_DELETE_LIST, oAlertListAdapter.itemsPendingRemoval);

        super.onSaveInstanceState(outState);
    }

    // This callback is called only when there is a saved instance previously saved using
    // onSaveInstanceState(). We restore some state in onCreate() while we can optionally restore
    // other state here, possibly usable after onStart() has completed.
    // The savedInstanceState Bundle is same as the one used in onCreate().
    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        oAlertListAdapter.items = savedInstanceState.getParcelableArrayList(ALERT_LIST);
        oAlertListAdapter.itemsPendingRemoval = savedInstanceState.getIntegerArrayList(ALERT_DELETE_LIST);
    }

    @Override
    public void OnAlertSaved(int pos, final Alert oAlert) {
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

    @Override
    public boolean OnItemDeleted(int pos, final Alert oAlert) {
        //WRONG: for some reason the Activity get stuck if I use this code
        //if ( sendDeleteAlerts(new ArrayList<Alert>() {{ add(oAlert); }}) ) {
        //  return mDB.deleteAlert(oAlert);
        //}

        //RIGHT: this fixed the Activity to be frozen
        ArrayList<Alert> alAlerts = new ArrayList<Alert>();
        alAlerts.add(oAlert);
        sendDeleteAlerts(alAlerts);
        return false;

        //return mDB.deleteAlert(oAlert);
    }
}

