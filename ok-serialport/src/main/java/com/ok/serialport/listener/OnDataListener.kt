package com.ok.serialport.listener

/**
 * 全局数据监听，仅最后设置处生效
 * @author Leyi
 * @date 2025/1/10 16:12
 */
interface OnDataListener {
    /**
     * 数据请求监听
     * @param data ByteArray
     */
    fun onRequest(data: ByteArray)

    /**
     * 数据响应监听
     * @param data ByteArray
     */
    fun onResponse(data: ByteArray)
}
