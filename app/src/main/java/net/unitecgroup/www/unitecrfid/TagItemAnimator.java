package net.unitecgroup.www.unitecrfid;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.graphics.drawable.ColorDrawable;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;

import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import java.util.HashMap;
import java.util.List;

/**
 * Created by joaocarlosss on 03/08/2017.
 * http://frogermcs.github.io/recyclerview-animations-androiddevsummit-write-up/
 */

public class TagItemAnimator extends DefaultItemAnimator {

    HashMap<RecyclerView.ViewHolder, AnimatorInfo> animatorMap = new HashMap<>();


    @Override
    public boolean animateChange(@NonNull RecyclerView.ViewHolder oldHolder,
                                 @NonNull final RecyclerView.ViewHolder newHolder,
                                 @NonNull ItemHolderInfo preInfo,
                                 @NonNull ItemHolderInfo postInfo) {
        final TagListAdapter.ViewHolder holder = (TagListAdapter.ViewHolder) newHolder;
        final ColorTextInfo preColorTextInfo = (ColorTextInfo) preInfo;
        final ColorTextInfo postColorTextInfo = (ColorTextInfo) postInfo;

        ObjectAnimator fadeToBlack = ObjectAnimator.ofArgb(holder.mCardView, "cardBackgroundColor", preColorTextInfo.color, Color.GREEN);
        ObjectAnimator fadeFromBlack = ObjectAnimator.ofArgb(holder.mCardView, "cardBackgroundColor", Color.GREEN, postColorTextInfo.color);
        AnimatorSet bgAnim = new AnimatorSet();
        fadeToBlack.setDuration(100);
        fadeFromBlack.setDuration(100);
        bgAnim.playSequentially(fadeToBlack, fadeFromBlack);


        AnimatorSet overallAnim = new AnimatorSet();
        overallAnim.playTogether(bgAnim);
        overallAnim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                dispatchAnimationFinished(newHolder);
                animatorMap.remove(newHolder);
            }
        });

        AnimatorInfo animatorInfo = animatorMap.get(newHolder);
        /*
        if (animatorInfo != null) {
            boolean firstHalf = false; //animatorInfo.oldTextRotator.isRunning();
            if (firstHalf) {
                fadeToBlack.setCurrentPlayTime(animatorInfo.fadeToBlackAnim.getCurrentPlayTime());
            } else {
                fadeToBlack.setCurrentPlayTime(animatorInfo.fadeToBlackAnim.getDuration());
                fadeFromBlack.setIntValues((Integer)animatorInfo.fadeFromBlackAnim.getAnimatedValue(), postColorTextInfo.color);
            }

            animatorInfo.overallAnim.cancel();
            animatorMap.remove(newHolder);
        }
        */
        animatorMap.put(newHolder, new AnimatorInfo(
                overallAnim, fadeToBlack, fadeFromBlack, null, null
        ));

        overallAnim.start();

        return super.animateChange(oldHolder, newHolder, preInfo, postInfo);
    }

    @Override
    public boolean canReuseUpdatedViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, @NonNull List<Object> payloads) {
        return true;
        //return super.canReuseUpdatedViewHolder(viewHolder, payloads);
    }

    @NonNull
    @Override
    public ItemHolderInfo recordPreLayoutInformation(@NonNull RecyclerView.State state,
                                                     @NonNull RecyclerView.ViewHolder viewHolder,
                                                     int changeFlags,
                                                     @NonNull List<Object> payloads) {
        ColorTextInfo colorTextInfo = new ColorTextInfo();
        colorTextInfo.setFrom(viewHolder);
        return colorTextInfo;
    }

    @NonNull
    @Override
    public ItemHolderInfo recordPostLayoutInformation(@NonNull RecyclerView.State state,
                                                      @NonNull RecyclerView.ViewHolder viewHolder) {
        ColorTextInfo colorTextInfo = new ColorTextInfo();
        colorTextInfo.setFrom(viewHolder);
        return colorTextInfo;
    }


    private class ColorTextInfo extends ItemHolderInfo {
        int color;
        String text;

        @Override
        public ItemHolderInfo setFrom(RecyclerView.ViewHolder holder) {
            if (holder instanceof TagListAdapter.ViewHolder) {
                TagListAdapter.ViewHolder tagViewHolder = (TagListAdapter.ViewHolder) holder;

                //color = ((ColorDrawable) tagViewHolder.mTextView.getBackground()).getColor();
                color = tagViewHolder.mCardView.getCardBackgroundColor().getDefaultColor();
                text = (String) tagViewHolder.mTextView.getText();
            }
            return super.setFrom(holder);
        }
    }

    private class AnimatorInfo {
        Animator overallAnim;
        ObjectAnimator fadeToBlackAnim, fadeFromBlackAnim, oldTextRotator, newTextRotator;

        public AnimatorInfo(Animator overallAnim, ObjectAnimator fadeToBlackAnim, ObjectAnimator fadeFromBlackAnim,
                            ObjectAnimator oldTextRotator, ObjectAnimator newTextRotator) {
            this.overallAnim = overallAnim;
            this.fadeToBlackAnim = fadeToBlackAnim;
            this.fadeFromBlackAnim = fadeFromBlackAnim;
            this.oldTextRotator = oldTextRotator;
            this.newTextRotator = newTextRotator;
        }
    }


}
