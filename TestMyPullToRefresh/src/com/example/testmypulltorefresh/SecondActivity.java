package com.example.testmypulltorefresh;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ListView;

import com.example.testmypulltorefresh.view3.MyRefreshableView;
import com.example.testmypulltorefresh.view3.MyRefreshableView.PullToRefreshListener;

public class SecondActivity extends Activity {

	MyRefreshableView refreshableView;
	ListView listView;
	GridView gridView;
	ArrayAdapter<String> adapter;
	String[] items = { "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K",
			"L" };

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.second);
		
//		listView = (ListView) findViewById(R.id.list_view);
//		adapter = new ArrayAdapter<String>(this,
//				android.R.layout.simple_list_item_1, items);
//		listView.setAdapter(adapter);
//		listView.setOnItemClickListener(new OnItemClickListener(){
//			@Override
//			public void onItemClick(AdapterView<?> parent, View view,
//					int position, long id) {
//				Log.v("tt","onItemClick "+position);
//				
//			}
//		});
		
//		gridView = (GridView) findViewById(R.id.grid_view);
//		adapter = new ArrayAdapter<String>(this,
//				android.R.layout.simple_list_item_1, items);
//		gridView.setAdapter(adapter);
//		gridView.setOnItemClickListener(new OnItemClickListener(){
//			@Override
//			public void onItemClick(AdapterView<?> parent, View view,
//					int position, long id) {
//				Log.v("tt","onItemClick "+position);
//				
//			}
//		});
		
		
		
		
		refreshableView = (MyRefreshableView) findViewById(R.id.refreshable_view);
		refreshableView.setOnRefreshListener(0,5000,new PullToRefreshListener() {
			@Override
			public void onRefresh() {
				Log.v("tt","onRefresh");
			}

			@Override
			public void onTimeOut() {
				Log.v("tt","onTimeOut");
			}

			@Override
			public void onCancel() {
				Log.v("tt","onCancel");
			}
		});
	}
	
	public void btnClick(View v){
		Log.v("tt","btnClick");
	}
}
