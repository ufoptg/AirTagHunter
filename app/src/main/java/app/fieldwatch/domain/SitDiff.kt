package app.fieldwatch.domain

/** Two sit windows, keyed kind + MAC. BLE rotation is a new row. */
object SitDiff {
    data class Radio(
        val key: String,
        val kind: RadioKind,
        val mac: String,
        val name: String,
        val extraAttention: Boolean,
        val named: Boolean,
        val bookmarked: Boolean = false,
        val fleetNames: List<String>,
        val fleetIds: List<String> = emptyList(),
        val randomized: Boolean = false,
        val lat: Double? = null,
        val lon: Double? = null,
        val observerNotes: String = "",
        val gpsTrail: List<GpsSample> = emptyList(),
        val firstSeen: Long = 0L,
        val lastSeen: Long = 0L,
        val liveDecode: List<LiveDecodeChip> = emptyList(),
        val payloadLat: Double? = null,
        val payloadLon: Double? = null,
        val payloadAlt: Double? = null,
        val payloadHeading: Double? = null,
        val payloadSpeed: Double? = null,
        val payloadOpLat: Double? = null,
        val payloadOpLon: Double? = null,
        val payloadUasId: String = "",
        val payloadTrail: List<PayloadFix> = emptyList(),
    )

    data class Side(
        val name: String,
        val ram: Boolean,
        val radios: List<Radio>,
        val path: List<GpsSample> = emptyList(),
    ) {
        val keys: Set<String> get() = radios.map { it.key }.toSet()
    }

    fun secondSitChoices(
        closed: List<SitSummary>,
        thisSavedId: String?,
    ): List<SitSummary> = closed.filter { it.id != thisSavedId }

    fun defaultSecondSitId(
        closed: List<SitSummary>,
        thisSavedId: String?,
    ): String? = secondSitChoices(closed, thisSavedId).firstOrNull()?.id

    fun thisSavedId(open: SitSummary?, selectedId: String?): String? =
        if (open != null) null else selectedId

    fun fromSighting(
        device: Sighting,
        fleets: List<Fleet>,
        customNames: Map<String, String>,
        observerNotes: Map<String, String> = emptyMap(),
        bookmarkedKeys: Set<String> = emptySet(),
    ): Radio = Radio(
        key = device.key,
        kind = device.kind,
        mac = device.mac,
        name = device.reportName(customNames),
        extraAttention = device.attentionNotes(fleets).isNotEmpty(),
        named = device.key in customNames,
        bookmarked = device.key in bookmarkedKeys,
        fleetNames = device.fleetIds.map { id -> fleets.firstOrNull { it.id == id }?.name ?: id },
        fleetIds = device.fleetIds.toList(),
        randomized = device.randomized,
        lat = SitPathPlot.loudestFix(device)?.lat ?: device.latitude,
        lon = SitPathPlot.loudestFix(device)?.lon ?: device.longitude,
        observerNotes = observerNotes[device.key].orEmpty(),
        gpsTrail = device.gpsTrail,
        firstSeen = device.firstSeen,
        lastSeen = device.lastSeen,
        liveDecode = device.liveDecode,
        payloadLat = device.payloadLat,
        payloadLon = device.payloadLon,
        payloadAlt = device.payloadAlt,
        payloadHeading = device.payloadHeading,
        payloadSpeed = device.payloadSpeed,
        payloadOpLat = device.payloadOpLat,
        payloadOpLon = device.payloadOpLon,
        payloadUasId = device.payloadUasId?.trim().orEmpty(),
        payloadTrail = device.payloadTrail,
    )

    fun fromSitRadio(
        row: SitRadio,
        fleets: List<Fleet>,
        customNames: Map<String, String>,
        observerNotes: Map<String, String> = emptyMap(),
        bookmarkedKeys: Set<String> = emptySet(),
    ): Radio = Radio(
        key = row.key,
        kind = row.kind,
        mac = row.mac,
        name = customNames[row.key]?.trim()?.takeIf { it.isNotEmpty() } ?: row.name.ifBlank { row.mac },
        extraAttention = row.extraAttention,
        named = row.key in customNames,
        bookmarked = row.key in bookmarkedKeys,
        fleetNames = row.fleetIds.map { id -> fleets.firstOrNull { it.id == id }?.name ?: id },
        fleetIds = row.fleetIds.toList(),
        randomized = row.randomized,
        lat = SitPathPlot.loudestFix(row.gpsTrail)?.lat,
        lon = SitPathPlot.loudestFix(row.gpsTrail)?.lon,
        observerNotes = observerNotes[row.key].orEmpty(),
        gpsTrail = row.gpsTrail,
        firstSeen = row.firstSeen,
        lastSeen = row.lastSeen,
        liveDecode = row.liveDecode,
        payloadLat = row.payloadLat,
        payloadLon = row.payloadLon,
        payloadAlt = row.payloadAlt,
        payloadHeading = row.payloadHeading,
        payloadSpeed = row.payloadSpeed,
        payloadOpLat = row.payloadOpLat,
        payloadOpLon = row.payloadOpLon,
        payloadUasId = row.payloadUasId?.trim().orEmpty(),
        payloadTrail = row.payloadTrail,
    )

