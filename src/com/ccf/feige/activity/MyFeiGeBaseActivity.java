package com.ccf.feige.activity;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.LinkedList;

import com.ccf.feige.net.NetTcpFileReceiveThread;
import com.ccf.feige.net.NetThreadHelper;
import com.ccf.feige.utils.IpMessageConst;
import com.ccf.feige.utils.IpMessageProtocol;
import com.ccf.feige.utils.UsedConst;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

/**
 * ��������ʵ����ʹ��Handle�������첽������Ϣ
 * ֻҪ�̳б��࣬����дvoid processMessage(Message msg)��������
 * @author ccf
 *
 */
public abstract class MyFeiGeBaseActivity extends Activity {
	private static int notification_id = 9786970;
	private NotificationManager mNotManager;
	private Notification mNotification;
	
	protected static LinkedList<MyFeiGeBaseActivity> queue = new LinkedList<MyFeiGeBaseActivity>();
	private static MediaPlayer player;
	protected static NetThreadHelper netThreadHelper;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		netThreadHelper = NetThreadHelper.newInstance();
		
		//����notification
		mNotManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		mNotification = new Notification(android.R.drawable.stat_sys_download, "�ɸ�����ļ�", System.currentTimeMillis());
		mNotification.contentView = new RemoteViews(getPackageName(), R.layout.file_download_notification);
		mNotification.contentView.setProgressBar(R.id.pd_download, 100, 0, false);
		Intent notificationIntent = new Intent(this,MyFeiGeBaseActivity.class);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
		mNotification.contentIntent = contentIntent;
		
		
		if(!queue.contains(this))
			queue.add(this);
		if(player == null){
			player = MediaPlayer.create(this, R.raw.msg);
			try {
				player.prepare();
			} catch (IllegalStateException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	public static MyFeiGeBaseActivity getActivity(int index){
		if (index < 0 || index >= queue.size())
			throw new IllegalArgumentException("out of queue");
		return queue.get(index);
	}
	
	public static MyFeiGeBaseActivity getCurrentActivity(){
		return queue.getLast();
	}
	
	public void makeTextShort(String text) {
		Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
	}

	public void makeTextLong(String text) {
		Toast.makeText(this, text, Toast.LENGTH_LONG).show();
	}
	
	public abstract void processMessage(Message msg);
	
	@Override
	public void finish() {
		// TODO Auto-generated method stub
		super.finish();
		queue.removeLast();
	}

	public static void sendMessage(int cmd, String text) {
		Message msg = new Message();
		msg.obj = text;
		msg.what = cmd;
		sendMessage(msg);
	}

	public static void sendMessage(Message msg) {
		handler.sendMessage(msg);
	}

	public static void sendEmptyMessage(int what) {
		handler.sendEmptyMessage(what);
	}

	private static Handler handler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			// TODO Auto-generated method stub
			switch (msg.what) {
			case IpMessageConst.IPMSG_SENDMSG | IpMessageConst.IPMSG_FILEATTACHOPT:{
				//�յ������ļ�����
				
				final String[] extraMsg = (String[]) msg.obj;	//�õ������ļ���Ϣ,�ַ������飬�ֱ����  IP�������ļ���Ϣ,���������ƣ���ID
				Log.d("receive file....", "receive file from :" + extraMsg[2] + "(" + extraMsg[0] +")");
				Log.d("receive file....", "receive file info:" + extraMsg[1]);
				byte[] bt = {0x07};		//���ڷָ���������ļ����ַ�
				String splitStr = new String(bt);
				final String[] fileInfos = extraMsg[1].split(splitStr);	//ʹ�÷ָ��ַ����зָ�
				
				Log.d("feige", "�յ��ļ���������,����" + fileInfos.length + "���ļ�");
				
				String infoStr = "������IP:\t" + extraMsg[0] + "\n" + 
								 "����������:\t" + extraMsg[2] + "\n" +
								 "�ļ�����:\t" + fileInfos.length +"��";
				
				new AlertDialog.Builder(queue.getLast())
					.setIcon(android.R.drawable.ic_dialog_info)
					.setTitle("�յ��ļ���������")
					.setMessage(infoStr)
					.setPositiveButton("����", 
							new DialogInterface.OnClickListener() {
								
								@Override
								public void onClick(DialogInterface dialog, int which) {
									// TODO Auto-generated method stub
									Thread fileReceiveThread = new Thread(new NetTcpFileReceiveThread(extraMsg[3], extraMsg[0],fileInfos));	//�½�һ�������ļ��߳�
									fileReceiveThread.start();	//�����߳�
									
									Toast.makeText(getCurrentActivity(), "��ʼ�����ļ�", Toast.LENGTH_SHORT).show();
									
									queue.getLast().showNotification();	//��ʾnotification
								}
							})
					 .setNegativeButton("ȡ��", 
							 new DialogInterface.OnClickListener() {
						 		public void onClick(DialogInterface dialog, int which) {
						 			//���;ܾ�����
						 			//����ܾ�����
									IpMessageProtocol ipmsgSend = new IpMessageProtocol();
									ipmsgSend.setVersion("" +IpMessageConst.VERSION);	//�ܾ�������
									ipmsgSend.setCommandNo(IpMessageConst.IPMSG_RELEASEFILES);
									ipmsgSend.setSenderName("android�ɸ�");
									ipmsgSend.setSenderHost("android");
									ipmsgSend.setAdditionalSection(extraMsg[3] + "\0");	//������Ϣ����ȷ���յ��İ��ı��
						 			
									InetAddress sendAddress = null;
									try {
										sendAddress = InetAddress.getByName(extraMsg[0]);
									} catch (UnknownHostException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									}
									
									netThreadHelper.sendUdpData(ipmsgSend.getProtocolString(), sendAddress, IpMessageConst.PORT);
									
						 		}
					 }).show();
					 
			}
				break;
				
			case UsedConst.FILERECEIVEINFO:{	//���½����ļ�������
				int[] sendedPer = (int[]) msg.obj;	//�õ���Ϣ
				queue.getLast().mNotification.contentView.setProgressBar(R.id.pd_download, 100, sendedPer[1], false);
				queue.getLast().mNotification.contentView.setTextViewText(R.id.fileRec_info, "�ļ�"+ (sendedPer[0] + 1) +"������:" + sendedPer[1] + "%");
				
				queue.getLast().showNotification();	//��ʾnotification
			}
				break;
				
			case UsedConst.FILERECEIVESUCCESS:{	//�ļ����ճɹ�
				int[] successNum = (int[]) msg.obj;
				
				queue.getLast().mNotification.contentView.setTextViewText(R.id.fileRec_info, "��"+ successNum[0] +"���ļ����ճɹ�");
				queue.getLast().makeTextShort("��"+ successNum[0] +"���ļ����ճɹ�");
				if(successNum[0] == successNum[1]){
					queue.getLast().mNotification.contentView.setTextViewText(R.id.fileRec_info, "�����ļ����ճɹ�");
//					queue.getLast().mNotManager.cancel(notification_id);
					
					queue.getLast().makeTextShort("�����ļ����ճɹ�");
				}
				queue.getLast().showNotification();
			}
				break;
			default:
				if(queue.size() > 0)
					queue.getLast().processMessage(msg);
				break;
			}
		}

	};
	
	public void exit() {
		while (queue.size() > 0)
			queue.getLast().finish();
	}
	
	public static void playMsg(){
		player.start();
	}
	
	protected void showNotification(){
		mNotManager.notify(notification_id, mNotification);
	}
	

}
