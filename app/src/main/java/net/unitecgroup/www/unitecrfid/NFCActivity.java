package net.unitecgroup.www.unitecrfid;

import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.tech.Ndef;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.IOException;
import java.nio.charset.Charset;

public class NFCActivity extends AppCompatActivity {

    public static final String TAG = "NFCTAG" ;
    ListView oTagsList;

    TextView mTvMessage;
    ProgressBar mProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nfc);

        oTagsList = (ListView) findViewById(R.id.tagsList);
    }

    private void readFromNFC(Ndef ndef) {

        try {
            ndef.connect();
            NdefMessage ndefMessage = ndef.getNdefMessage();
            String message = new String(ndefMessage.getRecords()[0].getPayload());
            Log.d(TAG, "readFromNFC: "+message);
            mTvMessage.setText(message);
            ndef.close();

        } catch (IOException | FormatException e) {
            e.printStackTrace();

        }
    }

    private void writeToNfc(Ndef ndef, String message){

        mTvMessage.setText("Writing NFC");
        if (ndef != null) {

            try {
                ndef.connect();
                NdefRecord mimeRecord = NdefRecord.createMime("text/plain", message.getBytes(Charset.forName("US-ASCII")));
                ndef.writeNdefMessage(new NdefMessage(mimeRecord));
                ndef.close();
                //Write Successful
                mTvMessage.setText("Success");

            } catch (IOException | FormatException e) {
                e.printStackTrace();
                mTvMessage.setText("Error");

            } finally {
                mProgress.setVisibility(View.GONE);
            }

        }
    }

}
