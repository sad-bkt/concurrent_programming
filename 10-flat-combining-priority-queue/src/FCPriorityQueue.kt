import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.atomicArrayOfNulls
import java.util.*

class FCPriorityQueue<E : Comparable<E>> {
    private val q = PriorityQueue<E>()
    private var fcArray = FCArray<E>(20)

    class FCArray<E>(val size: Int) {
        private val requests = atomicArrayOfNulls<Request<E>>(size)
        private val locked = atomic(false)
        private val random = Random()
        fun tryLock(): Boolean {
            return locked.compareAndSet(false, true)
        }

        fun unlock() {
            locked.value = false
        }

        fun add(request: Request<E>) {
            var i = random.nextInt(size)
            while (!requests[i].compareAndSet(null, request)) {
                i = (i + 1) % size
            }
        }

        fun get(i: Int): Request<E>? {
            return requests[i].value
        }

        fun put(i: Int, value: Request<E>?) {
            requests[i].value = value
        }
    }

    enum class OperationType {
        POLL,
        PEEK,
        ADD
    }

    class Request<E>(val type: OperationType, val element: E?) {
        var active = true
        var result: E? = null
    }


    /**
     * Retrieves the element with the highest priority
     * and returns it as the result of this function;
     * returns `null` if the queue is empty.
     */
    fun poll(): E? {
        return getResponse(Request(OperationType.POLL, null))
    }

    /**
     * Returns the element with the highest priority
     * or `null` if the queue is empty.
     */
    fun peek(): E? {
        return getResponse(Request(OperationType.PEEK, null))
    }

    /**
     * Adds the specified element to the queue.
     */
    fun add(element: E) {
        getResponse(Request(OperationType.ADD, element))
    }

    private fun getResponse(request: Request<E>): E? {
        fcArray.add(request)
        while (true) {
            if (fcArray.tryLock()) {
                for (i in 0 until fcArray.size) {
                    val curRequest = fcArray.get(i) ?: continue
                    // curRequest.doOperation()
                    if (curRequest.active) {
                        curRequest.result = when (curRequest.type) {
                            OperationType.POLL -> q.poll()
                            OperationType.PEEK -> q.peek()
                            OperationType.ADD -> q.add(curRequest.element) as E // костыль, конечно
                        }
                    }
                    fcArray.put(i, null)
                }
                fcArray.unlock()
                return request.result
            } else if (!request.active) {
                return request.result
            }
        }
    }
}