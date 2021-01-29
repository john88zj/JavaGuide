package io;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;

/**
 * @Description
 * @Author zhujun
 * @Email
 * @Date 2021/1/28  4:09 PM
 * @Version
 **/
public class NioServer {
	
	public static void main(String[] args) {
		try {
			ServerSocketChannel ssc = ServerSocketChannel.open();
			ssc.socket().bind(new InetSocketAddress("127.0.0.1", 8000));
			ssc.configureBlocking(false);
			
			Selector selector = Selector.open();
			// 注册 channel，并且指定感兴趣的事件是 Accept
			ssc.register(selector, SelectionKey.OP_ACCEPT);
			
			ByteBuffer readBuff = ByteBuffer.allocate(1024);
			ByteBuffer writeBuff = ByteBuffer.allocate(1024);
//			writeBuff.put((LocalDateTime.now().toString()+" received :").getBytes());
//			writeBuff.flip();
			
			while (true) {
				int nReady = selector.select(1);
				Set<SelectionKey> keys = selector.selectedKeys();
				Iterator<SelectionKey> it = keys.iterator();
				
				while (it.hasNext()) {
					SelectionKey key = it.next();
					it.remove();
					
					if (key.isAcceptable()) {
//						System.out.println("key.isAcceptable");
						// 创建新的连接，并且把连接注册到selector上，而且，
						// 声明这个channel只对读操作感兴趣。
						SocketChannel socketChannel = ssc.accept();
						socketChannel.configureBlocking(false);
						socketChannel.register(selector, SelectionKey.OP_READ);
					}
					else if (key.isReadable()) {
//						System.out.println("key.isReadable");
						SocketChannel socketChannel = (SocketChannel) key.channel();
						readBuff.clear();
						socketChannel.read(readBuff);
						
						readBuff.flip();
						System.out.println("---channel:"+socketChannel+" date:"+LocalDateTime.now().toString()+" thread:"+Thread.currentThread().getId()+"  received : " + new String(readBuff.array()));
						key.interestOps(SelectionKey.OP_WRITE);
						
					}
					else if (key.isWritable()) {
//						System.out.println("key.isWritable");
						
						writeBuff.rewind();
						SocketChannel socketChannel = (SocketChannel) key.channel();
						writeBuff.put(("===channel:"+socketChannel+" date:"+LocalDateTime.now().toString()+" thread:"+Thread.currentThread().getId()+" send.").getBytes());
						writeBuff.flip();
						socketChannel.write(writeBuff);
						key.interestOps(SelectionKey.OP_READ);
						System.out.println("===channel:"+socketChannel+" date:"+LocalDateTime.now().toString()+" thread:"+Thread.currentThread().getId()+" send.");
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
