package net.unitecgroup.www.unitecrfid;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.DhcpInfo;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link ScanWifiFragment.OnScanButtonListener} interface
 * to handle interaction events.
 * Use the {@link ScanWifiFragment#newInstance} factory method to
 * create an instance of this fragment.
 *
 * https://stackoverflow.com/questions/40051204/android-studio-getscanresults-returns-empty-but-permission-is-given
 *
 * ListFragment
 * http://www.vogella.com/tutorials/AndroidListView/article.html#listfragments
 *
 * Remove duplicate SSID and order by RSSI
 * https://stackoverflow.com/questions/16119985/duplicate-ssid-in-scanning-wifi-result/27046433#27046433
 *
 * Connection WAP or WEP
 * https://gist.github.com/Cheesebaron/5844638
 *
 * Get WiFi Connection Status
 * https://stackoverflow.com/questions/10328215/retrieve-wifi-connection-statusandroid
 */
public class ScanWifiFragment extends ListFragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    public static final int WIFIFRAGMENT = 0;
    public static final int BEACONFRAGMENT = 1;


    // TODO: Rename and change types of parameters
    private int mFragmentType;
    private String mParam2;

    WifiManager wifi;
    ListView lv;
    TextView textStatus;
    Button buttonScan;
    Boolean bScanning = true;

    List<ScanResult> mAllResults;
    List<WiFiResult> mResults;

    WifiListAdapter adapter;
    WifiConfiguration mConf;

    private static final int PERMISSIONS_REQUEST_CODE_ACCESS_COARSE_LOCATION = 1001;

    private OnScanButtonListener mScanButtonListener;
    private OnBeaconConnectedListener mBeaconConnectedListener;
    private BroadcastReceiver mBroadcastScan;
    private BroadcastReceiver mBroadcastConnection;


    public ScanWifiFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment ScanWifiFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static ScanWifiFragment newInstance(int param1, String param2) {
        ScanWifiFragment fragment = new ScanWifiFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mResults = new ArrayList<>();

        this.adapter = new WifiListAdapter(getActivity(), R.layout.listview_scan_wifi_row, mResults, wifi);
        this.setListAdapter(this.adapter);

        if (getArguments() != null) {
            mFragmentType = getArguments().getInt(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }

        if (mFragmentType == WIFIFRAGMENT) {
            textStatus.setText("Search Beacons");
        } else {
            textStatus.setText("Set Beacon WiFi");
        }

        wifi = (WifiManager) this.getActivity().getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        if (mFragmentType == WIFIFRAGMENT) {
            if (wifi.isWifiEnabled() == false)
            {
                //Toast.makeText(getApplicationContext(), "wifi is disabled..making it enabled", Toast.LENGTH_LONG).show();
                Toast.makeText(getContext(), "wifi is disabled..making it enabled", Toast.LENGTH_LONG).show();
                wifi.setWifiEnabled(true);
            }

            registerScanResults();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && getActivity().checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                        PERMISSIONS_REQUEST_CODE_ACCESS_COARSE_LOCATION);
                //After this point you wait for callback in onRequestPermissionsResult(int, String[], int[]) overriden method
            } else {
                //Already Granted
                buttonScan.setText("Scanning");
                bScanning = true;
                wifi.startScan();
            }
        } else if (mFragmentType == BEACONFRAGMENT) {
            buttonScan.setText("Scan");
            bScanning = false;
        }
    }

    private void registerScanResults() {
        mBroadcastScan = new BroadcastReceiver()
        {
            @Override
            public void onReceive(Context c, Intent intent)
            {
                mAllResults = wifi.getScanResults();

                buttonScan.setText("Scan");
                bScanning = false;

                getActivity().unregisterReceiver(this);
                addWiFiList(mAllResults);
            }
        };

        getActivity().registerReceiver(mBroadcastScan, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
    }

    private void unregisterScanResults() {
        getActivity().unregisterReceiver(mBroadcastScan);
    }

    private void registerWiFiConnection() {
        if (mBroadcastConnection == null) {
            mBroadcastConnection = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    String action = intent.getAction();
                    if (action.equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                    } else if (action.equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) {
                        int iTemp = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE,
                                WifiManager.WIFI_STATE_UNKNOWN);
                        //LogUtil.d(LOG_SET, "+++++++-----------wifiStateReceiver------+++++++", DEBUG);
                        Log.d("WIFISTATECHANGED", "checkState");
                        checkState(iTemp);
                    } else if (action.equals(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION)) {
                        NetworkInfo.DetailedState state = WifiInfo.getDetailedStateOf((SupplicantState)
                                intent.getParcelableExtra(WifiManager.EXTRA_SUPPLICANT_CONNECTED));
                        WifiInfo info = intent.getParcelableExtra(WifiManager.EXTRA_WIFI_INFO);
                        changeState(state, info);
                        Log.d("SUPPLICANTSTATECHANGED", "changeState");
                    } else if (action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
                        NetworkInfo.DetailedState state =
                                ((NetworkInfo) intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO)).getDetailedState();
                        WifiInfo info = intent.getParcelableExtra(WifiManager.EXTRA_WIFI_INFO);
                        Log.d("NETWORKSTATECHANGED", "changeState");
                        changeState(state, info);
                    }
                }

                private void changeState(NetworkInfo.DetailedState aState, WifiInfo info) {
                    //LogUtil.d(LOG_SET, ">>>>>>>>>>>>>>>>>>changeState<<<<<<<<<<<<<<<<"+aState, DEBUG);
                    if (aState == NetworkInfo.DetailedState.SCANNING) {
                        Log.d("wifiSupplicanState", "SCANNING");
                    } else if (aState == NetworkInfo.DetailedState.CONNECTING) {
                        Log.d("wifiSupplicanState", "CONNECTING");
                    } else if (aState == NetworkInfo.DetailedState.OBTAINING_IPADDR) {
                        Log.d("wifiSupplicanState", "OBTAINING_IPADDR");
                    } else if (aState == NetworkInfo.DetailedState.CONNECTED) {
                        Log.d("wifiSupplicanState", "CONNECTED");
                        //Only change the screen if the WiFi connection to the beacon was succeed
                        //Get the BeaconIP address to exchange messages to it.
                        if (info.getSSID().equals(mConf.SSID)) {
                            DhcpInfo dinfo = wifi.getDhcpInfo();
                            Log.i("NETWORKSTATECHANGED", "info: "+ dinfo.toString()+"");

                            if(mBeaconConnectedListener != null) {
                                mBeaconConnectedListener.OnBeaconConnected(intToIp(dinfo.serverAddress));
                            }

                        }
                    } else if (aState == NetworkInfo.DetailedState.DISCONNECTING) {
                        Log.d("wifiSupplicanState", "DISCONNECTING");
                    } else if (aState == NetworkInfo.DetailedState.DISCONNECTED) {
                        Log.d("wifiSupplicanState", "DISCONNECTTED");
                    } else if (aState == NetworkInfo.DetailedState.FAILED) {
                    }
                }

                public void checkState(int aInt) {
                    //LogUtil.d(LOG_SET,"==>>>>>>>>checkState<<<<<<<<"+aInt, DEBUG);
                    if (aInt == WifiManager.WIFI_STATE_ENABLING) {
                        Log.d("WifiManager", "WIFI_STATE_ENABLING");
                    } else if (aInt == WifiManager.WIFI_STATE_ENABLED) {
                        Log.d("WifiManager", "WIFI_STATE_ENABLED");
                    } else if (aInt == WifiManager.WIFI_STATE_DISABLING) {
                        Log.d("WifiManager", "WIFI_STATE_DISABLING");
                    } else if (aInt == WifiManager.WIFI_STATE_DISABLED) {
                        Log.d("WifiManager", "WIFI_STATE_DISABLED");
                    }
                }
            };
        }
        IntentFilter filter = new IntentFilter();

        filter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        filter.addAction(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION);

        getActivity().registerReceiver(mBroadcastConnection, filter);
    }

    private void gotoBeaconFragment() {
        unregisterWiFiConnection();
        ((ScanActivity) getActivity()).changeToNextFragment();
    }

    @SuppressLint("DefaultLocale")
    private String intToIp(int ip) {
        return String.format("%d.%d.%d.%d", (ip & 0xff),
                    (ip >> 8 & 0xff), (ip >> 16 & 0xff),
                    (ip >> 24 & 0xff));
    }

    public void unregisterWiFiConnection() {
        if (mBroadcastConnection != null) {
            getActivity().unregisterReceiver(mBroadcastConnection);
            mBroadcastConnection = null;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_scan_wifi, container, false);

        textStatus = (TextView) rootView.findViewById(R.id.textStatus);
        buttonScan = (Button) rootView.findViewById(R.id.buttonScan);
        lv = (ListView) rootView.findViewById(android.R.id.list);
        final ScanWifiFragment sf = this;

        buttonScan.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                //Uri uri = null;
                //onButtonPressed(uri);
                //arraylist.clear();
                if (mFragmentType == WIFIFRAGMENT) {
                    if (!bScanning) {
                        registerScanResults();
                        buttonScan.setText("Scanning");
                        bScanning = true;
                        wifi.startScan();
                    } else {
                        bScanning = false;
                        buttonScan.setText("Scan");
                        unregisterScanResults();
                    }
                } else if (mFragmentType == BEACONFRAGMENT) {
                    if (!bScanning) {
                        buttonScan.setText("Scanning");
                        bScanning = true;
                        getBeaconWifi();
                    } else {
                        bScanning = false;
                        buttonScan.setText("Scan");
                    }
                }
            }
        });

        //TextView textView = (TextView) rootView.findViewById(R.id.section_label);
        //textView.setText(getString(R.string.section_format, getArguments().getInt(ARG_SECTION_NUMBER)));

        return rootView;
    }

    public void addWiFiList(List<ScanResult> aList) {
        Toast.makeText(getActivity(), "Scanning...." + mAllResults.size(), Toast.LENGTH_SHORT).show();

        HashMap<String, Integer> signalStrength = new HashMap<String, Integer>();
        HashMap<String, String> item;

        try
        {
            mResults.clear();

            for (int i = 0; i < mAllResults.size(); i++) {
                ScanResult result = mAllResults.get(i);
                if (!result.SSID.isEmpty()) {
                    String key = result.SSID; // + " " + result.capabilities;
                    int currentLevel = wifi.calculateSignalLevel(result.level, 5);
                    if (!signalStrength.containsKey(key)) {
                        //stores de SSID into the selected SSID and also its position from
                        //mAllResults to allow us to exchange it if necesary
                        signalStrength.put(key, i);
                        WiFiResult newResult = new WiFiResult(result);
                        //mResults.add(result);
                        mResults.add(newResult);
                    } else {
                        //replaces the same SSID already found by one with a stronger signals
                        int oldPosition = signalStrength.get(key);
                        ScanResult oldResult = mAllResults.get(oldPosition);
                        int oldLevel = wifi.calculateSignalLevel(oldResult.level, 5);

                        if (currentLevel > oldLevel) {
                            WiFiResult newResult = new WiFiResult(result);
                            //mResults.set(oldPosition, result);
                            mResults.set(oldPosition, newResult);
                        }
                    }
                }
            }

            Collections.sort(mResults, new Comparator<WiFiResult>(){
                public int compare(WiFiResult obj1, WiFiResult obj2) {
                    // ## Ascending order
                    //return obj1.firstName.compareToIgnoreCase(obj2.firstName); // To compare string values
                    //return Integer.valueOf(obj1.level).compareTo(obj2.level); // To compare integer values

                    // ## Descending order
                    // return obj2.firstName.compareToIgnoreCase(obj1.firstName); // To compare string values
                    return Integer.valueOf(obj2.level).compareTo(obj1.level); // To compare integer values
                }
            });

            adapter.notifyDataSetChanged();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mScanButtonListener != null) {
            mScanButtonListener.onScanButtonClicked(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnScanButtonListener) {
            mScanButtonListener = (OnScanButtonListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnScanButtonListener");
        }
        if (context instanceof OnBeaconConnectedListener) {
            mBeaconConnectedListener = (OnBeaconConnectedListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnBeaconConnectedListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        unregisterWiFiConnection();
        mScanButtonListener = null;
        mBeaconConnectedListener = null;
    }

    @Override
    public void onStop() {
        super.onStop();
        unregisterWiFiConnection();
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
    public interface OnScanButtonListener {
        // TODO: Update argument type and name
        void onScanButtonClicked(Uri uri);
    }

    /**
     * Everytime the beacon WiFi connection is a success, call this callback
     */
    public interface OnBeaconConnectedListener {
        // TODO: Update argument type and name
        void OnBeaconConnected(String sBeaconIP);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST_CODE_ACCESS_COARSE_LOCATION
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // Do something with granted permission
            wifi.startScan();
        }
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);

        //Use inflater to create a new instance of the Dialog and set the view objects attributes
        LayoutInflater inflater = (LayoutInflater) getActivity().getBaseContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View oDialogView = inflater.inflate(R.layout.dialog_wifi_login, null);

        //Configure TextViews:
        TextView oTextViewSSID = (TextView) oDialogView.findViewById(R.id.textViewSSID);
        final EditText oEditTextPassword = (EditText) oDialogView.findViewById(R.id.editTextPassword);
        final int iWiFiPosition = position;

        final WiFiResult result = mResults.get(iWiFiPosition);
        oTextViewSSID.setText(result.SSID);

        AlertDialog.Builder oDialogBuilder = new AlertDialog.Builder(getActivity());
        oDialogBuilder
                .setTitle("Connect")
                .setView(oDialogView)
                //.setIcon(R.drawable.alert_dialog_icon)
                .setPositiveButton("Ok",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                //((FragmentAlertDialog)getActivity()).doPositiveClick();
                                //Connect to WiFi
                                if (mFragmentType == WIFIFRAGMENT) {
                                    connectToAP(iWiFiPosition, oEditTextPassword.getText().toString());
                                } else if (mFragmentType == BEACONFRAGMENT) {
                                    result.password = oEditTextPassword.getText().toString();
                                    setBeaconWiFi(result);
                                }
                            }
                        }
                )
                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                //((FragmentAlertDialog)getActivity()).doNegativeClick();
                                dialog.cancel();
                            }
                        }
                );

        //Check if the network was previoully created, just connect
        if (wifi != null && mFragmentType == WIFIFRAGMENT) {
            List<WifiConfiguration> list = wifi.getConfiguredNetworks();
            WifiConfiguration conf = null;
            if (list != null & list.size() > 0) {
                for (WifiConfiguration i : list) {
                    if (i.SSID != null && i.SSID.equals("\"" + result.SSID + "\"")) {
                        conf = i;
                    }
                }

                if (conf != null) {
                    connectToConf(conf);
                } else {
                    AlertDialog oAlertDialog = oDialogBuilder.create();
                    oAlertDialog.show();
                }
            }
        } else if (mFragmentType == BEACONFRAGMENT) {
            AlertDialog oAlertDialog = oDialogBuilder.create();
            oAlertDialog.show();
        }
    }

    private void connectToAP(int iWiFiPosition, String networkPass) {

        WiFiResult sc = mResults.get(iWiFiPosition);
        String networkSSID = sc.SSID;
        String type = sc.capabilities;

        WifiConfiguration conf = new WifiConfiguration();
        conf.SSID = "\"" + networkSSID + "\""; // Please note the quotes. String
        // should contain ssid in quotes

        if (type.contains("WEP")) {
            // wep
            conf.wepKeys[0] = "\"" + networkPass + "\"";
            conf.wepTxKeyIndex = 0;
            conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
        } else if (type.contains("WPA")) {
            // wpa
            conf.preSharedKey = "\"" + networkPass + "\"";
        } else if (type.contains("OPEN")) {
            // open
            conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
        }

        wifi.addNetwork(conf);

        List<WifiConfiguration> list = wifi.getConfiguredNetworks();
        for (WifiConfiguration i : list) {
            if (i.SSID != null && i.SSID.equals("\"" + networkSSID + "\"")) {
                connectToConf(i);
                break;
            }
        }
    }

    private void connectToConf(WifiConfiguration conf) {
        wifi.disconnect();

        mConf = conf;
        registerWiFiConnection();

        if(!wifi.enableNetwork(conf.networkId, true)){
            Toast.makeText(getActivity(), "Incorrect Password", Toast.LENGTH_LONG).show();
        }

        wifi.reconnect();
    }

    //Get WiFi List from Beacon
    private void getBeaconWifi() {
        final ScanActivity oParent = (ScanActivity) this.getActivity();

        String requestPath = "http://"+ oParent.mBeaconIP; //Application.loadServerPath();
        requestPath += "/scan";


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
                                JSONArray aJSONWifi = response.getJSONArray("networks");
                                callbackGetBeaconWifi(aJSONWifi);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        bScanning = false;
                        buttonScan.setText("Scan");

                        if (Status == 1) {
                            Toast.makeText(oParent, "Success on Loading Networks", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(oParent, "No networks found", Toast.LENGTH_LONG).show();
                        }
                    }
                },

                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(oParent, "Error on loading networks", Toast.LENGTH_LONG).show();
                        bScanning = false;
                        buttonScan.setText("Scan");
                    }
                }
        );

        // Instantiate the RequestQueue.
        RequestQueue queue = Application.getVolleyRequestQueue();

        // Add the request to the RequestQueue.
        queue.add(JsonRequest);
    }

    private void callbackGetBeaconWifi(JSONArray aJSONWifi) {

        mResults.clear();
        for (int i = 0; i < aJSONWifi.length(); i++) {
            try {
                JSONObject JSONNetwork = aJSONWifi.getJSONObject(i);
                WiFiResult newResult = new WiFiResult();
                newResult.SSID = JSONNetwork.getString("ssid");
                newResult.BSSID = JSONNetwork.getString("bssid");
                newResult.RSSI = JSONNetwork.getInt("rssi");
                newResult.level = newResult.RSSI;
                JSONNetwork.getInt("channel");
                //newResult.capabilities = JSONNetwork.getInt("enc");
                mResults.add(newResult);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        Collections.sort(mResults, new Comparator<WiFiResult>(){
            public int compare(WiFiResult obj1, WiFiResult obj2) {
                // ## Ascending order
                //return obj1.firstName.compareToIgnoreCase(obj2.firstName); // To compare string values
                //return Integer.valueOf(obj1.level).compareTo(obj2.level); // To compare integer values

                // ## Descending order
                // return obj2.firstName.compareToIgnoreCase(obj1.firstName); // To compare string values
                return Integer.valueOf(obj2.level).compareTo(obj1.level); // To compare integer values
            }
        });

        adapter.notifyDataSetChanged();
    }

    private boolean setBeaconWiFi(WiFiResult result) {
        final ScanActivity oParent = (ScanActivity) this.getActivity();
        String requestPath = "http://"+ oParent.mBeaconIP; //Application.loadServerPath();
        requestPath += "/scan";
        final boolean[] bSuccess = {false};

        GsonBuilder builder = new GsonBuilder();
        builder.excludeFieldsWithoutExposeAnnotation();
        Gson gson = builder.create();

        String gsonString = gson.toJson(result);
        JSONObject json = null;

        try {
            json = new JSONObject(gsonString);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        final JSONObject finalJSON = json;

        JsonObjectRequest JsonRequest = new JsonObjectRequest(
                Request.Method.POST,
                requestPath,
                finalJSON,
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
                            Toast.makeText(oParent, "Success on beacon connection", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(oParent, "Error on beacon connection", Toast.LENGTH_LONG).show();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(oParent, "Error on beacon connection", Toast.LENGTH_LONG).show();
                    }
                }
        );

        // Instantiate the RequestQueue.
        RequestQueue queue = Application.getVolleyRequestQueue();

        // Add the request to the RequestQueue.
        queue.add(JsonRequest);

        return bSuccess[0];
    }

    public class WiFiResult {
        @Expose(serialize = true)
        @SerializedName("ssid")
        public String SSID;
        @Expose(serialize = true)
        public String BSSID;
        @Expose(serialize = true)
        public String capabilities;
        @Expose(serialize = true)
        public int RSSI;
        @Expose(serialize = true)
        public int level;
        @Expose(serialize = true)
        public String password;

        public WiFiResult() {

        }

        public WiFiResult(ScanResult result) {
            this.SSID = result.SSID;
            this.BSSID = result.BSSID;
            this.RSSI = result.level;
            this.capabilities = result.capabilities;
            this.level = result.level;
        }

    }
}
