package vn.tuvi.core.interpret

/** Tầng vận hạn ứng với một điểm định vị. */
fun LimitPoint.toLayer(): Layer = when (this) {
    LimitPoint.DAI_HAN -> Layer.DAI_HAN
    LimitPoint.TIEU_HAN -> Layer.TIEU_HAN
    LimitPoint.LUU_NIEN_DAI_HAN -> Layer.LUU_NIEN
    LimitPoint.LUU_THAI_TUE -> Layer.LUU_THAI_TUE
}

/**
 * Các tầng chắc chắn tham gia khi luật kích hoạt: tầng neo + tầng mà điều kiện bắt buộc phải xét
 * (`allOf` lấy hợp, `anyOf` lấy giao, `noneOf` không tính). Luật lá số gốc thuộc tầng GOC.
 * Dùng cho quy tắc đồng thanh tương ứng (mục 9): đếm số tầng cùng chỉ một sự kiện.
 */
fun Rule.layers(): Set<Layer> {
    if (scope == Scope.GOC) return setOf(Layer.GOC)
    val base = (anchor as? Anchor.Layer)?.let { setOf(it.layer.toLayer()) } ?: emptySet()
    val all = base + layersOf(condition)
    return all.ifEmpty { setOf(if (scope == Scope.DAI_HAN) Layer.DAI_HAN else Layer.LUU_THAI_TUE) }
}

/** Tầng mà finding của luật này đóng góp vào khi chấm điểm năm (mục 9 bước 6). */
fun Rule.scoringLayer(): Layer = when {
    scope == Scope.GOC -> Layer.GOC
    scope == Scope.DAI_HAN -> Layer.DAI_HAN
    anchor is Anchor.Layer -> (anchor as Anchor.Layer).layer.toLayer()
    else -> Layer.LUU_THAI_TUE
}

private fun layersOf(p: Predicate): Set<Layer> = when (p) {
    is Predicate.AllOf -> p.items.flatMapTo(HashSet()) { layersOf(it) }
    is Predicate.AnyOf -> p.items.map { layersOf(it) }.reduceOrNull { a, b -> a intersect b } ?: emptySet()
    is Predicate.NoneOf -> emptySet()
    is Predicate.StarIn -> if (p.luu) setOf(Layer.LUU_THAI_TUE) else emptySet()
    is Predicate.LuuHoaIn -> setOf(Layer.LUU_THAI_TUE)
    is Predicate.CountOf -> if (p.luu) setOf(Layer.LUU_THAI_TUE) else emptySet()
    is Predicate.HoaOf -> if (p.luu) setOf(Layer.LUU_THAI_TUE) else emptySet()
    is Predicate.YearCond -> if (p.thienThoi != null) setOf(Layer.DAI_HAN) else emptySet()
    is Predicate.At -> buildSet {
        when (val l = p.locator) {
            is Locator.Point -> add(l.point.toLayer())
            is Locator.LuuStarAt -> add(Layer.LUU_THAI_TUE)
            else -> {}
        }
        (p.same as? Locator.Point)?.let { add(it.point.toLayer()) }
        if (p.hasLuuStar != null) add(Layer.LUU_THAI_TUE)
    }
    else -> emptySet()
}
