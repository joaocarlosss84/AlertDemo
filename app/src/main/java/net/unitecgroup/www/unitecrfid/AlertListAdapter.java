package net.unitecgroup.www.unitecrfid;

import android.graphics.Color;
import android.os.Handler;
import android.support.v7.widget.RecyclerView;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by 20006030 on 05/06/2017.
 *
 *
 */

class AlertListAdapter extends RecyclerView.Adapter {
    private static final int PENDING_REMOVAL_TIMEOUT = 3000; // 3sec

    ArrayList<Alert> items;
    ArrayList<Integer> itemsPendingRemoval;
    private boolean undoOn = false; // is undo on, you can turn it on from the toolbar menu

    private Handler handler = new Handler(); // hanlder for running delayed runnables
    // map of items to pending runnables, so we can cancel a removal if need be
    private SparseArray<Runnable> pendingRunnables = new SparseArray<>();

    //OnItemLongClickListener mItemClickListener;
    private OnItemClickListener mItemClickListener;
    private OnItemDeletedListener mItemDeletedListener;


    public AlertListAdapter(OnItemDeletedListener callback) {
        this.mItemDeletedListener = callback;
        items = new ArrayList<>();
        itemsPendingRemoval = new ArrayList<>();
        setHasStableIds(true);
    }

    public AlertListAdapter(OnItemDeletedListener callback, ArrayList<Alert> alerts) {
        this.mItemDeletedListener = callback;
        items = alerts;
        itemsPendingRemoval = new ArrayList<>();
        setHasStableIds(true);
    }


    public void addAlert(Alert oAlert) {
        try {
            items.add(oAlert);
            notifyItemInserted(items.size() - 1);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public long getItemId(int position) {
        Alert oAlert = items.get(position);
        return oAlert.get_id();
        //return super.getItemId(position);
    }

    private int getPositionForId(long id) {
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).get_id() == id) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new AlertListHolder(parent);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        AlertListHolder viewHolder = (AlertListHolder)holder;
        final int iPos = position;
        final Alert oAlert = items.get(position);

        if (itemsPendingRemoval.contains(oAlert.get_id())) {
            // we need to show the "undo" state of the row
            viewHolder.itemView.setBackgroundColor(Color.RED);
            viewHolder.titleTextView.setVisibility(View.GONE);
            viewHolder.undoButton.setVisibility(View.VISIBLE);
            viewHolder.undoButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int id = oAlert.get_id();
                    int iNewPos = getPositionForId(id);

                    // user wants to undo the removal, let's cancel the pending task
                    Runnable pendingRemovalRunnable = pendingRunnables.get(id);
                    if (pendingRemovalRunnable != null) {
                        pendingRunnables.remove(id);
                        handler.removeCallbacks(pendingRemovalRunnable);
                    }
                    int iPendingPos = itemsPendingRemoval.indexOf(id);
                    if (iPendingPos >= 0) {
                        itemsPendingRemoval.remove(iPendingPos);
                        // this will rebind the row in "normal" state
                        notifyItemChanged(iNewPos);
                    } else {
                        //item already deleted, just need to refresh
                        notifyItemRemoved(iPos);
                    }
                }
            });
        } else {
            // we need to show the "normal" state
            viewHolder.itemView.setBackgroundColor(Color.WHITE);
            viewHolder.titleTextView.setVisibility(View.VISIBLE);
            viewHolder.titleTextView.setText(oAlert.toString());
            viewHolder.undoButton.setVisibility(View.GONE);
            viewHolder.undoButton.setOnClickListener(null);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void removeAll() {
        //must clear all running threads to avoid future problems
        handler.removeCallbacksAndMessages(null);
        pendingRunnables.clear();

        itemsPendingRemoval.clear();

        int iMax = items.size();
        items.clear();

        notifyItemRangeRemoved(0, iMax);

    }

    public void updateAlert(Alert oAlert) {
        //test if position exists
        int iPos = getPositionForId(oAlert.get_id());
        items.set(iPos, oAlert);
        notifyItemChanged(iPos);
    }

    public void setUndoOn(boolean undoOn) {
        this.undoOn = undoOn;
    }

    public boolean isUndoOn() {
        return undoOn;
    }

    public void pendingRemoval(int position) {
        Alert oAlert = items.get(position);
        final int id = oAlert.get_id();

        if (!itemsPendingRemoval.contains(id)) {
            itemsPendingRemoval.add(id);
            // this will redraw row in "undo" state
            notifyItemChanged(position);
            // let's create, store and post a runnable to remove the item
            Runnable pendingRemovalRunnable = new Runnable() {
                @Override
                public void run() {
                    removeById(id);
                }
            };
            handler.postDelayed(pendingRemovalRunnable, PENDING_REMOVAL_TIMEOUT);
            pendingRunnables.put(id, pendingRemovalRunnable);
        }
    }

    public void remove(int position) {

        boolean bSuccess = false;
        try {
            bSuccess = mItemDeletedListener.OnItemDeleted(position, items.get(position));
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (bSuccess) {
            if (itemsPendingRemoval.contains(position)) {
                itemsPendingRemoval.remove(itemsPendingRemoval.indexOf(position));
            }

            if (items.get(position) != null) {
                items.remove(position);
                notifyItemRemoved(position);
                notifyItemRangeChanged(position, items.size());
            }
        }
    }

    private void removeById(int id) {
        //String item = items.get(position);
        int position = getPositionForId(id);
        if (position >= 0) {
            boolean bSuccess = mItemDeletedListener.OnItemDeleted(position, items.get(position));

            if (bSuccess) {
                if (itemsPendingRemoval.contains(id)) {
                    itemsPendingRemoval.remove(itemsPendingRemoval.indexOf(id));
                }

                if (items.get(position) != null) {
                    items.remove(position);
                    notifyItemRemoved(position);
                    notifyItemRangeChanged(position, items.size());
                }
            }
        }
    }

    // Container Activity must implement this interface
    public interface OnItemDeletedListener {
        boolean OnItemDeleted(int pos, Alert oAlert);
    }

    public boolean isPendingRemoval(int position) {
        //String item = items.get(position);
        return itemsPendingRemoval.contains(position);
    }

    //Container Activity must implement this interface
    public interface OnItemClickListener {
        void onItemClick(View view, int position, String id);
    }

    public void SetOnItemClickListener(final OnItemClickListener mItemClickListener) {
         this.mItemClickListener = mItemClickListener;
    }

    /**
     * ViewHolder capable of presenting two states: "normal" and "undo" state.
     */
    private class AlertListHolder extends RecyclerView.ViewHolder implements ViewGroup.OnClickListener {

        TextView titleTextView;
        Button undoButton;

        public AlertListHolder(ViewGroup parent) {
            super(LayoutInflater.from(parent.getContext()).inflate(R.layout.row_view, parent, false));
            titleTextView = (TextView) itemView.findViewById(R.id.title_text_view);
            undoButton = (Button) itemView.findViewById(R.id.undo_button);

            titleTextView.setOnClickListener(this);
            //itemView.setOnClickListener(this);
        }

        /*
        @Override
        public boolean onLongClick(View v) {
            mItemClickListener.onItemLongClick(v, getAdapterPosition(), this.titleTextView.getText().toString());
            return true;
        }
        */

        @Override
        public void onClick(View v) {
            mItemClickListener.onItemClick(v, getAdapterPosition(), this.titleTextView.getText().toString());
        }
    }


}

