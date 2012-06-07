package com.ccf.feige.net;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.Vector;
import java.util.concurrent.ConcurrentLinkedQueue;

import android.os.Message;
import android.util.Log;

import com.ccf.feige.activity.MyFeiGeActivity;
import com.ccf.feige.activity.MyFeiGeBaseActivity;
import com.ccf.feige.data.ChatMessage;
import com.ccf.feige.data.User;
import com.ccf.feige.interfaces.ReceiveMsgListener;
import com.ccf.feige.utils.IpMessageConst;
import com.ccf.feige.utils.IpMessageProtocol;

/**
 * �ɸ������ͨ�Ÿ�����
 * ʵ��UDPͨ���Լ�UDP�˿ڼ���
 * �˿ڼ������ö��̷߳�ʽ
 * 
 * ����ģʽ
 * @author ccf
 * 
 * V1.0 2012/2/14����į�����˽ڰ汾���ٺ�
 *
 */

public class NetThreadHelper implements Runnable{
	public static final String TAG = "NetThreadHelper";
	
	private static NetThreadHelper instance;
	
	private static final int BUFFERLENGTH = 1024; //�����С
	private boolean onWork = false;	//�̹߳�����ʶ
	private String selfName;
	private String selfGroup;
	
	private Thread udpThread = null;	//����UDP�����߳�
	private DatagramSocket udpSocket = null;	//���ڽ��պͷ���udp���ݵ�socket
	private DatagramPacket udpSendPacket = null;	//���ڷ��͵�udp���ݰ�
	private DatagramPacket udpResPacket = null;	//���ڽ��յ�udp���ݰ�
	private byte[] resBuffer = new byte[BUFFERLENGTH];	//�������ݵĻ���
	private byte[] sendBuffer = null;
	
	private Map<String,User> users;	//��ǰ�����û��ļ��ϣ���IPΪKEY
	private int userCount = 0; //�û�������ע�⣬����ֵֻ���ڵ���getSimpleExpandableListAdapter()�Ż���£�Ŀ������adapter���û���������һ��
	
	private Queue<ChatMessage> receiveMsgQueue;	//��Ϣ����,��û�����촰��ʱ�����յ���Ϣ�ŵ����������
	private Vector<ReceiveMsgListener> listeners;	//ReceiveMsgListener��������һ�����촰�ڴ�ʱ��������롣һ��Ҫ�ǵ���ʱ�����Ƴ�
	
	private NetThreadHelper(){
		users = new HashMap<String, User>();
		receiveMsgQueue = new ConcurrentLinkedQueue<ChatMessage>();
		listeners = new Vector<ReceiveMsgListener>();
		
		selfName = "android�ɸ�";
		selfGroup = "android";
	}
	
	public static NetThreadHelper newInstance(){
		if(instance == null)
			instance = new NetThreadHelper();
		return instance;
	}
	
	public Map<String, User> getUsers(){
		return users;
	}
	
	public int getUserCount(){
		return userCount;
	}
	
	public Queue<ChatMessage> getReceiveMsgQueue(){
		return receiveMsgQueue;
	}
	
	//���listener��������
	public void addReceiveMsgListener(ReceiveMsgListener listener){
		if(!listeners.contains(listener)){
			listeners.add(listener);
		}
	}
	
	//���������Ƴ���Ӧlistener
	public void removeReceiveMsgListener(ReceiveMsgListener listener){
		if(listeners.contains(listener)){
			listeners.remove(listener);
		}
	}
	
	/**
	 * 
	 * �˷��������ж��Ƿ��д���ǰ̨�����촰�ڶ�Ӧ��activity�������յ������ݡ�
	 */
	private boolean receiveMsg(ChatMessage msg){
		for(int i = 0; i < listeners.size(); i++){
			ReceiveMsgListener listener = listeners.get(i);
			if(listener.receive(msg)){
				return true;
			}
		}
		return false;
	}
	
	
	public void noticeOnline(){	// �������߹㲥
		IpMessageProtocol ipmsgSend = new IpMessageProtocol();
		ipmsgSend.setVersion(String.valueOf(IpMessageConst.VERSION));
		ipmsgSend.setSenderName(selfName);
		ipmsgSend.setSenderHost(selfGroup);
		ipmsgSend.setCommandNo(IpMessageConst.IPMSG_BR_ENTRY);	//��������
		ipmsgSend.setAdditionalSection(selfName + "\0" );	//������Ϣ������û����ͷ�����Ϣ
		
		InetAddress broadcastAddr;
		try {
			broadcastAddr = InetAddress.getByName("255.255.255.255");	//�㲥��ַ
			sendUdpData(ipmsgSend.getProtocolString()+"\0", broadcastAddr, IpMessageConst.PORT);	//��������
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Log.e(TAG, "noticeOnline()....�㲥��ַ����");
		}
		
	}
	
