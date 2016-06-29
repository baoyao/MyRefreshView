package com.example.testmypulltorefresh.view3;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.ScrollView;

/**
 * @author houen.bao
 * @date Jun 29, 2016 4:31:45 PM
 */
@SuppressLint("NewApi")
public class MyScrollView extends ScrollView {

	public MyScrollView(Context context) {
		super(context, null, 0, 0);
		// TODO Auto-generated constructor stub
	}

	public MyScrollView(Context context, AttributeSet attrs) {
		super(context, attrs, 0, 0);
		// TODO Auto-generated constructor stub
	}

	public MyScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr, 0);
		// TODO Auto-generated constructor stub
	}

	public MyScrollView(Context context, AttributeSet attrs, int defStyleAttr,
			int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();
		findTargetView(this);
	}

	private ViewGroup mTargetView;

	private boolean isFindTargetView = false;

	private void findTargetView(ViewGroup viewGroup) {
		for (int i = 0; i < viewGroup.getChildCount(); i++) {
			if (isFindTargetView)
				return;
			if (viewGroup.getChildAt(i) instanceof HorizontalScrollView) {
				isFindTargetView = true;
				mTargetView = (ViewGroup) getChildAt(i);
				return;
			}
			if (viewGroup.getChildAt(i) instanceof ViewGroup) {
				findTargetView((ViewGroup) viewGroup.getChildAt(i));
			}
		}
	}

	private float downX, downY;
	
	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		// 屏幕的原点在左上角,往下Y++，往右X++
		float x = ev.getRawX();
		float y = ev.getRawY();
		switch (ev.getAction()) {
		case MotionEvent.ACTION_DOWN:
			downX = x;
			downY = y;
			if (mTargetView != null) {
				mTargetView.onTouchEvent(ev);
			}
			break;
		case MotionEvent.ACTION_MOVE:
			float changeX = x - downX;
			float changeY = y - downY;
			if (Math.abs(changeX) > 16 || Math.abs(changeY) > 16) {
				if (Math.abs(changeX) > Math.abs(changeY)) {// 横向
					if (mTargetView != null) {
						mTargetView.onTouchEvent(ev);
					}
					if (changeX > 0) {// 往右
					}
					if (changeX < 0) {// 往左
					}
				}
				if (Math.abs(changeX) < Math.abs(changeY)) {// 纵向
					if (changeY > 0) {// 往下
					}
					if (changeY < 0) {// 往上
					}
				}
			}
			break;
		case MotionEvent.ACTION_UP:
		default:
			if (mTargetView != null) {
				mTargetView.onTouchEvent(ev);
			}
			break;
		}
		return super.onTouchEvent(ev);
	}

}
