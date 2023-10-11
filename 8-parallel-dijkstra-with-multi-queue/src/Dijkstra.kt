package dijkstra

import java.util.*
import java.util.concurrent.Phaser
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread

private val NODE_DISTANCE_COMPARATOR = Comparator<Node> { o1, o2 -> Integer.compare(o1!!.distance, o2!!.distance) }

class MultiQueue<E>(private val workers: Int, private val comparator: Comparator<E>) {
    private val queues = List(workers) { PriorityQueue(workers, comparator) }
//    private val locks = List(workers) { ReentrantLock() }
    private val random = Random()
    fun poll(): E? {
        var i1 = random.nextInt(workers)
        var i2 = random.nextInt(workers)
        while (i1 == i2) {
            i2 = random.nextInt(workers)
        }
        if (i1 > i2) {
            i1 = i2.also { i2 = i1 }
        }

        synchronized(queues[i1]) {
            val q1top = queues[i1].poll()
            if (q1top != null) {
                return q1top
            }
        }
        synchronized(queues[i2]) {
            val q2top = queues[i2].poll()
            if (q2top != null) {
                return q2top
            }
        }
        return null
    }

    fun add(e: E) {
        val i = random.nextInt(workers)
        synchronized(queues[i]) {
            queues[i].add(e)
        }
//        try {
//            locks[i].lock()
//            queues[i].add(e)
//        } finally {
//            locks[i].unlock()
//        }
    }
}

// Returns `Integer.MAX_VALUE` if a path has not been found.
fun shortestPathParallel(start: Node) {
    val workers = Runtime.getRuntime().availableProcessors()
    // The distance to the start node is `0`
    start.distance = 0
    // Create a priority (by distance) queue and add the start node into it
    val q = MultiQueue(workers, NODE_DISTANCE_COMPARATOR)
    q.add(start)
    val activeNodes = AtomicInteger(1)
    // Run worker threads and wait until the total work is done
    val onFinish = Phaser(workers + 1) // `arrive()` should be invoked at the end by each worker
    repeat(workers) {
        thread {
            while (activeNodes.get() > 0) {
                val u: Node = q.poll() ?: continue
                for (v in u.outgoingEdges) {
                    while (true) {
                        val d = u.distance + v.weight
                        val relaxed = v.to.distance
                        if (relaxed <= d) break
                        if (v.to.casDistance(relaxed, d)) {
                            activeNodes.incrementAndGet()
                            q.add(v.to)
                            break
                        }
                    }
                }
                activeNodes.decrementAndGet()
            }
            onFinish.arrive()
        }
    }
    onFinish.arriveAndAwaitAdvance()
}