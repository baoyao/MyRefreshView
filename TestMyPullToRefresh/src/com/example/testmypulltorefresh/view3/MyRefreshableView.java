package com.example.testmypulltorefresh.view3;

import com.example.testmypulltorefresh.R;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewConfiguration;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

/**
 * @author houen.bao
 * @date Jun 20, 2016 10:41:28 AM
 */

public class MyRefreshableView extends LinearLayout implements OnTouchListener {
	

	private final String TAG=MyRefreshableView.class.getSimpleName();
	
	private final boolean DEBUG = true;
	/**
	 * 下拉头部回滚的速度
	 */
	private static final int SCROLL_SPEED = -20;
	
	/**
	 * 超时时间
	 */
	private long mTimeOut = 5*1000;
	
	/**
	 * 下拉的阻尼
	 */
	private static final int SCROLL_DAMP= 5;
	/**
	 * 下拉状态
	 */
	private static final int STATUS_PULL_TO_REFRESH = 0;
	/**
	 * 释放立即刷新状态
	 */
	private static final int STATUS_RELEASE_TO_REFRESH = 1;
	/**
	 * 正在刷新状态
	 */
	private static final int STATUS_REFRESHING = 2;
	/**
	 * 刷新完成或未刷新状态
	 */
	private static final int STATUS_REFRESH_FINISHED = 3;
	
	/**
	 * 一分钟的毫秒值，用于判断上次的更新时间
	 */
	private static final long ONE_MINUTE = 60 * 1000;
	/**
	 * 一小时的毫秒值，用于判断上次的更新时间
	 */
	private static final long ONE_HOUR = 60 * ONE_MINUTE;
	/**
	 * 一天的毫秒值，用于判断上次的更新时间
	 */
	private static final long ONE_DAY = 24 * ONE_HOUR;
	/**
	 * 一月的毫秒值，用于判断上次的更新时间
	 */
	private static final long ONE_MONTH = 30 * ONE_DAY;
	/**
	 * 一年的毫秒值，用于判断上次的更新时间
	 */
	private static final long ONE_YEAR = 12 * ONE_MONTH;
	/**
	 * 上次更新时间的字符串常量，用于作为SharedPreferences的键值
	 */
	private static final String UPDATED_AT = "updated_at";
	/**
	 * 下拉刷新的回调接口
	 */
	private PullToRefreshListener mListener;
	/**
	 * 用于存储上次更新时间
	 */
	private SharedPreferences preferences;
	/**
	 * 下拉头的View
	 */
	private View header;
	/**
	 * 需要去下拉刷新的ListView
	 */
//	private ListView listView;
	/**
	 * 刷新时显示的进度条
	 */
	private ProgressBar progressBar;
	/**
	 * 指示下拉和释放的箭头
	 */
	private ImageView arrow;
	/**
	 * 指示下拉和释放的文字描述
	 */
	private TextView description;
	/**
	 * 上次更新时间的文字描述
	 */
	private TextView updateAt;
	/**
	 * 下拉头的布局参数
	 */
	private MarginLayoutParams headerLayoutParams;
	/**
	 * 上次更新时间的毫秒值
	 */
	private long lastUpdateTime;
	/**
	 * 为了防止不同界面的下拉刷新在上次更新时间上互相有冲突，使用id来做区分
	 */
	private int mId = -1;
	/**
	 * 下拉头的高度
	 */
	private int mHideHeaderHeight;
	/**
	 * 当前处理什么状态，可选值有STATUS_PULL_TO_REFRESH, STATUS_RELEASE_TO_REFRESH,
	 * STATUS_REFRESHING 和 STATUS_REFRESH_FINISHED
	 */
	private int currentStatus = STATUS_REFRESH_FINISHED;;
	/**
	 * 记录上一次的状态是什么，避免进行重复操作
	 */
	private int lastStatus = currentStatus;
	/**
	 * 手指按下时的屏幕纵坐标
	 */
	private float yDown;
	/**
	 * 在被判定为滚动之前用户手指可以移动的最大值。
	 */
	private int touchSlop;
	/**
	 * 是否已加载过一次layout，这里onLayout中的初始化只需加载一次
	 */
	private boolean loadOnce;
	/**
	 * 当前是否可以下拉，只有ListView滚动到头的时候才允许下拉
	 */
	private boolean ableToPull;

