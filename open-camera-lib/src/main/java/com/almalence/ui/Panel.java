/*
The contents of this file are subject to the Mozilla Public License
Version 1.1 (the "License"); you may not use this file except in
compliance with the License. You may obtain a copy of the License at
http://www.mozilla.org/MPL/

Software distributed under the License is distributed on an "AS IS"
basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
License for the specific language governing rights and limitations
under the License.

The Original Code is collection of files collectively known as Open Camera.

The Initial Developer of the Original Code is Almalence Inc.
Portions created by Initial Developer are Copyright (C) 2013 
by Almalence Inc. All Rights Reserved.
 */

package com.almalence.ui;


import com.almalence.opencam.CameraScreenActivity;
import com.almalence.opencam.R;
//-+- -->

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.GridView;
import android.widget.LinearLayout;

/**
 * Panel - implements sliding panel
 * *
 */

public class Panel extends LinearLayout {
    /**
     * Callback invoked when the panel is opened/closed.
     */
    public static interface OnPanelListener {
        /**
         * Invoked when the panel becomes fully closed.
         */
        public void onPanelClosed(Panel panel);

        /**
         * Invoked when the panel becomes fully opened.
         */
        public void onPanelOpened(Panel panel);
    }

    private boolean mIsShrinking;
    private final int mPosition;
    private final int mDuration;
    private final float downSpace;
    private final boolean mLinearFlying;
    private View mHandle;
    private View mContent;
    private final Drawable mOpenedHandle;
    private boolean mOpened;
    private boolean startScrolling = false;
    private boolean firstTime = true;
    private boolean toTheTop = false;
    private boolean locationTop = false;
    private int moving = 0;
    private boolean handle = true;
    private final Drawable mClosedHandle;
    private float mTrackX;
    private float mTrackY;
    private float mVelocity;

    private boolean outsideControl = false;

    private OnPanelListener panelListener;

    private static final int TOP = 0;
    private static final int BOTTOM = 1;
    private static final int LEFT = 2;
    private static final int RIGHT = 3;

    private enum State {
        ABOUT_TO_ANIMATE, ANIMATING, READY, TRACKING, FLYING,
    }

    private State mState;
    private Interpolator mInterpolator;
    private int mContentHeight;
    private int mContentWidth;
    private final int mOrientation;

    public Panel(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.Panel);
        mDuration = a.getInteger(R.styleable.Panel_animationDuration, 750); // duration
        mPosition = a.getInteger(R.styleable.Panel_position, BOTTOM); // position
        downSpace = a.getDimension(R.styleable.Panel_downSpace, 60);

