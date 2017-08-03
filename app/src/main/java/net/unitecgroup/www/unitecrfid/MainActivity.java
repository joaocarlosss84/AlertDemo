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
import android.support.v7.widget.DefaultItemAnimator;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setTitle("Main");

        overridePendingTransition(0, 0);

        if (savedInstanceState == null) {
            //myDataset = new HashSet<>();
            myDataset = new ArrayList<>();
            /*
            for (int i = 0; i < 3; i++) {
                myDataset.add("row " + i);
            }
            */
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
        if (mNfcAdapter == null || !mNfcAdapter.isEnabled()) {
            createNfcEnableDialog();
            mEnableNfc.show();
            mResume = false;
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
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
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
        handleIntent(intent);
    }

    public void handleIntent(Intent intent) {
        Tag tag = null;

        Log.d("MAIN", "onNewIntent: "+intent.getAction());
        String action = intent.getAction();
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action) || NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)) {
            tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            readTagData(tag);
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
                mAdapter.notifyItemChanged(index);
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


            String[][] techList = new String[][]{
                    new String[] { NfcA.class.getName() }
            };
            techList = null;

            // Notice that this is the same filter as in our manifest.
            IntentFilter[] filters = new IntentFilter[1];
            filters[0] = new IntentFilter();
            filters[0].addAction(NfcAdapter.ACTION_NDEF_DISCOVERED);
            filters[0].addCategory(Intent.CATEGORY_DEFAULT);
            filters = null;

            mNfcAdapter.enableForegroundDispatch(
                    targetActivity, pendingIntent, filters, techList);
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


}