	/**
	 * 下拉刷新控件的构造函数，会在运行时动态添加一个下拉头的布局。
	 * 
	 * @param context
	 * @param attrs
	 */
	public MyRefreshableView(Context context, AttributeSet attrs) {
		super(context, attrs);
		preferences = PreferenceManager.getDefaultSharedPreferences(context);
		header = LayoutInflater.from(context).inflate(R.layout.pull_to_refresh,
				null, true);
		progressBar = (ProgressBar) header.findViewById(R.id.progress_bar);
		arrow = (ImageView) header.findViewById(R.id.arrow);
		description = (TextView) header.findViewById(R.id.description);
		updateAt = (TextView) header.findViewById(R.id.updated_at);
		touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();//16 移动大于这个值才算移动
		refreshUpdatedAtValue();
		setOrientation(VERTICAL);
		addView(header, 0);
	}

	/**
	 * 进行一些关键性的初始化操作，比如：将下拉头向上偏移进行隐藏，给ListView注册touch事件。
	 */
	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		super.onLayout(changed, l, t, r, b);
		if (changed && !loadOnce) {
			mHideHeaderHeight = -header.getHeight();
			headerLayoutParams = (MarginLayoutParams) header.getLayoutParams();
			headerLayoutParams.topMargin = mHideHeaderHeight;
//			listView = (ListView) getChildAt(2);
//			listView.setOnTouchListener(this);
//			this.setOnTouchListener(this);
			loadOnce = true;
		}
	}
	
	

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		
		return touch(event);
	}

	/**
	 * 当ListView被触摸时调用，其中处理了各种下拉刷新的具体逻辑。
	 */
	@Override
	public boolean onTouch(View v, MotionEvent event) {
		return false;
	}
	
	private boolean touch(MotionEvent event){
		setIsAbleToPull(event);
		if (ableToPull) {
			float x=event.getRawX();
			float y=event.getRawY();
			switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				yDown = y;
				if(DEBUG)Log.v(TAG,"\n\n\nACTION_DOWN x: "+x+" y: "+y);
				break;
			case MotionEvent.ACTION_MOVE:
				if(DEBUG)Log.v(TAG,"ACTION_MOVE x: "+x+" y: "+y);
				if(DEBUG)Log.v(TAG,"Status: "+currentStatus);
				int distance = (int) (y - yDown);//(distance 上滑-- 下滑++)
				if (Math.abs(distance) < touchSlop) {//16 移动大于这个值才算移动//添加Math.ads解决正在刷新的时候不能上滑
					return false;
				}
				if(DEBUG)Log.v(TAG,"distance: "+distance);
				if(DEBUG)Log.v(TAG,"topMargin: "+headerLayoutParams.topMargin);
				
				// 如果手指是下滑状态，并且下拉头是完全隐藏的，就屏蔽下拉事件 (topMargin 上滑-- 下滑++)
				if (distance <= 0
						&& headerLayoutParams.topMargin <= mHideHeaderHeight) {//hideHeaderHeight=-120 正在刷新的时候不会进去
					if(DEBUG)Log.v(TAG,"return");
					return false;
				}
				
				int hideHeaderHeight = mHideHeaderHeight;
				if (currentStatus != STATUS_REFRESHING) {
					if (headerLayoutParams.topMargin > 0) {
						currentStatus = STATUS_RELEASE_TO_REFRESH;
					} else {
						currentStatus = STATUS_PULL_TO_REFRESH;
					}
				}else{
					hideHeaderHeight=0;
				}
				// 通过偏移下拉头的topMargin值，来实现下拉效果
				headerLayoutParams.topMargin = (distance / SCROLL_DAMP)
						+ hideHeaderHeight;
				if(DEBUG)Log.v(TAG,"topMargin: "+headerLayoutParams.topMargin);
				header.setLayoutParams(headerLayoutParams);
				break;
			case MotionEvent.ACTION_UP:
			default:
				if(DEBUG)Log.v(TAG,"ACTION_UP x: "+x+" y: "+y);
				if (currentStatus == STATUS_RELEASE_TO_REFRESH) {
					// 松手时如果是释放立即刷新状态，就去调用正在刷新的任务
					new RefreshingTask().execute();
				} else if (currentStatus == STATUS_PULL_TO_REFRESH) {
					// 松手时如果是下拉状态，就去调用隐藏下拉头的任务
					new HideHeaderTask().execute();
				}else if(currentStatus == STATUS_REFRESHING){
					//如果是正在刷新中，往下拉得太长就返回到中间位置,往上拉就隐藏
					new PullToTask().execute();
				}
				//解决当up的时候让ListView失去焦点
//				listView.onTouchEvent(event);
				break;
			}
			// 时刻记得更新下拉头中的信息
			if (currentStatus == STATUS_PULL_TO_REFRESH
					|| currentStatus == STATUS_RELEASE_TO_REFRESH) {
				updateHeaderView();
				// 当前正处于下拉或释放状态，要让ListView失去焦点，否则被点击的那一项会一直处于选中状态
//				listView.setPressed(false);
//				listView.setFocusable(false);
//				listView.setFocusableInTouchMode(false);
				lastStatus = currentStatus;
				// 当前正处于下拉或释放状态，通过返回true屏蔽掉ListView的滚动事件
				return true;
			}
		}
		return true;
	}

	/**
	 * 给下拉刷新控件注册一个监听器。
	 * 	@param id
	 *            为了防止不同界面的下拉刷新在上次更新时间上互相有冲突， 请不同界面在注册下拉刷新监听器时一定要传入不同的id。
	 * @param timeOut 设置刷新超时等待时间
	 * @param listener 监听器的实现
	 */
	public void setOnRefreshListener(int id,long timeOut,PullToRefreshListener listener) {
		mId = id;
		mTimeOut = timeOut;
		mListener = listener;
	}

	/**
	 * 当所有的刷新逻辑完成后，记录调用一下，否则你的ListView将一直处于正在刷新状态。
	 */
	private void finishRefreshing() {
		currentStatus = STATUS_REFRESH_FINISHED;
		preferences.edit()
				.putLong(UPDATED_AT + mId, System.currentTimeMillis()).commit();
		new HideHeaderTask().execute();
	}

	/**
	 * 根据当前ListView的滚动状态来设定 {@link #ableToPull}
	 * 的值，每次都需要在onTouch中第一个执行，这样可以判断出当前应该是滚动ListView，还是应该进行下拉。
	 * 
	 * @param event
	 */
	private void setIsAbleToPull(MotionEvent event) {
//		View firstChild = listView.getChildAt(0);
//		if (firstChild != null) {
		if (true) {
//			int firstVisiblePos = listView.getFirstVisiblePosition();
//			if (firstVisiblePos == 0 && firstChild.getTop() == 0) {
			if (true) {
				if (!ableToPull) {
					yDown = event.getRawY();
				}
				// 如果首个元素的上边缘，距离父布局值为0，就说明ListView滚动到了最顶部，此时应该允许下拉刷新
				ableToPull = true;
			} else {
				if (headerLayoutParams.topMargin != mHideHeaderHeight) {
					headerLayoutParams.topMargin = mHideHeaderHeight;
					header.setLayoutParams(headerLayoutParams);
				}
				ableToPull = false;
			}
		} else {
			// 如果ListView中没有元素，也应该允许下拉刷新
			ableToPull = true;
		}
	}

	/**
	 * 更新下拉头中的信息。
	 */
	private void updateHeaderView() {
		if (lastStatus != currentStatus) {
			if (currentStatus == STATUS_PULL_TO_REFRESH) {
				description.setText(getResources().getString(
						R.string.pull_to_refresh));
				arrow.setVisibility(View.VISIBLE);
				progressBar.setVisibility(View.GONE);
				rotateArrow();
			} else if (currentStatus == STATUS_RELEASE_TO_REFRESH) {
				description.setText(getResources().getString(
						R.string.release_to_refresh));
				arrow.setVisibility(View.VISIBLE);
				progressBar.setVisibility(View.GONE);
				rotateArrow();
			} else if (currentStatus == STATUS_REFRESHING) {
				description.setText(getResources().getString(
						R.string.refreshing));
				progressBar.setVisibility(View.VISIBLE);
				arrow.clearAnimation();
				arrow.setVisibility(View.GONE);
			}
			refreshUpdatedAtValue();
		}
	}

	/**
	 * 根据当前的状态来旋转箭头。
	 */
	private void rotateArrow() {
		float pivotX = arrow.getWidth() / 2f;
		float pivotY = arrow.getHeight() / 2f;
		float fromDegrees = 0f;
		float toDegrees = 0f;
		if (currentStatus == STATUS_PULL_TO_REFRESH) {
			fromDegrees = 180f;
			toDegrees = 360f;
		} else if (currentStatus == STATUS_RELEASE_TO_REFRESH) {
			fromDegrees = 0f;
			toDegrees = 180f;
		}
		RotateAnimation animation = new RotateAnimation(fromDegrees, toDegrees,
				pivotX, pivotY);
		animation.setDuration(100);
		animation.setFillAfter(true);
		arrow.startAnimation(animation);
	}

	/**
	 * 刷新下拉头中上次更新时间的文字描述。
	 */
	private void refreshUpdatedAtValue() {
		lastUpdateTime = preferences.getLong(UPDATED_AT + mId, -1);
		long currentTime = System.currentTimeMillis();
		long timePassed = currentTime - lastUpdateTime;
		long timeIntoFormat;
		String updateAtValue;
		if (lastUpdateTime == -1) {
			updateAtValue = getResources().getString(R.string.not_updated_yet);
		} else if (timePassed < 0) {
			updateAtValue = getResources().getString(R.string.time_error);
		} else if (timePassed < ONE_MINUTE) {
			updateAtValue = getResources().getString(R.string.updated_just_now);
		} else if (timePassed < ONE_HOUR) {
			timeIntoFormat = timePassed / ONE_MINUTE;
			String value = timeIntoFormat + "分钟";
			updateAtValue = String.format(
					getResources().getString(R.string.updated_at), value);
		} else if (timePassed < ONE_DAY) {
			timeIntoFormat = timePassed / ONE_HOUR;
			String value = timeIntoFormat + "小时";
			updateAtValue = String.format(
					getResources().getString(R.string.updated_at), value);
		} else if (timePassed < ONE_MONTH) {
			timeIntoFormat = timePassed / ONE_DAY;
			String value = timeIntoFormat + "天";
			updateAtValue = String.format(
					getResources().getString(R.string.updated_at), value);
		} else if (timePassed < ONE_YEAR) {
			timeIntoFormat = timePassed / ONE_MONTH;
			String value = timeIntoFormat + "个月";
			updateAtValue = String.format(
					getResources().getString(R.string.updated_at), value);
		} else {
			timeIntoFormat = timePassed / ONE_YEAR;
			String value = timeIntoFormat + "年";
			updateAtValue = String.format(
					getResources().getString(R.string.updated_at), value);
		}
		updateAt.setText(updateAtValue);
	}
	/**
	 * 如果是正在刷新中，往下拉得太长就返回到中间位置,往上拉就隐藏
	 * @author houen.bao
	 *
	 */
	class PullToTask extends AsyncTask<Void, Integer, Void> {
		@Override
		protected Void doInBackground(Void... params) {
			int topMargin = headerLayoutParams.topMargin;
			int oldTopMargin=topMargin;
			while (true) {
				if(oldTopMargin>=0){
					topMargin = topMargin + SCROLL_SPEED;//往下拉得太长就返回到中间位置
					if (topMargin <= 0) {
						topMargin = 0;
						break;
					}
				}else{
					topMargin = topMargin + SCROLL_SPEED;//往上拉就隐藏
					if (topMargin <= mHideHeaderHeight) {
						topMargin = mHideHeaderHeight;
						break;
					}
				}
				publishProgress(topMargin);
				sleep(10);
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			
		}

		@Override
		protected void onProgressUpdate(Integer... topMargin) {
			headerLayoutParams.topMargin = topMargin[0];
			header.setLayoutParams(headerLayoutParams);
		}
	}

	/**
	 * 正在刷新的任务，在此任务中会去回调注册进来的下拉刷新监听器。
	 * 
	 * @author guolin
	 */
	class RefreshingTask extends AsyncTask<Void, Integer, Void> {
		@Override
		protected Void doInBackground(Void... params) {
			int topMargin = headerLayoutParams.topMargin;
			while (true) {
				topMargin = topMargin + SCROLL_SPEED;//拉的太长返回原位
				if (topMargin <= 0) {
					topMargin = 0;
					break;
				}
				publishProgress(topMargin);
				sleep(10);
			}
			currentStatus = STATUS_REFRESHING;
			publishProgress(0);
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			if (mListener != null) {
				mListener.onRefresh();
				new Handler().postDelayed(new Runnable(){
					@Override
					public void run() {
						finishRefreshing();
					}
				},mTimeOut);
			}
		}

		@Override
		protected void onProgressUpdate(Integer... topMargin) {
			updateHeaderView();
			headerLayoutParams.topMargin = topMargin[0];
			header.setLayoutParams(headerLayoutParams);
		}
	}

	/**
	 * 隐藏下拉头的任务，当未进行下拉刷新或下拉刷新完成后，此任务将会使下拉头重新隐藏。
	 * 
	 * @author guolin
	 */
	class HideHeaderTask extends AsyncTask<Void, Integer, Integer> {
		@Override
		protected Integer doInBackground(Void... params) {
			int topMargin = headerLayoutParams.topMargin;
			while (true) {
				topMargin = topMargin + SCROLL_SPEED;
				if (topMargin <= mHideHeaderHeight) {
					topMargin = mHideHeaderHeight;
					break;
				}
				publishProgress(topMargin);
				sleep(10);
			}
			return topMargin;
		}

		@Override
		protected void onProgressUpdate(Integer... topMargin) {
			headerLayoutParams.topMargin = topMargin[0];
			header.setLayoutParams(headerLayoutParams);
		}

		@Override
		protected void onPostExecute(Integer topMargin) {
			headerLayoutParams.topMargin = topMargin;
			header.setLayoutParams(headerLayoutParams);
			currentStatus = STATUS_REFRESH_FINISHED;
		}
	}

	/**
	 * 使当前线程睡眠指定的毫秒数。
	 * 
	 * @param time
	 *            指定当前线程睡眠多久，以毫秒为单位
	 */
	private void sleep(int time) {
		try {
			Thread.sleep(time);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	

	/**
	 * 下拉刷新的监听器，使用下拉刷新的地方应该注册此监听器来获取刷新回调。
	 * 
	 * @author guolin
	 */
	public interface PullToRefreshListener {
		/**
		 * 刷新时会去回调此方法，在方法内编写具体的刷新逻辑。注意此方法是在子线程中调用的， 你可以不必另开线程来进行耗时操作。
		 */
		void onRefresh();
		/**
		 * 超时回调
		 */
		void onTimeOut();
	}
}