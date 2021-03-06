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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.RandomAccess;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

/**
 * IntList接口的大小可变数组的实现。
 * <p>
 * 实现了所有可选列表操作。除了实现 IntList 接口外，此类还提供一些方法来操作内部用来存储列表的数组的大小。<br>
 * （此类大致上等同于 Java的List&lt;Integer&gt;类）
 * 
 * @since 3.0
 * @author 小流氓(176543888@qq.com)
 */
public class IntArrayList implements IntList, RandomAccess {
	private static final int DEFAULT_CAPACITY = 10;
	/** 共享的空数组 */
	private static final int[] EMPTY_ELEMENT_DATA = {};

	private int[] elementData;
	/** 列表中元素的个数 */
	private int size = 0;

	/**
	 * 构建一个空的列表.
	 */
	public IntArrayList() {
		this.elementData = EMPTY_ELEMENT_DATA;
	}

	/**
	 * 指定容量的方式构建一个IntList.
	 * 
	 * @param initialCapacity 初始容量
	 */
	public IntArrayList(int initialCapacity) {
		if (initialCapacity > 0) {
			this.elementData = new int[initialCapacity];
		} else if (initialCapacity == 0) {
			this.elementData = EMPTY_ELEMENT_DATA;
		} else {
			throw new IllegalArgumentException("Illegal Capacity: " + initialCapacity);
		}
	}

	/**
	 * 直接传入一个数组进行初始化.
	 * 
	 * @param array 一个数组
	 */
	public IntArrayList(int[] array) {
		this.elementData = array;
		this.size = elementData.length;
	}

	/**
	 * 将此IntArrayList实例的容量调整为列表的当前大小。
	 * <p>
	 * 应用程序可以使用此操作来最小化 IntArrayList实例的存储量。
	 */
	public void trimToSize() {
		if (size < elementData.length) {
			elementData = (size == 0) ? EMPTY_ELEMENT_DATA : Arrays.copyOf(elementData, size);
		}
	}

	@Override
	public int random() {
		return elementData[ThreadLocalRandom.current().nextInt(0, size)];
	}

	@Override
	public int size() {
		return size;
	}

	@Override
	public boolean isEmpty() {
		return size == 0;
	}

	@Override
	public boolean contains(int o) {
		return indexOf(o) >= 0;
	}

	@Override
	public int indexOf(int o) {
		for (int i = 0; i < size; i++) {
			if (o == elementData[i]) {
				return i;
			}
		}
		return -1;
	}

	@Override
	public int lastIndexOf(int x) {
		for (int i = size - 1; i >= 0; i--) {
			if (x == elementData[i]) {
				return i;
			}
		}
		return -1;
	}

	@Override
	public int[] toArray() {
		return Arrays.copyOf(elementData, size);
	}

	@Override
	public int get(int index) {
		rangeCheck(index);
		return elementData[index];
	}

	@Override
	public boolean add(int e) {
		ensureCapacityInternal(size + 1);
		elementData[size++] = e;
		return true;
	}

	@Override
	public boolean remove(int o) {
		for (int index = 0; index < size; index++) {
			if (o == elementData[index]) {
				fastRemove(index);
				return true;
			}
		}
		return false;
	}

	/**
	 * Private remove method that skips bounds checking and does not return the
	 * value removed.
	 */
	private void fastRemove(int index) {
		int numMoved = size - index - 1;
		if (numMoved > 0) {
			System.arraycopy(elementData, index + 1, elementData, index, numMoved);
		}
		elementData[--size] = 0;
	}

	@Override
	public void clear() {
		size = 0;
	}

	/**
	 * Appends all of the elements in the specified collection to the end of
	 * this list, in the order that they are returned by the specified
	 * collection's Iterator. The behavior of this operation is undefined if the
	 * specified collection is modified while the operation is in progress.
	 * (This implies that the behavior of this call is undefined if the
	 * specified collection is this list, and this list is nonempty.)
	 *
	 * @param a collection containing elements to be added to this list
	 * @return <tt>true</tt> if this list changed as a result of the call
	 * @throws NullPointerException if the specified collection is null
	 */
	public boolean addAll(int[] a) {
		int numNew = a.length;
		ensureCapacityInternal(size + numNew);
		System.arraycopy(a, 0, elementData, size, numNew);
		size += numNew;
		return numNew != 0;
	}

	/**
	 * Checks if the given index is in range. If not, throws an appropriate
	 * runtime exception. This method does *not* check if the index is negative:
	 * It is always used immediately prior to an array access, which throws an
	 * ArrayIndexOutOfBoundsException if index is negative.
	 */
	private void rangeCheck(int index) {
		if (index >= size) {
			throw new IndexOutOfBoundsException(outOfBoundsMsg(index));
		}
	}

	/**
	 * Constructs an IndexOutOfBoundsException detail message. Of the many
	 * possible refactorings of the error handling code, this "outlining"
	 * performs best with both server and client VMs.
	 */
	private String outOfBoundsMsg(int index) {
		return "Index: " + index + ", Size: " + size;
	}

	private void ensureCapacityInternal(int minCapacity) {
		if (elementData == EMPTY_ELEMENT_DATA) {
			minCapacity = Math.max(DEFAULT_CAPACITY, minCapacity);
		}

		ensureExplicitCapacity(minCapacity);
	}

	private void ensureExplicitCapacity(int minCapacity) {
		// overflow-conscious code
		if (minCapacity - elementData.length > 0) {
			grow(minCapacity);
		}
	}

	/**
	 * The maximum size of array to allocate. Some VMs reserve some header words
	 * in an array. Attempts to allocate larger arrays may result in
	 * OutOfMemoryError: Requested array size exceeds VM limit
	 */
	private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;

	/**
	 * Increases the capacity to ensure that it can hold at least the number of
	 * elements specified by the minimum capacity argument.
	 *
	 * @param minCapacity the desired minimum capacity
	 */
	private void grow(int minCapacity) {
		// overflow-conscious code
		int oldCapacity = elementData.length;
		int newCapacity = oldCapacity + (oldCapacity >> 1);
		if (newCapacity - minCapacity < 0) {
			newCapacity = minCapacity;
		}
		if (newCapacity - MAX_ARRAY_SIZE > 0) {
			newCapacity = hugeCapacity(minCapacity);
		}
		// minCapacity is usually close to size, so this is a win:
		elementData = Arrays.copyOf(elementData, newCapacity);
	}

	private static int hugeCapacity(int minCapacity) {
		if (minCapacity < 0) {
			throw new OutOfMemoryError();
		}
		return (minCapacity > MAX_ARRAY_SIZE) ? Integer.MAX_VALUE : MAX_ARRAY_SIZE;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(elementData);
		result = prime * result + size;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		IntArrayList other = (IntArrayList) obj;
		if (!Arrays.equals(elementData, other.elementData)) {
			return false;
		}
		if (size != other.size) {
			return false;
		}
		return true;
	}

	public void forEach(Consumer<? super Integer> action) {
		Objects.requireNonNull(action);
		final int size = this.size;
		for (int i = 0; i < size; i++) {
			action.accept(elementData[i]);
		}
	}

	@Override
	public List<Integer> toList() {
		final List<Integer> result = new ArrayList<>(size);
		for (int i = 0; i < size; i++) {
			result.add(Integer.valueOf(elementData[i]));
		}
		return result;
	}
}