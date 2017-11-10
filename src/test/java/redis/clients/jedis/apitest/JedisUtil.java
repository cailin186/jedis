package redis.clients.jedis.apitest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import redis.clients.jedis.BinaryClient.LIST_POSITION;

//import org.apache.log4j.Logger;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.SortingParams;
import redis.clients.util.SafeEncoder;

/**
 * redis api test
 * 
 * @author cailin
 *
 */

public class JedisUtil {

	// private Logger log = Logger.getLogger(this.getClass());
	/** 缓存生存时间 */
	private final int expire = 60000;
	/** 操作Key的方法 */
	public Keys KEYS;
	/** 对存储结构为String类型的操作 */
	public Strings STRINGS;
	/** 对存储结构为List类型的操作 */
	public Lists LISTS;
	/** 对存储结构为Set类型的操作 */
	public Sets SETS;
	/** 对存储结构为HashMap类型的操作 */
	public Hash HASH;
	/** 对存储结构为Set(排序的)类型的操作 */
	public SortSet SORTSET;
	private static JedisPool jedisPool = null;

	private JedisUtil() {

	}

	static {
		JedisPoolConfig config = new JedisPoolConfig();
		// 控制一个pool可分配多少个jedis实例，通过pool.getResource()来获取；
		// 如果赋值为-1，则表示不限制；如果pool已经分配了maxActive个jedis实例，则此时pool的状态为exhausted(耗尽)。
		config.setMaxTotal(500);
		// 控制一个pool最多有多少个状态为idle(空闲的)的jedis实例。
		config.setMaxIdle(5);
		// 表示当borrow(引入)一个jedis实例时，最大的等待时间，如果超过等待时间，则直接抛出JedisConnectionException；
		config.setMaxWaitMillis(1000 * 100);
		// 在borrow一个jedis实例时，是否提前进行validate操作；如果为true，则得到的jedis实例均是可用的；
		config.setTestOnBorrow(true);

		// redis如果设置了密码：
		/*
		 * jedisPool = new JedisPool(config, JRedisPoolConfig.REDIS_IP,
		 * JRedisPoolConfig.REDIS_PORT, 10000,JRedisPoolConfig.REDIS_PASSWORD);
		 */

		// redis未设置了密码：
		jedisPool = new JedisPool(config, "localhost", 6379);
	}

	public JedisPool getPool() {
		return jedisPool;
	}

	/**
	 * 从jedis连接池中获取获取jedis对象
	 * 
	 * @return
	 */
	public Jedis getJedis() {
		return jedisPool.getResource();
	}

	private static final JedisUtil jedisUtil = new JedisUtil();

	/**
	 * 获取JedisUtil实例
	 * 
	 * @return
	 */
	public static JedisUtil getInstance() {
		return jedisUtil;
	}

	/**
	 * 回收jedis(放到finally中)
	 * 
	 * @param jedis
	 */
	public void returnJedis(Jedis jedis) {
		if (null != jedis && null != jedisPool) {
			jedisPool.returnResource(jedis);
		}
	}

	/**
	 * 销毁连接(放到catch中)
	 * 
	 * @param pool
	 * @param jedis
	 */
	public static void returnBrokenResource(Jedis jedis) {
		if (null != jedis && null != jedisPool) {
			jedisPool.returnResource(jedis);
		}
	}

	/**
	 * 设置过期时间
	 * 
	 * @author ruan 2013-4-11
	 * @param key
	 * @param seconds
	 */
	public void expire(String key, int seconds) {
		if (seconds <= 0) {
			return;
		}
		Jedis jedis = getJedis();
		jedis.expire(key, seconds);
		returnJedis(jedis);
	}

	/**
	 * 设置默认过期时间
	 * 
	 * @author ruan 2013-4-11
	 * @param key
	 */
	public void expire(String key) {
		expire(key, expire);
	}

	// *******************************************Keys*******************************************//
	public class Keys {

		/**
		 * 清空所有key
		 */
		public String flushAll() {
			Jedis jedis = getJedis();
			String stata = jedis.flushAll();
			returnJedis(jedis);
			return stata;
		}

		/**
		 * 更改key
		 * 
		 * @param String
		 *            oldkey
		 * @param String
		 *            newkey
		 * @return 状态码
		 */
		public String rename(String oldkey, String newkey) {
			return rename(SafeEncoder.encode(oldkey), SafeEncoder.encode(newkey));
		}

		/**
		 * 更改key,仅当新key不存在时才执行
		 * 
		 * @param String
		 *            oldkey
		 * @param String
		 *            newkey
		 * @return 状态码
		 */
		public long renamenx(String oldkey, String newkey) {
			Jedis jedis = getJedis();
			long status = jedis.renamenx(oldkey, newkey);
			returnJedis(jedis);
			return status;
		}

		/**
		 * 更改key
		 * 
		 * @param String
		 *            oldkey
		 * @param String
		 *            newkey
		 * @return 状态码
		 */
		public String rename(byte[] oldkey, byte[] newkey) {
			Jedis jedis = getJedis();
			String status = jedis.rename(oldkey, newkey);
			returnJedis(jedis);
			return status;
		}

		/**
		 * 设置key的过期时间，以秒为单位
		 * 
		 * @param String
		 *            key
		 * @param 时间,已秒为单位
		 * @return 影响的记录数
		 */
		public long expired(String key, int seconds) {
			Jedis jedis = getJedis();
			long count = jedis.expire(key, seconds);
			returnJedis(jedis);
			return count;
		}

		/**
		 * 设置key的过期时间,它是距历元（即格林威治标准时间 1970 年 1 月 1 日的 00:00:00，格里高利历）的偏移量。
		 * 
		 * @param String
		 *            key
		 * @param 时间,已秒为单位
		 * @return 影响的记录数
		 */
		public long expireAt(String key, long timestamp) {
			Jedis jedis = getJedis();
			long count = jedis.expireAt(key, timestamp);
			returnJedis(jedis);
			return count;
		}

		/**
		 * 查询key的过期时间
		 * 
		 * @param String
		 *            key
		 * @return 以秒为单位的时间表示
		 */
		public long ttl(String key) {
			// ShardedJedis sjedis = getShardedJedis();
			Jedis sjedis = getJedis();
			long len = sjedis.ttl(key);
			returnJedis(sjedis);
			return len;
		}

		/**
		 * 取消对key过期时间的设置
		 * 
		 * @param key
		 * @return 影响的记录数
		 */
		public long persist(String key) {
			Jedis jedis = getJedis();
			long count = jedis.persist(key);
			returnJedis(jedis);
			return count;
		}

		/**
		 * 删除keys对应的记录,可以是多个key
		 * 
		 * @param String
		 *            ... keys
		 * @return 删除的记录数
		 */
		public long del(String... keys) {
			Jedis jedis = getJedis();
			long count = jedis.del(keys);
			returnJedis(jedis);
			return count;
		}

		/**
		 * 删除keys对应的记录,可以是多个key
		 * 
		 * @param String
		 *            .. keys
		 * @return 删除的记录数
		 */
		public long del(byte[]... keys) {
			Jedis jedis = getJedis();
			long count = jedis.del(keys);
			returnJedis(jedis);
			return count;
		}

