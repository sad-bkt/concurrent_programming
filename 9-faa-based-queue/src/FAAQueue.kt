package mpp.faaqueue

import kotlinx.atomicfu.AtomicRef
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.atomicArrayOfNulls

val BREAK = Any()

class FAAQueue<E> {
    private val head: AtomicRef<Segment> // Head pointer, similarly to the Michael-Scott queue (but the first node is _not_ sentinel)
    private val tail: AtomicRef<Segment> // Tail pointer, similarly to the Michael-Scott queue
    private val enqIdx = atomic(0L)
    private val deqIdx = atomic(0L)

    init {
        val firstNode = Segment()
        head = atomic(firstNode)
        tail = atomic(firstNode)
    }

    /**
     * Adds the specified element [element] to the queue.
     */
    fun enqueue(element: E) {
        while (true) {
            var curTail = tail.value
            val i = enqIdx.getAndIncrement()
            val s = findSegment(curTail, i)
            while (s.id > tail.value.id) {
                curTail = tail.value
                if(tail.compareAndSet(curTail, s)) {
                    break
                }
            }
            if (s.cas((i % SEGMENT_SIZE).toInt(), null, element)) {
                return
            }
        }
    }

    /**
     * Retrieves the first element from the queue and returns it;
     * returns `null` if the queue is empty.
     */
    fun dequeue(): E? {
        while (true) {
            if (deqIdx.value >= enqIdx.value) {
                return null
            }
            var curHead = head.value
            val i = deqIdx.getAndIncrement()

            val s = findSegment(curHead, i)
            while (s.id > head.value.id) {
                curHead = head.value
                if(head.compareAndSet(curHead, s)) {
                    break
                }
            }
            if (s.cas((i % SEGMENT_SIZE).toInt(), null, BREAK)) {
                continue
            }
            return s.get((i % SEGMENT_SIZE).toInt()) as E?
        }
    }

    private fun findSegment(s: Segment, i: Long): Segment {
        var start: Segment = s
        while (start.id < (i / SEGMENT_SIZE).toInt()) {
            var next = start.next.value
            if (next != null) {
                start = next
            }
            else {
                next = Segment()
                next.id = start.id + 1
                if(start.next.compareAndSet(null, next)) {
                    start = next
                }
            }
        }
        return start
    }


    /**
     * Returns `true` if this queue is empty, or `false` otherwise.
     */
    val isEmpty: Boolean
        get() {
            if (deqIdx.value <= enqIdx.value) {
                return true
            }
            return false
        }
}

class Segment {
    val next: AtomicRef<Segment?> = atomic(null)
    val elements = atomicArrayOfNulls<Any>(SEGMENT_SIZE)
    var id: Int = 0
    fun get(i: Int) = elements[i].value
    fun cas(i: Int, expect: Any?, update: Any?) = elements[i].compareAndSet(expect, update)
    fun put(i: Int, value: Any?) {
        elements[i].value = value
    }
}

const val SEGMENT_SIZE = 2 // DO NOT CHANGE, IMPORTANT FOR TESTS

