package com.ccf.feige.activity;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import com.ccf.feige.adapter.ChatListAdapter;
import com.ccf.feige.data.ChatMessage;
import com.ccf.feige.interfaces.ReceiveMsgListener;
import com.ccf.feige.net.NetTcpFileSendThread;
import com.ccf.feige.utils.IpMessageConst;
import com.ccf.feige.utils.IpMessageProtocol;
import com.ccf.feige.utils.UsedConst;

import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

/**
 * ���촰��activity
 * @author ccf
 * 
 * 2012/2/21
 *
 */
public class MyFeiGeChatActivity extends MyFeiGeBaseActivity implements OnClickListener,ReceiveMsgListener{
	
//	private NetThreadHelper netThreadHelper;
	
	
//	private ImageView chat_item_head;	//ͷ��
	private TextView chat_name;			//���ּ�IP
	private TextView chat_mood;			//����
	private Button chat_quit;			//�˳���ť
	private ListView chat_list;			//�����б�
	private EditText chat_input;		//���������
	private Button chat_send;			//���Ͱ�ť
	
	private List<ChatMessage> msgList;	//������ʾ����Ϣlist
	private String receiverName;			//Ҫ���ձ�activity�����͵���Ϣ���û�����
	private String receiverIp;			//Ҫ���ձ�activity�����͵���Ϣ���û�IP
	private String receiverGroup;			//Ҫ���ձ�activity�����͵���Ϣ���û�����
	private ChatListAdapter adapter;	//ListView��Ӧ��adapter
	private String selfName;
	private String selfGroup;
	
	private final static int MENU_ITEM_SENDFILE = Menu.FIRST;	//�����ļ�
	private final static int MENU_ITEM_EXIT = Menu.FIRST + 1;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.chat);
		
		findViews();
		