		/**
		 * 判断key是否存在
		 * 
		 * @param String
		 *            key
		 * @return boolean
		 */
		public boolean exists(String key) {
			// ShardedJedis sjedis = getShardedJedis();
			Jedis sjedis = getJedis();
			boolean exis = sjedis.exists(key);
			returnJedis(sjedis);
			return exis;
		}

		/**
		 * 对List,Set,SortSet进行排序,如果集合数据较大应避免使用这个方法
		 * 
		 * @param String
		 *            key
		 * @return List<String> 集合的全部记录
		 **/
		public List<String> sort(String key) {
			// ShardedJedis sjedis = getShardedJedis();
			Jedis sjedis = getJedis();
			List<String> list = sjedis.sort(key);
			returnJedis(sjedis);
			return list;
		}

		/**
		 * 对List,Set,SortSet进行排序或limit
		 * 
		 * @param String
		 *            key
		 * @param SortingParams
		 *            parame 定义排序类型或limit的起止位置.
		 * @return List<String> 全部或部分记录
		 **/
		public List<String> sort(String key, SortingParams parame) {
			// ShardedJedis sjedis = getShardedJedis();
			Jedis sjedis = getJedis();
			List<String> list = sjedis.sort(key, parame);
			returnJedis(sjedis);
			return list;
		}

		/**
		 * 返回指定key存储的类型
		 * 
		 * @param String
		 *            key
		 * @return String string|list|set|zset|hash
		 **/
		public String type(String key) {
			// ShardedJedis sjedis = getShardedJedis();
			Jedis sjedis = getJedis();
			String type = sjedis.type(key);
			returnJedis(sjedis);
			return type;
		}

		/**
		 * 查找所有匹配给定的模式的键
		 * 
		 * @param String
		 *            key的表达式,*表示多个，？表示一个
		 */
		public Set<String> keys(String pattern) {
			Jedis jedis = getJedis();
			Set<String> set = jedis.keys(pattern);
			returnJedis(jedis);
			return set;
		}
	}

	// *******************************************Sets*******************************************//
	public class Sets {

		/**
		 * 向Set添加一条记录，如果member已存在返回0,否则返回1
		 * 
		 * @param String
		 *            key
		 * @param String
		 *            member
		 * @return 操作码,0或1
		 */
		public long sadd(String key, String member) {
			Jedis jedis = getJedis();
			long s = jedis.sadd(key, member);
			returnJedis(jedis);
			return s;
		}

		public long sadd(byte[] key, byte[] member) {
			Jedis jedis = getJedis();
			long s = jedis.sadd(key, member);
			returnJedis(jedis);
			return s;
		}

		/**
		 * 获取给定key中元素个数
		 * 
		 * @param String
		 *            key
		 * @return 元素个数
		 */
		public long scard(String key) {
			// ShardedJedis sjedis = getShardedJedis();
			Jedis sjedis = getJedis();
			long len = sjedis.scard(key);
			returnJedis(sjedis);
			return len;
		}

		/**
		 * 返回从第一组和所有的给定集合之间的差异的成员
		 * 
		 * @param String
		 *            ... keys
		 * @return 差异的成员集合
		 */
		public Set<String> sdiff(String... keys) {
			Jedis jedis = getJedis();
			Set<String> set = jedis.sdiff(keys);
			returnJedis(jedis);
			return set;
		}

		/**
		 * 这个命令等于sdiff,但返回的不是结果集,而是将结果集存储在新的集合中，如果目标已存在，则覆盖。
		 * 
		 * @param String
		 *            newkey 新结果集的key
		 * @param String
		 *            ... keys 比较的集合
		 * @return 新集合中的记录数
		 **/
		public long sdiffstore(String newkey, String... keys) {
			Jedis jedis = getJedis();
			long s = jedis.sdiffstore(newkey, keys);
			returnJedis(jedis);
			return s;
		}

		/**
		 * 返回给定集合交集的成员,如果其中一个集合为不存在或为空，则返回空Set
		 * 
		 * @param String
		 *            ... keys
		 * @return 交集成员的集合
		 **/
		public Set<String> sinter(String... keys) {
			Jedis jedis = getJedis();
			Set<String> set = jedis.sinter(keys);
			returnJedis(jedis);
			return set;
		}

		/**
		 * 这个命令等于sinter,但返回的不是结果集,而是将结果集存储在新的集合中，如果目标已存在，则覆盖。
		 * 
		 * @param String
		 *            newkey 新结果集的key
		 * @param String
		 *            ... keys 比较的集合
		 * @return 新集合中的记录数
		 **/
		public long sinterstore(String newkey, String... keys) {
			Jedis jedis = getJedis();
			long s = jedis.sinterstore(newkey, keys);
			returnJedis(jedis);
			return s;
		}

		/**
		 * 确定一个给定的值是否存在
		 * 
		 * @param String
		 *            key
		 * @param String
		 *            member 要判断的值
		 * @return 存在返回1，不存在返回0
		 **/
		public boolean sismember(String key, String member) {
			// ShardedJedis sjedis = getShardedJedis();
			Jedis sjedis = getJedis();
			boolean s = sjedis.sismember(key, member);
			returnJedis(sjedis);
			return s;
		}

		/**
		 * 返回集合中的所有成员
		 * 
		 * @param String
		 *            key
		 * @return 成员集合
		 */
		public Set<String> smembers(String key) {
			// ShardedJedis sjedis = getShardedJedis();
			Jedis sjedis = getJedis();
			Set<String> set = sjedis.smembers(key);
			returnJedis(sjedis);
			return set;
		}

		public Set<byte[]> smembers(byte[] key) {
			// ShardedJedis sjedis = getShardedJedis();
			Jedis sjedis = getJedis();
			Set<byte[]> set = sjedis.smembers(key);
			returnJedis(sjedis);
			return set;
		}

		/**
		 * 将成员从源集合移出放入目标集合 <br/>
		 * 如果源集合不存在或不包哈指定成员，不进行任何操作，返回0<br/>
		 * 否则该成员从源集合上删除，并添加到目标集合，如果目标集合中成员已存在，则只在源集合进行删除
		 * 
		 * @param String
		 *            srckey 源集合
		 * @param String
		 *            dstkey 目标集合
		 * @param String
		 *            member 源集合中的成员
		 * @return 状态码，1成功，0失败
		 */
		public long smove(String srckey, String dstkey, String member) {
			Jedis jedis = getJedis();
			long s = jedis.smove(srckey, dstkey, member);
			returnJedis(jedis);
			return s;
		}

		/**
		 * 从集合中删除成员
		 * 
		 * @param String
		 *            key
		 * @return 被删除的成员
		 */
		public String spop(String key) {
			Jedis jedis = getJedis();
			String s = jedis.spop(key);
			returnJedis(jedis);
			return s;
		}

		/**
		 * 从集合中删除指定成员
		 * 
		 * @param String
		 *            key
		 * @param String
		 *            member 要删除的成员
		 * @return 状态码，成功返回1，成员不存在返回0
		 */
		public long srem(String key, String member) {
			Jedis jedis = getJedis();
			long s = jedis.srem(key, member);
			returnJedis(jedis);
			return s;
		}

