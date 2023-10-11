import java.util.concurrent.atomic.*

class Solution(private val env: Environment) : Lock<Solution.Node> {
    private val tail = AtomicReference<Node?>(null)

    override fun lock(): Node {
        val my = Node() // сделали узел
        my.locked.compareAndSet(false, true)
        val pred = tail.getAndSet(my)
        if (pred != null) {
            pred.next.compareAndSet(null, my)
            while (my.locked.value)
                env.park()
        }
        return my // вернули узел
    }

    override fun unlock(node: Node) {
        if (node.next.value == null) {
            if (tail.compareAndSet(node, null))
                return
            else {
                while(node.next.value == null) {
                    continue
                }
            }
        }
        node.next.value!!.locked.compareAndSet(true, false)
        env.unpark(node.next.value!!.thread)
    }

    class Node {
        val thread = Thread.currentThread() // запоминаем поток, которые создал узел
        val next = AtomicReference<Node?>(null)
        val locked = AtomicReference<Boolean>(false)
    }
}