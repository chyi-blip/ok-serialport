package com.ok.serialport.data

import com.ok.serialport.listener.OnResponseListener

/**
 * 响应处理
 * @author Leyi
 * @date 2025/1/10 16:35
 */
abstract class ResponseProcess {
    companion object {
        /**
         * 最大响应次数：无限次响应
         */
        const val MAX_COUNT = 10000
    }

    internal var responseRules = mutableListOf<ResponseRule>()

    internal var onResponseListener: OnResponseListener? = null

    private var count: Int = 1

    /**
     * 添加 MatchRule
     *
     * @param responseRule
     * @return
     */
    open fun addResponseRule(responseRule: ResponseRule): ResponseProcess {
        responseRules.add(responseRule)
        return this
    }

    /**
     * 是否存在响应规则
     *
     * @return
     */
    internal fun isResponseRule(): Boolean {
        return responseRules.isNotEmpty()
    }

    /**
     * 设置响应监听 OnResponseListener
     *
     * @param listener
     * @return
     */
    open fun onResponseListener(listener: OnResponseListener): ResponseProcess {
        this.onResponseListener = listener
        return this
    }

    /**
     * 设置响应次数
     *
     * @param count
     * @return
     */
    open fun responseCount(count: Int): ResponseProcess {
        this.count = count
        return this
    }

    /**
     * 无限响应
     *
     * @return
     */
    open fun infiniteResponse(): ResponseProcess {
        this.count = MAX_COUNT
        return this
    }

    internal fun match(request: Request?, receive: ByteArray): Boolean {
        try {
            responseRules.forEach {
                val isMatch = it.match(request, receive)
                if (!isMatch) {
                    return false
                }
            }
        } catch (e: Exception) {
            return false
        }
        return true
    }

    /**
     * 处理次数
     *
     * @return 是否消耗完毕
     */
    internal fun deductCount(): Boolean {
        if (count >= MAX_COUNT) {
            return false
        }
        count -= 1
        if (count <= 0) {
            count = 0
            return true
        }
        return false
    }
}