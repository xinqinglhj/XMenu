package com.andview.example.widget.xmenu;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.Scroller;

import com.andview.example.utils.LogUtils;

import java.util.ArrayList;
import java.util.List;

public class ContentView extends ViewGroup {

    /**
     * 菜单宽度
     */
    public int menuWidth = 400;
    /**
     * 速率采样间隔
     */
    private static final int SNAP_VELOCITY = 1000;
    private final static int TOUCH_STATE_REST = 0;
    private final static int TOUCH_STATE_SCROLLING = 1;

    public int mTouchState = TOUCH_STATE_REST;

    private FrameLayout mContainer;

    private Scroller mScroller;
    private VelocityTracker mVelocityTracker;
    /**
     * 系统所能识别的最小滑动距离
     */
    private int mTouchSlop;
    /**
     * 手指点击屏幕的最初位置
     */
    private float mLastMotionX;
    private float mLastMotionY;

    private int mShadowWidth = 15;
    private Drawable mShadowDrawable;

    private int mTouchMode = XMenu.TOUCHMODE_MARGIN;
    private List<View> mIgnoredViews = new ArrayList<View>();

    public ContentView(Context context) {
        super(context);
        init();
    }

    public ContentView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ContentView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        mContainer.measure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        final int width = r - l;
        final int height = b - t;
        mContainer.layout(0, 0, width, height);
    }

    private void init() {
        setWillNotDraw(false);

        mContainer = new FrameLayout(getContext());
        mScroller = new Scroller(getContext());
        mTouchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
        super.addView(mContainer);
    }

    public void setView(View v) {
        if (mContainer.getChildCount() > 0) {
            mContainer.removeAllViews();
        }
        if (v.getParent() != null) {
            throw new RuntimeException(
                    "the view has parent,please detach this view first");
        }
        mContainer.addView(v);
    }

    public void setLeftShadowDrawable(Drawable drawable) {
        mShadowDrawable = drawable;
    }

    public void setLeftShadowWidth(int width) {
        mShadowWidth = width;
    }

    @Override
    public void scrollTo(int x, int y) {
        super.scrollTo(x, y);
        postInvalidate();
    }

    @Override
    public void computeScroll() {
        if (!mScroller.isFinished()) {
            if (mScroller.computeScrollOffset()) {
                int oldX = getScrollX();
                int oldY = getScrollY();
                int x = mScroller.getCurrX();
                int y = mScroller.getCurrY();
                if (oldX != x || oldY != y) {
                    scrollTo(x, y);
                }
                // Keep on drawing until the animation has finished.
                postInvalidate();
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (null != mShadowDrawable) {
            mShadowDrawable.setBounds(-mShadowWidth, 0, 0, getHeight());
            mShadowDrawable.draw(canvas);
        }
    }

    private MotionEvent mLastMoveEvent;
    private boolean isCloseMenu = false;

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        final int action = ev.getAction();
        final float x = ev.getX();
        final float y = ev.getY();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mLastMotionX = x;
                if (isMenuShowing() && mLastMotionX >= menuWidth) {
                    isCloseMenu = true;
                }
                if(thisTouchAllowed(ev)){
                    mIntercept = true;
                }
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                if (isCloseMenu) {
                    showContent();
                    isCloseMenu = false;
                }
                mIntercept = false;
                break;

            default:
                break;
        }
        return super.dispatchTouchEvent(ev);
    }
