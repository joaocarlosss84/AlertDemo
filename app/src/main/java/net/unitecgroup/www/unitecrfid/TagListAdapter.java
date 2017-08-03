package net.unitecgroup.www.unitecrfid;


import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by 20006030 on 01/08/2017.
 */

class TagListAdapter extends RecyclerView.Adapter<TagListAdapter.ViewHolder> {
    private ArrayList<String> mDataset;
    //private HashSet<String> mDataset;

    // Provide a suitable constructor (depends on the kind of dataset)
    public TagListAdapter(ArrayList<String> myDataset) {
        mDataset = myDataset;
    }

    public void addTag(String sId) {

        try {
            mDataset.add(sId);
            notifyItemInserted(mDataset.size() - 1);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public static class ViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        public TextView mTextView;
        public ViewHolder(View v) {
            super(v);
            mTextView = (TextView) v.findViewById(R.id.textView);
        }
    }


    // Create new views (invoked by the layout manager)
    @Override
    public TagListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // create a new view
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.tag_view, parent, false);
        // set the view's size, margins, paddings and layout parameters

        ViewHolder vh = new ViewHolder(v);
        return vh;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element

        try {
            if (holder.mTextView != null)
                holder.mTextView.setText(mDataset.get(position));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mDataset.size();
    }
}
