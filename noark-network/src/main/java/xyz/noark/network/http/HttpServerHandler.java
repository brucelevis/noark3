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
package xyz.noark.network.http;

import static xyz.noark.log.LogHelper.logger;

import java.io.IOException;
import java.lang.reflect.Parameter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import xyz.noark.core.converter.ConvertManager;
import xyz.noark.core.converter.Converter;
import xyz.noark.core.exception.ConvertException;
import xyz.noark.core.exception.UnrealizedException;
import xyz.noark.core.ioc.manager.HttpMethodManager;
import xyz.noark.core.ioc.wrap.method.HttpMethodWrapper;
import xyz.noark.core.ioc.wrap.param.HttpParamWrapper;
import xyz.noark.core.util.Md5Utils;
import xyz.noark.core.util.StringUtils;

/**
 * HTTP封包处理逻辑类.
 *
 * @since 3.0
 * @author 小流氓(176543888@qq.com)
 */
public class HttpServerHandler extends ChannelInboundHandlerAdapter {
	private static final Charset DEFAULT_CHARSET = Charset.forName("UTF-8");
	private static final String SIGN = "sign";// 签名Key...
	private static final String TIME = "time";// 时间戳Key...

	private final String secretKey;

	public HttpServerHandler(String secretKey) {
		this.secretKey = secretKey;
	}

	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) {
		ctx.flush();
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) {
		if (msg instanceof FullHttpRequest) {
			HttpResult result = this.exec((FullHttpRequest) msg);
			ByteBuf buf = Unpooled.wrappedBuffer(JSON.toJSONString(result).getBytes(DEFAULT_CHARSET));
			FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, buf);
			ctx.write(response).addListener(ChannelFutureListener.CLOSE);
		}
	}

	private HttpResult exec(FullHttpRequest fhr) {
		HttpMethodWrapper handler = HttpMethodManager.getInstance().getHttpHandler(fhr.uri());

		// API不存在...
		if (handler == null) {
			return new HttpResult(HttpErrorCode.NO_API, "client request's API Unrealized.");
		}

		// 已废弃
		if (handler.isDeprecated()) {
			return new HttpResult(HttpErrorCode.API_DEPRECATED, "client request's API Deprecated.");
		}

		// 解析参数...
		Map<String, String> parameters = Collections.emptyMap();
		try {
			final ByteBuf buf = fhr.content();
			byte[] bs = new byte[buf.readableBytes()];
			buf.readBytes(bs);
			parameters = JSON.parseObject(new String(bs), new TypeReference<Map<String, String>>() {});
		} catch (Exception e) {
			return new HttpResult(HttpErrorCode.PARAMETERS_INVALID, "client request's parameters not json.");
		}

		// 签名失败...
		if (!checkSign(parameters.getOrDefault(TIME, StringUtils.EMPTY), parameters.get(SIGN))) {
			return new HttpResult(HttpErrorCode.SIGN_FAILED, "client request's sign failed.");
		}

		// 参数解析...
		Object[] args = null;
		try {
			args = this.analysisParam(handler, fhr.uri(), parameters);
		} catch (Exception e) {
			return new HttpResult(HttpErrorCode.PARAMETERS_INVALID, "client request's parameters are invalid, " + e.getMessage());
		}

		// 逻辑执行...
		try {
			Object returnValue = null;
			if (args == null) {
				returnValue = handler.invoke();
			} else {
				returnValue = handler.invoke(args);
			}

			HttpResult result = new HttpResult(HttpErrorCode.OK);
			result.setData(returnValue);
			return result;
		} catch (Exception e) {
			return new HttpResult(HttpErrorCode.INTERNAL_ERROR, "server internal error, " + e.getMessage());
		}
	}

	public Object[] analysisParam(HttpMethodWrapper handler, String uri, Map<String, String> parameters) throws IOException {
		if (handler.getParameters().isEmpty()) {// 如果没有参数，返回null.
			return null;
		}

		List<Object> args = new ArrayList<>(handler.getParameters().size());
		for (HttpParamWrapper param : handler.getParameters()) {
			Converter<?> converter = this.getConverter(param.getParameter());
			String data = parameters.get(param.getName());

			if (param.getRequestParam().required() || data != null) {
				try {
					args.add(converter.convert(data));
				} catch (Exception e) {
					throw new ConvertException("HTTP request param error. uri=" + uri + "," + param.getName() + "=" + data + "-->" + converter.buildErrorMsg(), e);
				}
			} else {
				try {
					args.add(converter.convert(param.getRequestParam().defaultValue()));
				} catch (Exception e) {
					throw new ConvertException("HTTP request default param error. uri=" + uri + "," + param.getName() + "=" + param.getRequestParam().defaultValue() + "-->" + converter.buildErrorMsg(), e);
				}
			}
		}
		return args.toArray();
	}

	private Converter<?> getConverter(Parameter field) {
		Converter<?> result = ConvertManager.getInstance().getConverter(field.getType());
		if (result == null) {
			throw new UnrealizedException("未实现的注入(" + field.getType().getName() + ")" + field.getName());
		}
		return result;
	}

	/**
	 * 检测签名.
	 */
	private boolean checkSign(String time, String sign) {
		return Md5Utils.encrypt(new StringBuilder(time.length() + secretKey.length() + 1).append(secretKey).append("+").append(time).toString()).equalsIgnoreCase(sign);
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		logger.debug("HTTP异常. channel={}", ctx.channel(), cause);
		ctx.close();
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		super.channelActive(ctx);
		logger.debug("发现HTTP客户端链接，channel={}", ctx.channel());
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		super.channelInactive(ctx);
		logger.debug("HTTP客户端断开链接. channel={}", ctx.channel());
	}
}