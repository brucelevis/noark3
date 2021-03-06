/*
 * Copyright © 2018 www.noark.xyz All Rights Reserved.
 * 
 * 感谢您选择Noark框架，希望我们的努力能为您提供一个简单、易用、稳定的服务器端框架 ！
 * 除非符合Noark许可协议，否则不得使用该文件，您可以下载许可协议文件：
 * 
 * 		http://www.noark.xyz/LICENSE
 *
 * 1.未经许可，任何公司及个人不得以任何方式或理由对本框架进行修改、使用和传播;
 * 2.禁止在本项目或任何子项目的基础上发展任何派生版本、修改版本或第三方版本;
 * 3.无论你对源代码做出任何修改和改进，版权都归Noark研发团队所有，我们保留所有权利;
 * 4.凡侵犯Noark版权等知识产权的，必依法追究其法律责任，特此郑重法律声明！
 */
package xyz.noark.core.util;

import java.util.HashMap;
import java.util.Map;

/**
 * Map类型的工具类.
 *
 * @since 3.1
 * @author 小流氓(176543888@qq.com)
 */
public class MapUtils {

	/**
	 * 检测集合是否为{@code null}或长度为0
	 *
	 * @param map 被检测集合
	 * @return 如果字符为{@code null}或长度为0则返回true,否则返回false.
	 */
	public static boolean isEmpty(final Map<?, ?> map) {
		return map == null || map.isEmpty();
	}

	/**
	 * 检测集合是否不为{@code null}或长度大于0
	 *
	 * @param map 被检测集合
	 * @return 如果字符不为{@code null}或长度大于0则返回true,否则返回false.
	 */
	public static boolean isNotEmpty(final Map<?, ?> map) {
		return !isEmpty(map);
	}

	/**
	 * 使用Key与Value直接构建一个HashMap.
	 * 
	 * @param <K> 键的类型
	 * @param <V> 值的类型
	 * @param key 键
	 * @param value 值
	 * @return HashMap对象
	 */
	public static <K, V> HashMap<K, V> of(K key, V value) {
		HashMap<K, V> result = new HashMap<>(1, 1);
		result.put(key, value);
		return result;
	}

	/**
	 * 从map中获取指定的Key，如果Key对应的值不存在，那就获取最大的Key所对应的值.
	 * <p>
	 * 常用于策划配置次数，比如获取20次所对应的资源，如果只配置到15，那就使用15对应的资源<br>
	 * 1=10<br>
	 * 2=15<br>
	 * 3=20<br>
	 * 4=25<br>
	 * 5=30<br>
	 * 10=100<br>
	 * 如此配置，如果参数为6，应该返回是30，如果参数是12，应该返回100
	 * 
	 * @param <V> 值的类型
	 * @param map 配置Map
	 * @param key 指定Key
	 * @return 只要配置了值肯定会返回一个最近的值...
	 */
	public static <V> V getOrMaxKey(final Map<Integer, V> map, Integer key) {
		V v = map.get(key);
		if (v == null) {
			// 遍历查找最优解...
			int max = Integer.MIN_VALUE;
			for (Map.Entry<Integer, V> e : map.entrySet()) {
				if (v == null) {
					v = e.getValue();
					max = e.getKey().intValue();
				} else if (e.getKey().intValue() > max && e.getKey().intValue() <= key.intValue()) {
					v = e.getValue();
					max = e.getKey().intValue();
				}
			}
		}
		return v;
	}

	/**
	 * 将第二个Map加到第一个Map上.
	 * <p>
	 * 建议使用{@link MapUtils#addByIntValue(Map, Map)}
	 * 
	 * @param <K> 键的类型
	 * @param source 存结果的Map对象.
	 * @param value 要加的Map对象
	 */
	@Deprecated
	public static <K> void add(Map<K, Integer> source, Map<K, Integer> value) {
		MapUtils.addByIntValue(source, value);
	}

	/**
	 * 将第二个Map加到第一个Map上.
	 * <p>
	 * 如果K为对象注意要重写HashCode与Equips方法
	 * 
	 * @param <K> 键的类型
	 * @param source 存结果的Map对象.
	 * @param value 要加的Map对象，此对象可以为空或null
	 */
	public static <K> void addByIntValue(Map<K, Integer> source, Map<K, Integer> value) {
		if (MapUtils.isNotEmpty(value)) {
			value.forEach((k, v) -> source.merge(k, v, (v1, v2) -> v1 + v2));
		}
	}

	/**
	 * 将第二个Map加到第一个Map上.
	 * <p>
	 * 如果K为对象注意要重写HashCode与Equips方法
	 * 
	 * @param <K> 键的类型
	 * @param source 存结果的Map对象.
	 * @param value 要加的Map对象，此对象可以为空或null
	 */
	public static <K> void addByLongValue(Map<K, Long> source, Map<K, Long> value) {
		if (MapUtils.isNotEmpty(value)) {
			value.forEach((k, v) -> source.merge(k, v, (v1, v2) -> v1 + v2));
		}
	}
}