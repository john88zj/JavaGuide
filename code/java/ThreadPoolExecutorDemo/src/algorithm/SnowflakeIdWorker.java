package algorithm;

import com.sun.deploy.util.SystemUtils;
import com.sun.tools.javac.util.ArrayUtils;
import com.sun.tools.javac.util.StringUtils;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.Enumeration;


/**
 *
 * @Description
 * Twitter_Snowflake<br>
 * SnowFlake的结构如下(每部分用-分开):<br>
 * 0 - 0000000000 0000000000 0000000000 0000000000 0 - 00000 - 00000 - 000000000000 <br>
 * 1位标识，由于long基本类型在Java中是带符号的，最高位是符号位，正数是0，负数是1，所以id一般是正数，最高位是0<br>
 * 41位时间截(毫秒级)，注意，41位时间截不是存储当前时间的时间截，而是存储时间截的差值（当前时间截 - 开始时间截)
 * 得到的值），这里的的开始时间截，一般是我们的id生成器开始使用的时间，由我们程序来指定的（如下下面程序IdWorker类的startTime属性）。41位的时间截，可以使用69年，年T = (1L << 41) / (1000L * 60 * 60 * 24 * 365) = 69<br>
 * 10位的数据机器位，可以部署在1024个节点，包括5位datacenterId和5位workerId<br>
 * 12位序列，毫秒内的计数，12位的计数顺序号支持每个节点每毫秒(同一机器，同一时间截)产生4096个ID序号<br>
 * 加起来刚好64位，为一个Long型。<br>
 * SnowFlake的优点是，整体上按照时间自增排序，并且整个分布式系统内不会产生ID碰撞(由数据中心ID和机器ID作区分)，并且效率较高，经测试，SnowFlake每秒能够产生26万ID左右。
 * @Author zhujun
 * @Email
 * @Date 2021/2/24  3:14 PM
 * @Version
 */


/**
 * @Description
 * @Author zhujun
 * @Email
 * @Date 2021/2/24  3:14 PM
 * @Version
 **/
public class SnowflakeIdWorker {
	
	//下面两个每个5位，加起来就是10位的工作机器id
	/**
	 * 机器ID(0~31)
	 */
	private long workerId;    //机器id
	/**
	 * 数据标识ID(0~31)
	 */
	private long datacenterId;   //数据中心id
	/**
	 *毫秒内序列(0~4095)
	 */
	private long sequence;
	
	/**
	 * 	初始时间戳(取一个离当前时间最近的,从此时间戳开始够用69年 2021-02-24 )
	 */
	private long twepoch = 1614096000000L;
	
	/**
	 * 机器ID占据的位数
	 */
	private final long workerIdBits = 5L;
	/**
	 * 数据标识位占据的位数
	 */
	private final long datacenterIdBits = 5L;
	/**
	 * 支持的最大机器id，结果是31 (这个移位算法可以很快的计算出几位二进制数所能表示的最大十进制数)
	 */
	private final long maxWorkerId = -1L ^ (-1L << workerIdBits);
	/**
	 * 支持的最大数据标识id，结果是31
	 */
	private final long maxDatacenterId = -1L ^ (-1L << datacenterIdBits);
	/**
	 * 序列在id中占的位数 12位
	 */
	private final long sequenceBits = 12L;
	/**
	 * 生成序列的掩码，这里为4095 (0b111111111111=0xfff=4095)
	 */
	private final long sequenceMask = -1L ^ (-1L << sequenceBits);
	
	/**
	 * 工作id需要左移的位数，12位
	 */
	private final long workerIdShift = sequenceBits;
	/**
	 * 数据id需要左移位数 12+5=17位
	 */
	private final long datacenterIdShift = sequenceBits + workerIdBits;
	/**
	 * 时间戳需要左移位数 12+5+5=22位
	 */
	private final long timestampLeftShift = sequenceBits + workerIdBits + datacenterIdBits;
	
	/**
	 * 上次时间戳，初始值为负数
	 */
	private long lastTimestamp = -1L;
	
	public SnowflakeIdWorker() {
		this.datacenterId = getDatacenterId(maxDatacenterId);
		this.workerId = getMaxWorkerId(datacenterId, maxWorkerId);
		this.sequence = 0L;
		System.out.printf("worker starting. timestamp left shift %d, datacenter id bits %d, worker id bits %d, sequence bits %d, datacenterid  %d, workerid %d\r\n",
				timestampLeftShift, datacenterIdBits, workerIdBits, sequenceBits, datacenterId, workerId);
	}
	
	public SnowflakeIdWorker(long workerId, long datacenterId, long sequence) {
		// sanity check for workerId
		if(workerId > maxWorkerId || workerId < 0) {
			throw new IllegalArgumentException(String.format("worker Id can't be greater than %d or less than 0\r\n", maxWorkerId));
		}
		if(datacenterId > maxDatacenterId || datacenterId < 0) {
			throw new IllegalArgumentException(String.format("datacenter Id can't be greater than %d or less than 0\r\n", maxDatacenterId));
		}
		System.out.printf("worker starting. timestamp left shift %d, datacenter id bits %d, worker id bits %d, sequence bits %d, datacenterid  %d, workerid %d\r\n",
				timestampLeftShift, datacenterIdBits, workerIdBits, sequenceBits, datacenterId, workerId);
		
		this.workerId = workerId;
		this.datacenterId = datacenterId;
		this.sequence = sequence;
	}
	