		/**
		 * 合并多个集合并返回合并后的结果，合并后的结果集合并不保存<br/>
		 * 
		 * @param String
		 *            ... keys
		 * @return 合并后的结果集合
		 * @see sunionstore
		 */
		public Set<String> sunion(String... keys) {
			Jedis jedis = getJedis();
			Set<String> set = jedis.sunion(keys);
			returnJedis(jedis);
			return set;
		}

		/**
		 * 合并多个集合并将合并后的结果集保存在指定的新集合中，如果新集合已经存在则覆盖
		 * 
		 * @param String
		 *            newkey 新集合的key
		 * @param String
		 *            ... keys 要合并的集合
		 **/
		public long sunionstore(String newkey, String... keys) {
			Jedis jedis = getJedis();
			long s = jedis.sunionstore(newkey, keys);
			returnJedis(jedis);
			return s;
		}
	}

	// *******************************************SortSet*******************************************//
	public class SortSet {

		/**
		 * 向集合中增加一条记录,如果这个值已存在，这个值对应的权重将被置为新的权重
		 * 
		 * @param String
		 *            key
		 * @param double
		 *            score 权重
		 * @param String
		 *            member 要加入的值，
		 * @return 状态码 1成功，0已存在member的值
		 */
		public long zadd(String key, double score, String member) {
			Jedis jedis = getJedis();
			long s = jedis.zadd(key, score, member);
			returnJedis(jedis);
			return s;
		}

		/*
		 * public long zadd(String key, Map<Double, String> scoreMembers) { Jedis jedis
		 * = getJedis(); long s = jedis.zadd(key, scoreMembers); returnJedis(jedis);
		 * return s; }
		 */

		/**
		 * 获取集合中元素的数量
		 * 
		 * @param String
		 *            key
		 * @return 如果返回0则集合不存在
		 */
		public long zcard(String key) {
			// ShardedJedis sjedis = getShardedJedis();
			Jedis sjedis = getJedis();
			long len = sjedis.zcard(key);
			returnJedis(sjedis);
			return len;
		}

		/**
		 * 获取指定权重区间内集合的数量
		 * 
		 * @param String
		 *            key
		 * @param double
		 *            min 最小排序位置
		 * @param double
		 *            max 最大排序位置
		 */
		public long zcount(String key, double min, double max) {
			// ShardedJedis sjedis = getShardedJedis();
			Jedis sjedis = getJedis();
			long len = sjedis.zcount(key, min, max);
			returnJedis(sjedis);
			return len;
		}

		/**
		 * 获得set的长度
		 * 
		 * @param key
		 * @return
		 */
		public long zlength(String key) {
			long len = 0;
			Set<String> set = zrange(key, 0, -1);
			len = set.size();
			return len;
		}

		/**
		 * 权重增加给定值，如果给定的member已存在
		 * 
		 * @param String
		 *            key
		 * @param double
		 *            score 要增的权重
		 * @param String
		 *            member 要插入的值
		 * @return 增后的权重
		 */
		public double zincrby(String key, double score, String member) {
			Jedis jedis = getJedis();
			double s = jedis.zincrby(key, score, member);
			returnJedis(jedis);
			return s;
		}

		/**
		 * 返回指定位置的集合元素,0为第一个元素，-1为最后一个元素
		 * 
		 * @param String
		 *            key
		 * @param int
		 *            start 开始位置(包含)
		 * @param int
		 *            end 结束位置(包含)
		 * @return Set<String>
		 */
		public Set<String> zrange(String key, int start, int end) {
			// ShardedJedis sjedis = getShardedJedis();
			Jedis sjedis = getJedis();
			Set<String> set = sjedis.zrange(key, start, end);
			returnJedis(sjedis);
			return set;
		}

		/**
		 * 返回指定权重区间的元素集合
		 * 
		 * @param String
		 *            key
		 * @param double
		 *            min 上限权重
		 * @param double
		 *            max 下限权重
		 * @return Set<String>
		 */
		public Set<String> zrangeByScore(String key, double min, double max) {
			// ShardedJedis sjedis = getShardedJedis();
			Jedis sjedis = getJedis();
			Set<String> set = sjedis.zrangeByScore(key, min, max);
			returnJedis(sjedis);
			return set;
		}

		/**
		 * 获取指定值在集合中的位置，集合排序从低到高
		 * 
		 * @see zrevrank
		 * @param String
		 *            key
		 * @param String
		 *            member
		 * @return long 位置
		 */
		public long zrank(String key, String member) {
			// ShardedJedis sjedis = getShardedJedis();
			Jedis sjedis = getJedis();
			long index = sjedis.zrank(key, member);
			returnJedis(sjedis);
			return index;
		}

		/**
		 * 获取指定值在集合中的位置，集合排序从高到低
		 * 
		 * @see zrank
		 * @param String
		 *            key
		 * @param String
		 *            member
		 * @return long 位置
		 */
		public long zrevrank(String key, String member) {
			// ShardedJedis sjedis = getShardedJedis();
			Jedis sjedis = getJedis();
			long index = sjedis.zrevrank(key, member);
			returnJedis(sjedis);
			return index;
		}

		/**
		 * 从集合中删除成员
		 * 
		 * @param String
		 *            key
		 * @param String
		 *            member
		 * @return 返回1成功
		 */
		public long zrem(String key, String member) {
			Jedis jedis = getJedis();
			long s = jedis.zrem(key, member);
			returnJedis(jedis);
			return s;
		}

		/**
		 * 删除
		 * 
		 * @param key
		 * @return
		 */
		public long zrem(String key) {
			Jedis jedis = getJedis();
			long s = jedis.del(key);
			returnJedis(jedis);
			return s;
		}

		/**
		 * 删除给定位置区间的元素
		 * 
		 * @param String
		 *            key
		 * @param int
		 *            start 开始区间，从0开始(包含)
		 * @param int
		 *            end 结束区间,-1为最后一个元素(包含)
		 * @return 删除的数量
		 */
		public long zremrangeByRank(String key, int start, int end) {
			Jedis jedis = getJedis();
			long s = jedis.zremrangeByRank(key, start, end);
			returnJedis(jedis);
			return s;
		}

		/**
		 * 删除给定权重区间的元素
		 * 
		 * @param String
		 *            key
		 * @param double
		 *            min 下限权重(包含)
		 * @param double
		 *            max 上限权重(包含)
		 * @return 删除的数量
		 */
		public long zremrangeByScore(String key, double min, double max) {
			Jedis jedis = getJedis();
			long s = jedis.zremrangeByScore(key, min, max);
			returnJedis(jedis);
			return s;
		}

		/**
		 * 获取给定区间的元素，原始按照权重由高到低排序
		 * 
		 * @param String
		 *            key
		 * @param int
		 *            start
		 * @param int
		 *            end
		 * @return Set<String>
		 */
		public Set<String> zrevrange(String key, int start, int end) {
			// ShardedJedis sjedis = getShardedJedis();
			Jedis sjedis = getJedis();
			Set<String> set = sjedis.zrevrange(key, start, end);
			returnJedis(sjedis);
			return set;
		}

