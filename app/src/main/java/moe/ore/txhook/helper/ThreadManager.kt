package moe.ore.txhook.helper

import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.RejectedExecutionHandler
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * @author luoluo
 * @date 2020/10/1 15:24
 */
class ThreadManager private constructor(val uin: Long = 0) {

    /**
     * 通过调度线程周期性的执行缓冲队列中任务
     */
    private val taskHandler: ScheduledFuture<*>
    private val linkedBlockingQueue = LinkedBlockingQueue<Runnable>()

    /**
     * 线程池超出界线时将任务加入缓冲队列
     */
    private val handler =
        RejectedExecutionHandler { task: Runnable, _: ThreadPoolExecutor? ->
            linkedBlockingQueue.offer(
                task
            )
        }
    private val threadPool: ThreadPoolExecutor = ThreadPoolExecutor(
        corePoolSize,
        (maxCachePoolSize + corePoolSize),
        // 线程池最大线程数
        keepAliveTime,
        TimeUnit.MILLISECONDS,
        linkedBlockingQueue,
        handler
    )

    /**
     * 消息队列检查方法
     */
    fun hasMoreAcquire(): Boolean {
        return !linkedBlockingQueue.isEmpty()
    }

    val isTaskEnd: Boolean
        get() = threadPool.activeCount == 0

    /**
     * 向线程池中添加任务方法
     */
    fun addTask(task: Runnable): Runnable {
        // if (task is Thread) task.isDaemon = true
        threadPool.execute(task)
        return task
    }

    /**
     * 向线程池中添加任务方法
     */
    fun <T> addTaskWithResult(task: ResultThread<T>, default: T): T {
        val queue: BlockingQueue<T> = ArrayBlockingQueue(1)
        task.setQueue(queue)
        threadPool.execute(task)
        return try {
            queue.take()
        } catch (e: InterruptedException) {
            e.printStackTrace()
            default
        }
    }

    /**
     * 向线程池中添加任务方法
     */
    fun <T> addTaskWithResult(task: Callable<T>): Future<T> {
        return threadPool.submit(task)
    }

    val threadCount: Int
        get() = threadPool.activeCount

    /**
     * 请确保放进线程池的线程做好了错误捕捉，否则将无法关闭线程池导致出现心跳资源浪费
     */
    fun shutdown() {
        linkedBlockingQueue.clear()
        threadPool.shutdown() // 使新任务无法提交.
        try {
            // 等待未完成任务结束
            if (!threadPool.awaitTermination(1000 * 3, TimeUnit.MILLISECONDS)) {
                threadPool.shutdownNow() // 取消当前执行的任务
                // 等待任务取消的响应
            }
        } catch (ie: InterruptedException) {
            // 重新取消当前线程进行中断
            threadPool.shutdownNow()
            // 保留中断状态
            Thread.currentThread().interrupt()
        }
        THREAD_MAP.remove(uin)
    }

    companion object {
        @JvmStatic
        private val THREAD_MAP = hashMapOf<Long, ThreadManager>()

        /**
         * 核心线程数
         */
        @JvmStatic
        private val corePoolSize = DebugUtil.getMixThreadPoolSize() + 10

        /**
         * 线程池缓存队列最大数
         */
        @JvmStatic
        private val maxCachePoolSize = corePoolSize * 2

        /**
         * 空闲线程最大存活时间（毫秒）
         */
        private const val keepAliveTime = 3 * 1000L

        /**
         * 创建一个Bot专属的线程池
         *
         * @param uin Bot
         */
        @JvmStatic
        fun getInstance(uin: Long): ThreadManager {
            return THREAD_MAP.getOrPut(uin) { ThreadManager(uin) }
        }

        @JvmStatic
        operator fun get(uin: Long): ThreadManager {
            return getInstance(uin)
        }
    }

    init {
        threadPool.setKeepAliveTime(keepAliveTime, TimeUnit.MILLISECONDS)
        threadPool.allowCoreThreadTimeOut(true)
        /**
         * 将缓冲队列中的任务重新加载到线程池
         */

        /**
         * 创建一个调度线程池
         */
        val scheduler = Executors.newScheduledThreadPool(1)
        taskHandler = scheduler.scheduleAtFixedRate(
            object : Thread("accessBufferThread") {
                init {
                    isDaemon = true
                }

                override fun run() {
                    if (hasMoreAcquire()) {
                        threadPool.execute(linkedBlockingQueue.poll())
                    }
                }
            }, 0,
            1,
            //任务调度周期
            TimeUnit.NANOSECONDS
        )
    }
}