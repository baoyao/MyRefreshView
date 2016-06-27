package com.example.testmypulltorefresh;

import android.app.Activity;
import android.os.Bundle;
import android.widget.ArrayAdapter;

import com.example.testmypulltorefresh.view3.MyRefreshableView;
import com.example.testmypulltorefresh.view3.MyRefreshableView.PullToRefreshListener;

public class SecondActivity extends Activity {

	MyRefreshableView refreshableView;
	ArrayAdapter<String> adapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.second);
		refreshableView = (MyRefreshableView) findViewById(R.id.refreshable_view);
		refreshableView.setOnRefreshListener(0,5000,new PullToRefreshListener() {
			@Override
			public void onRefresh() {
				
			}

			@Override
			public void onTimeOut() {
				
			}
		});
	}
}
