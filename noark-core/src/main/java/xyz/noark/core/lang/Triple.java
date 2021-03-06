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
package xyz.noark.core.lang;

/**
 * 由三个元素组成的一个抽象对象.
 * <p>
 * 
 * @param <L> 左边元素的类型
 * @param <M> 中间元素的类型
 * @param <R> 右边元素的类型
 * @since 3.1
 * @author 小流氓(176543888@qq.com)
 */
public interface Triple<L, M, R> {

	/**
	 * 根据参数类型自动推断出一个不可变的抽象对象.
	 * <p>
	 * 具体实现可参考{@link ImmutableTriple#of(Object, Object, Object)}
	 * 
	 * @param <L> 左边元素的类型
	 * @param <M> 中间元素的类型
	 * @param <R> 右边元素的类型
	 * @param left 左边元素
	 * @param middle 中间元素
	 * @param right 右边元素
	 * @return 一个不可变的抽象对象
	 */
	public static <L, M, R> AbstractTriple<L, M, R> of(final L left, final M middle, final R right) {
		return ImmutableTriple.of(left, middle, right);
	}

	/**
	 * 获取左边的那个元素.
	 *
	 * @return 左边的那个元素
	 */
	public L getLeft();

	/**
	 * 获取中间的那个元素.
	 *
	 * @return 中间的那个元素
	 */
	public M getMiddle();

	/**
	 * 获取右边的那个元素.
	 *
	 * @return 右边的那个元素
	 */
	public R getRight();
}