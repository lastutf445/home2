package com.lastutf445.home2.util;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.NonNull;

import com.lastutf445.home2.loaders.DataLoader;

public class SimpleAnimator {

    @Deprecated
    public static void alpha(@NonNull View view, float from, float to, int duration) {
        alpha(view, from, to, duration, null);
    }

    @Deprecated
    public static void alpha(View view, float from, float to, int duration, Animation.AnimationListener listener) {
        AlphaAnimation anim = new AlphaAnimation(from, to);
        anim.setDuration(duration);
        anim.setFillAfter(true);

        anim.setAnimationListener(listener);
        //view.clearAnimation();
        view.startAnimation(anim);
    }

    @Deprecated
    public static void fadeIn(@NonNull View view, int duration) {
        fadeIn(view, duration, null);
    }

    @Deprecated
    public static void fadeIn(View view, int duration, Animation.AnimationListener listener) {
        view.setVisibility(View.VISIBLE);
        alpha(view, 0, 1, duration, listener);
    }

    @Deprecated
    public static void fadeOut(@NonNull View view, int duration) {
        fadeIn(view, duration, null);
    }

    @Deprecated
    public static void fadeOut(@NonNull View view, int duration, Animation.AnimationListener listener) {
        alpha(view, 1, 0, duration, listener);
    }

    public static void alpha2(@NonNull final View view, int duration, float targetAlpha) {
        alpha2(view, duration, targetAlpha, null);
    }

    public static void alpha2(@NonNull final View view, int duration, float targetAlpha, Animator.AnimatorListener listener) {
        float prevAlpha = view.getAlpha();

        ValueAnimator anim = ValueAnimator.ofFloat(prevAlpha, targetAlpha);
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                view.setAlpha((float) animation.getAnimatedValue());
                view.requestLayout();
            }
        });

        if (listener != null) {
            anim.addListener(listener);
        }

        anim.setInterpolator(new DecelerateInterpolator());
        anim.setDuration(duration);
        anim.start();
    }

    public static void expand(@NonNull final View view, int duration) {
        int matchParentMeasureSpec = View.MeasureSpec.makeMeasureSpec(((View) view.getParent()).getWidth(), View.MeasureSpec.EXACTLY);
        int wrapContentMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        view.measure(matchParentMeasureSpec, wrapContentMeasureSpec);
        int targetHeight = view.getMeasuredHeight();
        int height = view.getHeight();

        ValueAnimator anim = ValueAnimator.ofInt(height, targetHeight);
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                view.getLayoutParams().height = (int) animation.getAnimatedValue();
                view.requestLayout();
            }
        });

        anim.setInterpolator(new DecelerateInterpolator());
        anim.setDuration(duration);
        anim.start();
    }

    public static void collapse(@NonNull final View view, int duration) {
        int height = view.getHeight();

        ValueAnimator anim = ValueAnimator.ofInt(height, 0);
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                view.getLayoutParams().height = (int) animation.getAnimatedValue();
                view.requestLayout();
            }
        });

        anim.setInterpolator(new AccelerateInterpolator());
        anim.setDuration(duration);
        anim.start();
    }

    public static float dp2float(int dp) {
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp,
                DataLoader.getAppResources().getDisplayMetrics()
        );
    }
}
