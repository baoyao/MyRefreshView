package com.example.testmypulltorefresh.view3;

import com.example.testmypulltorefresh.R;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewConfiguration;
import android.view.animation.RotateAnimation;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

/**
 * @author houen.bao
 * @date Jun 20, 2016 10:41:28 AM
 */
public class MyRefreshableView extends LinearLayout implements OnTouchListener{

	private final String TAG = MyRefreshableView.class.getSimpleName();

	private final boolean DEBUG = true;

	/** 下拉头部回滚的速度 */
	private static final int SCROLL_SPEED = -20;

	/** 超时时间 */
	private long mTimeOut = 5 * 1000;

	/** 下拉的阻尼 */
	private static int scroll_damp = 4;
	private static int scroll_damp_old = scroll_damp;

	/** 下拉状态 */
	private static final int STATUS_PULL_TO_REFRESH = 0;
	/** 释放立即刷新状态 */
	private static final int STATUS_RELEASE_TO_REFRESH = 1;
	/** 正在刷新状态 */
	private static final int STATUS_REFRESHING = 2;
	/** 刷新完成或未刷新状态 */
	private static final int STATUS_REFRESH_FINISHED = 3;

	/**
	 * 当前处理什么状态，可选值有 STATUS_PULL_TO_REFRESH, STATUS_RELEASE_TO_REFRESH,
	 * STATUS_REFRESHING, STATUS_REFRESH_FINISHED
	 */
	private int mCurrentStatus = STATUS_REFRESH_FINISHED;

	/** 记录上一次的状态是什么，避免进行重复操作 */
	private int mLastStatus = mCurrentStatus;

	/** 一分钟的毫秒值，用于判断上次的更新时间 */
	private static final long ONE_MINUTE = 60 * 1000;
	/** 一小时的毫秒值，用于判断上次的更新时间 */
	private static final long ONE_HOUR = 60 * ONE_MINUTE;
	/** 一天的毫秒值，用于判断上次的更新时间 */
	private static final long ONE_DAY = 24 * ONE_HOUR;
	/** 一月的毫秒值，用于判断上次的更新时间 */
	private static final long ONE_MONTH = 30 * ONE_DAY;
	/** 一年的毫秒值，用于判断上次的更新时间 */
	private static final long ONE_YEAR = 12 * ONE_MONTH;

	/** 上次更新时间的字符串常量，用于作为SharedPreferences的键值 */
	private static final String UPDATED_AT = "updated_at";

	/** 下拉刷新的回调接口 */
	private PullToRefreshListener mListener;

	/** 用于存储上次更新时间 */
	private SharedPreferences mLastPullTimePreferences;

	/** 下拉头的View */
	private View mHeaderView;

	/** 需要去下拉刷新的AdapterView */
	private AdapterView mAdapterView;

	/** 刷新时显示的进度条 */
	private ProgressBar mHeaderProgressBar;

	/** 指示下拉和释放的箭头 */
	private ImageView mHeaderArrowView;

	/** 指示下拉和释放的文字描述 */
	private TextView mHeaderDescription;

	/** 上次更新时间的文字描述 */
	private TextView mHeaderViewUpdateTime;

	/** 下拉头的布局参数 */
	private MarginLayoutParams mHeaderLayoutParams;

	/** 上次更新时间的毫秒值 */
	private long mLastUpdateTime;

	/** 为了防止不同界面的下拉刷新在上次更新时间上互相有冲突，使用id来做区分 */
	private int mCallbackId = -1;

	/** 下拉头的高度 */
	private int mHeaderHideHeight;

	/** 手指按下时的屏幕纵坐标 */
	private float mDownY;

	/** 在被判定为滚动之前用户手指可以移动的最大值，移动大于这个值才算移动 */
	private int mTouchSlop;

	/** 是否已加载过一次layout，这里onLayout中的初始化只需加载一次 */
	private boolean mIsFirstLoad = true;

	/** 当前是否可以下拉，只有ListView滚动到头的时候才允许下拉 */
	private boolean mAbleToPull;