    fun report(
        thisSit: Side,
        second: Side,
        demoMode: Boolean,
    ): String {
        val macs = (thisSit.radios + second.radios).map { it.mac }
        return document(thisSit, second).withDemoMacs(macs, demoMode).toPlainText()
    }

    /** Same shape as Debrief so Compare (PDF) uses the Debrief letter layout. */
    fun document(
        thisSit: Side,
        second: Side,
        watchedFleetIds: Set<String> = emptySet(),
    ): DebriefDoc {
        val thisKeys = thisSit.keys
        val secondKeys = second.keys
        val byKey = (thisSit.radios + second.radios).associateBy { it.key }
        val onlyThis = thisKeys.minus(secondKeys)
        val onlySecond = secondKeys.minus(thisKeys)
        val both = thisKeys.intersect(secondKeys)
        val ramNote = if (thisSit.ram || second.ram) {
            "Last 15 minutes is the Live RAM set (about 400 radios). " +
                "A named sit keeps up to ${Sit.RADIO_CAP}. Counts are not the same net."
        } else {
            null
        }
        val extraHits = exclusiveExtra(onlyThis, onlySecond, byKey)
        val sections = ArrayList<DebriefSection>()
        var n = 1
        fun next() = n++.toString()
        sections += DebriefSection(
            next(),
            "Windows",
            buildString {
                append(sideBlock("This sit", thisSit))
                append(sideBlock("Second sit", second))
                if (ramNote != null) {
                    appendLine(ramNote)
                    appendLine()
                }
                append("Radios this phone heard. Kind + MAC. BLE rotation is a new row. Not a radio fix.")
            },
        )
        val thisCraft = AircraftTrail.pictures(thisSit.radios.mapNotNull { it.toCraftSource() }, thisSit.path)
        val secondCraft = AircraftTrail.pictures(second.radios.mapNotNull { it.toCraftSource() }, second.path)
        val aircraftBody = AircraftTrail.compareBody(thisSit.name, thisCraft, second.name, secondCraft)
        if (aircraftBody.isNotEmpty()) {
            sections += DebriefSection(next(), "Aircraft", aircraftBody)
        }
        observerNotesSection(thisSit, second)?.let { body ->
            sections += DebriefSection(next(), "Observer notes", body)
        }
        if (extraHits.isNotEmpty()) {
            sections += DebriefSection(
                next(),
                "Extra attention",
                extraHits.joinToString("\n") { "${it.radioLabel}\n${it.note}" },
                alert = true,
            )
        }
        sections += DebriefSection(next(), "Only in this sit (${onlyThis.size})", listBody(onlyThis, byKey))
        sections += DebriefSection(next(), "Only in second sit (${onlySecond.size})", listBody(onlySecond, byKey))
        sections += DebriefSection(next(), "In both (${both.size})", bothBody(both, thisSit, second))
        val meta = buildList {
            add("This sit" to thisSit.name)
            add("Second sit" to second.name)
            add("This radios" to thisSit.radios.size.toString())
            add("Second radios" to second.radios.size.toString())
            if (ramNote != null) add("Caps" to "RAM ~400 vs sit ${Sit.RADIO_CAP}")
        }
        return DebriefDoc(
            generatedUtc = "",
            windowLine = "${thisSit.name} vs ${second.name}",
            meta = meta,
            disclaimer = FieldwatchDisclaimer.compare(),
            trackingAlert = extraHits.isNotEmpty(),
            takeaway = "${onlyThis.size} only in this sit · ${onlySecond.size} only in the second · ${both.size} in both.",
            sections = sections,
            extraAttention = extraHits,
            heading = "FIELDWATCH SIT COMPARE",
            pdfKicker = "SIT COMPARE",
            pdfTitle = "Sit compare",
            pathFigure = AircraftTrail.applyWalk(
                AircraftTrail.applyWalk(
                    pathFigure(thisSit, second, watchedFleetIds),
                    thisCraft,
                    secondary = false,
                ),
                secondCraft,
                secondary = true,
            ),
            extraFigures = AircraftTrail.compareOwnFigures(thisCraft, secondCraft),
        )
    }