		/**
		 * 获取给定值在集合中的权重
		 * 
		 * @param String
		 *            key
		 * @param memeber
		 * @return double 权重
		 */
		public double zscore(String key, String memebr) {
			// ShardedJedis sjedis = getShardedJedis();
			Jedis sjedis = getJedis();
			Double score = sjedis.zscore(key, memebr);
			returnJedis(sjedis);
			if (score != null)
				return score;
			return 0;
		}
	}

	// *******************************************Hash*******************************************//
	public class Hash {

		/**
		 * 从hash中删除指定的存储
		 * 
		 * @param String
		 *            key
		 * @param String
		 *            fieid 存储的名字
		 * @return 状态码，1成功，0失败
		 */
		public long hdel(String key, String fieid) {
			Jedis jedis = getJedis();
			long s = jedis.hdel(key, fieid);
			returnJedis(jedis);
			return s;
		}

		public long hdel(String key) {
			Jedis jedis = getJedis();
			long s = jedis.del(key);
			returnJedis(jedis);
			return s;
		}

		/**
		 * 测试hash中指定的存储是否存在
		 * 
		 * @param String
		 *            key
		 * @param String
		 *            fieid 存储的名字
		 * @return 1存在，0不存在
		 */
		public boolean hexists(String key, String fieid) {
			// ShardedJedis sjedis = getShardedJedis();
			Jedis sjedis = getJedis();
			boolean s = sjedis.hexists(key, fieid);
			returnJedis(sjedis);
			return s;
		}

		/**
		 * 返回hash中指定存储位置的值
		 * 
		 * @param String
		 *            key
		 * @param String
		 *            fieid 存储的名字
		 * @return 存储对应的值
		 */
		public String hget(String key, String fieid) {
			// ShardedJedis sjedis = getShardedJedis();
			Jedis sjedis = getJedis();
			String s = sjedis.hget(key, fieid);
			returnJedis(sjedis);
			return s;
		}

		public byte[] hget(byte[] key, byte[] fieid) {
			// ShardedJedis sjedis = getShardedJedis();
			Jedis sjedis = getJedis();
			byte[] s = sjedis.hget(key, fieid);
			returnJedis(sjedis);
			return s;
		}

		/**
		 * 以Map的形式返回hash中的存储和值
		 * 
		 * @param String
		 *            key
		 * @return Map<Strinig,String>
		 */
		public Map<String, String> hgetAll(String key) {
			// ShardedJedis sjedis = getShardedJedis();
			Jedis sjedis = getJedis();
			Map<String, String> map = sjedis.hgetAll(key);
			returnJedis(sjedis);
			return map;
		}

		/**
		 * 添加一个对应关系
		 * 
		 * @param String
		 *            key
		 * @param String
		 *            fieid
		 * @param String
		 *            value
		 * @return 状态码 1成功，0失败，fieid已存在将更新，也返回0
		 **/
		public long hset(String key, String fieid, String value) {
			Jedis jedis = getJedis();
			long s = jedis.hset(key, fieid, value);
			returnJedis(jedis);
			return s;
		}

		public long hset(String key, String fieid, byte[] value) {
			Jedis jedis = getJedis();
			long s = jedis.hset(key.getBytes(), fieid.getBytes(), value);
			returnJedis(jedis);
			return s;
		}

		/**
		 * 添加对应关系，只有在fieid不存在时才执行
		 * 
		 * @param String
		 *            key
		 * @param String
		 *            fieid
		 * @param String
		 *            value
		 * @return 状态码 1成功，0失败fieid已存
		 **/
		public long hsetnx(String key, String fieid, String value) {
			Jedis jedis = getJedis();
			long s = jedis.hsetnx(key, fieid, value);
			returnJedis(jedis);
			return s;
		}

		/**
		 * 获取hash中value的集合
		 * 
		 * @param String
		 *            key
		 * @return List<String>
		 */
		public List<String> hvals(String key) {
			// ShardedJedis sjedis = getShardedJedis();
			Jedis sjedis = getJedis();
			List<String> list = sjedis.hvals(key);
			returnJedis(sjedis);
			return list;
		}

		/**
		 * 在指定的存储位置加上指定的数字，存储位置的值必须可转为数字类型
		 * 
		 * @param String
		 *            key
		 * @param String
		 *            fieid 存储位置
		 * @param String
		 *            long value 要增加的值,可以是负数
		 * @return 增加指定数字后，存储位置的值
		 */
		public long hincrby(String key, String fieid, long value) {
			Jedis jedis = getJedis();
			long s = jedis.hincrBy(key, fieid, value);
			returnJedis(jedis);
			return s;
		}

		/**
		 * 返回指定hash中的所有存储名字,类似Map中的keySet方法
		 * 
		 * @param String
		 *            key
		 * @return Set<String> 存储名称的集合
		 */
		public Set<String> hkeys(String key) {
			// ShardedJedis sjedis = getShardedJedis();
			Jedis sjedis = getJedis();
			Set<String> set = sjedis.hkeys(key);
			returnJedis(sjedis);
			return set;
		}

		/**
		 * 获取hash中存储的个数，类似Map中size方法
		 * 
		 * @param String
		 *            key
		 * @return long 存储的个数
		 */
		public long hlen(String key) {
			// ShardedJedis sjedis = getShardedJedis();
			Jedis sjedis = getJedis();
			long len = sjedis.hlen(key);
			returnJedis(sjedis);
			return len;
		}

		/**
		 * 根据多个key，获取对应的value，返回List,如果指定的key不存在,List对应位置为null
		 * 
		 * @param String
		 *            key
		 * @param String
		 *            ... fieids 存储位置
		 * @return List<String>
		 */
		public List<String> hmget(String key, String... fieids) {
			// ShardedJedis sjedis = getShardedJedis();
			Jedis sjedis = getJedis();
			List<String> list = sjedis.hmget(key, fieids);
			returnJedis(sjedis);
			return list;
		}

		public List<byte[]> hmget(byte[] key, byte[]... fieids) {
			// ShardedJedis sjedis = getShardedJedis();
			Jedis sjedis = getJedis();
			List<byte[]> list = sjedis.hmget(key, fieids);
			returnJedis(sjedis);
			return list;
		}

		/**
		 * 添加对应关系，如果对应关系已存在，则覆盖
		 * 
		 * @param Strin
		 *            key
		 * @param Map
		 *            <String,String> 对应关系
		 * @return 状态，成功返回OK
		 */
		public String hmset(String key, Map<String, String> map) {
			Jedis jedis = getJedis();
			String s = jedis.hmset(key, map);
			returnJedis(jedis);
			return s;
		}

		/**
		 * 添加对应关系，如果对应关系已存在，则覆盖
		 * 
		 * @param Strin
		 *            key
		 * @param Map
		 *            <String,String> 对应关系
		 * @return 状态，成功返回OK
		 */
		public String hmset(byte[] key, Map<byte[], byte[]> map) {
			Jedis jedis = getJedis();
			String s = jedis.hmset(key, map);
			returnJedis(jedis);
			return s;
		}

	}

