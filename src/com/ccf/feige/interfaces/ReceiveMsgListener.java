package com.ccf.feige.interfaces;

import com.ccf.feige.data.ChatMessage;

/**
 * ������Ϣ������listener�ӿ�
 * @author ccf
 *
 */
public interface ReceiveMsgListener {
	public boolean receive(ChatMessage msg);

}
