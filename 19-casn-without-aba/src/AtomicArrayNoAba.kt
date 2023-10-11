import kotlinx.atomicfu.*

class AtomicArrayNoAba<E>(size: Int, initialValue: E) {
    private val array: Array<Ref<E>> = Array(size) { Ref(initialValue) }
    fun get(index: Int) = array[index].value!!
    fun set(index: Int, update: E) {
        array[index].value = update
    }

    fun cas(index: Int, expected: E, update: E): Boolean {
        return array[index].cas(expected, update)
    }

    private fun completingDescriptor(index1: Int, expected1: E, update1: E,
                                     index2: Int, expected2: E, update2: E): Boolean {
        val descriptor = Descriptor(array[index1], expected1, update1, array[index2], expected2, update2)
        if (array[index1].cas(expected1, descriptor)) {
            descriptor.complete()
            return descriptor.state.value == "SUCCESS"
        }
        return false
    }

    fun cas2(index1: Int, expected1: E, update1: E, index2: Int, expected2: E, update2: E): Boolean {
        if (index1 == index2) {
            return expected1 == expected2
                    && cas(index1, expected1, (expected1.toString().toInt() + 2) as E)
        }
        return if (index1 >= index2) {
            completingDescriptor(index2, expected2, update2, index1, expected1, update1)
        } else {
            completingDescriptor(index1, expected1, update1, index2, expected2, update2)
        }
    }
}

class Descriptor<E>(
    private val first: Ref<E>, private val expectFirst: E, private val updateFirst: E,
    private val second: Ref<E>, private val expectSecond: E, private val updateSecond: E
) {
    val state: Ref<String> = Ref("UNDECIDED")

    private fun changes(changeState: String, changeFirst: E, changeSecond: E) {
        state.v.compareAndSet("UNDECIDED", changeState)
        first.v.compareAndSet(this, changeFirst)
        second.v.compareAndSet(this, changeSecond)
    }

    fun complete() {
        val way = if (second.v.value != this) second.cas(expectSecond, this) else true
        if (way) {
            changes("SUCCESS", updateFirst, updateSecond)
        } else {
            changes("FAIL", expectFirst, expectSecond)
        }
    }
}

class Ref<E>(initial: E) {
    val v: AtomicRef<Any?> = atomic(initial)
    var value: E
        get() = v.loop {
            if (it !is Descriptor<*>)
                return it as E
            else
                it.complete()
        }
        set(update) {
            v.loop {
                if (it is Descriptor<*>) {
                    it.complete()
                } else if (v.compareAndSet(it, update)) {
                    return
                }
            }
        }

    fun cas(expect: Any?, update: Any?): Boolean {
        while (true) {
            if (v.compareAndSet(expect, update)) return true
            val current: Any? = v.value
            if (current is Descriptor<*>) current.complete()
            else if (current != expect) return false
        }
    }
}