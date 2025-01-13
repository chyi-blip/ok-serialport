package com.ok.serialport

import androidx.annotation.IntRange
import com.ok.serialport.jni.SerialPort
import com.ok.serialport.data.Request
import com.ok.serialport.data.ResponseProcess
import com.ok.serialport.jni.SerialPortClient
import com.ok.serialport.data.DataProcess
import com.ok.serialport.exception.ReconnectFailException
import com.ok.serialport.listener.OnConnectListener
import com.ok.serialport.listener.OnDataListener
import com.ok.serialport.stick.AbsStickPacketHandle
import com.ok.serialport.stick.BaseStickPacketHandle
import com.ok.serialport.utils.SerialLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 管理串口连接和请求处理
 * @author Leyi
 * @date 2024/10/31 11:47
 */
class OkSerialPort private constructor(
    // 串口地址
    private val devicePath: String,
    // 波特率
    private val baudRate: Int,
    // 标志位
    private val flags: Int,
    // 数据位
    private val dataBit: Int,
    // 停止位
    private val stopBit: Int,
    // 校验位：0 表示无校验位，1 表示奇校验，2 表示偶校验
    private val parity: Int,
    // 连接最大重试次数 需要大于0 =0 不重试
    private val retryCount: Int,
    // 连接重试间隔
    private val retryInterval: Long,
    // 发送间隔
    internal val sendInterval: Long,
    // 读取间隔
    internal val readInterval: Long,
    // 离线识别间隔
    private val offlineIntervalSecond: Int,
    // 日志
    internal val logger: SerialLogger,
    // 串口粘包处理
    internal val stickPacketHandle: AbsStickPacketHandle
) : SerialPort() {
    companion object {
        //超过最大限制为无限次重试，重试时间间隔增加
        private const val MAX_RETRY_COUNT = 100
    }

    internal val serialPortClient: SerialPortClient by lazy {
        SerialPortClient(devicePath, baudRate, flags, dataBit, stopBit, parity, logger)
    }
    private val dataProcess by lazy {
        DataProcess(this)
    }
    private val isConnected = AtomicBoolean(false)
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    //重连次数
    private var retryTimes = 0

    // 串口连接监听
    private var onConnectListener: OnConnectListener? = null

    // 串口全局数据监听
    internal var onDataListener: OnDataListener? = null

    /**
     * 串口连接
     */
    fun connect() {
        if (isConnect()) return
        try {
            serialPortClient.connect()
        } catch (e: Exception) {
            logger.log("串口(${devicePath}:${baudRate})连接失败：${e.message}")
            onConnectListener?.onDisconnect(devicePath, e)
            reconnect()
            return
        }
        setConnected(true)
        logger.log("串口(${devicePath}:${baudRate})连接成功")
        try {
            dataProcess.start(coroutineScope)
            onConnectListener?.onConnect(devicePath)
            retryTimes = 0
        } catch (e: Exception) {
            logger.log("读写线程启动失败：${e.message}")
            onConnectListener?.onDisconnect(devicePath, e)
            reconnect()
        }
    }

    private fun setConnected(value: Boolean) {
        isConnected.set(value)
    }

    fun isConnect(): Boolean = isConnected.get()

    /**
     * 发送数据
     * @param request Request
     */
    fun request(request: Request) {
        if (isConnect()) {
            dataProcess.addRequest(request)
        }
    }

    /**
     * 取消请求
     * @param request SerialRequest
     */
    fun cancel(request: Request): Boolean {
        return dataProcess.cancelRequest(request)
    }

    /**
     * 添加数据数据处理器
     */
    fun addProcess(process: ResponseProcess) {
        dataProcess.addResponseProcess(process)
    }

    /**
     * 移除数据处理
     * 针对无限次重试
     */
    fun removeProcess(process: ResponseProcess) {
        dataProcess.removeResponseProcess(process)
    }

    /**
     * 添加连接监听器
     */
    fun addConnectListener(onConnectListener: OnConnectListener) {
        this.onConnectListener = onConnectListener
    }

    /**
     * 移除连接监听器
     */
    fun removeConnectListener() {
        this.onConnectListener = null
    }

    /**
     * 添加数据监听器
     */
    fun addDataListener(onDataListener: OnDataListener) {
        this.onDataListener = onDataListener
    }

    /**
     * 移除数据监听器
     */
    fun removeDataListener() {
        this.onDataListener = null
    }

    /**
     * 重连
     */
    private fun reconnect() {
        coroutineScope.launch {
            delay(retryInterval)
            logger.log("开始重连，进度：${retryCount + 1} / $retryCount")
            connect()
            retryTimes++
            delay(100)
            if (retryTimes >= retryCount && !isConnect()) {
                onConnectListener?.onDisconnect(devicePath, ReconnectFailException("重连失败"))
            }
        }
    }

    /**
     * 断开串口连接
     */
    fun disconnect() {
        dataProcess.cancel()
        if (isConnect()) {
            serialPortClient.disconnect()
            setConnected(false)
        }
    }

    class Builder {
        // 串口地址
        private var devicePath: String? = null

        // 波特率
        private var baudRate: Int? = null

        // 标志位
        private var flags: Int = 0

        // 数据位
        private var dataBit: Int = 8

        // 停止位
        private var stopBit: Int = 1

        // 校验位：0 表示无校验位，1 表示奇校验，2 表示偶校验
        @IntRange(from = 0, to = 2)
        private var parity: Int = 0

        // 连接最大重试次数 需要大于0 =0 不重试
        private var maxRetry: Int = 3

        // 连接重试间隔
        private var retryInterval: Long = 1000L

        // 发送间隔
        var sendInterval: Long = 300L

        // 读取间隔
        private var readInterval: Long = 50L

        // 离线识别间隔
        private var offlineIntervalSecond: Int = 0

        // 日志
        private var logger: SerialLogger = SerialLogger()

        // 串口粘包处理
        private var stickPacketHandle: AbsStickPacketHandle = BaseStickPacketHandle()

        fun devicePath(devicePath: String) = apply {
            this.devicePath = devicePath
        }

        fun baudRate(baudRate: Int) = apply {
            this.baudRate = baudRate
        }

        fun flags(flags: Int) = apply {
            this.flags = flags
        }

        fun dataBit(dataBit: Int) = apply {
            this.dataBit = dataBit
        }

        fun stopBit(stopBit: Int) = apply {
            this.stopBit = stopBit
        }

        fun parity(parity: Int) = apply {
            this.parity = parity
        }

        fun maxRetry(maxRetry: Int) = apply {
            this.maxRetry = maxRetry
        }

        fun retryInterval(retryInterval: Long) = apply {
            this.retryInterval = retryInterval
        }

        fun sendInterval(sendInterval: Long) = apply {
            this.sendInterval = sendInterval
        }

        fun readInterval(readInterval: Long) = apply {
            this.readInterval = readInterval
        }

        fun offlineIntervalSecond(offlineIntervalSecond: Int) = apply {
            this.offlineIntervalSecond = offlineIntervalSecond
        }

        fun logger(logger: com.ok.serialport.utils.SerialLogger) = apply {
            this.logger = logger
        }

        fun stickPacketHandle(stickPacketHandle: AbsStickPacketHandle) = apply {
            this.stickPacketHandle = stickPacketHandle
        }

        fun build(): OkSerialPort {
            require(devicePath != null) { "串口地址devicePath不能为空" }
            require(baudRate != null && baudRate!! > 0) { "串口波特率baudRate不能为空或者小于0" }
            require(flags >= 0) { "标识位不能小于0" }
            require(dataBit >= 0) { "数据位不能小于0" }
            require(stopBit >= 0) { "停止位不能小于0" }
            require(parity >= 0) { "校验位不能小于0" }
            require(maxRetry >= 0) { "重试次数不能小于0" }
            require(retryInterval >= 500) { "重试时间间隔不能小于500毫秒" }
            require(sendInterval >= 100) { "发送数据时间间隔不能小于100毫秒" }
            require(readInterval >= 10) { "读取数据时间间隔不能小于10毫秒" }

            return OkSerialPort(
                devicePath!!, baudRate!!, flags, dataBit, stopBit, parity, maxRetry, retryInterval,
                sendInterval, readInterval, offlineIntervalSecond, logger, stickPacketHandle
            )
        }
    }
}

