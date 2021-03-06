package net.unitecgroup.www.unitecrfid;

import android.content.Context;
import android.net.DhcpInfo;
import android.net.Uri;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;
import java.util.Timer;

public class AlertsPageActivity extends BaseActivity
        implements  AlertsPageFragment.OnFragmentInteractionListener,
                    ScanWifiFragment.OnScanButtonListener,
                    ScanWifiFragment.OnBeaconConnectedListener,
                    AddAlertDialog.OnAlertSavedListener {

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    private SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private ViewPager mViewPager;

    private WifiInfo mWifiInit;
    private WifiInfo mWifiInfo;
    private WifiManager wifi;
    private Timer oTimerBeacon;
    private Runnable oRunnableBeacon;
    private Handler oBeaconHandler;

    private Fragment mCurrentFragment;
    private Fragment mAlertFragment;
    public String mBeaconIP;
    public String mBeaconName;
    public FloatingActionButton oFAB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alerts_page);


        //Floating Action Button
        oFAB = (FloatingActionButton) findViewById(R.id.fab);

        //Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        //setSupportActionBar(toolbar);
        //getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        //detects which page is being shown
        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                if (oFAB != null) {
                    if (position == 1) {
                        oFAB.setVisibility(View.VISIBLE);
                    } else {
                        oFAB.setVisibility(View.GONE);
                    }
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        //Set current WiFi Connection to restore it onDestroy
        wifi = (WifiManager) this.getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        //Get Current WiFi Connection
        if (wifi != null) {
            mWifiInit = wifi.getConnectionInfo();
        }

        mBeaconIP = "";
        mBeaconName = "";

        oTimerBeacon = new Timer();
        getBeaconIP();
    }

    public void getBeaconIP() {
        if (wifi != null) {
            mWifiInfo = wifi.getConnectionInfo();
            String sSSID = mWifiInfo.getSSID().replaceAll("\"", "");

            if (sSSID.toLowerCase().startsWith("atmosphera")) {
                DhcpInfo dinfo = wifi.getDhcpInfo();
                mBeaconIP = ScanWifiFragment.intToIp(dinfo.serverAddress);
                mBeaconName = sSSID;
            }
        }
    }

    //Disconnect from Beacon and reconnect to previous WiFi
    @Override
    protected void onDestroy() {
        super.onDestroy();
        restoreWifi();
    }

    @Override
    public void onPause() {
        super.onPause();
        restoreWifi();
    }

    private void restoreWifi() {
        //Restore the previous WiFi Connection
        if (wifi != null) {
            List<WifiConfiguration> list = wifi.getConfiguredNetworks();
            for (WifiConfiguration conf : list) {
                //"\"" + networkSSID + "\""
                if (conf.SSID != null && conf.SSID.equals(mWifiInit.getSSID())) {
                    wifi.disconnect();
                    wifi.enableNetwork(conf.networkId, true);
                    wifi.reconnect();
                    break;
                }
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        //reconnect to previous beacon if possible:
        if (wifi != null && mBeaconName != "") {
            List<WifiConfiguration> list = wifi.getConfiguredNetworks();
            for (WifiConfiguration conf : list) {
                //"\"" + networkSSID + "\""
                if (conf.SSID != null && conf.SSID.indexOf(mBeaconName) > 0) {
                    wifi.disconnect();
                    wifi.enableNetwork(conf.networkId, true);
                    wifi.reconnect();
                    break;
                }
            }
        }
    }

    @Override
    protected int getNavigationDrawerID() {
        return R.id.nav_alerts;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.menu_scan, menu);
        getMenuInflater().inflate(R.menu.alerts, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (mCurrentFragment instanceof AlertsPageFragment ) {
            mCurrentFragment.onOptionsItemSelected(item);
            mAlertFragment = mCurrentFragment;
        }

        return super.onOptionsItemSelected(item);
    }

    public void changeToNextFragment() {
        if (mViewPager.getCurrentItem() < mSectionsPagerAdapter.getCount()) {

            if (oBeaconHandler != null && oRunnableBeacon != null) {
                oBeaconHandler.removeCallbacks(oRunnableBeacon);
            }

            changeToFragment(mViewPager.getCurrentItem()+1);
        }
    }

    public void changeToFragment(int iFragment) {
        mViewPager.setCurrentItem(iFragment);

    }

    @Override
    public void onFragmentInteraction(Uri uri) {
        //TODO: do something with scan click from Fragment
    }

    @Override
    public void onScanButtonClicked(Uri uri) {
        //TODO: do something with scan click from Fragment
    }

    @Override
    public void OnAlertSaved(int pos, Alert oAlert) {
        ((AlertsPageFragment) mCurrentFragment).OnAlertSaved(pos, oAlert);
    }

    @Override
    public void OnBeaconConnected(String sBeaconIP, String sBeaconName) {
        //Execute after a few seconds the Beacon Update message
        mBeaconIP = sBeaconIP;
        mBeaconName = sBeaconName;

        /*
        oTimerBeacon.scheduleAtFixedRate(new TimerTask() {
                                  @Override
                                  public void run() {
                                      //Called each time when 1000 milliseconds (1 second) (the period parameter)
                                      if (mAlertFragment instanceof  AlertsPageFragment) {
                                          if (((AlertsPageFragment) mAlertFragment).bLoaded) {
                                              changeToNextFragment();
                                          }
                                      }
                                  }
                              },
                //Set how long before to start calling the TimerTask (in milliseconds)
                0,
                //Set the amount of time between each execution (in milliseconds)
                500);
        */

        // Create the Handler object (on the main thread by default)
        if (oBeaconHandler == null) {
            oBeaconHandler = new Handler();
        }

        // Define the code block to be executed
        if (oRunnableBeacon == null) {
            oRunnableBeacon = new Runnable() {
                @Override
                public void run() {
                    // Do something here on the main thread
                    if (mAlertFragment instanceof AlertsPageFragment) {
                        if (((AlertsPageFragment) mAlertFragment).bLoaded) {
                            changeToNextFragment();
                        } else {
                            ((AlertsPageFragment) mAlertFragment).refreshBeacon();
                        }
                    }
                    // Repeat this the same runnable code block again another 1 seconds
                    oBeaconHandler.postDelayed(oRunnableBeacon, 1000);
                }
            };
        }
        // Start the initial runnable task by posting through the handler
        oBeaconHandler.post(oRunnableBeacon);

        /*
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mAlertFragment instanceof  AlertsPageFragment) {
                    if (((AlertsPageFragment) mAlertFragment).bLoaded) {
                        changeToNextFragment();
                    }
                }
            }
        }, 500);
        */
        /*
        changeToNextFragment();
        if (mCurrentFragment instanceof  AlertsPageFragment) {
            ((AlertsPageFragment) mCurrentFragment).refreshBeacon();
        }
        */
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";

        public PlaceholderFragment() {
        }

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_scan, container, false);
            TextView textView = (TextView) rootView.findViewById(R.id.section_label);
            textView.setText(getString(R.string.section_format, getArguments().getInt(ARG_SECTION_NUMBER)));
            return rootView;
        }
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            if (position == 0) {
                return ScanWifiFragment.newInstance(ScanWifiFragment.WIFIFRAGMENT, "wifi");
            } else if (position == 1) {
                mAlertFragment = AlertsPageFragment.newInstance("Alert", "wifi");
                return mAlertFragment;
            } else {
                return PlaceholderFragment.newInstance(position + 1);
            }
        }

        @Override
        public void setPrimaryItem(ViewGroup container, int position, Object object) {
            if (mCurrentFragment != object) {
                mCurrentFragment = (Fragment) object;

                if (mCurrentFragment instanceof AlertsPageFragment ) {
                    mAlertFragment = mCurrentFragment;
                }
            }
            super.setPrimaryItem(container, position, object);
        }

        // Show total pages.
        @Override
        public int getCount() {
            return 2;
        }
    }
}
