package net.unitecgroup.www.unitecrfid;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.MifareClassic;
import android.nfc.tech.MifareUltralight;
import android.nfc.tech.Ndef;
import android.nfc.tech.NfcA;
import android.nfc.tech.NfcV;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.util.ArrayList;

import static android.R.attr.name;

/**
 * https://code.tutsplus.com/tutorials/reading-nfc-tags-with-android--mobile-17278
 */
public class MainActivity extends BaseActivity {

    private static final String ITEM_LIST = "TAGS";
    public static final String TAGBLINK = "TAGBLINK";
    private boolean mResume = true;

    private AlertDialog mEnableNfc;
    //private Button mReadTag;
    //private Button mWriteTag;

    private NfcAdapter mNfcAdapter;
    private Intent mOldIntent = null;


    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    //private HashSet<String> myDataset;
    private ArrayList<String> myDataset;

    private DataDevice mDataDevice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setTitle("Main");

        overridePendingTransition(0, 0);

        if (savedInstanceState == null) {
            //myDataset = new HashSet<>();
            myDataset = new ArrayList<>();
        } else {
            myDataset = savedInstanceState.getStringArrayList(ITEM_LIST);
            //myDataset = (HashSet<String>) savedInstanceState.getSerializable(ITEM_LIST);
        }
        mRecyclerView = (RecyclerView) findViewById(R.id.mainListView);

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        mRecyclerView.setHasFixedSize(true);

        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);

        // specify an adapter (see also next example)
        mAdapter = new TagListAdapter(myDataset);
        mRecyclerView.setAdapter(mAdapter);

        setRecyclerViewItemTouchListener();
        setRecyclerViewItemAnimator();

        // Check if there is an NFC hardware component.
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        if (mNfcAdapter == null || !mNfcAdapter.isEnabled()) {
            createNfcEnableDialog();
            mEnableNfc.show();
            mResume = false;
        }

        if (mNfcAdapter != null && mNfcAdapter.isEnabled()) {
            handleIntent(getIntent());
        }
    }

    private void setRecyclerViewItemTouchListener() {

        //1
        ItemTouchHelper.SimpleCallback itemTouchCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder viewHolder1) {
                //2
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int swipeDir) {
                //3
                final int position = viewHolder.getAdapterPosition();
                final String item = myDataset.get(position);

                Snackbar.make(viewHolder.itemView, name + " was removed", Snackbar.LENGTH_INDEFINITE).setAction("UNDO", new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        myDataset.add(position, item);
                        mAdapter.notifyItemInserted(position);
                        mAdapter.notifyDataSetChanged();
                    }
                }).show();
                myDataset.remove(position);
                mRecyclerView.getAdapter().notifyItemRemoved(position);
            }
        };

        //4
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(itemTouchCallback);
        itemTouchHelper.attachToRecyclerView(mRecyclerView);
    }

    private void setRecyclerViewItemAnimator() {
        mRecyclerView.setItemAnimator(new TagItemAnimator());
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        //outState.putStringArrayList(ITEM_LIST, myDataset);
        outState.putSerializable(ITEM_LIST, myDataset);
        super.onSaveInstanceState(outState);
    }

    // This callback is called only when there is a saved instance previously saved using
    // onSaveInstanceState(). We restore some state in onCreate() while we can optionally restore
    // other state here, possibly usable after onStart() has completed.
    // The savedInstanceState Bundle is same as the one used in onCreate().
    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        myDataset = savedInstanceState.getStringArrayList(ITEM_LIST);
    }

    /**
     * Create a dialog that send user to NFC settings if NFC is off (and save
     * the dialog in {@link #mEnableNfc}). Alternatively the user can choos to
     * use the App in editor only mode or exit the App.
     */
    private void createNfcEnableDialog() {
        mEnableNfc = new AlertDialog.Builder(this)
                .setTitle("NFC not enabled")
                .setMessage("NFC adapter is not enabled. Please go to your Settings and enable it")
                .setIcon(android.R.drawable.ic_dialog_info)
                .setPositiveButton("Goto NFC Settings",
                        new DialogInterface.OnClickListener() {
                            @Override
                            @SuppressLint("InlinedApi")
                            public void onClick(DialogInterface dialog, int which) {
                                // Goto NFC Settings.
                                if (Build.VERSION.SDK_INT >= 16) {
                                    startActivity(new Intent(
                                            Settings.ACTION_NFC_SETTINGS));
                                } else {
                                    startActivity(new Intent(
                                            Settings.ACTION_WIRELESS_SETTINGS));
                                }
                            }
                        })
                .setNeutralButton("Use App in Offline mode",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // Only use Editor.
                                //Common.setUseAsEditorOnly(true);
                            }
                        })
                .setNegativeButton("Exit App",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                // Exit the App.
                                finish();
                            }
                        }).create();
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

    //Used to handle the Application Navigation
    @Override
    protected int getNavigationDrawerID() {
        return R.id.nav_home;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    public void handleIntent(Intent intent) {
        Tag tag = null;

        Log.d("MAIN", "onNewIntent: "+intent.getAction());
        String action = intent.getAction();
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action) || NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)) {
            tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            //readTagData(tag);

            mDataDevice = new DataDevice(); //(DataDevice) this.getApplication();
            mDataDevice.setCurrentTag(tag);

            byte[] GetSystemInfoAnswer = NFCCommand.SendGetSystemInfoCommandCustom(tag, mDataDevice);

            if(DecodeGetSystemInfoResponse(GetSystemInfoAnswer))
            {

            }

        }

        if(tag != null) {

            // Show Toast message with UID.
            String sUID = "UID: ";
            sUID += byte2HexString(tag.getId());

            if (!myDataset.contains(sUID)) {
                String id = "New NFC (" + sUID + ")";
                Toast.makeText(this, id, Toast.LENGTH_LONG).show();

                myDataset.add(sUID);
                mAdapter.notifyItemInserted(myDataset.size() - 1);
            } else {
                int index = myDataset.indexOf(sUID);
                mAdapter.notifyItemChanged(index, TAGBLINK);
            }

            /*
            if (isDialogDisplayed) {

                if (isWrite) {

                    String messageToWrite = mEtMessage.getText().toString();
                    mNfcWriteFragment = (NFCWriteFragment) getFragmentManager().findFragmentByTag(NFCWriteFragment.TAG);
                    mNfcWriteFragment.onNfcDetected(ndef,messageToWrite);

                } else {

                    mNfcReadFragment = (NFCReadFragment)getFragmentManager().findFragmentByTag(NFCReadFragment.TAG);
                    mNfcReadFragment.onNfcDetected(ndef);
                }
            }
            */
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        checkNfc();
    }

    @Override
    public void onPause() {
        super.onPause();

        if (mNfcAdapter != null)
            mNfcAdapter.disableForegroundDispatch(this);
    }

    /**
     * Check if NFC adapter is enabled. If not, show the user a dialog and let
     * him choose between "Goto NFC Setting", "Use Editor Only" and "Exit App".
     * Also enable NFC foreground dispatch system.
     */
    private void checkNfc() {
        // Check if the NFC hardware is enabled.
        if (mNfcAdapter != null) {
            if (!mNfcAdapter.isEnabled()) {
                // NFC is disabled.
                createNfcEnableDialog();
            } else {
                // NFC is enabled. Hide dialog and enable NFC
                // foreground dispatch.
                if (mOldIntent != getIntent()) {
                    int typeCheck = -1; //treatAsNewTag(getIntent(), this);
                    if (typeCheck == -1 || typeCheck == -2) {
                        // Device or tag does not support MIFARE Classic.
                        // Run the only thing that is possible: The tag info tool.
                        //TODO
                        //Intent i = new Intent(this, TagInfoTool.class);
                        //startActivity(i);
                    }
                    mOldIntent = getIntent();
                }
                enableNfcForegroundDispatch(this);
                //Common.setUseAsEditorOnly(false);
                if (mEnableNfc == null) {
                    createNfcEnableDialog();
                }
                mEnableNfc.hide();

            }
        }
    }

    /**
     * Enables the NFC foreground dispatch system for the given Activity.
     * @param targetActivity The Activity that is in foreground and wants to
     * have NFC Intents.
     */
    public void enableNfcForegroundDispatch(Activity targetActivity) {
        if (mNfcAdapter != null && mNfcAdapter.isEnabled()) {

            Intent intent = new Intent(targetActivity,
                    targetActivity.getClass()).addFlags(
                    Intent.FLAG_ACTIVITY_SINGLE_TOP);
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    targetActivity, 0, intent, 0);

            // Notice that this is the same filter as in our manifest.
            IntentFilter[] filters = new IntentFilter[1];
            filters[0] = new IntentFilter();
            filters[0].addAction(NfcAdapter.ACTION_NDEF_DISCOVERED);
            filters[0].addCategory(Intent.CATEGORY_DEFAULT);
            filters = null;

            mNfcAdapter.enableForegroundDispatch(
                    targetActivity, pendingIntent, filters, mTechList);
        }
    }




    private void readTagData(Tag tag) {
        byte[] id = tag.getId();
        boolean techFound = false;

        for (String tech : tag.getTechList()) {

            if (tech.equals(NfcV.class.getName())) {
                // Get an instance of NfcV for the given tag:
                NfcV nfcvTag = NfcV.get(tag);
                techFound = true;
            } else if (tech.equals(NfcA.class.getName())) {
                NfcA nfca = NfcA.get(tag);
                byte[] atqa = nfca.getAtqa();
                techFound = true;
            } else if (tech.equals(Ndef.class.getName())) {
                Ndef ndef = Ndef.get(tag);
                techFound = true;
            } else if (tech.equals(MifareUltralight.class.getName())) {
                MifareUltralight multra = MifareUltralight.get(tag);
                techFound = true;
            } else if (tech.equals(MifareClassic.class.getName())) {
                MifareClassic mclassic = MifareClassic.get(tag);
                techFound = true;
            }
        }

        if (techFound == false) {
            //showMessage(R.string.error, R.string.unknown_tech);
        }

    }

    /**
     * Convert an array of bytes into a string of hex values.
     * @param bytes Bytes to convert.
     * @return The bytes in hex string format.
     */
    public static String byte2HexString(byte[] bytes) {
        String ret = "";
        if (bytes != null) {
            for (Byte b : bytes) {
                ret += String.format("%02X", b.intValue() & 0xFF);
            }
        }
        return ret;
    }


    //***********************************************************************/
    //* the function Decode the tag answer for the GetSystemInfo command
    //* the function fills the values (dsfid / afi / memory size / icRef /..)
    //* in the myApplication class. return true if everything is ok.
    //***********************************************************************/
    public boolean DecodeGetSystemInfoResponse (byte[] GetSystemInfoResponse)
    {
        DataDevice ma = mDataDevice; //(DataDevice)getApplication();
        //if the tag has returned a good response
        if(GetSystemInfoResponse[0] == (byte) 0x00 && GetSystemInfoResponse.length >= 12)
        {
            //DataDevice ma = (DataDevice)getApplication();
            String uidToString = "";
            byte[] uid = new byte[8];
            // change uid format from byteArray to a String
            for (int i = 1; i <= 8; i++)
            {
                uid[i - 1] = GetSystemInfoResponse[10 - i];
                uidToString += Helper.ConvertHexByteToString(uid[i - 1]);
            }

            //***** TECHNO ******
            ma.setUid(uidToString);
            if(uid[0] == (byte) 0xE0)
                ma.setTechno("ISO 15693");
            else if (uid[0] == (byte) 0xD0)
                ma.setTechno("ISO 14443");
            else
                ma.setTechno("Unknown techno");

            //***** MANUFACTURER ****
            if(uid[1]== (byte) 0x02)
                ma.setManufacturer("STMicroelectronics");
            else if(uid[1]== (byte) 0x04)
                ma.setManufacturer("NXP");
            else if(uid[1]== (byte) 0x07)
                ma.setManufacturer("Texas Instruments");
            else if (uid[1] == (byte) 0x01) //MOTOROLA (updated 20140228)
                ma.setManufacturer("Motorola");
            else if (uid[1] == (byte) 0x03) //HITASHI (updated 20140228)
                ma.setManufacturer("Hitachi");
            else if (uid[1] == (byte) 0x04) //NXP SEMICONDUCTORS
                ma.setManufacturer("NXP");
            else if (uid[1] == (byte) 0x05) //INFINEON TECHNOLOGIES (updated 20140228)
                ma.setManufacturer("Infineon");
            else if (uid[1] == (byte) 0x06) //CYLINC (updated 20140228)
                ma.setManufacturer("Cylinc");
            else if (uid[1] == (byte) 0x07) //TEXAS INSTRUMENTS TAG-IT
                ma.setManufacturer("Texas Instruments");
            else if (uid[1] == (byte) 0x08) //FUJITSU LIMITED (updated 20140228)
                ma.setManufacturer("Fujitsu");
            else if (uid[1] == (byte) 0x09) //MATSUSHITA ELECTRIC INDUSTRIAL (updated 20140228)
                ma.setManufacturer("Matsushita");
            else if (uid[1] == (byte) 0x0A) //NEC (updated 20140228)
                ma.setManufacturer("NEC");
            else if (uid[1] == (byte) 0x0B) //OKI ELECTRIC (updated 20140228)
                ma.setManufacturer("Oki");
            else if (uid[1] == (byte) 0x0C) //TOSHIBA (updated 20140228)
                ma.setManufacturer("Toshiba");
            else if (uid[1] == (byte) 0x0D) //MITSUBISHI ELECTRIC (updated 20140228)
                ma.setManufacturer("Mitsubishi");
            else if (uid[1] == (byte) 0x0E) //SAMSUNG ELECTRONICS (updated 20140228)
                ma.setManufacturer("Samsung");
            else if (uid[1] == (byte) 0x0F) //HUYNDAI ELECTRONICS (updated 20140228)
                ma.setManufacturer("Hyundai");
            else if (uid[1] == (byte) 0x10) //LG SEMICONDUCTORS (updated 20140228)
                ma.setManufacturer("LG");
            else
                ma.setManufacturer("Unknown manufacturer");

            if(uid[1]== (byte) 0x02)
            {
                //**** PRODUCT NAME *****
                if(uid[2] >= (byte) 0x04 && uid[2] <= (byte) 0x07)
                {
                    ma.setProductName("LRI512");
                    ma.setMultipleReadSupported(false);
                    ma.setMemoryExceed2048bytesSize(false);
                }
                else if(uid[2] >= (byte) 0x14 && uid[2] <= (byte) 0x17)
                {
                    ma.setProductName("LRI64");
                    ma.setMultipleReadSupported(false);
                    ma.setMemoryExceed2048bytesSize(false);
                }
                else if(uid[2] >= (byte) 0x20 && uid[2] <= (byte) 0x23)
                {
                    ma.setProductName("LRI2K");
                    ma.setMultipleReadSupported(true);
                    ma.setMemoryExceed2048bytesSize(false);
                }
                else if(uid[2] >= (byte) 0x28 && uid[2] <= (byte) 0x2B)
                {
                    ma.setProductName("LRIS2K");
                    ma.setMultipleReadSupported(false);
                    ma.setMemoryExceed2048bytesSize(false);
                }
                else if(uid[2] >= (byte) 0x2C && uid[2] <= (byte) 0x2F)
                {
                    ma.setProductName("M24LR64");
                    ma.setMultipleReadSupported(true);
                    ma.setMemoryExceed2048bytesSize(true);
                }
                else if(uid[2] >= (byte) 0x40 && uid[2] <= (byte) 0x43)
                {
                    ma.setProductName("LRI1K");
                    ma.setMultipleReadSupported(true);
                    ma.setMemoryExceed2048bytesSize(false);
                }
                else if(uid[2] >= (byte) 0x44 && uid[2] <= (byte) 0x47)
                {
                    ma.setProductName("LRIS64K");
                    ma.setMultipleReadSupported(true);
                    ma.setMemoryExceed2048bytesSize(true);
                }
                else if(uid[2] >= (byte) 0x48 && uid[2] <= (byte) 0x4B)
                {
                    ma.setProductName("M24LR01E");
                    ma.setMultipleReadSupported(true);
                    ma.setMemoryExceed2048bytesSize(false);
                }
                else if(uid[2] >= (byte) 0x4C && uid[2] <= (byte) 0x4F)
                {
                    ma.setProductName("M24LR16E");
                    ma.setMultipleReadSupported(true);
                    ma.setMemoryExceed2048bytesSize(true);
                    if(ma.isBasedOnTwoBytesAddress() == false)
                        return false;
                }
                else if(uid[2] >= (byte) 0x50 && uid[2] <= (byte) 0x53)
                {
                    ma.setProductName("M24LR02E");
                    ma.setMultipleReadSupported(true);
                    ma.setMemoryExceed2048bytesSize(false);
                }
                else if(uid[2] >= (byte) 0x54 && uid[2] <= (byte) 0x57)
                {
                    ma.setProductName("M24LR32E");
                    ma.setMultipleReadSupported(true);
                    ma.setMemoryExceed2048bytesSize(true);
                    if(ma.isBasedOnTwoBytesAddress() == false)
                        return false;
                }
                else if(uid[2] >= (byte) 0x58 && uid[2] <= (byte) 0x5B)
                {
                    ma.setProductName("M24LR04E");
                    ma.setMultipleReadSupported(true);
                    ma.setMemoryExceed2048bytesSize(true);
                }
                else if(uid[2] >= (byte) 0x5C && uid[2] <= (byte) 0x5F)
                {
                    ma.setProductName("M24LR64E");
                    ma.setMultipleReadSupported(true);
                    ma.setMemoryExceed2048bytesSize(true);
                    if(ma.isBasedOnTwoBytesAddress() == false)
                        return false;
                }
                else if(uid[2] >= (byte) 0x60 && uid[2] <= (byte) 0x63)
                {
                    ma.setProductName("M24LR08E");
                    ma.setMultipleReadSupported(true);
                    ma.setMemoryExceed2048bytesSize(true);
                }
                else if(uid[2] >= (byte) 0x64 && uid[2] <= (byte) 0x67)
                {
                    ma.setProductName("M24LR128E");
                    ma.setMultipleReadSupported(true);
                    ma.setMemoryExceed2048bytesSize(true);
                    if(ma.isBasedOnTwoBytesAddress() == false)
                        return false;
                }
                else if(uid[2] >= (byte) 0x6C && uid[2] <= (byte) 0x6F)
                {
                    ma.setProductName("M24LR256E");
                    ma.setMultipleReadSupported(true);
                    ma.setMemoryExceed2048bytesSize(true);
                    if(ma.isBasedOnTwoBytesAddress() == false)
                        return false;
                }
                else if(uid[2] >= (byte) 0xF8 && uid[2] <= (byte) 0xFB)
                {
                    ma.setProductName("detected product");
                    ma.setBasedOnTwoBytesAddress(true);
                    ma.setMultipleReadSupported(true);
                    ma.setMemoryExceed2048bytesSize(true);
                }
                else
                {
                    ma.setProductName("Unknown product");
                    ma.setBasedOnTwoBytesAddress(false);
                    ma.setMultipleReadSupported(false);
                    ma.setMemoryExceed2048bytesSize(false);
                }

                //*** DSFID ***
                ma.setDsfid(Helper.ConvertHexByteToString(GetSystemInfoResponse[10]));

                //*** AFI ***
                ma.setAfi(Helper.ConvertHexByteToString(GetSystemInfoResponse[11]));

                //*** MEMORY SIZE ***
                if(ma.isBasedOnTwoBytesAddress())
                {
                    String temp = new String();
                    temp += Helper.ConvertHexByteToString(GetSystemInfoResponse[13]);
                    temp += Helper.ConvertHexByteToString(GetSystemInfoResponse[12]);
                    ma.setMemorySize(temp);
                }
                else
                    ma.setMemorySize(Helper.ConvertHexByteToString(GetSystemInfoResponse[12]));

                //*** BLOCK SIZE ***
                if(ma.isBasedOnTwoBytesAddress())
                    ma.setBlockSize(Helper.ConvertHexByteToString(GetSystemInfoResponse[14]));
                else
                    ma.setBlockSize(Helper.ConvertHexByteToString(GetSystemInfoResponse[13]));

                //*** IC REFERENCE ***
                if(ma.isBasedOnTwoBytesAddress())
                    ma.setIcReference(Helper.ConvertHexByteToString(GetSystemInfoResponse[15]));
                else
                    ma.setIcReference(Helper.ConvertHexByteToString(GetSystemInfoResponse[14]));
            }
            else
            {
                ma.setProductName("Unknown product");
                ma.setBasedOnTwoBytesAddress(false);
                ma.setMultipleReadSupported(false);
                ma.setMemoryExceed2048bytesSize(false);
                //ma.setAfi("00 ");
                ma.setAfi(Helper.ConvertHexByteToString(GetSystemInfoResponse[11]));				//changed 22-10-2014
                //ma.setDsfid("00 ");
                ma.setDsfid(Helper.ConvertHexByteToString(GetSystemInfoResponse[10]));				//changed 22-10-2014
                //ma.setMemorySize("FF ");
                ma.setMemorySize(Helper.ConvertHexByteToString(GetSystemInfoResponse[12]));		//changed 22-10-2014
                //ma.setBlockSize("03 ");
                ma.setBlockSize(Helper.ConvertHexByteToString(GetSystemInfoResponse[13]));			//changed 22-10-2014
                //ma.setIcReference("00 ");
                ma.setIcReference(Helper.ConvertHexByteToString(GetSystemInfoResponse[14]));		//changed 22-10-2014
            }

            return true;
        }

        // in case of Inventory OK and Get System Info HS
        else if (ma.getTechno() == "ISO 15693")
        {
            ma.setProductName("Unknown product");
            ma.setBasedOnTwoBytesAddress(false);
            ma.setMultipleReadSupported(false);
            ma.setMemoryExceed2048bytesSize(false);
            ma.setAfi("00 ");
            ma.setDsfid("00 ");
            ma.setMemorySize("3F ");				//changed 22-10-2014
            ma.setBlockSize("03 ");
            ma.setIcReference("00 ");
            return true;
        }

        //if the tag has returned an error code
        else
            return false;

    }


}
