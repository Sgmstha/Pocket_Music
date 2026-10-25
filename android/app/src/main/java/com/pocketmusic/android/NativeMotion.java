package com.pocketmusic.android;

import android.animation.ValueAnimator;
import android.view.*;
import android.view.animation.PathInterpolator;

/** Short, staggered transform/opacity reveals; honors Android's disabled-animation setting. */
final class NativeMotion {
    static void reveal(ViewGroup group){
        if(!ValueAnimator.areAnimatorsEnabled())return;
        for(int i=0;i<Math.min(group.getChildCount(),7);i++){
            View v=group.getChildAt(i);v.animate().cancel();v.setAlpha(0);v.setTranslationY(14*v.getResources().getDisplayMetrics().density);
            v.animate().alpha(1).translationY(0).setStartDelay(i*28L).setDuration(300).setInterpolator(new PathInterpolator(.18f,.85f,.25f,1)).start();
        }
    }
    static void finish(ViewGroup group){for(int i=0;i<group.getChildCount();i++){View v=group.getChildAt(i);v.animate().cancel();v.setAlpha(1);v.setTranslationY(0);}}
}