private boolean mIntercept = false;
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if(!mIntercept){
            return false;
        }

        final int action = ev.getAction();
        if ((action == MotionEvent.ACTION_MOVE)
                && (mTouchState != TOUCH_STATE_REST)) {
            Log.e("ad", "return true");
            return true;
        }

        final float x = ev.getX();
        final float y = ev.getY();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                // Remember location of down touch
                mLastMotionX = x;
                mLastMotionY = y;
                mTouchState = mScroller.isFinished() ? TOUCH_STATE_REST
                        : TOUCH_STATE_SCROLLING;
                Log.e("ad", "onInterceptTouchEvent  ACTION_DOWN  mTouchState=="
                        + (mTouchState == TOUCH_STATE_SCROLLING));

                break;
            case MotionEvent.ACTION_MOVE:
                final int xDiff = (int) Math.abs(x - mLastMotionX);
                final int yDiff = (int) Math.abs(y - mLastMotionY);

                final int touchSlop = mTouchSlop;
                boolean xMoved = xDiff > touchSlop;
                boolean yMoved = yDiff > touchSlop;

                if (xMoved || yMoved) {
                    if (xMoved) {
                        // Scroll if the user moved far enough along the X axis
                        mTouchState = TOUCH_STATE_SCROLLING;
                        Log.e("ad",
                                "onInterceptTouchEvent  ACTION_MOVE  mTouchState=="
                                        + (mTouchState == TOUCH_STATE_SCROLLING));
                        enableChildrenCache();
                    }
                }
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                // Release the drag
                clearChildrenCache();
                mTouchState = TOUCH_STATE_REST;
                Log.e("ad", "onInterceptTouchEvent  ACTION_UP  mTouchState=="
                        + (mTouchState == TOUCH_STATE_SCROLLING));
                break;
        }

     /*
     * The only time we want to intercept motion events is if we are in the
     * drag mode.
     */
        return mTouchState != TOUCH_STATE_REST;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(ev);

        final int action = ev.getAction();
        final float x = ev.getX();
        final float y = ev.getY();

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mLastMotionX = x;
                mLastMotionY = y;
                int oldScrollX = getScrollX();
                if (oldScrollX == 0) {
                    // 当菜单隐藏时，消费此事件解决点击穿透的问题
                    return true;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                final int xDiff = (int) Math.abs(x - mLastMotionX);
                final int yDiff = (int) Math.abs(y - mLastMotionY);
                final int touchSlop = mTouchSlop;
                boolean xMoved = xDiff > touchSlop;
                if (xMoved) {
                    mTouchState = TOUCH_STATE_SCROLLING;
                    LogUtils.i("onInterceptTouchEvent ACTION_MOVE  mTouchState=="
                            + (mTouchState == TOUCH_STATE_SCROLLING));
                    enableChildrenCache();
                }
                if (mTouchState == TOUCH_STATE_SCROLLING) {
                    final float deltaX = mLastMotionX - x;
                    mLastMotionX = x;
                    oldScrollX = getScrollX();
                    float scrollX = oldScrollX + deltaX;
                    final float leftBound = 0;
                    final float rightBound = -menuWidth;
                    if (scrollX > leftBound) {
                        scrollX = leftBound;
                    } else if (scrollX < rightBound) {
                        scrollX = rightBound;
                    }
                    scrollTo((int) scrollX, getScrollY());

                }
                break;
            case MotionEvent.ACTION_UP:
                if (mTouchState == TOUCH_STATE_SCROLLING) {
                    final VelocityTracker velocityTracker = mVelocityTracker;
                    velocityTracker.computeCurrentVelocity(SNAP_VELOCITY);
                    int velocityX = (int) velocityTracker.getXVelocity();
                    oldScrollX = getScrollX();
                    LogUtils.i("oldScrollX == " + oldScrollX + ";menuWidth=" + menuWidth);
                    int dx = 0;
                    if (oldScrollX < -menuWidth / 2) {
                        dx = -menuWidth - oldScrollX;
                    } else {
                        dx = -oldScrollX;
                    }
                    smoothScrollTo(dx);
                    if (mVelocityTracker != null) {
                        mVelocityTracker.recycle();
                        mVelocityTracker = null;
                    }
                } else {
                    showContent();
                }
            case MotionEvent.ACTION_CANCEL:
                mTouchState = TOUCH_STATE_REST;
                return true;
        }

        return false;
    }

    public void toggle() {
        int oldScrollX = getScrollX();
        if (oldScrollX == 0) {
            smoothScrollTo(-menuWidth);
        } else if (oldScrollX == -menuWidth) {
            smoothScrollTo(menuWidth);
        }
    }

    public boolean isMenuShowing() {
        int oldScrollX = getScrollX();
        if (oldScrollX == 0) {
            return false;
        } else if (oldScrollX == -menuWidth) {
            return true;
        }
        return false;
    }

    public void showContent() {
        if (isMenuShowing()) {
            smoothScrollTo(menuWidth);
        }
    }

    public void setMenuWidth(int menuWidth) {
        this.menuWidth = menuWidth;
    }

    public void addIgnoredView(View v) {
        if (!mIgnoredViews.contains(v)) {
            mIgnoredViews.add(v);
        }
    }

    public void removeIgnoredView(View v) {
        mIgnoredViews.remove(v);
    }

    public void clearIgnoredViews() {
        mIgnoredViews.clear();
    }

    private boolean isInIgnoredView(MotionEvent ev) {
        Rect rect = new Rect();
        for (View v : mIgnoredViews) {
            v.getHitRect(rect);
            if (rect.contains((int) ev.getX(), (int) ev.getY())) return true;
        }
        return false;
    }

    void smoothScrollTo(int dx) {
        int duration = 500;
        int oldScrollX = getScrollX();
        mScroller.startScroll(oldScrollX, getScrollY(), dx, getScrollY(),
                duration);
        invalidate();
    }

    void enableChildrenCache() {
        final int count = getChildCount();
        for (int i = 0; i < count; i++) {
            final View layout = (View) getChildAt(i);
            layout.setDrawingCacheEnabled(true);
        }
    }

    void clearChildrenCache() {
        final int count = getChildCount();
        for (int i = 0; i < count; i++) {
            final View layout = (View) getChildAt(i);
            layout.setDrawingCacheEnabled(false);
        }
    }

    public void setTouchMode(int touchMode) {
        mTouchMode = touchMode;
    }

    public int getTouchMode() {
        return mTouchMode;
    }

    private boolean thisTouchAllowed(MotionEvent ev) {
        int x = (int) (ev.getX());
        LogUtils.i("thisTouchAllowed x="+x);
        if(isMenuShowing()){
           return true;
        }
        switch (mTouchMode) {
            case XMenu.TOUCHMODE_FULLSCREEN:
                return !isInIgnoredView(ev);
            case XMenu.TOUCHMODE_NONE:
                return false;
            case XMenu.TOUCHMODE_MARGIN:
                return x<=mEdgeWith;
        }
        return false;
    }
    private int mEdgeWith;
    public void setEdgeWith(int width){
        mEdgeWith = width;
    }
}