package mpp.counter

import kotlinx.atomicfu.AtomicIntArray
import kotlin.random.Random

class ShardedCounter {
    private val counters = AtomicIntArray(ARRAY_SIZE)

    /**
     * Atomically increments by one the current value of the counter.
     */
    fun inc() {
        val i = Random.nextInt(0, ARRAY_SIZE) // Random.nextInt() % ARRAY_SIZE может выдавать отрицательное значение
        counters[i] += 1 // // точка линеаризации
    }


    /**
     * Returns the current counter value.
     */

    fun get(): Int {
        var result = 0
        for (i in 0 until ARRAY_SIZE) {
            result += counters[i].value // точка линеаризации
        }
        return result
    }

    // контр-пример :
    // P   inc(2)
    // Q   get():1
    fun inc(delta: Int) {
        val i = Random.nextInt(0, ARRAY_SIZE)
        // counters[i].addAndGet(delta) // линеаризуемо, операция атомарна
        for (j in 0 until delta) { // нелинеаризуемо
            counters[i] += 1
        }

    }
}

private const val ARRAY_SIZE = 2 // DO NOT CHANGE ME

/*
решение линеаризуемо <=> когда можно выбрать точки линеаризации, которые можно упорядочить отношением "произошло до"
56 слайд https://docs.google.com/presentation/d/1TDsV4pZT5lp-noCoNFHd_kQzbGIAJv4LdqqGW0IlrDw/edit#slide=id.p58

1) В inc точка линеаризации - изменение ячейки массива.

2) В get возвращение result-а не может быть точкой линеаризации, иначе может быть так:
P   inc() inc()
Q   get():1
1 inc поменяет ту часть массива, которую get уже прошел, а 2 inc - повлияет на get.
Тогда нам нужно, чтобы точкой линеаризации было само считывание ячейки массива в get.
Докажем дальше, что в этом случае мы сможем однозначно определить последовательность выполнения.

3) 2 вызова get не влияют друг на друга, как и 2 вызова inc или последовательное выполнение get и inc,
поэтому надо рассмотреть параллельное выполнение get и inc.
Утверждение: между каждым инкрементом будет точка линеаризации из get у другого потока, может быть 2 ситуации:
P inc()   inc()
  [   .] [   .]
Q get():1
  [ .    .     ]

или

P inc()   inc()
  [   .] [   .]
Q get():2
  [     .     .]
Дальше по индукции добавляем get и inc, и понимаем, что все хорошо.
 */