	public void noticeOffline(){	//�������߹㲥
		IpMessageProtocol ipmsgSend = new IpMessageProtocol();
		ipmsgSend.setVersion(String.valueOf(IpMessageConst.VERSION));
		ipmsgSend.setSenderName(selfName);
		ipmsgSend.setSenderHost(selfGroup);
		ipmsgSend.setCommandNo(IpMessageConst.IPMSG_BR_EXIT);	//��������
		ipmsgSend.setAdditionalSection(selfName + "\0" + selfGroup);	//������Ϣ������û����ͷ�����Ϣ
		
		InetAddress broadcastAddr;
		try {
			broadcastAddr = InetAddress.getByName("255.255.255.255");	//�㲥��ַ
			sendUdpData(ipmsgSend.getProtocolString() + "\0", broadcastAddr, IpMessageConst.PORT);	//��������
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Log.e(TAG, "noticeOnline()....�㲥��ַ����");
		}

	}
	
	public void refreshUsers(){	//ˢ�������û�
		users.clear();	//��������û��б�
		noticeOnline(); //��������֪ͨ
		MyFeiGeBaseActivity.sendEmptyMessage(IpMessageConst.IPMSG_BR_ENTRY);
	}
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		while(onWork){
			
			try {
				udpSocket.receive(udpResPacket);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				onWork = false;
				
				if(udpResPacket != null){
					udpResPacket = null;
				}
				
				if(udpSocket != null){
					udpSocket.close();
					udpSocket = null;
				}
				
				udpThread = null;
				Log.e(TAG, "UDP���ݰ�����ʧ�ܣ��߳�ֹͣ");
				break;
			} 
			
			if(udpResPacket.getLength() == 0){
				Log.i(TAG, "�޷�����UDP���ݻ��߽��յ���UDP����Ϊ��");
				continue;
			}
			String ipmsgStr = "";
			try {
				ipmsgStr = new String(resBuffer, 0, udpResPacket.getLength(),"gbk");
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				Log.e(TAG, "��������ʱ��ϵͳ��֧��GBK����");
			}//��ȡ�յ�������
			Log.i(TAG, "���յ���UDP��������Ϊ:" + ipmsgStr);
			IpMessageProtocol ipmsgPro = new IpMessageProtocol(ipmsgStr);	//
			int commandNo = ipmsgPro.getCommandNo();
			int commandNo2 = 0x000000FF & commandNo;	//��ȡ������
			switch(commandNo2){
			case IpMessageConst.IPMSG_BR_ENTRY:	{	//�յ��������ݰ�������û���������IPMSG_ANSENTRYӦ��
				addUser(ipmsgPro);	//����û�
				
				MyFeiGeBaseActivity.sendEmptyMessage(IpMessageConst.IPMSG_BR_ENTRY);
				
				//���湹����ͱ�������
				IpMessageProtocol ipmsgSend = new IpMessageProtocol();
				ipmsgSend.setVersion(String.valueOf(IpMessageConst.VERSION));
				ipmsgSend.setSenderName(selfName);
				ipmsgSend.setSenderHost(selfGroup);
				ipmsgSend.setCommandNo(IpMessageConst.IPMSG_ANSENTRY);	//���ͱ�������
				ipmsgSend.setAdditionalSection(selfName + "\0" );	//������Ϣ������û����ͷ�����Ϣ
				
				sendUdpData(ipmsgSend.getProtocolString(), udpResPacket.getAddress(), udpResPacket.getPort());	//��������
			}	
				break;
			
			case IpMessageConst.IPMSG_ANSENTRY:	{	//�յ�����Ӧ�𣬸��������û��б�
				addUser(ipmsgPro);
				MyFeiGeBaseActivity.sendEmptyMessage(IpMessageConst.IPMSG_ANSENTRY);
			}	
				break;
			
			case IpMessageConst.IPMSG_BR_EXIT:{	//�յ����߹㲥��ɾ��users�ж�Ӧ��ֵ
				String userIp = udpResPacket.getAddress().getHostAddress();
				users.remove(userIp);
				MyFeiGeBaseActivity.sendEmptyMessage(IpMessageConst.IPMSG_BR_EXIT);
				
				Log.i(TAG, "�������߱��ĳɹ�ɾ��ipΪ" + userIp + "���û�");
			}	
				break;
			
			case IpMessageConst.IPMSG_SENDMSG:{ //�յ���Ϣ������
				String senderIp = udpResPacket.getAddress().getHostAddress();	//�õ�������IP
				String senderName = ipmsgPro.getSenderName();	//�õ������ߵ�����
				String additionStr = ipmsgPro.getAdditionalSection();	//�õ�������Ϣ
				Date time = new Date();	//�յ���Ϣ��ʱ��
				String msgTemp;		//ֱ���յ�����Ϣ�����ݼ���ѡ���ж��Ƿ��Ǽ�����Ϣ
				String msgStr;		//���ܺ����Ϣ����
				
				//����������ĸ����ֶε��ж�
				
				//���������ִ�����֤ѡ���������յ���Ϣ����
				if( (commandNo & IpMessageConst.IPMSG_SENDCHECKOPT) == IpMessageConst.IPMSG_SENDCHECKOPT){
					//����ͨ���յ���Ϣ����
					IpMessageProtocol ipmsgSend = new IpMessageProtocol();
					ipmsgSend.setVersion("" +IpMessageConst.VERSION);	//ͨ���յ���Ϣ������
					ipmsgSend.setCommandNo(IpMessageConst.IPMSG_RECVMSG);
					ipmsgSend.setSenderName(selfName);
					ipmsgSend.setSenderHost(selfGroup);
					ipmsgSend.setAdditionalSection(ipmsgPro.getPacketNo() + "\0");	//������Ϣ����ȷ���յ��İ��ı��
					
					sendUdpData(ipmsgSend.getProtocolString(), udpResPacket.getAddress(), udpResPacket.getPort());	//��������
				}
				
				String[] splitStr = additionStr.split("\0"); //ʹ��"\0"�ָ���и����ļ���Ϣ�����ָ����
				msgTemp = splitStr[0]; //����Ϣ����ȡ��
				
				//�Ƿ��з����ļ���ѡ��.���У��򸽼���Ϣ���ȡ���������ļ���Ϣ
				if((commandNo & IpMessageConst.IPMSG_FILEATTACHOPT) == IpMessageConst.IPMSG_FILEATTACHOPT){	
					//������з����ļ���ش���
					
					Message msg = new Message();
					msg.what = (IpMessageConst.IPMSG_SENDMSG | IpMessageConst.IPMSG_FILEATTACHOPT);
					//�ַ������飬�ֱ����  IP�������ļ���Ϣ,���������ƣ���ID
					String[] extraMsg = {senderIp, splitStr[1],senderName,ipmsgPro.getPacketNo()};	
					msg.obj = extraMsg;	//�����ļ���Ϣ����
					MyFeiGeBaseActivity.sendMessage(msg);
					
					break;
				}
				
				
				//�Ƿ��м���ѡ���ȱ
				msgStr = msgTemp;
				
				// ��ֻ�Ƿ�����Ϣ��������Ϣ
				ChatMessage msg = new ChatMessage(senderIp, senderName, msgStr, time);
				if(!receiveMsg(msg)){	//û�����촰�ڶ�Ӧ��activity
					receiveMsgQueue.add(msg);	// ��ӵ���Ϣ����
					MyFeiGeBaseActivity.playMsg();
					//֮�������ЩUI��ʾ�Ĵ�����sendMessage()�����У���ȱ
					MyFeiGeBaseActivity.sendEmptyMessage(IpMessageConst.IPMSG_SENDMSG);	//����������UI
				}
				
				
			}
				break;
				
			case IpMessageConst.IPMSG_RELEASEFILES:{ //�ܾ������ļ�
				MyFeiGeBaseActivity.sendEmptyMessage(IpMessageConst.IPMSG_RELEASEFILES);
			}
				break;
			
				
			}	//end of switch
			
			if(udpResPacket != null){	//ÿ�ν�����UDP���ݺ����ó��ȡ�������ܻᵼ���´��յ����ݰ����ضϡ�
				udpResPacket.setLength(BUFFERLENGTH);
			}
			
		}
		
