package com.lastutf445.home2.util;

import android.support.annotation.NonNull;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;

public class SimpleAnimator {

    public static void alpha(@NonNull View view, float from, float to, int duration) {
        alpha(view, from, to, duration, null);
    }

    public static void alpha(View view, float from, float to, int duration, Animation.AnimationListener listener) {
        AlphaAnimation anim = new AlphaAnimation(from, to);
        anim.setDuration(duration);
        anim.setFillAfter(true);

        anim.setAnimationListener(listener);
        //view.clearAnimation();
        view.startAnimation(anim);
    }

    public static void fadeIn(@NonNull View view, int duration) {
        fadeIn(view, duration, null);
    }

    public static void fadeIn(View view, int duration, Animation.AnimationListener listener) {
        view.setVisibility(View.VISIBLE);
        alpha(view, 0, 1, duration, listener);
    }

    public static void fadeOut(@NonNull View view, int duration) {
        fadeIn(view, duration, null);
    }

    public static void fadeOut(@NonNull View view, int duration, Animation.AnimationListener listener) {
        alpha(view, 1, 0, duration, listener);
    }
}
