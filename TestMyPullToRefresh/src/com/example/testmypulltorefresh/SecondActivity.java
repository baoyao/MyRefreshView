package com.example.testmypulltorefresh;

import android.app.Activity;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.example.testmypulltorefresh.view3.MyRefreshableView;
import com.example.testmypulltorefresh.view3.MyRefreshableView.PullToRefreshListener;

public class SecondActivity extends Activity {

	MyRefreshableView refreshableView;
	ListView listView;
	ArrayAdapter<String> adapter;
	String[] items = { "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K",
			"L" };

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.second);
		
		listView = (ListView) findViewById(R.id.list_view);
		adapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1, items);
		listView.setAdapter(adapter);
		
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
