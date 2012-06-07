package com.ccf.feige.net;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.UnknownHostException;

import android.os.Message;
import android.util.Log;

import com.ccf.feige.activity.MyFeiGeBaseActivity;
import com.ccf.feige.utils.IpMessageConst;
import com.ccf.feige.utils.IpMessageProtocol;
import com.ccf.feige.utils.UsedConst;

/**
 * Tcp�����ļ��߳���
 * @author ccf
 *
 * 2012/2/28
 */
public class NetTcpFileReceiveThread implements Runnable {
	private final static String TAG = "NetTcpFileReceiveThread";
	
	private String[] fileInfos;	//�ļ���Ϣ�ַ�����
	private String senderIp;
	private long packetNo;	//�����
	private String savePath;	//�ļ�����·��
	
	private String selfName;
	private String selfGroup;
	
	private Socket socket;
	private BufferedInputStream bis;	
	private BufferedOutputStream bos;
	BufferedOutputStream fbos;
	private byte[] readBuffer = new byte[512];
	
	public NetTcpFileReceiveThread(String packetNo,String senderIp, String[] fileInfos){
		this.packetNo = Long.valueOf(packetNo);
		this.fileInfos = fileInfos;
		this.senderIp = senderIp;
		
		selfName = "android�ɸ�";
		selfGroup = "android";
		savePath= "/mnt/sdcard/FeigeRec/";
		
		//�жϽ����ļ����ļ����Ƿ���ڣ��������ڣ��򴴽�
		File fileDir = new File(savePath);
		if( !fileDir.exists()){	//��������
			fileDir.mkdir();
		}
		
	}
	@Override
	public void run() {
		// TODO Auto-generated method stub
		for(int i = 0; i < fileInfos.length; i++){	//ѭ������ÿ���ļ�
			//ע�⣬������ʱδ�����ļ�������ð�ŵ�������ɸ�Э��涨�����ļ�������ð�ţ�����˫ð���������������������ʱû��
			String[] fileInfo = fileInfos[i].split(":");	//ʹ��:�ָ��õ��ļ���Ϣ����
			//�ȷ���һ��ָ����ȡ�ļ��İ�
			IpMessageProtocol ipmsgPro = new IpMessageProtocol();
			ipmsgPro.setVersion(String.valueOf(IpMessageConst.VERSION));
			ipmsgPro.setCommandNo(IpMessageConst.IPMSG_GETFILEDATA);
			ipmsgPro.setSenderName(selfName);
			ipmsgPro.setSenderHost(selfGroup);
			String additionStr = Long.toHexString(packetNo) + ":" + i + ":" + "0:";
			ipmsgPro.setAdditionalSection(additionStr);
			
			
			try {
				socket = new Socket(senderIp, IpMessageConst.PORT);
				Log.d(TAG, "�������Ϸ��Ͷ�");
				bos = new BufferedOutputStream(socket.getOutputStream());
				
				//������ȡ�ļ��ɸ�����
				byte[] sendBytes = ipmsgPro.getProtocolString().getBytes("gbk");
				bos.write(sendBytes, 0, sendBytes.length);
				bos.flush();
				
				Log.d(TAG, "ͨ��TCP���ͽ���ָ���ļ�������������ǣ�" + ipmsgPro.getProtocolString());
				
				
				//�����ļ�
				File receiveFile = new File(savePath + fileInfo[1]);
				if(receiveFile.exists()){	//����Ӧ�ļ������ļ��Ѵ��ڣ���ɾ��ԭ�����ļ�
					receiveFile.delete();
				}
				fbos = new BufferedOutputStream(new FileOutputStream(receiveFile));
				Log.d(TAG, "׼����ʼ�����ļ�....");
				bis = new BufferedInputStream(socket.getInputStream());
				int len = 0;
				long sended = 0;	//�ѽ����ļ���С
				long total = Long.parseLong(fileInfo[2], 16);	//�ļ��ܴ�С
				int temp = 0;
				while((len = bis.read(readBuffer)) != -1){
					fbos.write(readBuffer, 0, len);
					fbos.flush();
					
					sended += len;	//�ѽ����ļ���С
					int sendedPer = (int) (sended * 100 / total);	//���հٷֱ�
					if(temp != sendedPer){	//ÿ����һ���ٷֱȣ�����һ��message
						int[] msgObj = {i, sendedPer};
						Message msg = new Message();
						msg.what = UsedConst.FILERECEIVEINFO;
						msg.obj = msgObj;
						MyFeiGeBaseActivity.sendMessage(msg);
						temp = sendedPer;
					}
					if(len < readBuffer.length) break;
				}
				
				Log.i(TAG, "��" + (i+1) + "���ļ����ճɹ����ļ���Ϊ"  + fileInfo[1]);
				int[] success = {i+1, fileInfos.length};
				Message msg4success = new Message();
				msg4success.what = UsedConst.FILERECEIVESUCCESS;
				msg4success.obj = success;
				MyFeiGeBaseActivity.sendMessage(msg4success);
				
			}catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				Log.e(TAG, "....ϵͳ��֧��GBK����");
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				Log.e(TAG, "Զ��IP��ַ����");
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				Log.e(TAG, "�ļ�����ʧ��");
			}catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				Log.e(TAG, "����IO����");
			}finally{	//����
				
				if(bos != null){	
					try {
						bos.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					bos = null;
				}
				
				if(fbos != null){
					try {
						fbos.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					fbos = null;
				}
				
				if(bis != null){
					try {
						bis.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					bis = null;
				}
				
				if(socket != null){
					try {
						socket.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					socket = null;
				}
				
			}
			
			
			
		}

	}

}
