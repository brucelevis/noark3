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
package xyz.noark.game;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import xyz.noark.core.env.EnvConfigHolder;
import xyz.noark.core.exception.ServerBootstrapException;
import xyz.noark.core.util.StringUtils;
import xyz.noark.game.crypto.StringEncryptor;

/**
 * 属性文件加载器.
 *
 * @since 3.0
 * @author 小流氓(176543888@qq.com)
 */
class NoarkPropertiesLoader {
	private static final String DEFAULT_PROPERTIES = "application.properties";
	private static final String TEST_PROPERTIES = "application-test.properties";
	private static final String PROFILE_PREFIX = "application-";
	private static final String PROFILE_SUFFIX = ".properties";

	/**
	 * 加载系统配置文件中的内容.
	 * <p>
	 * application-test.properties中的内容会覆盖application.properties中的配置
	 * 
	 * @param profile profile
	 * @return 返回配置内容
	 */
	Map<String, String> loadProperties(String profile) {
		final ClassLoader loader = Noark.class.getClassLoader();
		HashMap<String, String> result = new HashMap<>(256, 1);

		loadProperties(loader, DEFAULT_PROPERTIES, result);

		// 加载指定的Profile
		if (StringUtils.isNotEmpty(profile)) {
			loadProperties(loader, PROFILE_PREFIX + profile + PROFILE_SUFFIX, result);
		}
		// 没有配置的情况，要加载那个Test配置
		else {
			loadProperties(loader, TEST_PROPERTIES, result);
		}

		// 密文解密
		final StringEncryptor encryptor = new StringEncryptor(result);
		for (Map.Entry<String, String> e : result.entrySet()) {
			e.setValue(encryptor.decrypt(e.getValue()));
		}

		// 表达式引用...
		for (Map.Entry<String, String> e : result.entrySet()) {
			e.setValue(EnvConfigHolder.fillExpression(e.getValue(), result, true));
		}

		// 系统配置
		result.put(NoarkConstant.NOARK_VERSION, Noark.getVersion());

		return result;
	}

	private void loadProperties(ClassLoader loader, String filename, HashMap<String, String> result) {
		try (InputStream in = loader.getResourceAsStream(filename)) {
			if (in != null) {
				try (InputStreamReader isr = new InputStreamReader(in, "utf-8")) {
					Properties props = new Properties();
					props.load(isr);
					for (Entry<Object, Object> e : props.entrySet()) {
						String key = e.getKey().toString();
						String value = e.getValue().toString();
						if (result.put(key, value) != null) {
							System.err.println("覆盖配置 >>" + key + "=" + value);
						}
					}
				}
			}
		} catch (IOException e) {
			throw new ServerBootstrapException("配置文件格式异常... filename=" + filename);
		}
	}
}