        mLinearFlying = a.getBoolean(R.styleable.Panel_linearFlying, false); // linearFlying
        mOpenedHandle = a.getDrawable(R.styleable.Panel_openedHandle);
        mClosedHandle = a.getDrawable(R.styleable.Panel_closedHandle);
        a.recycle();
        mOrientation = (mPosition == TOP || mPosition == BOTTOM) ? VERTICAL : HORIZONTAL;
        setOrientation(mOrientation);
        mState = State.READY;
    }

    /**
     * Gets Panel's mHandle
     *
     * @return Panel's mHandle
     */
    public View getHandle() {
        return mHandle;
    }

    /**
     * Gets Panel's mContent
     *
     * @return Panel's mContent
     */
    public View getContent() {
        return mContent;
    }

    /**
     * Sets the acceleration curve for panel's animation.
     *
     * @param i The interpolator which defines the acceleration curve
     */
    public void setInterpolator(Interpolator i) {
        mInterpolator = i;
    }

    /**
     * Returns the opened status for Panel.
     *
     * @return True if Panel is opened, false otherwise.
     */
    public boolean isOpen() {
        return mContent.getVisibility() == VISIBLE;
    }

    private void SetOnTouchListener(ViewGroup vg, OnTouchListener lst) {
        for (int i = 0; i < vg.getChildCount(); i++) {
            View child = vg.getChildAt(i);
            if (child instanceof GridView) {
                (child).setOnTouchListener(lst);
            } else if (child instanceof ViewGroup) {
                SetOnTouchListener((ViewGroup) child, lst);
            }
        }
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mHandle = findViewById(R.id.panelHandle);
        if (mHandle == null)
            throw new RuntimeException("Your Panel must have a View whose id attribute is 'R.id.panelHandle'");
        mHandle.setOnTouchListener(touchListener);

        mContent = findViewById(R.id.panelContent);
        if (mContent == null)
            throw new RuntimeException("Your Panel must have a View whose id attribute is 'R.id.panelContent'");

        mContent.setOnTouchListener(touchListener);

        SetOnTouchListener((ViewGroup) mContent, touchListener);

        // reposition children
        removeView(mHandle);
        removeView(mContent);
        if (mPosition == TOP || mPosition == LEFT) {
            addView(mContent);
            addView(mHandle);
        } else {
            addView(mHandle);
            addView(mContent);
        }

        if (mClosedHandle != null) {
            mHandle.setBackgroundDrawable(mClosedHandle);
            mOpened = false;
        }
        mContent.setVisibility(GONE);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        mContentWidth = mContent.getWidth();
        mContentHeight = mContent.getHeight();
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        if (mState == State.ABOUT_TO_ANIMATE && !mIsShrinking) {
            int delta = mOrientation == VERTICAL ? mContentHeight : mContentWidth;
            if (mPosition == LEFT || mPosition == TOP) {
                delta = -delta;
            }
            if (mOrientation == VERTICAL) {
                canvas.translate(0, delta);
            } else {
                canvas.translate(delta, 0);
            }
        }
        if (mState == State.TRACKING || mState == State.FLYING) {
            canvas.translate(mTrackX, mTrackY);
            mContent.getBackground().setAlpha((int) (255 - 255 * Math.abs(mTrackY / mContentHeight)));
        }
        super.dispatchDraw(canvas);
    }

    public OnTouchListener touchListener = new OnTouchListener() {
        int initX;
        int initY;
        boolean setInitialPosition;

        public boolean onTouch(View v, MotionEvent event) {
            if (CameraScreenActivity.getGUIManager().lockControls)
                return false;

            int action = event.getAction();

            if (v == CameraScreenActivity.getPreviewSurfaceView()
                    || v == (CameraScreenActivity.getInstance()
                    .findViewById(R.id.mainLayout1))
                    || v.getParent() == CameraScreenActivity
                    .getInstance().findViewById(
                            R.id.paramsLayout)) {
                if (!mOpened) {
                    handle = false;
                    if (action == MotionEvent.ACTION_DOWN) {
                        if (event.getRawY() > ((20 + (toTheTop ? 0
                                : 65)) * CameraScreenActivity
                                .getMainContext().getResources()
                                .getDisplayMetrics().density))
                            return false;
                        else
                            startScrolling = true;
                    }

                    if (action == MotionEvent.ACTION_MOVE) {
                        if (!startScrolling)
                            return false;
                        if (event.getY() < ((toTheTop ? 0 : 65) * CameraScreenActivity
                                .getMainContext().getResources()
                                .getDisplayMetrics().density))
                            return false;
                        reorder(true, false);
                    }
                    if ((action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP)
                            && !startScrolling)
                        return false;
                    outsideControl = false;
                } else {
                    if ((event.getY() > (mContentHeight + 30))
                            && (action != MotionEvent.ACTION_DOWN)
                            && (action != MotionEvent.ACTION_UP))
                        return false;
                    outsideControl = true;
                }

            } else {
                handle = true;
                outsideControl = false;
            }
            if (action == MotionEvent.ACTION_DOWN) {
                initX = 0;
                initY = 0;
                if (mContent.getVisibility() == GONE) {
                    mContent.getBackground().setAlpha(0);
                    if (mOrientation == VERTICAL) {
                        initY = mPosition == TOP ? -1 : 1;
                    } else {
                        initX = mPosition == LEFT ? -1 : 1;
                    }
                }
                setInitialPosition = true;
            } else {
                if (setInitialPosition) {
                    initX *= mContentWidth;
                    initY *= mContentHeight;
                    setInitialPosition = false;
                    initX = -initX;
                    initY = -initY;
                }
                event.offsetLocation(initX, initY);
            }
            return false;
        }
    };

    // corrects margin between content and handler
    public void reorder(boolean toTop, boolean isFromGUI) {
        if (isFromGUI)
            locationTop = toTop;
        toTheTop = toTop;
        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) this.findViewById(R.id.panelHandle)
                .getLayoutParams();
        float d = CameraScreenActivity.getMainContext().getResources().getDisplayMetrics().density;
        if (toTheTop) {
            if (!isFromGUI && (moving != 0) && !locationTop && handle) {
                moving = moving + 5;
                int margin = (int) (downSpace - moving);
                if (margin < 0)
                    margin = 0;
                lp.topMargin = margin;
            } else
                lp.topMargin = 0;
        } else {
            lp.topMargin = (int) (downSpace);
        }
        mHandle.setLayoutParams(lp);
    }
}
