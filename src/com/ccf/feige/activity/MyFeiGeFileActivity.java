package com.ccf.feige.activity;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

/**
 * �ļ�����࣬�������û�ѡ���ļ���������Ҫ�����ļ��ľ���·��
 * @author ccf
 * 
 * 2012/2/23
 *
 */
public class MyFeiGeFileActivity extends MyFeiGeBaseActivity implements OnItemClickListener, OnClickListener{
	private final static String TAG = "MyFeiGeFileActivity";
	
	private String path = "/";	//��ǰ·��
	
	private ListView itemList;
	private TextView filePath;
	private Button sendButton;
	
	private List<Map<String, Object>> adapterList;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.files);
		
		findViews();
		
		refreshListItems(path);
	}

	// ˢ��ListView
	private void refreshListItems(String path) {
		// TODO Auto-generated method stub
		filePath.setText(path);
		adapterList = buildListForSimpleAdapter(path);
		SimpleAdapter listAdapter = new SimpleAdapter(this, adapterList, R.layout.file_item, 
				new String[]{"name", "path", "img"}, 
				new int[]{R.id.file_name, R.id.file_path, R.id.file_img});
		
		itemList.setAdapter(listAdapter);
		itemList.setOnItemClickListener(this);
		itemList.setSelection(0);
		
	}

	private List<Map<String, Object>> buildListForSimpleAdapter(String path) {
		// TODO Auto-generated method stub
		File nowFile = new File(path);
		
		
		adapterList = new ArrayList<Map<String, Object>>();
		
		//���ϸ�Ŀ¼
		Map<String, Object> root = new HashMap<String, Object>();
		root.put("name", "/");
		root.put("img", R.drawable.file_root);
		root.put("path", "�ظ�Ŀ¼");
		adapterList.add(root);
		
		//���ϸ�Ŀ¼
		Map<String, Object> pMap = new HashMap<String, Object>();
		pMap.put("name", "..");
		pMap.put("img", R.drawable.file_parent);
		pMap.put("path", "��һ��");
		adapterList.add(pMap);
		
		if(!nowFile.isDirectory()){	//���ǵ�ǰ·����Ӧ�����ļ����򷵻�
			sendButton.setEnabled(true);	//���Ͱ�ť����
			return adapterList;
		}
		
		File[] files = nowFile.listFiles();	//�õ�path�������ļ�
		
		for(File file:files){
			Map<String, Object> map = new HashMap<String, Object>();
			map.put("name", file.getName());
			map.put("path", file.getPath());
			if(file.isDirectory()){
				map.put("img", R.drawable.file_directory);
			}else{
				map.put("img", R.drawable.file_doc);
			}
			adapterList.add(map);
		}
		
		sendButton.setEnabled(false);	//��ǰ·����Ӧ�����ļ��У����Ͱ�ť������
		return adapterList;
	}

	private void findViews() {
		// TODO Auto-generated method stub
		itemList = (ListView) findViewById(R.id.file_detail);
		filePath = (TextView) findViewById(R.id.file_path);
		sendButton = (Button) findViewById(R.id.file_send);
		sendButton.setOnClickListener(this);
		sendButton.setEnabled(false);	//��ʼʱ���ɵ����ֻ��ѡ�е�·�����ļ�ʱ�ſ��Ե��
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
		// TODO Auto-generated method stub
		Log.i(TAG, "λ��[" + position + "]�ϵ�item�����");
		if(position == 0){	//�ظ�Ŀ¼
			path = "/";
			refreshListItems(path);
		}else if(position == 1){	//�ص���һ��
			goToParent();
		}else{
			path = (String) adapterList.get(position).get("path");
			refreshListItems(path);
		}
	}

	private void goToParent() {
		// TODO Auto-generated method stub
		File file = new File(path);
		File pFile = file.getParentFile();	//�õ����ļ�
		if(pFile == null){
			Toast.makeText(this, "��ǰ·���Ѿ��Ǹ�Ŀ¼����������һ��", 
					Toast.LENGTH_SHORT).show();
			refreshListItems(path);
		}else{
			path = pFile.getAbsolutePath();
			refreshListItems(path);
		}
	
	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		
		Intent intent = new Intent();
		intent.putExtra("filePaths", path);
		setResult(RESULT_OK, intent);
		finish();
	}

	@Override
	public void processMessage(Message msg) {
		// TODO Auto-generated method stub
		
	}

}
