package vn.tuvi.indyfix;

import java.util.Comparator;
import kotlin.comparisons.ComparisonsKt;
import kotlin.jvm.functions.Function1;

/**
 * Thay thế 5 hàm trong kotlin-stdlib (kotlin.comparisons) vốn tạo Comparator bằng invokedynamic/LambdaMetafactory —
 * thứ Android không có. StripIndy chuyển thân các hàm đó sang gọi lớp này; ngữ nghĩa giữ nguyên như stdlib.
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public final class Comparators {
    private Comparators() {}

    public static Comparator compareBy(final Function1[] selectors) {
        if (selectors.length == 0) throw new IllegalArgumentException("Failed requirement.");
        return new Comparator() {
            public int compare(Object a, Object b) {
                for (Function1 fn : selectors) {
                    int diff = ComparisonsKt.compareValues((Comparable) fn.invoke(a), (Comparable) fn.invoke(b));
                    if (diff != 0) return diff;
                }
                return 0;
            }
        };
    }

    public static Comparator then(final Comparator first, final Comparator second) {
        return new Comparator() {
            public int compare(Object a, Object b) {
                int r = first.compare(a, b);
                return r != 0 ? r : second.compare(a, b);
            }
        };
    }

    public static Comparator thenDescending(final Comparator first, final Comparator second) {
        return new Comparator() {
            public int compare(Object a, Object b) {
                int r = first.compare(a, b);
                return r != 0 ? r : second.compare(b, a);
            }
        };
    }

    public static Comparator nullsFirst(final Comparator c) {
        return new Comparator() {
            public int compare(Object a, Object b) {
                if (a == b) return 0;
                if (a == null) return -1;
                if (b == null) return 1;
                return c.compare(a, b);
            }
        };
    }

    public static Comparator nullsLast(final Comparator c) {
        return new Comparator() {
            public int compare(Object a, Object b) {
                if (a == b) return 0;
                if (a == null) return 1;
                if (b == null) return -1;
                return c.compare(a, b);
            }
        };
    }
}
