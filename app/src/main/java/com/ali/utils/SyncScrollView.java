package com.ali.utils;


import android.content.Context;
import android.util.AttributeSet;
import android.widget.ScrollView;

public class SyncScrollView extends ScrollView {

    private ScrollView syncedScrollView;
    private Runnable scrollRunnable;

    public SyncScrollView(Context context) {
        super(context);
        init();
    }

    public SyncScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SyncScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        scrollRunnable = () -> {
            if (syncedScrollView != null) {
                syncedScrollView.scrollTo(0, getScrollY());
            }
        };
    }

    public void setSyncedScrollView(ScrollView scrollView) {
        this.syncedScrollView = scrollView;
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
        if (syncedScrollView != null) {
            removeCallbacks(scrollRunnable);
            post(scrollRunnable);
        }
    }
}
