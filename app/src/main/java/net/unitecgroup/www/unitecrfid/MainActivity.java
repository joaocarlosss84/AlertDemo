package net.unitecgroup.www.unitecrfid;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import java.util.ArrayList;
import java.util.Collections;

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
public class MainActivity extends BaseActivity implements
        AddAlertDialog.OnAlertSavedListener,
        AlertListAdapter.OnItemDeletedListener {

    private static final String ALERT_LIST = "alertList";
    private static final String ALERT_DELETE_LIST = "alertDeleteList";
    static AlertListAdapter oAlertListAdapter;
    RecyclerView mRecyclerView;

    FragmentManager fm = getSupportFragmentManager();
    AddAlertDialog oAddAlert;

    DatabaseTable mDB;

    ServerCommunication mServer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Floating Action Button
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                oAddAlert = new AddAlertDialog();
                oAddAlert.show(fm, "Dialog Fragment");
            }
        });

        /*
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        */

        mRecyclerView = (RecyclerView) findViewById(R.id.mainListView);
        mDB = new DatabaseTable(this);

        mServer = new ServerCommunication(this);

        // Reading all contacts
        ArrayList<Alert> alerts = mDB.getAllAlerts();

        if (oAlertListAdapter == null)
            oAlertListAdapter = new AlertListAdapter(this, alerts);

        /*
        for (Alert cn : alerts) {
            oAlertListAdapter.addAlert(cn);
        }
        */

        setUpRecyclerView();

        if (savedInstanceState != null) {
            //Treating Screen Rotation
        }

    }

    @Override
    protected void onStop() {
        super.onStop();
        mDB.close();
    }

    private void setUpRecyclerView() {

        oAlertListAdapter.setUndoOn(true);
        //mRecyclerView.setLayoutManager(new GridLayoutManager(this,2));
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setAdapter(oAlertListAdapter);
        //mRecyclerView.swapAdapter(oAlertListAdapter, false);

        mRecyclerView.setHasFixedSize(true);

        //Create the Alert Edit Dialog, load the previous data.
        oAlertListAdapter.SetOnItemClickListener(
            new AlertListAdapter.OnItemClickListener() {
                @Override
                public void onItemClick(View view, int position, String id) {
                    Bundle args = new Bundle(); //Bundle containing data you are passing to the dialog

                    Alert oAlert = oAlertListAdapter.items.get(position);
                    args.putInt(ALERT_POS, position);
                    args.putInt(ALERT_ID, oAlert.get_id());
                    args.putString(ALERT_TIME, oAlert.get_time());
                    args.putString(ALERT_DURATION, oAlert.get_duration());
                    //Creates a ArrayList<Integer> from String "[1,2,3]"
                    args.putIntegerArrayList(ALERT_WEEKDAYS, arrayStringToIntegerArrayList(oAlert.get_weekdays()));

                    oAddAlert = new AddAlertDialog();
                    oAddAlert.setArguments(args);
                    oAddAlert.show(fm, "Dialog Fragment");

                    /*
                    //Split the Row String data to save into the Alert Dialog
                    String sAlert = oAlertListAdapter.items.get(position).toString();
                    String[] aAlert = TextUtils.split(sAlert, "-");

                    if (aAlert.length == 4) {
                        args.putInt(ALERT_ID, Integer.parseInt(aAlert[0].trim()));
                        args.putString(ALERT_TIME, aAlert[1].trim());
                        args.putString(ALERT_DURATION, aAlert[2].trim());
                        //Creates a ArrayList<Integer> from String "[1,2,3]"
                        args.putIntegerArrayList(ALERT_WEEKDAYS, arrayStringToIntegerArrayList(aAlert[3].trim()));

                        oAddAlert = new AddAlertDialog();
                        oAddAlert.setArguments(args);
                        oAddAlert.show(fm, "Dialog Fragment");
                    }
                    */
                }
            }
        );

        setUpItemTouchHelper();
        setUpAnimationDecoratorHelper();
    }


    public static ArrayList<Integer> arrayStringToIntegerArrayList(String arrayString){
        String removedBrackets = arrayString.substring(1, arrayString.length() - 1);
        String[] individualNumbers = removedBrackets.split(",");
        ArrayList<Integer> integerArrayList = new ArrayList<>();
        for(String numberString : individualNumbers){
            integerArrayList.add(Integer.parseInt(numberString.trim()));
        }
        Collections.sort(integerArrayList);
        return integerArrayList;
    }

    public void addAlerts() {

        ArrayList<Alert> aAlerts = new ArrayList<>();
        aAlerts.add(new Alert("06:30", "00:15", "[2,3,4,5,6]"));
        aAlerts.add(new Alert("12:00", "00:30", "[2,3]"));
        aAlerts.add(new Alert("18:30", "00:10", "[4,5,6]"));

        for (Alert cn : aAlerts) {
            if (mDB.addAlert(cn) >= 0) {
                oAlertListAdapter.addAlert(cn);
            }
        }
    }

    public void removeAlerts() {
        //This will erase the DB content
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
                xMark = ContextCompat.getDrawable(MainActivity.this, R.drawable.ic_clear_24dp);
                xMark.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);
                xMarkMargin = (int) MainActivity.this.getResources().getDimension(R.dimen.fab_margin);
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
                        if (child.getTranslationY() < 0) {
                            // view is coming down
                            lastViewComingDown = child;
                        } else if (child.getTranslationY() > 0) {
                            // view is coming up
                            if (firstViewComingUp == null) {
                                firstViewComingUp = child;
                            }
                        }
                    }

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

                    background.setBounds(left, top, right, bottom);
                    background.draw(c);

                }
                super.onDraw(c, parent, state);
            }

        });
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected int getNavigationDrawerID() {
        return R.id.nav_home;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
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
        }

        return super.onOptionsItemSelected(item);
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

    /*
    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        Fragment fragment = null;
        String title = getString(R.string.app_name);

        if (id == R.id.nav_camera) {
            // Handle the camera action
            //fragment = new NFCActivity();
            //title  = "NFC";
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }

        if (fragment != null) {
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.replace(R.id.content_frame, fragment);
            ft.commit();
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
    */


    @Override
    public void OnAlertSaved(int pos, Alert oAlert) {
        if (oAlert.get_id() < 0) {
            //Adding new alert to DB
            if (mDB.addAlert(oAlert) >= 0) {
                oAlertListAdapter.addAlert(oAlert);
            }
        } else {
            //update row at DB
            if (mDB.updateAlert(oAlert)) {
                oAlertListAdapter.updateAlert(oAlert);
            }
        }
    }

    @Override
    public boolean OnItemDeleted(int pos, Alert oAlert) {
        return mDB.deleteAlert(oAlert);
    }
}