	public long getWorkerId() {
		return workerId;
	}

	public long getDatacenterId() {
		return datacenterId;
	}
	
	
	//下一个ID生成算法
	public synchronized long nextId() {
		long timestamp = timeGen();
		
		//获取当前时间戳如果小于上次时间戳，则表示时间戳获取出现异常
		if(timestamp < lastTimestamp) {
			System.err.printf("clock is moving backwards.  Rejecting requests until %d.", lastTimestamp);
			throw new RuntimeException(String.format("Clock moved backwards.  Refusing to generate id for %d milliseconds",
					lastTimestamp - timestamp));
		}
		
		//获取当前时间戳如果等于上次时间戳（同一毫秒内），则在序列号加一；否则序列号赋值为0，从0开始。
		if(lastTimestamp == timestamp) {
			sequence = (sequence + 1) & sequenceMask;
			if(sequence == 0) {
				timestamp = tillNextMillis(lastTimestamp);
			}
		} else {
			sequence = 0;
		}
		
		//将上次时间戳值刷新
		lastTimestamp = timestamp;
		
		/**
		 * 返回结果：
		 * (timestamp - twepoch) << timestampLeftShift) 表示将时间戳减去初始时间戳，再左移相应位数
		 * (datacenterId << datacenterIdShift) 表示将数据id左移相应位数
		 * (workerId << workerIdShift) 表示将工作id左移相应位数
		 * | 是按位或运算符，例如：x | y，只有当x，y都为0的时候结果才为0，其它情况结果都为1。
		 * 因为个部分只有相应位上的值有意义，其它位上都是0，所以将各部分的值进行 | 运算就能得到最终拼接好的id
		 */
		return ((timestamp - twepoch) << timestampLeftShift) |
				(datacenterId << datacenterIdShift) |
				(workerId << workerIdShift) |
				sequence;
	}
	
	//获取时间戳，并与上次时间戳比较
	private long tillNextMillis(long lastTimestamp) {
		long timestamp = timeGen();
		while (timestamp <= lastTimestamp) {
			timestamp = timeGen();
		}
		return timestamp;
	}
	
	//获取系统时间戳
	private long timeGen() {
		return System.currentTimeMillis();
	}
	
	protected static long getMaxWorkerId(long datacenterId, long maxWorkerId) {
		StringBuffer mpid = new StringBuffer();
		mpid.append(datacenterId);
		String name = ManagementFactory.getRuntimeMXBean().getName();
		if(!name.isEmpty()) {
			/*
			 * GET jvmPid
			 */
			mpid.append(name.split("@")[0]);
		}
		/*
		 * MAC + PID 的 hashcode 获取16个低位
		 */
		long workId = (mpid.toString().hashCode() & 0xffff) % (maxWorkerId + 1);
//		System.out.println("WorkerId:" + workId);
		return workId;
	}
	
	protected static long getDatacenterId(long maxDatacenterId) {
		long datacenterId = 0L;
		try {
			InetAddress ip = InetAddress.getLocalHost();
			NetworkInterface network = NetworkInterface.getByInetAddress(ip);
			if(network == null) {
				datacenterId = 1L;
			} else {
				byte[] mac = network.getHardwareAddress();
				if(null != mac){
					datacenterId = ((0x000000FF & (long) mac[mac.length - 1])
							| (0x0000FF00 & (((long) mac[mac.length - 2]) << 8))) >> 6;
				}else{
					//通过hostName
					String hostName = ip.getHostName();
					int[] result = toCodePoints(hostName);
					for (int i: result){
						datacenterId += i;
					}
				}
				
				datacenterId = datacenterId % (maxDatacenterId + 1);//32模运算
			}
		} catch (Exception e) {
			e.printStackTrace();
//			System.out.println(" getDatacenterId: " + e.getMessage());
		}
//		System.out.println("DatacenterId:" + datacenterId);
		return datacenterId;
	}
	
	private static int[] toCodePoints(String str){
		if(str == null){
			return null;
		}else if(str.length()==0){
			return new int[0];
		}else{
			int[] result = new int[str.codePointCount(0, str.length())];
			int index = 0;
			for (int i = 0; i <result.length ; ++i){
				result[i] = str.codePointAt(index);
				index += Character.charCount(result[i]);
			}
			return result;
		}
	}
	
	
	//==============================Test=============================================
	
	/**
	 * 测试
	 */
	public static void main(String[] args) throws Exception{
//		System.out.println(System.currentTimeMillis());
//		long startTime = System.nanoTime();
//		for (int i = 0; i < 50000; i++) {
//			long id = SnowflakeIdWorker.getDatacenterId(100);
//			System.out.println(id);
//		}
//		System.out.println((System.nanoTime()-startTime)/1000000+"ms");

//		SnowflakeIdWorker worker = new SnowflakeIdWorker();
//		System.out.println("====="+worker.getDatacenterId());

		for (int i = 0; i < 100; i++){
			SnowflakeIdWorker worker = new SnowflakeIdWorker();
			new Thread(() -> {
				for (int j = 0; j < 10; j++){
					System.out.println(Thread.currentThread().getName() + " :" + worker.nextId());
				}
			}).start();
		}
	
	}
	
	
}