		if(udpResPacket != null){
			udpResPacket = null;
		}
		
		if(udpSocket != null){
			udpSocket.close();
			udpSocket = null;
		}
		
		udpThread = null;
		
	}
	
	public boolean connectSocket(){	//�����˿ڣ�����UDP����
		boolean result = false;
		
		try {
			if(udpSocket == null){
				udpSocket = new DatagramSocket(IpMessageConst.PORT);	//�󶨶˿�
				Log.i(TAG, "connectSocket()....��UDP�˿�" + IpMessageConst.PORT + "�ɹ�");
			}
			if(udpResPacket == null)
				udpResPacket = new DatagramPacket(resBuffer, BUFFERLENGTH);
			onWork = true;  //���ñ�ʶΪ�̹߳���
			startThread();	//�����߳̽���udp����
			result = true;
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			disconnectSocket();
			Log.e(TAG, "connectSocket()....��UDP�˿�" + IpMessageConst.PORT + "ʧ��");
		}
		
		return result;
	}
	
	public void disconnectSocket(){	// ֹͣ����UDP����
		onWork = false;	// �����߳����б�ʶΪ������
		
		stopThread();
	}
	

	private void stopThread() {	//ֹͣ�߳�
		// TODO Auto-generated method stub
		if(udpThread != null){
			udpThread.interrupt();	//���̶߳��������ж�
		}
		Log.i(TAG, "ֹͣ����UDP����");
	}

	private void startThread() {	//�����߳�
		// TODO Auto-generated method stub
		if(udpThread == null){
			udpThread = new Thread(this);
			udpThread.start();
			Log.i(TAG, "���ڼ���UDP����");
		}
	}
	
	public synchronized void sendUdpData(String sendStr, InetAddress sendto, int sendPort){	//����UDP���ݰ��ķ���
		try {
			sendBuffer = sendStr.getBytes("gbk");
			// ���췢�͵�UDP���ݰ�
			udpSendPacket = new DatagramPacket(sendBuffer, sendBuffer.length, sendto, sendPort);
			udpSocket.send(udpSendPacket);	//����udp���ݰ�
			Log.i(TAG, "�ɹ���IPΪ" + sendto.getHostAddress() + "����UDP���ݣ�" + sendStr);
			udpSendPacket = null;
			
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Log.e(TAG, "sendUdpData(String sendStr, int port)....ϵͳ��֧��GBK����");
		} catch (IOException e) {	//����UDP���ݰ�����
			// TODO Auto-generated catch block
			e.printStackTrace();
			udpSendPacket = null;
			Log.e(TAG, "sendUdpData(String sendStr, int port)....����UDP���ݰ�ʧ��");
		}
	}
	
	private synchronized void addUser(IpMessageProtocol ipmsgPro){ //����û���Users��Map��
		String userIp = udpResPacket.getAddress().getHostAddress();
		User user = new User();
//		user.setUserName(ipmsgPro.getSenderName());
		user.setAlias(ipmsgPro.getSenderName());	//�����ݶ�����������
		
		String extraInfo = ipmsgPro.getAdditionalSection();
		String[] userInfo = extraInfo.split("\0");	//�Ը�����Ϣ���зָ�,�õ��û����ͷ�����
		if(userInfo.length < 1){
			user.setUserName(ipmsgPro.getSenderName());
			if(userIp.equals(MyFeiGeActivity.hostIp))
				user.setGroupName("�Լ�");
			else
				user.setGroupName("�Է�δ�������");
		}else if (userInfo.length == 1){
			user.setUserName(userInfo[0]);
			if(userIp.equals(MyFeiGeActivity.hostIp))
				user.setGroupName("�Լ�");
			else
				user.setGroupName("�Է�δ�������");
		}else{
			user.setUserName(userInfo[0]);
			if(userIp.equals(MyFeiGeActivity.hostIp))
				user.setGroupName("�Լ�");
			else
				user.setGroupName(userInfo[1]);
		}
		
		user.setIp(userIp);
		user.setHostName(ipmsgPro.getSenderHost());
		user.setMac("");	//��ʱû������ֶ�
		users.put(userIp, user);
		Log.i(TAG, "�ɹ����ipΪ" + userIp + "���û�");
	}
	
}
