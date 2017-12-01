package net.unitecgroup.www.unitecrfid;

import android.Manifest;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
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
    List<ScanResult> mAllResults;
    List<ScanResult> mResults;

    String ITEM_KEY = "key";
    ArrayList<HashMap<String, String>> arraylist;
    SimpleAdapter adapter;

    private static final int PERMISSIONS_REQUEST_CODE_ACCESS_COARSE_LOCATION = 1001;

    private OnScanButtonListener mListener;


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

        this.adapter = new SimpleAdapter(getActivity(), arraylist, R.layout.listview_scan_wifi_row, new String[] { ITEM_KEY }, new int[] { R.id.list_value });
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
            wifi.startScan();
        }
    }

    private void registerScanResults() {
        this.getActivity().registerReceiver(new BroadcastReceiver()
        {
            @Override
            public void onReceive(Context c, Intent intent)
            {
                mAllResults = wifi.getScanResults();

                buttonScan.setText("Scan");

                getActivity().unregisterReceiver(this);
                addWiFiList(mAllResults);
            }
        }, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_scan_wifi, container, false);

        textStatus = (TextView) rootView.findViewById(R.id.textStatus);
        buttonScan = (Button) rootView.findViewById(R.id.buttonScan);
        lv = (ListView) rootView.findViewById(android.R.id.list);

        buttonScan.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                //Uri uri = null;
                //onButtonPressed(uri);
                //arraylist.clear();
                registerScanResults();

                buttonScan.setText("Scanning");
                wifi.startScan();
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
            mResults = new ArrayList<>();

            for (int i = 0; i < mAllResults.size(); i++) {
                ScanResult result = mAllResults.get(i);
                if (!result.SSID.isEmpty()) {
                    String key = result.SSID + " " + result.capabilities;
                    int CurrentLevel = wifi.calculateSignalLevel(result.level, 5);
                    if (!signalStrength.containsKey(key)) {
                        signalStrength.put(key, i);

                        item = new HashMap<String, String>();
                        item.put(ITEM_KEY, result.SSID + "  " + result.capabilities);
                        arraylist.add(item);

                        adapter.notifyDataSetChanged();
                    } else {
                        //replaces the same SSID already found by one with a stronger signals
                        int position = signalStrength.get(key);
                        ScanResult updateItem = mAllResults.get(position);
                        int OldLevel = wifi.calculateSignalLevel(updateItem.level, 5);

                        if (OldLevel > CurrentLevel) {
                            item = new HashMap<String, String>();
                            item.put(ITEM_KEY, updateItem.SSID + "  " + updateItem.capabilities);
                            arraylist.set(position, item);
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

        HashMap<String, String> item = arraylist.get(iWiFiPosition);
        oTextViewSSID.setText(item.get(ITEM_KEY));

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

        AlertDialog oAlertDialog = oDialogBuilder.create();
        oAlertDialog.show();
    }

    private void connectToAP(int iWiFiPosition, String password) {

        HashMap<String, String> item = arraylist.get(iWiFiPosition);

        String myNetworksSSID = item.get(ITEM_KEY);

        for (WifiConfiguration config : wifi.getConfiguredNetworks()) {
            String newSSID = config.SSID;

            if (myNetworksSSID.equals(newSSID)) {
                wifi.disconnect();
                wifi.enableNetwork(config.networkId, true);
                wifi.reconnect();

                return;
            }
        }

        //It didn't find the AP Network already register into the smartphone, create
        WifiConfiguration conf = new WifiConfiguration();
        conf.SSID = "\"" + myNetworksSSID + "\"";
        conf.preSharedKey = "\""+ password +"\"";
        wifi.addNetwork(conf);

    }
}