	// *******************************************Strings*******************************************//
	public class Strings {
		/**
		 * 根据key获取记录
		 * 
		 * @param String
		 *            key
		 * @return 值
		 */
		public String get(String key) {
			// ShardedJedis sjedis = getShardedJedis();
			Jedis sjedis = getJedis();
			String value = sjedis.get(key);
			returnJedis(sjedis);
			return value;
		}

		/**
		 * 根据key获取记录
		 * 
		 * @param byte[]
		 *            key
		 * @return 值
		 */
		public byte[] get(byte[] key) {
			// ShardedJedis sjedis = getShardedJedis();
			Jedis sjedis = getJedis();
			byte[] value = sjedis.get(key);
			returnJedis(sjedis);
			return value;
		}

		/**
		 * 添加有过期时间的记录
		 * 
		 * @param String
		 *            key
		 * @param int
		 *            seconds 过期时间，以秒为单位
		 * @param String
		 *            value
		 * @return String 操作状态
		 */
		public String setEx(String key, int seconds, String value) {
			Jedis jedis = getJedis();
			String str = jedis.setex(key, seconds, value);
			returnJedis(jedis);
			return str;
		}

		/**
		 * 添加有过期时间的记录
		 * 
		 * @param String
		 *            key
		 * @param int
		 *            seconds 过期时间，以秒为单位
		 * @param String
		 *            value
		 * @return String 操作状态
		 */
		public String setEx(byte[] key, int seconds, byte[] value) {
			Jedis jedis = getJedis();
			String str = jedis.setex(key, seconds, value);
			returnJedis(jedis);
			return str;
		}

		/**
		 * 添加一条记录，仅当给定的key不存在时才插入
		 * 
		 * @param String
		 *            key
		 * @param String
		 *            value
		 * @return long 状态码，1插入成功且key不存在，0未插入，key存在
		 */
		public long setnx(String key, String value) {
			Jedis jedis = getJedis();
			long str = jedis.setnx(key, value);
			returnJedis(jedis);
			return str;
		}

		/**
		 * 添加记录,如果记录已存在将覆盖原有的value
		 * 
		 * @param String
		 *            key
		 * @param String
		 *            value
		 * @return 状态码
		 */
		public String set(String key, String value) {
			return set(SafeEncoder.encode(key), SafeEncoder.encode(value));
		}

		/**
		 * 添加记录,如果记录已存在将覆盖原有的value
		 * 
		 * @param String
		 *            key
		 * @param String
		 *            value
		 * @return 状态码
		 */
		public String set(String key, byte[] value) {
			return set(SafeEncoder.encode(key), value);
		}

		/**
		 * 添加记录,如果记录已存在将覆盖原有的value
		 * 
		 * @param byte[]
		 *            key
		 * @param byte[]
		 *            value
		 * @return 状态码
		 */
		public String set(byte[] key, byte[] value) {
			Jedis jedis = getJedis();
			String status = jedis.set(key, value);
			returnJedis(jedis);
			return status;
		}

		/**
		 * 从指定位置开始插入数据，插入的数据会覆盖指定位置以后的数据<br/>
		 * 例:String str1="123456789";<br/>
		 * 对str1操作后setRange(key,4,0000)，str1="123400009";
		 * 
		 * @param String
		 *            key
		 * @param long
		 *            offset
		 * @param String
		 *            value
		 * @return long value的长度
		 */
		public long setRange(String key, long offset, String value) {
			Jedis jedis = getJedis();
			long len = jedis.setrange(key, offset, value);
			returnJedis(jedis);
			return len;
		}

		/**
		 * 在指定的key中追加value
		 * 
		 * @param String
		 *            key
		 * @param String
		 *            value
		 * @return long 追加后value的长度
		 **/
		public long append(String key, String value) {
			Jedis jedis = getJedis();
			long len = jedis.append(key, value);
			returnJedis(jedis);
			return len;
		}

		/**
		 * 将key对应的value减去指定的值，只有value可以转为数字时该方法才可用
		 * 
		 * @param String
		 *            key
		 * @param long
		 *            number 要减去的值
		 * @return long 减指定值后的值
		 */
		public long decrBy(String key, long number) {
			Jedis jedis = getJedis();
			long len = jedis.decrBy(key, number);
			returnJedis(jedis);
			return len;
		}

		/**
		 * <b>可以作为获取唯一id的方法</b><br/>
		 * 将key对应的value加上指定的值，只有value可以转为数字时该方法才可用
		 * 
		 * @param String
		 *            key
		 * @param long
		 *            number 要减去的值
		 * @return long 相加后的值
		 */
		public long incrBy(String key, long number) {
			Jedis jedis = getJedis();
			long len = jedis.incrBy(key, number);
			returnJedis(jedis);
			return len;
		}

		/**
		 * 对指定key对应的value进行截取
		 * 
		 * @param String
		 *            key
		 * @param long
		 *            startOffset 开始位置(包含)
		 * @param long
		 *            endOffset 结束位置(包含)
		 * @return String 截取的值
		 */
		public String getrange(String key, long startOffset, long endOffset) {
			// ShardedJedis sjedis = getShardedJedis();
			Jedis sjedis = getJedis();
			String value = sjedis.getrange(key, startOffset, endOffset);
			returnJedis(sjedis);
			return value;
		}

		/**
		 * 获取并设置指定key对应的value<br/>
		 * 如果key存在返回之前的value,否则返回null
		 * 
		 * @param String
		 *            key
		 * @param String
		 *            value
		 * @return String 原始value或null
		 */
		public String getSet(String key, String value) {
			Jedis jedis = getJedis();
			String str = jedis.getSet(key, value);
			returnJedis(jedis);
			return str;
		}

		/**
		 * 批量获取记录,如果指定的key不存在返回List的对应位置将是null
		 * 
		 * @param String
		 *            keys
		 * @return List<String> 值得集合
		 */
		public List<String> mget(String... keys) {
			Jedis jedis = getJedis();
			List<String> str = jedis.mget(keys);
			returnJedis(jedis);
			return str;
		}

		/**
		 * 批量存储记录
		 * 
		 * @param String
		 *            keysvalues 例:keysvalues="key1","value1","key2","value2";
		 * @return String 状态码
		 */
		public String mset(String... keysvalues) {
			Jedis jedis = getJedis();
			String str = jedis.mset(keysvalues);
			returnJedis(jedis);
			return str;
		}

		/**
		 * 获取key对应的值的长度
		 * 
		 * @param String
		 *            key
		 * @return value值得长度
		 */
		public long strlen(String key) {
			Jedis jedis = getJedis();
			long len = jedis.strlen(key);
			returnJedis(jedis);
			return len;
		}
	}

	// *******************************************Lists*******************************************//
	public class Lists {
		/**
		 * List长度
		 * 
		 * @param String
		 *            key
		 * @return 长度
		 */
		public long llen(String key) {
			return llen(SafeEncoder.encode(key));
		}

		/**
		 * List长度
		 * 
		 * @param byte[]
		 *            key
		 * @return 长度
		 */
		public long llen(byte[] key) {
			// ShardedJedis sjedis = getShardedJedis();
			Jedis sjedis = getJedis();
			long count = sjedis.llen(key);
			returnJedis(sjedis);
			return count;
		}

