package io;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Date;

/**
 * @Description
 * @Author zhujun
 * @Email
 * @Date 2021/1/28  4:10 PM
 * @Version
 **/
public class NioClient {
	
	public static void main(String[] args) throws IOException {
		try {
			SocketChannel socketChannel = SocketChannel.open();
			socketChannel.connect(new InetSocketAddress("127.0.0.1", 8000));
			
			ByteBuffer writeBuffer = ByteBuffer.allocate(32);
			ByteBuffer readBuffer = ByteBuffer.allocate(1024);
			
			writeBuffer.put(("thread:"+Thread.currentThread().getId()+"   "+" hello ").getBytes());
			writeBuffer.flip();
			
			while (true) {
				writeBuffer.rewind();
				socketChannel.write(writeBuffer);
				readBuffer.clear();
				socketChannel.read(readBuffer);
//				Thread.sleep(1000);

//				while (socketChannel.read(readBuffer) !=-1){
//					System.out.println("read:: "+new String(readBuffer.array()));
//				}
//
				
				System.out.println("read:: "+new String(readBuffer.array()));
				Thread.sleep(5000);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}catch (InterruptedException e){
			e.printStackTrace();
		}
	}
}
