package io;

import sun.jvm.hotspot.debugger.ThreadContext;
import sun.jvm.hotspot.runtime.Threads;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * @Description
 * @Author zhujun
 * @Email
 * @Date 2021/1/28  1:59 PM
 * @Version
 **/
public class IOServer {
	
	public static void main(String[] args) throws Exception {
		ServerSocket serverSocket = new ServerSocket(9999);
		
		// 接收到客户端连接请求之后为每个客户端创建一个新的线程进行链路处理
		new Thread(() -> {
			while (true) {
				try {
					// 阻塞方法获取新的连接
					Socket socket = serverSocket.accept();
					
					// 每一个新的连接都创建一个线程，负责读取数据
					new Thread(() -> {
						try {
							int len;
							byte[] data = new byte[1024];
							InputStream inputStream = socket.getInputStream();
							// 按字节流方式读取数据
							while ((len = inputStream.read(data)) != -1) {
								System.out.println("thread::"+Thread.currentThread().getId());
								System.out.println(new String(data, 0, len));
							}
						} catch (IOException e) {
						}
					}).start();
					
				} catch (IOException e) {
				}
				
			}
		}).start();
	}
}
