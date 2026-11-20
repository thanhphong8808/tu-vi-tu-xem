package vn.tuvi.core.engine

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.random.Random

/**
 * Chỉ tiêu mục 3: lập lá số < 5 ms. Đây là phép đo trên JVM máy dev (đại diện);
 * đo thật trên Galaxy S22 Ultra thuộc cổng Giai đoạn 5.
 */
class PerformanceTest {
    @Test
    fun `lap la so trung binh duoi 5 ms`() {
        val r = Random(7)
        val inputs = List(3_000) { RandomBirths.input(r) }
        inputs.take(500).forEach { ChartEngine.build(it) } // khởi động JIT, nạp brightness.json
        val start = System.nanoTime()
        inputs.forEach { ChartEngine.build(it) }
        val avgMs = (System.nanoTime() - start) / 1e6 / inputs.size
        println("Lập lá số trung bình: %.3f ms".format(avgMs))
        assertTrue(avgMs < 5.0, "trung bình $avgMs ms")
    }
}
