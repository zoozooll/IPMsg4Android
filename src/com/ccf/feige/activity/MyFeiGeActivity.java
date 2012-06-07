package com.ccf.feige.activity;



import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import org.apache.http.conn.util.InetAddressUtils;

import com.ccf.feige.adapter.UserExpandableListAdapter;
import com.ccf.feige.data.ChatMessage;
import com.ccf.feige.data.User;
import com.ccf.feige.utils.IpMessageConst;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.TextView;
import android.widget.Toast;

public class MyFeiGeActivity extends MyFeiGeBaseActivity implements OnClickListener{
	public static String hostIp;
	
	private ExpandableListView userList;
	
	private UserExpandableListAdapter adapter;
	private List<String> strGroups; //����һ���˵����Ƽ���
	private List<List<User>> children;
	
	private TextView totalUser;
	private Button refreshButton;
	private TextView ipTextView;;
    /** Called when the activity is first created. */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        if(!isWifiActive()){	//��wifiû�д򿪣���ʾ
        	Toast.makeText(this, R.string.no_wifi, Toast.LENGTH_LONG).show();
        }
        
        
        findViews();
        
        strGroups = new ArrayList<String>(); //����һ���˵����Ƽ���
		children = new ArrayList<List<User>>();
        
//        netThreadHelper = NetThreadHelper.newInstance();
        netThreadHelper.connectSocket();	//��ʼ��������
        netThreadHelper.noticeOnline();	//�㲥����
        
        adapter = new UserExpandableListAdapter(this, strGroups, children);
        userList.setAdapter(adapter);
        
        refreshButton.setOnClickListener(this);
        refreshViews();
    }
    
	@Override
	public void finish() {
		// TODO Auto-generated method stub
		super.finish();
		netThreadHelper.noticeOffline();	//֪ͨ����
		netThreadHelper.disconnectSocket(); //ֹͣ����
		
	}



	private void findViews() {
		// TODO Auto-generated method stub
		totalUser =(TextView) findViewById(R.id.totalUser);
		userList = (ExpandableListView) findViewById(R.id.userlist);
		refreshButton = (Button) findViewById(R.id.refresh);
		ipTextView = (TextView) findViewById(R.id.mymood);
		hostIp = getLocalIpAddress();
		ipTextView.setText(hostIp);	//����IP
	}


	@Override
	public void processMessage(Message msg) {
		// TODO Auto-generated method stub
		switch(msg.what){
		case IpMessageConst.IPMSG_BR_ENTRY:
		case IpMessageConst.IPMSG_BR_EXIT:
		case IpMessageConst.IPMSG_ANSENTRY:
		case IpMessageConst.IPMSG_SENDMSG:
			refreshViews();	
			break;
		}
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// TODO Auto-generated method stub
		if (keyCode == KeyEvent.KEYCODE_BACK)
			exit();
		return true;
	}
	
	//�������ݺ�UI��ʾ
	private void refreshViews(){	
		//�������
		strGroups.clear();
		children.clear();
		
		Map<String,User> currentUsers = new HashMap<String, User>();
		currentUsers.putAll(netThreadHelper.getUsers());
		Queue<ChatMessage> msgQueue = netThreadHelper.getReceiveMsgQueue();
		Map<String, Integer> ip2Msg = new HashMap<String, Integer>();	//IP��ַ��δ����Ϣ������map
		//������Ϣ���У����ip2Msg
		Iterator<ChatMessage> it = msgQueue.iterator();
		while(it.hasNext()){
			ChatMessage chatMsg = it.next();
			String ip = chatMsg.getSenderIp();	//�õ���Ϣ������IP
			Integer tempInt = ip2Msg.get(ip);
			if(tempInt == null){	//��map��û��IP��Ӧ����Ϣ����,���IP��ӽ�ȥ,ֵΪ1
				ip2Msg.put(ip, 1);
			}else{	//���Ѿ��ж�Ӧip������ֵ��һ
				ip2Msg.put(ip, ip2Msg.get(ip)+1);
			}
		}
		
		//����currentUsers,����strGroups��children
		Iterator<String> iterator = currentUsers.keySet().iterator();
		while (iterator.hasNext()) {
			User user = currentUsers.get(iterator.next());	
			//����ÿ�������û���Ӧ��δ����Ϣ����
			if(ip2Msg.get(user.getIp()) == null){
				user.setMsgCount(0);
			}else{
				user.setMsgCount(ip2Msg.get(user.getIp()));
			}
			
			String groupName = user.getGroupName();
			int index = strGroups.indexOf(groupName);
			if(index == -1){ //û����Ӧ���飬����ӷ��飬����Ӷ�Ӧchild
				strGroups.add(groupName);
//				List<Map<String,String>> childData = new ArrayList<Map<String,String>>();
//				Map<String, String> child = new HashMap<String,String>();
//				child.put("userName", user.getUserName());
//				childData.add(child);
//				children.add(childData);
				
				List<User> childData = new ArrayList<User>();
				childData.add(user);
				children.add(childData);
			}else{	//�Ѵ��ڷ��飬�򽫶�Ӧchild��ӵ����Ӧ������
//				Map<String,String> child = new HashMap<String,String>();
//				child.put("userName", user.getUserName());
//				children.get(index).add(child);
				children.get(index).add(user);
			}
			
		}
		
		//����groups
//		for(int i = 0; i < strGroups.size(); i++){
//			Map<String,String> groupMap = new HashMap<String,String>();
//			groupMap.put("group", strGroups.get(i));
//			groups.add(groupMap);
//		}
		
		
		adapter.notifyDataSetChanged();	//����ExpandableListView
		
		String countStr = "��ǰ����" + currentUsers.size() +"���û�";
        totalUser.setText(countStr);	//����TextView
		
	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		if(v.equals(refreshButton)){	//����ˢ��
			netThreadHelper.refreshUsers();
			refreshViews();
		}
	
	}
	
	//�ж�wifi�Ƿ��
	public boolean isWifiActive(){
		ConnectivityManager mConnectivity = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		if(mConnectivity != null){
			NetworkInfo[] infos = mConnectivity.getAllNetworkInfo();
			
			if(infos != null){
				for(NetworkInfo ni: infos){
					if("WIFI".equals(ni.getTypeName()) && ni.isConnected())
						return true;
				}
			}
		}
		
		return false;
	}
	
	//�õ�����IP��ַ
	public String getLocalIpAddress(){
		try{
			Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); 
			while(en.hasMoreElements()){
				NetworkInterface nif = en.nextElement();
				Enumeration<InetAddress> enumIpAddr = nif.getInetAddresses();
				while(enumIpAddr.hasMoreElements()){
					InetAddress mInetAddress = enumIpAddr.nextElement();
					if(!mInetAddress.isLoopbackAddress() && InetAddressUtils.isIPv4Address(mInetAddress.getHostAddress())){
						return mInetAddress.getHostAddress().toString();
					}
				}
			}
		}catch(SocketException ex){
			Log.e("MyFeiGeActivity", "��ȡ����IP��ַʧ��");
		}
		
		return null;
	}
	
	//��ȡ����MAC��ַ
	public String getLocalMacAddress(){
		WifiManager wifi = (WifiManager)getSystemService(Context.WIFI_SERVICE);
		WifiInfo info = wifi.getConnectionInfo();
		return info.getMacAddress();
	}
}