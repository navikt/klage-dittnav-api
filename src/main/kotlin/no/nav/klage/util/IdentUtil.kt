package no.nav.klage.util

fun isValidFnrOrDnr(fnr: String): Boolean {
    if (fnr.length != 11) {
        return false
    }
    val d1 = fnr.substring(startIndex = 0, endIndex = 1).toInt()
    val d2 = fnr.substring(startIndex = 1, endIndex = 2).toInt()
    val m1 = fnr.substring(startIndex = 2, endIndex = 3).toInt()
    val m2 = fnr.substring(startIndex = 3, endIndex = 4).toInt()
    val y1 = fnr.substring(startIndex = 4, endIndex = 5).toInt()
    val y2 = fnr.substring(startIndex = 5, endIndex = 6).toInt()
    val i1 = fnr.substring(startIndex = 6, endIndex = 7).toInt()
    val i2 = fnr.substring(startIndex = 7, endIndex = 8).toInt()
    val i3 = fnr.substring(startIndex = 8, endIndex = 9).toInt()

    var k1 = 11 - ((3 * d1 + 7 * d2 + 6 * m1 + 1 * m2 + 8 * y1 + 9 * y2 + 4 * i1 + 5 * i2 + 2 * i3) % 11)
    var k2 = 11 - ((5 * d1 + 4 * d2 + 3 * m1 + 2 * m2 + 7 * y1 + 6 * y2 + 5 * i1 + 4 * i2 + 3 * i3 + 2 * k1) % 11)

    if (k1 == 11) k1 = 0
    if (k2 == 11) k2 = 0

    return k1 < 10 && k2 < 10 && k1 < 10 && k2 < 10 && k1 == fnr.substring(startIndex = 9, endIndex = 10).toInt() && k2 ==
        fnr
            .substring(startIndex = 10, endIndex = 11)
            .toInt()
}