	/** 下拉刷新控件的构造函数，会在运行时动态添加一个下拉头的布局 */
	public MyRefreshableView(Context context, AttributeSet attrs) {
		super(context, attrs);
		mLastPullTimePreferences = PreferenceManager
				.getDefaultSharedPreferences(context);
		mHeaderView = LayoutInflater.from(context).inflate(
				R.layout.pull_to_refresh, null, true);
		mHeaderProgressBar = (ProgressBar) mHeaderView
				.findViewById(R.id.progress_bar);
		mHeaderArrowView = (ImageView) mHeaderView.findViewById(R.id.arrow);
		mHeaderDescription = (TextView) mHeaderView
				.findViewById(R.id.description);
		mHeaderViewUpdateTime = (TextView) mHeaderView
				.findViewById(R.id.updated_at);
		mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
		refreshUpdatedAtValue();
		setOrientation(VERTICAL);
		addView(mHeaderView, 0);
	}

	/** 进行一些关键性的初始化操作，比如：将下拉头向上偏移进行隐藏，给ListView注册touch事件 */
	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		super.onLayout(changed, l, t, r, b);
		if (changed && mIsFirstLoad) {
			mHeaderHideHeight = -mHeaderView.getHeight();
			mHeaderLayoutParams = (MarginLayoutParams) mHeaderView
					.getLayoutParams();
			mHeaderLayoutParams.topMargin = mHeaderHideHeight;
			// listView = (ListView) getChildAt(2);
			// listView.setOnTouchListener(this);
			mIsFirstLoad = false;
		}
	}

	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();
		for (int i = 0; i < getChildCount(); i++) {
			if (getChildAt(i) != null && getChildAt(i) instanceof AdapterView) {
				mAdapterView = (AdapterView) getChildAt(i);
				mAdapterView.setOnTouchListener(this);
				break;
			}
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		// 处理当内部没有控件或者为普通控件时也支持下拉刷新
		if(mAdapterView == null){
			return touch(event,true);
		}
		return super.onTouchEvent(event);
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		return touch(event,false);
	}

	private boolean touch(MotionEvent event, boolean isInterrupt){
		setIsAbleToPull(mAdapterView, event);
		if (mAbleToPull) {
			float x=event.getRawX();
			float y=event.getRawY();
			switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				mDownY = y;
				if(DEBUG)Log.v(TAG,"\n\n\nACTION_DOWN x: "+x+" y: "+y);
				break;
			case MotionEvent.ACTION_MOVE:
				if(DEBUG)Log.v(TAG,"ACTION_MOVE x: "+x+" y: "+y);
				if(DEBUG)Log.v(TAG,"Status: "+mCurrentStatus);
				
				int distance = (int) (y - mDownY);//(distance 上滑-- 下滑++)
				if(DEBUG)Log.v(TAG,"distance: "+distance);

				if (Math.abs(distance) < mTouchSlop) {//16 移动大于这个值才算移动//添加Math.ads解决正在刷新的时候不能上滑
					return false;
				}
				
				// 如果手指是下滑状态，并且下拉头是完全隐藏的，就屏蔽下拉事件 (topMargin 上滑-- 下滑++)
				if (distance <= 0
						&& mHeaderLayoutParams.topMargin <= mHeaderHideHeight) {//hideHeaderHeight=-120 正在刷新的时候不会进去
					if(DEBUG)Log.v(TAG,"return");
					return false;
				}

				int hideHeaderHeight = mHeaderHideHeight;
				scroll_damp = scroll_damp_old;
				if (mCurrentStatus != STATUS_REFRESHING) {
					if (mHeaderLayoutParams.topMargin > 0) {
						mCurrentStatus = STATUS_RELEASE_TO_REFRESH;
					} else {
						mCurrentStatus = STATUS_PULL_TO_REFRESH;
					}
				}else{
					if(distance < 0){
						scroll_damp = 1;
					}
					hideHeaderHeight=0;
				}
				// 通过偏移下拉头的topMargin值，来实现下拉效果
				mHeaderLayoutParams.topMargin = (distance / scroll_damp)
						+ hideHeaderHeight;
				if(DEBUG)Log.v(TAG,"topMargin: "+mHeaderLayoutParams.topMargin);
				mHeaderView.setLayoutParams(mHeaderLayoutParams);
				break;
			case MotionEvent.ACTION_UP:
			default:
				if(DEBUG)Log.v(TAG,"ACTION_UP x: "+x+" y: "+y);
				if (mCurrentStatus == STATUS_RELEASE_TO_REFRESH) {
					// 松手时如果是释放立即刷新状态，就去调用正在刷新的任务
					new RefreshingTask().execute();
				} else if (mCurrentStatus == STATUS_PULL_TO_REFRESH) {
					// 松手时如果是下拉状态，就去调用隐藏下拉头的任务
					new HideHeaderTask().execute();
				}else if(mCurrentStatus == STATUS_REFRESHING && mHeaderLayoutParams.topMargin<-50){
					// 此地只在没有listView的时候会调用到
					cancelRefreshing();
				}else if(mCurrentStatus == STATUS_REFRESHING){
					//如果是正在刷新中，往下拉得太长就返回到中间位置,往上拉就隐藏
					new PullToTask().execute();
				}
				//解决当up的时候让ListView失去焦点
				if(mAdapterView != null){
					mAdapterView.onTouchEvent(event);
				}
				break;
			}
			// 时刻记得更新下拉头中的信息
			if (mCurrentStatus == STATUS_PULL_TO_REFRESH
					|| mCurrentStatus == STATUS_RELEASE_TO_REFRESH) {
				updateHeaderView();
				// 当前正处于下拉或释放状态，要让ListView失去焦点，否则被点击的那一项会一直处于选中状态
				if (mAdapterView != null) {
					mAdapterView.setPressed(false);
					mAdapterView.setFocusable(false);
					mAdapterView.setFocusableInTouchMode(false);
				}
				mLastStatus = mCurrentStatus;
				// 当前正处于下拉或释放状态，通过返回true屏蔽掉ListView的滚动事件
				return true;
			}
		}
		return isInterrupt;
	}

	/**
	 * 根据当前ListView的滚动状态来设定 {@link #mAbleToPull}
	 * 的值，每次都需要在onTouch中第一个执行，这样可以判断出当前 应该是滚动ListView，还是应该进行下拉。
	 * 
	 * @param event
	 */
	private void setIsAbleToPull(AdapterView adapterView, MotionEvent event) {
		if (adapterView == null) {
			if (!mAbleToPull) {
				mDownY = event.getRawY();
			}
			mAbleToPull = true;
			return;
		}
		View firstChild = adapterView.getChildAt(0);
		if (firstChild != null) {
			int firstVisiblePos = adapterView.getFirstVisiblePosition();
			if(DEBUG)Log.v(TAG,"firstChild getTop: " + firstChild.getTop());
			if (firstVisiblePos == 0 && firstChild.getTop() == 0) {
				if (!mAbleToPull) {
					mDownY = event.getRawY();
				}
				// 如果首个元素的上边缘，距离父布局值为0，就说明ListView滚动到了最顶部，此时应该允许下拉刷新
				mAbleToPull = true;
			} else {
				if (mHeaderLayoutParams.topMargin != mHeaderHideHeight) {
					cancelRefreshing();
				}
				mAbleToPull = false;
			}
		} else {
			// 如果ListView中没有元素，也应该允许下拉刷新
			mAbleToPull = true;
		}
	}
	
	/**取消刷新*/
	private void cancelRefreshing(){
		mHeaderLayoutParams.topMargin = mHeaderHideHeight;
		mHeaderView.setLayoutParams(mHeaderLayoutParams);
		if(mCurrentStatus == STATUS_REFRESHING){
			mHandler.sendEmptyMessage(MSG_CANCEL_REFRESH);
		}
	}

	/** 当所有的刷新逻辑完成后，记录调用一下，否则你的ListView将一直处于正在刷新状态 */
	private void finishRefreshing() {
		mCurrentStatus = STATUS_REFRESH_FINISHED;
		mLastPullTimePreferences.edit()
				.putLong(UPDATED_AT + mCallbackId, System.currentTimeMillis())
				.commit();
		new HideHeaderTask().execute();
	}

	/** 更新下拉头中的信息 */
	private void updateHeaderView() {
		if (mLastStatus != mCurrentStatus) {
			if (mCurrentStatus == STATUS_PULL_TO_REFRESH) {
				mHeaderDescription.setText(getResources().getString(
						R.string.pull_to_refresh));
				mHeaderArrowView.setVisibility(View.VISIBLE);
				mHeaderProgressBar.setVisibility(View.GONE);
				rotateArrow();
			} else if (mCurrentStatus == STATUS_RELEASE_TO_REFRESH) {
				mHeaderDescription.setText(getResources().getString(
						R.string.release_to_refresh));
				mHeaderArrowView.setVisibility(View.VISIBLE);
				mHeaderProgressBar.setVisibility(View.GONE);
				rotateArrow();
			} else if (mCurrentStatus == STATUS_REFRESHING) {
				mHeaderDescription.setText(getResources().getString(
						R.string.refreshing));
				mHeaderProgressBar.setVisibility(View.VISIBLE);
				mHeaderArrowView.clearAnimation();
				mHeaderArrowView.setVisibility(View.GONE);
			}
			refreshUpdatedAtValue();
		}
	}

	/** 根据当前的状态来旋转箭头 */
	private void rotateArrow() {
		float pivotX = mHeaderArrowView.getWidth() / 2f;
		float pivotY = mHeaderArrowView.getHeight() / 2f;
		float fromDegrees = 0f;
		float toDegrees = 0f;
		if (mCurrentStatus == STATUS_PULL_TO_REFRESH) {
			fromDegrees = 180f;
			toDegrees = 360f;
		} else if (mCurrentStatus == STATUS_RELEASE_TO_REFRESH) {
			fromDegrees = 0f;
			toDegrees = 180f;
		}
		RotateAnimation animation = new RotateAnimation(fromDegrees, toDegrees,
				pivotX, pivotY);
		animation.setDuration(100);
		animation.setFillAfter(true);
		mHeaderArrowView.startAnimation(animation);
	}

	/**刷新下拉头中上次更新时间的文字描述*/
	private void refreshUpdatedAtValue() {
		mLastUpdateTime = mLastPullTimePreferences.getLong(UPDATED_AT + mCallbackId, -1);
		long currentTime = System.currentTimeMillis();
		long timePassed = currentTime - mLastUpdateTime;
		long timeIntoFormat;
		String updateAtValue;
		if (mLastUpdateTime == -1) {
			updateAtValue = getResources().getString(R.string.not_updated_yet);
		} else if (timePassed < 0) {
			updateAtValue = getResources().getString(R.string.time_error);
		} else if (timePassed < ONE_MINUTE) {
			updateAtValue = getResources().getString(R.string.updated_just_now);
		} else if (timePassed < ONE_HOUR) {
			timeIntoFormat = timePassed / ONE_MINUTE;
			String value = timeIntoFormat + getResources().getString(R.string.minute);
			updateAtValue = String.format(
					getResources().getString(R.string.updated_at), value);
		} else if (timePassed < ONE_DAY) {
			timeIntoFormat = timePassed / ONE_HOUR;
			String value = timeIntoFormat + getResources().getString(R.string.hour);
			updateAtValue = String.format(
					getResources().getString(R.string.updated_at), value);
		} else if (timePassed < ONE_MONTH) {
			timeIntoFormat = timePassed / ONE_DAY;
			String value = timeIntoFormat + getResources().getString(R.string.day);
			updateAtValue = String.format(
					getResources().getString(R.string.updated_at), value);
		} else if (timePassed < ONE_YEAR) {
			timeIntoFormat = timePassed / ONE_MONTH;
			String value = timeIntoFormat + getResources().getString(R.string.month);
			updateAtValue = String.format(
					getResources().getString(R.string.updated_at), value);
		} else {
			timeIntoFormat = timePassed / ONE_YEAR;
			String value = timeIntoFormat + getResources().getString(R.string.year);
			updateAtValue = String.format(
					getResources().getString(R.string.updated_at), value);
		}
		mHeaderViewUpdateTime.setText(updateAtValue);
	}

	private static final int MSG_FINISH_REFRESH = 1;
	private static final int MSG_CANCEL_REFRESH = 2;

	private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MSG_FINISH_REFRESH:
				finishRefreshing();
				mListener.onTimeOut();
				break;
			case MSG_CANCEL_REFRESH:
				mHandler.removeMessages(MSG_FINISH_REFRESH);
				mCurrentStatus = STATUS_REFRESH_FINISHED;
				mListener.onCancel();
				break;
			}
		}
	};

	/** 如果是正在刷新中，往下拉得太长就返回到中间位置,或者不小心往上一点也返回到中间位置 */
	private class PullToTask extends AsyncTask<Void, Integer, Void> {
		@Override
		protected Void doInBackground(Void... params) {
			int topMargin = mHeaderLayoutParams.topMargin;
			int oldTopMargin = topMargin;
			while (true) {
				if (oldTopMargin >= 0) {
					topMargin = topMargin + SCROLL_SPEED;
					if (topMargin <= 0) {// 往下拉得太长就返回到中间位置
						topMargin = 0;
						break;
					}
				} else {
					topMargin = topMargin - SCROLL_SPEED;
					if (topMargin >=0) {// 不小心往上一点也返回到中间位置
						topMargin = 0;
						break;
					}
				}
				publishProgress(topMargin);
				sleep(10);
			}
			return null;
		}

		@Override
		protected void onProgressUpdate(Integer... topMargin) {
			mHeaderLayoutParams.topMargin = topMargin[0];
			mHeaderView.setLayoutParams(mHeaderLayoutParams);
		}
	}

	/** 正在刷新的任务，在此任务中会去回调注册进来的下拉刷新监听器 */
	private class RefreshingTask extends AsyncTask<Void, Integer, Void> {
		@Override
		protected Void doInBackground(Void... params) {
			int topMargin = mHeaderLayoutParams.topMargin;
			while (true) {
				topMargin = topMargin + SCROLL_SPEED;
				if (topMargin <= 0) {// 往下拉得太长返回到中间位置
					topMargin = 0;
					break;
				}
				publishProgress(topMargin);
				sleep(10);
			}
			mCurrentStatus = STATUS_REFRESHING;
			publishProgress(0);
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			if (mListener != null) {
				mListener.onRefresh();
				mHandler.sendEmptyMessageDelayed(MSG_FINISH_REFRESH, mTimeOut);
			}
		}

		@Override
		protected void onProgressUpdate(Integer... topMargin) {
			updateHeaderView();
			mHeaderLayoutParams.topMargin = topMargin[0];
			mHeaderView.setLayoutParams(mHeaderLayoutParams);
		}
	}

	/** 隐藏下拉头的任务，当未进行下拉刷新或下拉刷新完成后，此任务将会使下拉头重新隐藏 */
	private class HideHeaderTask extends AsyncTask<Void, Integer, Integer> {
		@Override
		protected Integer doInBackground(Void... params) {
			int topMargin = mHeaderLayoutParams.topMargin;
			while (true) {
				topMargin = topMargin + SCROLL_SPEED;
				if (topMargin <= mHeaderHideHeight) {
					topMargin = mHeaderHideHeight;
					break;
				}
				publishProgress(topMargin);
				sleep(10);
			}
			return topMargin;
		}

		@Override
		protected void onProgressUpdate(Integer... topMargin) {
			mHeaderLayoutParams.topMargin = topMargin[0];
			mHeaderView.setLayoutParams(mHeaderLayoutParams);
		}

		@Override
		protected void onPostExecute(Integer topMargin) {
			mHeaderLayoutParams.topMargin = topMargin;
			mHeaderView.setLayoutParams(mHeaderLayoutParams);
			mCurrentStatus = STATUS_REFRESH_FINISHED;
		}
	}

	private void sleep(int time) {
		try {
			Thread.sleep(time);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 	给下拉刷新控件注册一个监听器
	 * 	@param id 为了防止不同界面的下拉刷新在上次更新时间上互相有冲突，
	 * 	请不同界面在注册下拉刷新监听器时一定要传入不同的id。
	 * @param timeOut 设置刷新超时等待时间
	 * @param pullToRefreshListener 监听器的实现
	 */
	public void setOnRefreshListener(int id,long timeOut,PullToRefreshListener pullToRefreshListener) {
		mCallbackId = id;
		mTimeOut = timeOut;
		mListener = pullToRefreshListener;
	}

	public interface PullToRefreshListener {
		
		/** 刷新回调 */
		void onRefresh();

		/** 取消回调 */
		void onCancel();

		/** 超时回调 */
		void onTimeOut();
	}

}