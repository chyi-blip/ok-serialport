package com.ok.serialport.data

import com.ok.serialport.listener.OnResponseListener
import com.ok.serialport.utils.ByteUtils

/**
 * 请求体
 * @see OnResponseListener 回调必须添加对应的[ResponseRule]
 * @author Leyi
 * @date 2025/1/10 16:35
 */
open class Request(var data: ByteArray) : ResponseProcess() {
    var tag: String = ""

    /**
     * 发送时间
     */
    internal var sendTime: Long = 0

    /**
     * 超时时间，默认5s
     */
    internal var timeout: Long = 5000L

    /**
     * 超时重试次数，默认不重试
     */
    internal var timeoutRetry: Int = 0

    /**
     * 请求数据
     *
     * @param data
     * @return
     */
    open fun data(data: ByteArray): Request {
        this.data = data
        return this
    }

    /**
     * 请求标记
     *
     * @param tag
     * @return
     */
    open fun tag(tag: String): Request {
        this.tag = tag
        return this
    }

    /**
     * 设置响应超时时间
     *
     * @param timeout
     * @return
     */
    open fun timeout(timeout: Long): Request {
        this.timeout = timeout
        return this
    }

    /**
     * 设置响应超时重试次数
     *
     * @param count
     * @return
     */
    open fun timeoutRetry(count: Int): Request {
        this.timeoutRetry = count
        return this
    }

    /**
     * 添加 MatchRule
     *
     * @param responseRule
     * @return
     */
    override fun addResponseRule(responseRule: ResponseRule): Request {
        super.addResponseRule(responseRule)
        return this
    }

    /**
     * 设置响应监听 OnResponseListener
     *
     * @param listener
     * @return
     */
    override fun onResponseListener(listener: OnResponseListener): Request {
        super.onResponseListener(listener)
        return this
    }

    /**
     * 设置响应次数
     *
     * @param count
     * @return
     */
    override fun responseCount(count: Int): Request {
        super.responseCount(count)
        return this
    }

    /**
     * 无限响应
     *
     * @return
     */
    override fun infiniteResponse(): Request {
        super.infiniteResponse()
        return this
    }

    /**
     * 超时重试次数
     *
     * @return 是否消耗完毕
     */
    internal fun deductTimeoutRetryCount(): Boolean {
        if (timeoutRetry <= 0) {
            timeoutRetry = 0
            return true
        }
        timeoutRetry -= 1
        return false
    }

    /**
     * 数据转Hex
     * @return String
     */
    open fun toHex(): String {
        return ByteUtils.byteArrToHexStr(data)
    }
}