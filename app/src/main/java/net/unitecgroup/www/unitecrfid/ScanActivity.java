package net.unitecgroup.www.unitecrfid;

import android.content.Context;
import android.net.Uri;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

//https://developer.android.com/training/basics/fragments/communicating.html#Implement
//https://stackoverflow.com/questions/5452940/how-can-i-get-android-wifi-scan-results-into-a-list
//
//How to switch between Fragments
//https://developer.android.com/reference/android/support/v4/app/FragmentPagerAdapter.html
public class ScanActivity extends BaseActivity
        implements  ScanWifiFragment.OnScanButtonListener,
                    ScanWifiFragment.OnBeaconConnectedListener {

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

    public String mBeaconIP;

    private WifiInfo mWifiInfo;
    private WifiManager wifi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        //Set current WiFi Connection to restore it onDestroy
        wifi = (WifiManager) this.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        mWifiInfo = wifi.getConnectionInfo();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        //Restore the previous WiFi Connection
        if (wifi != null) {
            List<WifiConfiguration> list = wifi.getConfiguredNetworks();
            for (WifiConfiguration conf : list) {
                //"\"" + networkSSID + "\""
                if (conf.SSID != null && conf.SSID.equals(mWifiInfo.getSSID())) {
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
        return R.id.nav_scan;
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_scan, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    @Override
    public void onScanButtonClicked(Uri uri) {
        //TODO: do something with scan click from Fragment
    }

    public void changeToNextFragment() {
        if (mViewPager.getCurrentItem() < mSectionsPagerAdapter.getCount()) {
            changeToFragment(mViewPager.getCurrentItem()+1);
        }
    }

    public void changeToFragment(int iFragment) {
        mViewPager.setCurrentItem(iFragment);
    }

    @Override
    public void OnBeaconConnected(String sBeaconIP) {
        //unregisterWiFiConnection();
        mBeaconIP = sBeaconIP;
        changeToNextFragment();
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
                return ScanWifiFragment.newInstance(ScanWifiFragment.BEACONFRAGMENT, "beacon");
            } else {
                return PlaceholderFragment.newInstance(position + 1);
            }
        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            return 3;
        }
    }
}