    private fun pathFigure(
        thisSit: Side,
        second: Side,
        watchedFleetIds: Set<String>,
    ): SitPathPlot.Figure? {
        val tracks = listOfNotNull(
            Geo.despikePath(thisSit.path).takeIf { it.size >= 2 }?.let {
                SitPathPlot.FigureTrack(thisSit.name, it, secondary = false)
            },
            Geo.despikePath(second.path).takeIf { it.size >= 2 }?.let {
                SitPathPlot.FigureTrack(second.name, it, secondary = true)
            },
        )
        if (tracks.isEmpty()) return null
        val pinNote = "A MAC alert or a signature alert is drawn once. A decoded latitude and longitude is the last advertised position. Anything else is the strongest hear. A number is that place (Path key)."
        val points = (thisSit.radios + second.radios)
            .filter { it.bookmarked || it.fleetIds.any { id -> id in watchedFleetIds } }
            .distinctBy { it.key }
            .mapNotNull { r ->
                val advertised = r.advertisedCoord()
                val pin = advertised ?: r.hearCoord() ?: return@mapNotNull null
                val notes = if (r.bookmarked) r.observerNotes else ""
                val label = r.name.ifBlank { r.mac }
                val kept = r.payloadTrail.lastOrNull { PayloadLocation.validCoord(it.lat, it.lon) }
                SitPathPlot.Dot(
                    key = r.key,
                    lat = pin.first,
                    lon = pin.second,
                    label = label,
                    extraAttention = r.extraAttention,
                    named = r.bookmarked || r.named,
                    kind = r.kind,
                    mac = r.mac,
                    fleetNames = r.fleetNames,
                    observerNotes = notes,
                    advertised = advertised != null,
                    advertisedNote = if (advertised != null) {
                        AircraftTrail.advertisedNote(
                            status = r.liveDecode.reportLabels().joinToString(", "),
                            uasId = r.payloadUasId,
                            label = label,
                            lat = pin.first,
                            lon = pin.second,
                            alt = kept?.alt ?: r.payloadAlt,
                            heading = kept?.heading ?: r.payloadHeading,
                            speed = kept?.speed ?: r.payloadSpeed,
                            pilotLat = r.payloadOpLat,
                            pilotLon = r.payloadOpLon,
                        )
                    } else {
                        ""
                    },
                )
            }
            .sortedBy { it.label }
            .take(48)
        val dots = points
        val all = tracks.flatMap { it.samples }
        val cap = if (tracks.size == 2) {
            "Two walks on one north-up frame. Green = this sit. Slate = second sit. $pinNote"
        } else {
            "North-up. Line is this phone. $pinNote"
        }
        return SitPathPlot.Figure(
            kicker = if (tracks.size == 2) "OPERATOR PATHS" else "OPERATOR PATH",
            tracks = tracks,
            dots = dots,
            lengthM = Geo.pathLengthM(all),
            spanM = Geo.spanM(all),
            caption = cap,
        )
    }

    private fun Radio.advertisedCoord(): Pair<Double, Double>? {
        val kept = payloadTrail.lastOrNull { PayloadLocation.validCoord(it.lat, it.lon) }
        if (kept != null) return kept.lat to kept.lon
        if (PayloadLocation.validCoord(payloadLat, payloadLon)) return payloadLat!! to payloadLon!!
        return null
    }

    private fun Radio.hearCoord(): Pair<Double, Double>? {
        val la = lat ?: return null
        val lo = lon ?: return null
        return if (PayloadLocation.validCoord(la, lo)) la to lo else null
    }

    private fun Radio.toCraftSource(): AircraftTrail.Source? {
        val fixes = payloadTrail.ifEmpty {
            val lat = payloadLat ?: return null
            val lon = payloadLon ?: return null
            listOf(PayloadFix(lastSeen, lat, lon, payloadAlt, payloadHeading, payloadSpeed))
        }.filter { PayloadLocation.validCoord(it.lat, it.lon) }
        if (fixes.isEmpty()) return null
        val last = fixes.last()
        return AircraftTrail.Source(
            uasId = payloadUasId,
            title = name.ifBlank { mac },
            lastSeen = lastSeen,
            status = liveDecode.reportLabels().joinToString(", "),
            fixes = fixes,
            alt = last.alt ?: payloadAlt,
            heading = last.heading ?: payloadHeading,
            speed = last.speed ?: payloadSpeed,
            pilotLat = payloadOpLat,
            pilotLon = payloadOpLon,
            key = key,
            mac = mac,
        )
    }

    private fun sideBlock(heading: String, side: Side): String {
        val net = if (side.ram) {
            "last 15 minutes in memory (about 400 radios)"
        } else {
            "named window (up to ${Sit.RADIO_CAP})"
        }
        return "$heading: ${side.name}\n${side.radios.size} radios · $net\n"
    }

