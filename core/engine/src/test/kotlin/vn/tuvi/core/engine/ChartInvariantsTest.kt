package vn.tuvi.core.engine

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/** Bất biến mục 14.2 trên 10.000 lá số ngẫu nhiên 1900–2100 (mọi cấu hình trường phái). Vét cạn: tools/StressCheck.kt. */
class ChartInvariantsTest {

    @Test
    @DisplayName("Bất biến 1–7 và tính toàn vẹn trên 10.000 lá số ngẫu nhiên")
    fun invariants() = Prop.forAll(
        iterations = 10_000,
        gen = { r -> RandomBirths.input(r) to RandomBirths.config(r) },
    ) { (input, config) ->
        ChartChecks.assertInvariants(ChartEngine.build(input, config), config)
    }
}
