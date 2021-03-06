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
package xyz.noark.game.bootstrap;

import static xyz.noark.log.LogHelper.logger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import xyz.noark.core.ModularManager;
import xyz.noark.core.env.EnvConfigHolder;
import xyz.noark.core.exception.ServerBootstrapException;
import xyz.noark.core.ioc.NoarkIoc;
import xyz.noark.core.network.PacketCodec;
import xyz.noark.core.network.PacketCodecHolder;
import xyz.noark.core.thread.NamedThreadFactory;
import xyz.noark.core.util.FileUtils;
import xyz.noark.core.util.StringUtils;
import xyz.noark.core.util.SystemUtils;
import xyz.noark.game.NoarkConstant;
import xyz.noark.log.LogManager;

/**
 * 抽象的启动服务类.
 * <p>
 * 自动初始化IOC容器，所以需要所有模块都在启动类的子目录下.<br>
 * <b>注意：此类的实现类位置很重要...</b>
 * 
 * <pre>
 * this.ioc = new NoarkIoc(this.getClass().getPackage().getName());
 * </pre>
 *
 * @since 3.0
 * @author 小流氓(176543888@qq.com)
 */
public abstract class AbstractServerBootstrap implements ServerBootstrap {
	/** IOC容器 */
	protected NoarkIoc ioc;
	protected ModularManager modularManager;
	/** PID文件名称 */
	private String pidFileName;

	/** 启动服务时，添加一个停机守护线程，用于清理异常情况. */
	public AbstractServerBootstrap() {
		Runtime.getRuntime().addShutdownHook(new ShutdownHook(this));
	}

	/**
	 * 返回当前服务器名称.
	 * 
	 * @return 服务器名称
	 */
	protected abstract String getServerName();

	@Override
	public void start() {
		logger.info("starting {} service...", this.getServerName());
		long startTime = System.nanoTime();
		try {
			// 启动IOC容器
			this.ioc = new NoarkIoc(this.getClass().getPackage().getName());
			this.modularManager = ioc.get(ModularManager.class);

			// 服务器启动之前的逻辑...
			this.onBeginStart();

			// 启动逻辑
			this.onStart();

			float interval = (System.nanoTime() - startTime) / 1000_000f;
			logger.info("{} is running, interval={} ms", this.getServerName(), interval);
			System.out.println(this.getServerName() + " is running, interval=" + interval + " ms");

			if (this.showBanner()) {
				FileUtils.getFileText(bannerFileName()).ifPresent(v -> printBanner(v));
			}
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("failed to starting service:{}", this.getServerName(), e);
			System.exit(1);
		}
	}

	protected void onBeginStart() {
		PacketCodecHolder.setPacketCodec(getPacketCodec());

		// 如果开启动了Debug模式且为Window那就要添加一个回车停服功能
		if (SystemUtils.IS_OS_WINDOWS) {
			ExecutorService singleThreadPool = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(1), new NamedThreadFactory("安全停服：测试启用"));
			singleThreadPool.execute(() -> {
				try {
					System.in.read();
				} catch (IOException e) {
					logger.error("{}", e);
				}
				System.exit(0);
			});
		}

		// 写入PID文件....
		this.pidFileName = EnvConfigHolder.getProperties().get(NoarkConstant.PID_FILE);
		this.createPidFile();
	}

	/**
	 * 尝试写入PID到文件
	 */
	protected void createPidFile() {
		if (StringUtils.isNotEmpty(pidFileName)) {
			try {
				File pidFile = new File(pidFileName);
				if (FileUtils.createNewFile(pidFile)) {
					logger.debug("PID文件创建成功.");
				}
				// PID文件已存在...
				else {
					final String fileName = pidFileName;
					this.pidFileName = null;
					throw new ServerBootstrapException("PID文件已存在，如果异常停服，请手动删除PID文件 >> " + fileName);
				}

				// 写入PID
				try (FileWriter fileWriter = new FileWriter(pidFile, false)) {
					fileWriter.write(SystemUtils.getPidStr());
					fileWriter.flush();
				}
			} catch (IOException e) {
				throw new ServerBootstrapException("PID文件创建失败，请确认一下权限是否正常 >> " + pidFileName, e);
			}
		}
	}

	/**
	 * 自定义封包结构需要重写当前方法.
	 * 
	 * @return 封包的编解码
	 */
	protected abstract PacketCodec getPacketCodec();

	/**
	 * 启动逻辑.
	 */
	protected abstract void onStart();

	@Override
	public void stop() {
		logger.info("stopping service: {}", this.getServerName());
		try {
			logger.info("goodbye {}", this.getServerName());
			System.out.println("goodbye " + this.getServerName());

			this.onStop();
		} catch (Exception e) {
			logger.error("failed to stopping service:{}", this.getServerName(), e);
		} finally {
			// IOC容器销毁
			if (ioc != null) {
				ioc.destroy();
			}
			// 日志框架Shutdown
			LogManager.shutdown();

			// 删除PID文件
			this.deletePidFile();
		}
	}

	/**
	 * 停服时尝试删除PID文件
	 */
	protected void deletePidFile() {
		if (StringUtils.isNotEmpty(pidFileName)) {
			File pidFile = new File(pidFileName);
			if (pidFile.exists()) {
				pidFile.delete();
			}
		}
	}

	/**
	 * 关闭逻辑.
	 */
	protected abstract void onStop();

	/**
	 * 显示Banner.
	 * 
	 * @return 如果显示返回true，否则返回false.
	 */
	protected boolean showBanner() {
		return true;
	}

	/**
	 * Banner文件名称.
	 * <p>
	 * 重载此方法可以替换默认的输出Banner图案
	 * 
	 * @return Banner文件名称
	 */
	protected String bannerFileName() {
		return NoarkConstant.BANNER_DEFAULT;
	}

	/**
	 * 打印Banner图案.
	 * 
	 * @param text Banner图案
	 */
	protected void printBanner(String text) {
		logger.info(EnvConfigHolder.fillExpression(text));
	}
}