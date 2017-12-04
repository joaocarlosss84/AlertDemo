package net.unitecgroup.www.unitecrfid;

import android.Manifest;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
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
 */
public class ScanWifiFragment extends ListFragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    WifiManager wifi;
    ListView lv;
    TextView textStatus;
    Button buttonScan;
    Boolean bScanning = true;

    List<ScanResult> mAllResults;
    List<ScanResult> mResults;

    String ITEM_KEY = "key";
    ArrayList<HashMap<String, String>> arraylist;
    //SimpleAdapter adapter;
    WifiListAdapter adapter;

    private static final int PERMISSIONS_REQUEST_CODE_ACCESS_COARSE_LOCATION = 1001;

    private OnScanButtonListener mListener;
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
    public static ScanWifiFragment newInstance(String param1, String param2) {
        ScanWifiFragment fragment = new ScanWifiFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        arraylist = new ArrayList<HashMap<String, String>>();
        HashMap<String, String> item = new HashMap<String, String>();
        item.put(ITEM_KEY, "TEST SSID 0");
        arraylist.add(item);

        mResults = new ArrayList<>();

        //this.adapter = new SimpleAdapter(getActivity(), arraylist, R.layout.listview_scan_wifi_row, new String[] { ITEM_KEY }, new int[] { R.id.list_value });
        this.adapter = new WifiListAdapter(getActivity(), R.layout.listview_scan_wifi_row, mResults, wifi);
        this.setListAdapter(this.adapter);

        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }

        wifi = (WifiManager) this.getActivity().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifi.isWifiEnabled() == false)
        {
            //Toast.makeText(getApplicationContext(), "wifi is disabled..making it enabled", Toast.LENGTH_LONG).show();
            Toast.makeText(getContext(), "wifi is disabled..making it enabled", Toast.LENGTH_LONG).show();
            wifi.setWifiEnabled(true);
        }

        registerScanResults();

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && getActivity().checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    PERMISSIONS_REQUEST_CODE_ACCESS_COARSE_LOCATION);
            //After this point you wait for callback in onRequestPermissionsResult(int, String[], int[]) overriden method
        } else {
            //Already Granted
            buttonScan.setText("Scanning");
            bScanning = true;
            wifi.startScan();
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
        mBroadcastConnection = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();

                if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(action)) {

                    NetworkInfo info = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);

                    switch (info.getState()) {
                        case CONNECTING:
                            break;
                        case CONNECTED:
                            break;
                        case DISCONNECTING:
                            break;
                        case DISCONNECTED:
                            break;
                        case SUSPENDED:
                            break;
                    }
                }
            }
        };
        getActivity().registerReceiver(mBroadcastConnection, new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION));
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
            arraylist.clear();
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
                        mResults.add(result);
                        adapter.notifyDataSetChanged();
                    } else {
                        //replaces the same SSID already found by one with a stronger signals
                        int oldPosition = signalStrength.get(key);
                        ScanResult oldResult = mAllResults.get(oldPosition);
                        int oldLevel = wifi.calculateSignalLevel(oldResult.level, 5);

                        if (currentLevel > oldLevel) {
                            mResults.set(oldPosition, result);
                            adapter.notifyDataSetChanged();
                        }
                    }
                }
            }

            //adapter.notifyDataSetChanged();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onScanButtonClicked(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnScanButtonListener) {
            mListener = (OnScanButtonListener) context;
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

        ScanResult sc = mResults.get(iWiFiPosition);
        oTextViewSSID.setText(sc.SSID);

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
                                connectToAP(iWiFiPosition, oEditTextPassword.getText().toString());
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
        List<WifiConfiguration> list = wifi.getConfiguredNetworks();
        WifiConfiguration conf = null;
        for (WifiConfiguration i : list) {
            if (i.SSID != null && i.SSID.equals("\"" + sc.SSID + "\"")) {
                conf = i;
            }
        }

        if (conf != null) {
            connectToConf(i);
        } else {
            AlertDialog oAlertDialog = oDialogBuilder.create();
            oAlertDialog.show();
        }
    }

    private void connectToAP(int iWiFiPosition, String networkPass) {

        registerWiFiConnection();

        ScanResult sc = mResults.get(iWiFiPosition);
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

        if(!wifi.enableNetwork(conf.networkId, true)){
            Toast.makeText(getActivity(), "Incorrect Password", Toast.LENGTH_LONG).show();
        }

        wifi.reconnect();
    }
}
