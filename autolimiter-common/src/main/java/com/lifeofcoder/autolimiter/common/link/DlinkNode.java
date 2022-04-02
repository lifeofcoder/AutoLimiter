package com.lifeofcoder.autolimiter.common.link;

import com.lifeofcoder.autolimiter.common.utils.ValidatorHelper;

/**
 * 双向链表节点
 *
 * @author xbc
 * @date 2022/4/2
 */
public class DlinkNode<T> {
    protected DlinkNode<T> pre;
    protected DlinkNode<T> next;
    private T value;

    private DlinkNode() {
    }

    public DlinkNode(T value) {
        this.value = value;
    }

    public void addNext(DlinkNode<T> node) {
        ValidatorHelper.requireNonNull(node);
        node.remove();
        DlinkNode<T> oldNext = this.next;
        this.next = node;
        node.next = oldNext;
        node.pre = this;
        if (oldNext != null) {
            oldNext.pre = node;
        }

    }

    public void addPrevious(DlinkNode<T> node) {
        ValidatorHelper.requireNonNull(node);
        node.remove();
        DlinkNode<T> oldPre = this.pre;
        this.pre = node;
        node.next = this;
        node.pre = oldPre;
        if (oldPre != null) {
            oldPre.next = node;
        }

    }

    public void remove() {
        if (this.pre != null) {
            this.pre.next = this.next;
        }

        if (this.next != null) {
            this.next.pre = this.pre;
        }

        this.next = null;
        this.pre = null;
    }

    public T value() {
        return this.value;
    }

    public DlinkNode<T> next() {
        return this.next;
    }

    public DlinkNode<T> pre() {
        return this.pre;
    }

    public static <S> DlinkNode<S> empty() {
        return new DlinkNode();
    }
}