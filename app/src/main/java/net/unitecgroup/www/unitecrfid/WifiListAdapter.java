package net.unitecgroup.www.unitecrfid;

import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

/**
 * Created by joaocarlosss on 02/12/2017.
 * https://www.journaldev.com/10416/android-listview-with-custom-adapter-example-tutorial
 */

public class WifiListAdapter extends ArrayAdapter<ScanWifiFragment.WiFiResult> {

    WifiManager mWifi;

    public WifiListAdapter(@NonNull Context context, int resource) {
        super(context, resource);
    }

    public WifiListAdapter(@NonNull Context context, int resource, @NonNull List<ScanWifiFragment.WiFiResult> objects, WifiManager wifi) {
        super(context, resource, objects);
        mWifi = wifi;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        //return super.getView(position, convertView, parent);
        View v = convertView;

        if (v == null) {
            LayoutInflater vi;
            vi = LayoutInflater.from(getContext());
            v = vi.inflate(R.layout.listview_scan_wifi_row, null);
        }

        //ScanResult sc = getItem(position);
        ScanWifiFragment.WiFiResult sc = getItem(position);

        if (sc != null) {
            TextView tt1 = (TextView) v.findViewById(R.id.ssid);
            ImageView iv = (ImageView) v.findViewById(R.id.signal);

            if (tt1 != null) {
                tt1.setText(sc.SSID);
            }

            if (iv != null) {
                int iLevel = mWifi.calculateSignalLevel(sc.level, 5);
                switch (iLevel) {
                    case 1:
                        iv.setImageResource(R.drawable.ic_signal_wifi_1_bar_black_24dp);
                        break;
                    case 2:
                        iv.setImageResource(R.drawable.ic_signal_wifi_2_bar_black_24dp);
                        break;
                    case 3:
                        iv.setImageResource(R.drawable.ic_signal_wifi_3_bar_black_24dp);
                        break;
                    case 4:
                        iv.setImageResource(R.drawable.ic_signal_wifi_4_bar_black_24dp);
                        break;
                    default:
                        iv.setImageResource(R.drawable.ic_signal_wifi_1_bar_black_24dp);
                        break;
                }
            }
        }

        return v;
    }
}
