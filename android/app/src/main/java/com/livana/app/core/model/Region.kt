package com.livana.app.core.model

/**
 * Canonical region vocabulary. Pool `region` is exact-match — the create-pool picker and the
 * explore filter MUST send/expect exactly these display strings.
 */
enum class Region(val display: String) {
    GLOBAL("Global"),
    SUB_SAHARAN_AFRICA("Sub-Saharan Africa"),
    NORTH_AFRICA("North Africa"),
    MIDDLE_EAST("Middle East"),
    SOUTH_ASIA("South Asia"),
    SOUTHEAST_ASIA("Southeast Asia"),
    EAST_ASIA("East Asia"),
    CENTRAL_ASIA("Central Asia"),
    EUROPE("Europe"),
    NORTH_AMERICA("North America"),
    LATIN_AMERICA_CARIBBEAN("Latin America & Caribbean"),
    OCEANIA("Oceania");

    companion object {
        fun fromDisplay(value: String?): Region? = entries.firstOrNull { it.display == value }
    }
}