		/**
		 * 覆盖操作,将覆盖List中指定位置的值
		 * 
		 * @param byte[]
		 *            key
		 * @param int
		 *            index 位置
		 * @param byte[]
		 *            value 值
		 * @return 状态码
		 */
		public String lset(byte[] key, int index, byte[] value) {
			Jedis jedis = getJedis();
			String status = jedis.lset(key, index, value);
			returnJedis(jedis);
			return status;
		}

		/**
		 * 覆盖操作,将覆盖List中指定位置的值
		 * 
		 * @param key
		 * @param int
		 *            index 位置
		 * @param String
		 *            value 值
		 * @return 状态码
		 */
		public String lset(String key, int index, String value) {
			return lset(SafeEncoder.encode(key), index, SafeEncoder.encode(value));
		}

		/**
		 * 在value的相对位置插入记录
		 * 
		 * @param key
		 * @param LIST_POSITION
		 *            前面插入或后面插入
		 * @param String
		 *            pivot 相对位置的内容
		 * @param String
		 *            value 插入的内容
		 * @return 记录总数
		 */
		public long linsert(String key, LIST_POSITION where, String pivot, String value) {
			return linsert(SafeEncoder.encode(key), where, SafeEncoder.encode(pivot), SafeEncoder.encode(value));
		}

		/**
		 * 在指定位置插入记录
		 * 
		 * @param String
		 *            key
		 * @param LIST_POSITION
		 *            前面插入或后面插入
		 * @param byte[]
		 *            pivot 相对位置的内容
		 * @param byte[]
		 *            value 插入的内容
		 * @return 记录总数
		 */
		public long linsert(byte[] key, LIST_POSITION where, byte[] pivot, byte[] value) {
			Jedis jedis = getJedis();
			long count = jedis.linsert(key, where, pivot, value);
			returnJedis(jedis);
			return count;
		}

		/**
		 * 获取List中指定位置的值
		 * 
		 * @param String
		 *            key
		 * @param int
		 *            index 位置
		 * @return 值
		 **/
		public String lindex(String key, int index) {
			return SafeEncoder.encode(lindex(SafeEncoder.encode(key), index));
		}

		/**
		 * 获取List中指定位置的值
		 * 
		 * @param byte[]
		 *            key
		 * @param int
		 *            index 位置
		 * @return 值
		 **/
		public byte[] lindex(byte[] key, int index) {
			// ShardedJedis sjedis = getShardedJedis();
			Jedis sjedis = getJedis();
			byte[] value = sjedis.lindex(key, index);
			returnJedis(sjedis);
			return value;
		}

		/**
		 * 将List中的第一条记录移出List
		 * 
		 * @param String
		 *            key
		 * @return 移出的记录
		 */
		public String lpop(String key) {
			return SafeEncoder.encode(lpop(SafeEncoder.encode(key)));
		}

		/**
		 * 将List中的第一条记录移出List
		 * 
		 * @param byte[]
		 *            key
		 * @return 移出的记录
		 */
		public byte[] lpop(byte[] key) {
			Jedis jedis = getJedis();
			byte[] value = jedis.lpop(key);
			returnJedis(jedis);
			return value;
		}

		/**
		 * 将List中最后第一条记录移出List
		 * 
		 * @param byte[]
		 *            key
		 * @return 移出的记录
		 */
		public String rpop(String key) {
			Jedis jedis = getJedis();
			String value = jedis.rpop(key);
			returnJedis(jedis);
			return value;
		}

		/**
		 * 向List尾部追加记录
		 * 
		 * @param String
		 *            key
		 * @param String
		 *            value
		 * @return 记录总数
		 */
		public long lpush(String key, String value) {
			return lpush(SafeEncoder.encode(key), SafeEncoder.encode(value));
		}

		/**
		 * 向List头部追加记录
		 * 
		 * @param String
		 *            key
		 * @param String
		 *            value
		 * @return 记录总数
		 */
		public long rpush(String key, String value) {
			Jedis jedis = getJedis();
			long count = jedis.rpush(key, value);
			returnJedis(jedis);
			return count;
		}

		/**
		 * 向List头部追加记录
		 * 
		 * @param String
		 *            key
		 * @param String
		 *            value
		 * @return 记录总数
		 */
		public long rpush(byte[] key, byte[] value) {
			Jedis jedis = getJedis();
			long count = jedis.rpush(key, value);
			returnJedis(jedis);
			return count;
		}

		/**
		 * 向List中追加记录
		 * 
		 * @param byte[]
		 *            key
		 * @param byte[]
		 *            value
		 * @return 记录总数
		 */
		public long lpush(byte[] key, byte[] value) {
			Jedis jedis = getJedis();
			long count = jedis.lpush(key, value);
			returnJedis(jedis);
			return count;
		}

		/**
		 * 获取指定范围的记录，可以做为分页使用
		 * 
		 * @param String
		 *            key
		 * @param long
		 *            start
		 * @param long
		 *            end
		 * @return List
		 */
		public List<String> lrange(String key, long start, long end) {
			// ShardedJedis sjedis = getShardedJedis();
			Jedis sjedis = getJedis();
			List<String> list = sjedis.lrange(key, start, end);
			returnJedis(sjedis);
			return list;
		}

		/**
		 * 获取指定范围的记录，可以做为分页使用
		 * 
		 * @param byte[]
		 *            key
		 * @param int
		 *            start
		 * @param int
		 *            end 如果为负数，则尾部开始计算
		 * @return List
		 */
		public List<byte[]> lrange(byte[] key, int start, int end) {
			// ShardedJedis sjedis = getShardedJedis();
			Jedis sjedis = getJedis();
			List<byte[]> list = sjedis.lrange(key, start, end);
			returnJedis(sjedis);
			return list;
		}

		/**
		 * 删除List中c条记录，被删除的记录值为value
		 * 
		 * @param byte[]
		 *            key
		 * @param int
		 *            c 要删除的数量，如果为负数则从List的尾部检查并删除符合的记录
		 * @param byte[]
		 *            value 要匹配的值
		 * @return 删除后的List中的记录数
		 */
		public long lrem(byte[] key, int c, byte[] value) {
			Jedis jedis = getJedis();
			long count = jedis.lrem(key, c, value);
			returnJedis(jedis);
			return count;
		}

		/**
		 * 删除List中c条记录，被删除的记录值为value
		 * 
		 * @param String
		 *            key
		 * @param int
		 *            c 要删除的数量，如果为负数则从List的尾部检查并删除符合的记录
		 * @param String
		 *            value 要匹配的值
		 * @return 删除后的List中的记录数
		 */
		public long lrem(String key, int c, String value) {
			return lrem(SafeEncoder.encode(key), c, SafeEncoder.encode(value));
		}

