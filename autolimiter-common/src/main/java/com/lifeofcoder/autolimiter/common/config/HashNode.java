package com.lifeofcoder.autolimiter.common.config;

import java.io.Serializable;

/**
 * 一致性Hash节点
 *
 * @author xbc
 * @date 2022/4/2
 */
public interface HashNode extends Serializable {
    String key();
}