//		netThreadHelper = NetThreadHelper.newInstance();
		msgList = new ArrayList<ChatMessage>();
		Intent intent = getIntent();
		Bundle bundle = intent.getExtras();
		receiverName = bundle.getString("receiverName");
		receiverIp = bundle.getString("receiverIp");
		receiverGroup = bundle.getString("receiverGroup");
		selfName = "android�ɸ�";
		selfGroup = "android";
		
		chat_name.setText(receiverName + "(" + receiverIp + ")");
		chat_mood.setText("������" + receiverGroup);
		chat_quit.setOnClickListener(this);
		chat_send.setOnClickListener(this);
		
		Iterator<ChatMessage> it = netThreadHelper.getReceiveMsgQueue().iterator();
		while(it.hasNext()){	//ѭ����Ϣ���У���ȡ�������뱾����activity�����Ϣ
			ChatMessage temp = it.next();
			//����Ϣ�����еķ������뱾activity����Ϣ������IP��ͬ���������Ϣ�ó�����ӵ���activityҪ��ʾ����Ϣlist��
			if(receiverIp.equals(temp.getSenderIp())){ 
				msgList.add(temp);	//��ӵ���ʾlist
				it.remove();		//������Ϣ����Ϣ�������Ƴ�
			}
		}
		
		adapter = new ChatListAdapter(this, msgList);
		chat_list.setAdapter(adapter);
		
		netThreadHelper.addReceiveMsgListener(this);	//ע�ᵽlisteners
	}
	
	private void findViews(){
//		chat_item_head = (ImageView) findViewById(R.id.chat_item_head);
		chat_name = (TextView) findViewById(R.id.chat_name);
		chat_mood = (TextView) findViewById(R.id.chat_mood);
		chat_quit = (Button) findViewById(R.id.chat_quit);
		chat_list = (ListView) findViewById(R.id.chat_list);
		chat_input = (EditText) findViewById(R.id.chat_input);
		chat_send = (Button) findViewById(R.id.chat_send);
	}

	@Override
	public void processMessage(Message msg) {
		// TODO Auto-generated method stub
		switch(msg.what){
		case IpMessageConst.IPMSG_SENDMSG:
			adapter.notifyDataSetChanged();	//ˢ��ListView
			break;
			
		case IpMessageConst.IPMSG_RELEASEFILES:{ //�ܾ������ļ�,ֹͣ�����ļ��߳�
			if(NetTcpFileSendThread.server != null){
				try {
					NetTcpFileSendThread.server.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
			break;
			
		case UsedConst.FILESENDSUCCESS:{	//�ļ����ͳɹ�
			makeTextShort("�ļ����ͳɹ�");
		}
			break;
			
			
		}	//end of switch
	}

	@Override
	public boolean receive(ChatMessage msg) {
		// TODO Auto-generated method stub
		if(receiverIp.equals(msg.getSenderIp())){	//����Ϣ�뱾activity�йأ������
			msgList.add(msg);	//������Ϣ��ӵ���ʾlist��
			sendEmptyMessage(IpMessageConst.IPMSG_SENDMSG); //ʹ��handle֪ͨ��������UI
			MyFeiGeBaseActivity.playMsg();
			return true;
		}
		
		return false;
	}

	@Override
	public void finish() {
		// TODO Auto-generated method stub
		//һ��Ҫ�Ƴ�����Ȼ��Ϣ���ջ��������
		netThreadHelper.removeReceiveMsgListener(this);
		super.finish();
	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		if(v == chat_send){
			sendAndAddMessage();	
		}else if(v == chat_quit){
			finish();
		}
	}
	
	/**
	 * ������Ϣ��������Ϣ��ӵ�UI��ʾ
	 */
	private void sendAndAddMessage(){
		String msgStr = chat_input.getText().toString().trim();
		if(!"".equals(msgStr)){
			//������Ϣ
			IpMessageProtocol sendMsg = new IpMessageProtocol();
			sendMsg.setVersion(String.valueOf(IpMessageConst.VERSION));
			sendMsg.setSenderName(selfName);
			sendMsg.setSenderHost(selfGroup);
			sendMsg.setCommandNo(IpMessageConst.IPMSG_SENDMSG);
			sendMsg.setAdditionalSection(msgStr);
			InetAddress sendto = null;
			try {
				sendto = InetAddress.getByName(receiverIp);
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				Log.e("MyFeiGeChatActivity", "���͵�ַ����");
			}
			if(sendto != null)
				netThreadHelper.sendUdpData(sendMsg.getProtocolString() + "\0", sendto, IpMessageConst.PORT);
			
			//�����Ϣ����ʾlist
			ChatMessage selfMsg = new ChatMessage("localhost", selfName, msgStr, new Date());
			selfMsg.setSelfMsg(true);	//����Ϊ������Ϣ
			msgList.add(selfMsg);	
			
		}else{
			makeTextShort("���ܷ��Ϳ�����");
		}
		
		chat_input.setText("");
		adapter.notifyDataSetChanged();//����UI
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// TODO Auto-generated method stub
		super.onCreateOptionsMenu(menu);
		menu.add(0, MENU_ITEM_SENDFILE, 0, "�����ļ�");
		menu.add(0, MENU_ITEM_EXIT, 0, "�˳�");
		
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// TODO Auto-generated method stub
		switch(item.getItemId()){
		case MENU_ITEM_SENDFILE:
			Intent intent = new Intent(this, MyFeiGeFileActivity.class);
			startActivityForResult(intent, 0);
			
			break;
		case MENU_ITEM_EXIT:
			finish();
			break;
		}
		
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub
		super.onActivityResult(requestCode, resultCode, data);
		if(resultCode == RESULT_OK){
			//�õ������ļ���·��
			Bundle bundle = data.getExtras();
			
			String filePaths = bundle.getString("filePaths");	//�����ļ���Ϣ��,����ļ�ʹ��"\0"���зָ�
//			Toast.makeText(this, filePaths, Toast.LENGTH_SHORT).show();
			
			String[] filePathArray = filePaths.split("\0");
			
			
			//���ʹ����ļ�UDP���ݱ�
			IpMessageProtocol sendPro = new IpMessageProtocol();
			sendPro.setVersion("" +IpMessageConst.VERSION);
			sendPro.setCommandNo(IpMessageConst.IPMSG_SENDMSG | IpMessageConst.IPMSG_FILEATTACHOPT);
			sendPro.setSenderName(selfName);
			sendPro.setSenderHost(selfGroup);
			String msgStr = "";	//���͵���Ϣ
			
			StringBuffer additionInfoSb = new StringBuffer();	//������ϸ����ļ���ʽ��sb
			for(String path:filePathArray){
				File file = new File(path);
				additionInfoSb.append("0:");
				additionInfoSb.append(file.getName() + ":");
				additionInfoSb.append(Long.toHexString(file.length()) + ":");		//�ļ���Сʮ�����Ʊ�ʾ
				additionInfoSb.append(Long.toHexString(file.lastModified()) + ":");	//�ļ�����ʱ�䣬������ʱ������޸�ʱ�����
				additionInfoSb.append(IpMessageConst.IPMSG_FILE_REGULAR + ":");
				byte[] bt = {0x07};		//���ڷָ���������ļ����ַ�
				String splitStr = new String(bt);
				additionInfoSb.append(splitStr);
			}
			
			sendPro.setAdditionalSection(msgStr + "\0" + additionInfoSb.toString() + "\0");
			
			InetAddress sendto = null;
			try {
				sendto = InetAddress.getByName(receiverIp);
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				Log.e("MyFeiGeChatActivity", "���͵�ַ����");
			}
			if(sendto != null)
				netThreadHelper.sendUdpData(sendPro.getProtocolString(), sendto, IpMessageConst.PORT);
			
			//����2425�˿ڣ�׼������TCP��������
			Thread netTcpFileSendThread = new Thread(new NetTcpFileSendThread(filePathArray));
			netTcpFileSendThread.start();	//�����߳�
		}
	}
	

}