		/**
		 * 算是删除吧，只保留start与end之间的记录
		 * 
		 * @param byte[]
		 *            key
		 * @param int
		 *            start 记录的开始位置(0表示第一条记录)
		 * @param int
		 *            end 记录的结束位置（如果为-1则表示最后一个，-2，-3以此类推）
		 * @return 执行状态码
		 */
		public String ltrim(byte[] key, int start, int end) {
			Jedis jedis = getJedis();
			String str = jedis.ltrim(key, start, end);
			returnJedis(jedis);
			return str;
		}

		/**
		 * 算是删除吧，只保留start与end之间的记录
		 * 
		 * @param String
		 *            key
		 * @param int
		 *            start 记录的开始位置(0表示第一条记录)
		 * @param int
		 *            end 记录的结束位置（如果为-1则表示最后一个，-2，-3以此类推）
		 * @return 执行状态码
		 */
		public String ltrim(String key, int start, int end) {
			return ltrim(SafeEncoder.encode(key), start, end);
		}
	}

	public static void main(String[] args) throws InterruptedException {
		JedisUtil jedisUtil = JedisUtil.getInstance();
		JedisUtil.Strings strings = jedisUtil.new Strings();
		strings.set("nnn", "nnnn");
		System.out.println("-----" + strings.get("nnn"));

		JedisUtil.Hash hash = jedisUtil.new Hash();
		hash.hset("keyname:", "fullname", "cailin");
		System.out.println("==" + hash.hget("keyname:", "fullname"));

		Jedis jedis = JedisUtil.getInstance().getJedis();
		for (int i = 0; i < 10; i++) {
			jedis.set("test", "test");
			System.out.println(i + "==" + jedis.get("test"));

		}

		///////////////////////////////////////////////////////////////

		System.out.println("===========添加一个list===========");
		testList(jedis);
        testString(jedis);
        testHash(jedis);
        testSortedSet(jedis);
        testSort(jedis);
        testSet(jedis);
		JedisUtil.getInstance().returnJedis(jedis);
	}

	private static void testList(Jedis jedis) {
		jedis.lpush("collections", "ArrayList", "Vector", "Stack", "HashMap", "WeakHashMap", "LinkedHashMap");
		jedis.lpush("collections", "HashSet");
		jedis.lpush("collections", "TreeSet");
		jedis.lpush("collections", "TreeMap");
		System.out.println("collections的内容：" + jedis.lrange("collections", 0, -1));// -1代表倒数第一个元素，-2代表倒数第二个元素
		System.out.println("collections区间0-3的元素：" + jedis.lrange("collections", 0, 3));
		System.out.println("===============================");
		// 删除列表指定的值 ，第二个参数为删除的个数（有重复时），后add进去的值先被删，类似于出栈
		System.out.println("删除指定元素个数：" + jedis.lrem("collections", 2, "HashMap"));
		System.out.println("collections的内容：" + jedis.lrange("collections", 0, -1));
		System.out.println("删除下表0-3区间之外的元素：" + jedis.ltrim("collections", 0, 3));
		System.out.println("collections的内容：" + jedis.lrange("collections", 0, -1));
		System.out.println("collections列表出栈（左端）：" + jedis.lpop("collections"));
		System.out.println("collections的内容：" + jedis.lrange("collections", 0, -1));
		System.out.println("collections添加元素，从列表右端，与lpush相对应：" + jedis.rpush("collections", "EnumMap"));
		System.out.println("collections的内容：" + jedis.lrange("collections", 0, -1));
		System.out.println("collections列表出栈（右端）：" + jedis.rpop("collections"));
		System.out.println("collections的内容：" + jedis.lrange("collections", 0, -1));
		System.out.println("修改collections指定下标1的内容：" + jedis.lset("collections", 1, "LinkedArrayList"));
		System.out.println("collections的内容：" + jedis.lrange("collections", 0, -1));
		System.out.println("===============================");
		System.out.println("collections的长度：" + jedis.llen("collections"));
		System.out.println("获取collections下标为2的元素：" + jedis.lindex("collections", 2));
		System.out.println("===============================");
		jedis.lpush("sortedList", "3", "6", "2", "0", "7", "4");
		System.out.println("sortedList排序前：" + jedis.lrange("sortedList", 0, -1));
		System.out.println(jedis.sort("sortedList"));
		System.out.println("sortedList排序后：" + jedis.lrange("sortedList", 0, -1));
	}

	public static void testSet(Jedis jedis) {
		jedis.flushDB();
		System.out.println("============向集合中添加元素============");
		System.out.println(jedis.sadd("eleSet", "e1", "e2", "e4", "e3", "e0", "e8", "e7", "e5"));
		System.out.println(jedis.sadd("eleSet", "e6"));
		System.out.println(jedis.sadd("eleSet", "e6"));
		System.out.println("eleSet的所有元素为：" + jedis.smembers("eleSet"));
		System.out.println("删除一个元素e0：" + jedis.srem("eleSet", "e0"));
		System.out.println("eleSet的所有元素为：" + jedis.smembers("eleSet"));
		System.out.println("删除两个元素e7和e6：" + jedis.srem("eleSet", "e7", "e6"));
		System.out.println("eleSet的所有元素为：" + jedis.smembers("eleSet"));
		System.out.println("随机的移除集合中的一个元素：" + jedis.spop("eleSet"));
		System.out.println("随机的移除集合中的一个元素：" + jedis.spop("eleSet"));
		System.out.println("eleSet的所有元素为：" + jedis.smembers("eleSet"));
		System.out.println("eleSet中包含元素的个数：" + jedis.scard("eleSet"));
		System.out.println("e3是否在eleSet中：" + jedis.sismember("eleSet", "e3"));
		System.out.println("e1是否在eleSet中：" + jedis.sismember("eleSet", "e1"));
		System.out.println("e1是否在eleSet中：" + jedis.sismember("eleSet", "e5"));
		System.out.println("=================================");
		System.out.println(jedis.sadd("eleSet1", "e1", "e2", "e4", "e3", "e0", "e8", "e7", "e5"));
		System.out.println(jedis.sadd("eleSet2", "e1", "e2", "e4", "e3", "e0", "e8"));
		System.out.println("将eleSet1中删除e1并存入eleSet3中：" + jedis.smove("eleSet1", "eleSet3", "e1"));
		System.out.println("将eleSet1中删除e2并存入eleSet3中：" + jedis.smove("eleSet1", "eleSet3", "e2"));
		System.out.println("eleSet1中的元素：" + jedis.smembers("eleSet1"));
		System.out.println("eleSet3中的元素：" + jedis.smembers("eleSet3"));
		System.out.println("============集合运算=================");
		System.out.println("eleSet1中的元素：" + jedis.smembers("eleSet1"));
		System.out.println("eleSet2中的元素：" + jedis.smembers("eleSet2"));
		System.out.println("eleSet1和eleSet2的交集:" + jedis.sinter("eleSet1", "eleSet2"));
		System.out.println("eleSet1和eleSet2的并集:" + jedis.sunion("eleSet1", "eleSet2"));
		System.out.println("eleSet1和eleSet2的差集:" + jedis.sdiff("eleSet1", "eleSet2"));// eleSet1中有，eleSet2中没有
	}

