package io;

import java.io.IOException;
import java.net.Socket;
import java.util.Date;

/**
 * @Description
 * @Author zhujun
 * @Email
 * @Date 2021/1/28  1:54 PM
 * @Version
 **/
public class IOClient {
	
	public static void main(String[] args) throws Exception{
		
		new Thread(()-> {
			try {
				Socket socket = new Socket("127.0.0.1",9999);
				while (true){
					socket.getOutputStream().write((Thread.currentThread().getId()+" hello world:::"+new Date()).getBytes());
					Thread.sleep(5000);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}catch (InterruptedException e){
				e.printStackTrace();
			}
		}).start();
	}
}
