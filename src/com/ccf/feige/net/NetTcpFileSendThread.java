package com.ccf.feige.net;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.ServerSocket;
import java.net.Socket;

import android.util.Log;

import com.ccf.feige.activity.MyFeiGeBaseActivity;
import com.ccf.feige.utils.IpMessageConst;
import com.ccf.feige.utils.IpMessageProtocol;
import com.ccf.feige.utils.UsedConst;

/**
 * Tcp�����ļ��߳�
 * @author ccf
 * 
 * 2012/2/28
 */
public class NetTcpFileSendThread implements Runnable{
	private final static String TAG = "NetTcpFileSendThread";
	private String[] filePathArray;	//���淢���ļ�·��������
	
	public static ServerSocket server;	
	private Socket socket;	
	private byte[] readBuffer = new byte[1024];
	
	public NetTcpFileSendThread(String[] filePathArray){
		this.filePathArray = filePathArray;
		
		try {
			server = new ServerSocket(IpMessageConst.PORT);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Log.e(TAG, "����tcp�˿�ʧ��");
		}
	}
	

	@Override
	public void run() {
		// TODO Auto-generated method stub
		for(int i = 0; i < filePathArray.length; i ++){
			try {
				socket = server.accept();
				Log.i(TAG, "��IPΪ" + socket.getInetAddress().getHostAddress() + "���û�����TCP����");
				BufferedOutputStream bos = new BufferedOutputStream(socket.getOutputStream());
				BufferedInputStream bis = new BufferedInputStream(socket.getInputStream());
//				DataInputStream dis = new DataInputStream(bis);
//				String ipmsgStr = dis.readUTF();
				int mlen = bis.read(readBuffer);
				String ipmsgStr = new String(readBuffer,0,mlen,"gbk");
				
				
				Log.d(TAG, "�յ���TCP������Ϣ�����ǣ�" + ipmsgStr);
				
				IpMessageProtocol ipmsgPro = new IpMessageProtocol(ipmsgStr);
				String fileNoStr = ipmsgPro.getAdditionalSection();
				String[] fileNoArray = fileNoStr.split(":");
				int sendFileNo = Integer.valueOf(fileNoArray[1]);
				
				Log.d(TAG, "���η��͵��ļ�����·��Ϊ" + filePathArray[sendFileNo]);
				File sendFile = new File(filePathArray[sendFileNo]);	//Ҫ���͵��ļ�
				BufferedInputStream fbis = new BufferedInputStream(new FileInputStream(sendFile));
				
				int rlen = 0;
				while((rlen = fbis.read(readBuffer)) != -1){
					bos.write(readBuffer, 0, rlen);
				}
				bos.flush();
				Log.i(TAG, "�ļ����ͳɹ�");
				
				if(bis != null){
					bis.close();
					bis = null;
				}
				
				if(fbis != null){
					fbis.close();
					fbis = null;
				}
				
				if(bos != null){
					bos.close();
					bos = null;
				}
				
				if(i == (filePathArray.length -1)){
					MyFeiGeBaseActivity.sendEmptyMessage(UsedConst.FILESENDSUCCESS);	//�ļ����ͳɹ�
				}
				
			}catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				Log.e(TAG, "��������ʱ��ϵͳ��֧��GBK����");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				Log.e(TAG, "����IO����");
				break;
			} finally{
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
		
		if(server != null){
			try {
				server.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			server = null;
		}
		
		
	}

}