	public static void testHash(Jedis jedis) {
		jedis.flushDB();
		Map<String, String> map = new HashMap<>();
		map.put("key1", "value1");
		map.put("key2", "value2");
		map.put("key3", "value3");
		map.put("key4", "value4");
		jedis.hmset("hash", map);
		jedis.hset("hash", "key5", "value5");
		System.out.println("散列hash的所有键值对为：" + jedis.hgetAll("hash"));// return Map<String,String>
		System.out.println("散列hash的所有键为：" + jedis.hkeys("hash"));// return Set<String>
		System.out.println("散列hash的所有值为：" + jedis.hvals("hash"));// return List<String>
		System.out.println("将key6保存的值加上一个整数，如果key6不存在则添加key6：" + jedis.hincrBy("hash", "key6", 6));
		System.out.println("散列hash的所有键值对为：" + jedis.hgetAll("hash"));
		System.out.println("将key6保存的值加上一个整数，如果key6不存在则添加key6：" + jedis.hincrBy("hash", "key6", 3));
		System.out.println("散列hash的所有键值对为：" + jedis.hgetAll("hash"));
		System.out.println("删除一个或者多个键值对：" + jedis.hdel("hash", "key2"));
		System.out.println("散列hash的所有键值对为：" + jedis.hgetAll("hash"));
		System.out.println("散列hash中键值对的个数：" + jedis.hlen("hash"));
		System.out.println("判断hash中是否存在key2：" + jedis.hexists("hash", "key2"));
		System.out.println("判断hash中是否存在key3：" + jedis.hexists("hash", "key3"));
		System.out.println("获取hash中的值：" + jedis.hmget("hash", "key3"));
		System.out.println("获取hash中的值：" + jedis.hmget("hash", "key3", "key4"));
	}

	public static void testSortedSet(Jedis jedis) {
		jedis.flushDB();
		Map<Double, String> map = new HashMap<>();
		map.put(1.2, "key2");
		map.put(4.0, "key3");
		map.put(5.0, "key4");
		map.put(0.2, "key5");
		System.out.println(jedis.zadd("zset", 3, "key1"));
		// System.out.println(jedis.zadd("zset",map));
		System.out.println("zset中的所有元素：" + jedis.zrange("zset", 0, -1));
		System.out.println("zset中的所有元素：" + jedis.zrangeWithScores("zset", 0, -1));
		System.out.println("zset中的所有元素：" + jedis.zrangeByScore("zset", 0, 100));
		System.out.println("zset中的所有元素：" + jedis.zrangeByScoreWithScores("zset", 0, 100));
		System.out.println("zset中key2的分值：" + jedis.zscore("zset", "key2"));
		System.out.println("zset中key2的排名：" + jedis.zrank("zset", "key2"));
		System.out.println("删除zset中的元素key3：" + jedis.zrem("zset", "key3"));
		System.out.println("zset中的所有元素：" + jedis.zrange("zset", 0, -1));
		System.out.println("zset中元素的个数：" + jedis.zcard("zset"));
		System.out.println("zset中分值在1-4之间的元素的个数：" + jedis.zcount("zset", 1, 4));
		System.out.println("key2的分值加上5：" + jedis.zincrby("zset", 5, "key2"));
		System.out.println("key3的分值加上4：" + jedis.zincrby("zset", 4, "key3"));
		System.out.println("zset中的所有元素：" + jedis.zrange("zset", 0, -1));
	}

	public static void testSort(Jedis jedis) {
		jedis.flushDB();
		jedis.lpush("collections", "ArrayList", "Vector", "Stack", "HashMap", "WeakHashMap", "LinkedHashMap");
		System.out.println("collections的内容：" + jedis.lrange("collections", 0, -1));
		SortingParams sortingParameters = new SortingParams();
		System.out.println(jedis.sort("collections", sortingParameters.alpha()));
		System.out.println("===============================");
		jedis.lpush("sortedList", "3", "6", "2", "0", "7", "4");
		System.out.println("sortedList排序前：" + jedis.lrange("sortedList", 0, -1));
		System.out.println("升序：" + jedis.sort("sortedList", sortingParameters.asc()));
		System.out.println("升序：" + jedis.sort("sortedList", sortingParameters.desc()));
		System.out.println("===============================");
		jedis.lpush("userlist", "33");
		jedis.lpush("userlist", "22");
		jedis.lpush("userlist", "55");
		jedis.lpush("userlist", "11");
		jedis.hset("user:66", "name", "66");
		jedis.hset("user:55", "name", "55");
		jedis.hset("user:33", "name", "33");
		jedis.hset("user:22", "name", "79");
		jedis.hset("user:11", "name", "24");
		jedis.hset("user:11", "add", "beijing");
		jedis.hset("user:22", "add", "shanghai");
		jedis.hset("user:33", "add", "guangzhou");
		jedis.hset("user:55", "add", "chongqing");
		jedis.hset("user:66", "add", "xi'an");
		sortingParameters = new SortingParams();
		sortingParameters.get("user:*->name");
		sortingParameters.get("user:*->add");
		System.out.println(jedis.sort("userlist", sortingParameters));
	}

	public static void testString(Jedis jedis) throws InterruptedException {
		jedis.flushDB();
		System.out.println("===========增加数据===========");
		System.out.println(jedis.set("key1", "value1"));
		System.out.println(jedis.set("key2", "value2"));
		System.out.println(jedis.set("key3", "value3"));
		System.out.println("删除键key2:" + jedis.del("key2"));
		System.out.println("获取键key2:" + jedis.get("key2"));
		System.out.println("修改key1:" + jedis.set("key1", "value1Changed"));
		System.out.println("获取key1的值：" + jedis.get("key1"));
		System.out.println("在key3后面加入值：" + jedis.append("key3", "End"));
		System.out.println("key3的值：" + jedis.get("key3"));
		System.out.println("增加多个键值对：" + jedis.mset("key01", "value01", "key02", "value02", "key03", "value03"));
		System.out.println("获取多个键值对：" + jedis.mget("key01", "key02", "key03"));
		System.out.println("获取多个键值对：" + jedis.mget("key01", "key02", "key03", "key04"));
		System.out.println("删除多个键值对：" + jedis.del(new String[] { "key01", "key02" }));
		System.out.println("获取多个键值对：" + jedis.mget("key01", "key02", "key03"));

		jedis.flushDB();
		System.out.println("===========新增键值对防止覆盖原先值==============");
		System.out.println(jedis.setnx("key1", "value1"));
		System.out.println(jedis.setnx("key2", "value2"));
		System.out.println(jedis.setnx("key2", "value2-new"));
		System.out.println(jedis.get("key1"));
		System.out.println(jedis.get("key2"));

		System.out.println("===========新增键值对并设置有效时间=============");
		System.out.println(jedis.setex("key3", 2, "value3"));
		System.out.println(jedis.get("key3"));
		TimeUnit.SECONDS.sleep(3);
		System.out.println(jedis.get("key3"));

		System.out.println("===========获取原值，更新为新值==========");// GETSET is an atomic set this value and return the old
																// value command.
		System.out.println(jedis.getSet("key2", "key2GetSet"));
		System.out.println(jedis.get("key2"));

		System.out.println("获得key2的值的字串：" + jedis.getrange("key2", 2, 4));
	}

}