    private fun exclusiveExtra(
        onlyThis: Set<String>,
        onlySecond: Set<String>,
        byKey: Map<String, Radio>,
    ): List<ExtraAttentionHit> {
        fun hits(keys: Set<String>, where: String) = keys.mapNotNull { key ->
            val row = byKey[key] ?: return@mapNotNull null
            if (!row.extraAttention) return@mapNotNull null
            ExtraAttentionHit(
                signature = row.fleetNames.firstOrNull() ?: "Extra attention",
                radioLabel = line(row),
                note = where,
            )
        }
        return hits(onlyThis, "Only in this sit.") + hits(onlySecond, "Only in second sit.")
    }

    private fun listBody(keys: Set<String>, byKey: Map<String, Radio>): String {
        if (keys.isEmpty()) return "(none)"
        return keys.mapNotNull { byKey[it] }
            .sortedWith(
                compareByDescending<Radio> { it.extraAttention }
                    .thenByDescending { it.named }
                    .thenBy { it.kind.name }
                    .thenBy { it.mac },
            )
            .joinToString("\n") { line(it) }
    }

    /** Kind + MAC is the same radio. A live label can still change between windows. */
    private fun bothBody(keys: Set<String>, thisSit: Side, second: Side): String {
        if (keys.isEmpty()) return "(none)"
        val earlier = thisSit.radios.associateBy { it.key }
        val later = second.radios.associateBy { it.key }
        return keys.mapNotNull { key ->
            val a = earlier[key] ?: return@mapNotNull null
            val b = later[key] ?: return@mapNotNull null
            a to b
        }.sortedWith(
            compareByDescending<Pair<Radio, Radio>> { it.second.extraAttention }
                .thenByDescending { it.second.named }
                .thenBy { it.second.kind.name }
                .thenBy { it.second.mac },
        ).joinToString("\n") { (a, b) -> bothLine(a, b) }
    }

    private fun bothLine(earlier: Radio, later: Radio): String = buildString {
        append(line(later, chips = false))
        val left = earlier.liveDecode.reportLabels()
        val right = later.liveDecode.reportLabels()
        when {
            left.isNotEmpty() && right.isNotEmpty() && left != right -> {
                append("  decoded value changed: ")
                append(left.joinToString(", "))
                append(" → ")
                append(right.joinToString(", "))
            }
            else -> append(chipSuffix(if (right.isNotEmpty()) later.liveDecode else earlier.liveDecode))
        }
    }

    private fun line(row: Radio, chips: Boolean = true): String = buildString {
        append(if (row.kind == RadioKind.WIFI) "WIFI" else "BLE ")
        append("  ")
        append(row.mac)
        val label = row.name.trim()
        if (label.isNotEmpty() && !label.equals(row.mac, ignoreCase = true)) {
            append("  ")
            append(label)
        }
        row.fleetNames.filter { it.isNotBlank() }.forEach { name ->
            append("  ")
            append(name)
        }
        if (row.extraAttention) append("  Extra attention")
        if (chips) append(chipSuffix(row.liveDecode))
    }

    private fun chipSuffix(chips: List<LiveDecodeChip>): String {
        val labels = chips.reportLabels()
        val notes = chips.map { it.note.trim() }.filter { it.isNotEmpty() }.distinct()
        if (labels.isEmpty() && notes.isEmpty()) return ""
        return buildString {
            if (labels.isNotEmpty()) {
                append("  ")
                append(labels.joinToString(", "))
            }
            if (notes.isNotEmpty()) {
                append("  ")
                append(notes.joinToString(" "))
            }
        }
    }

    private fun observerNotesSection(thisSit: Side, second: Side): String? {
        fun where(key: String): String = when {
            key in thisSit.keys && key in second.keys -> "both"
            key in thisSit.keys -> "this sit"
            else -> "second sit"
        }
        val rows = (thisSit.radios + second.radios)
            .distinctBy { it.key }
            .mapNotNull { r ->
                val note = r.observerNotes.trim().takeIf { it.isNotEmpty() } ?: return@mapNotNull null
                r to note
            }
        if (rows.isEmpty()) return null
        return buildString {
            appendLine("Your captions on radios heard in either window. Same KIND+MAC as Named radios. Not catalog Notes.")
            rows.sortedWith(
                compareBy<Pair<Radio, String>> { where(it.first.key) }.thenBy { it.first.mac },
            ).forEach { (r, note) ->
                val kind = if (r.kind == RadioKind.WIFI) "WIFI" else "BLE"
                val label = r.name.trim().takeIf { it.isNotEmpty() && !it.equals(r.mac, ignoreCase = true) }
                append("  · $kind  ${r.mac}")
                if (label != null) append("  ").append(label)
                append("  ").append(where(r.key))
                appendLine()
                appendLine("    $note")
            }
        }.trimEnd()
    }
}
