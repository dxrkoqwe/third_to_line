package com.example.match3

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntOffsetAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

// ============================================================
// COLORS
// ============================================================

val BgTop = Color(0xFF1A0E2E)
val BgBottom = Color(0xFF2D1B4E)
val CardBg = Color(0xFF33245C)
val Accent = Color(0xFFB388FF)
val Gold = Color(0xFFFFC107)

// ============================================================
// ENTRY
// ============================================================

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Accent, background = BgTop, surface = CardBg
                )
            ) { AppRoot() }
        }
    }
}

@Composable
fun GradientBg(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(BgTop, BgBottom)))
    ) {
        StarsBackground()
        content()
    }
}

@Composable
fun StarsBackground() {
    val stars = remember {
        val rng = Random(42)
        List(40) { Triple(rng.nextFloat(), rng.nextFloat(), rng.nextFloat()) }
    }
    val transition = rememberInfiniteTransition(label = "stars")
    val phase by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(24000, easing = LinearEasing)),
        label = "phase"
    )
    Canvas(modifier = Modifier.fillMaxSize()) {
        stars.forEach { (fx, fy, fsize) ->
            val y = ((fy + phase) % 1f) * size.height
            val x = fx * size.width
            drawCircle(
                color = Color.White.copy(alpha = 0.25f + fsize * 0.35f),
                radius = fsize * 2.2f + 0.6f,
                center = Offset(x, y)
            )
        }
    }
}

// ============================================================
// GEMS
// ============================================================

enum class TileType(
    val color: Color,
    val shadow: Color,
    val sides: Int,
    val rotation: Float
) {
    RED(Color(0xFFFF5252), Color(0xFFB71C1C), 6, 0f),
    BLUE(Color(0xFF448AFF), Color(0xFF0D47A1), 6, 30f),
    GREEN(Color(0xFF69F0AE), Color(0xFF1B5E20), 8, 22.5f),
    YELLOW(Color(0xFFFFD740), Color(0xFFF57F17), 8, 0f),
    PURPLE(Color(0xFFE040FB), Color(0xFF6A1B9A), 6, 15f),
    CYAN(Color(0xFF40E0D0), Color(0xFF00695C), 8, 22.5f),
    PINK(Color(0xFFFF80AB), Color(0xFF880E4F), 6, -15f);

    companion object {
        fun fromOrdinal(o: Int): TileType = entries[((o % entries.size) + entries.size) % entries.size]
    }
}

data class Tile(
    val id: Int,
    val type: Int,
    val matching: Boolean = false,
    val locked: Boolean = false,
    val stone: Boolean = false,
    val hasHeart: Boolean = false,
    val rainbow: Boolean = false
) {
    val isMovable: Boolean get() = !stone && !locked
}

fun buildPolygon(cx: Float, cy: Float, r: Float, sides: Int, rotationDeg: Float): Path {
    val path = Path()
    val startAngle = -PI / 2 + rotationDeg * PI / 180.0
    for (i in 0 until sides) {
        val angle = startAngle + i * 2 * PI / sides
        val px = cx + (cos(angle) * r).toFloat()
        val py = cy + (sin(angle) * r).toFloat()
        if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
    }
    path.close()
    return path
}

@Composable
fun GemTile(type: TileType, modifier: Modifier = Modifier, rainbow: Boolean = false) {
    Canvas(modifier = modifier) {
        val w = size.width; val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas
        val cx = w / 2f; val cy = h / 2f
        val r = min(w, h) / 2f * 0.94f
        val outer = buildPolygon(cx, cy, r, type.sides, type.rotation)
        val table = buildPolygon(cx, cy, r * 0.55f, type.sides, type.rotation)

        drawPath(outer, color = Color.Black.copy(alpha = 0.55f), style = Stroke(width = w * 0.09f))
        if (rainbow) {
            drawPath(outer, brush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFFFF5252), Color(0xFFFFD740), Color(0xFF69F0AE),
                    Color(0xFF40E0D0), Color(0xFF448AFF), Color(0xFFE040FB)
                ),
                start = Offset(0f, 0f), end = Offset(w, h)
            ))
        } else {
            drawPath(outer, brush = Brush.verticalGradient(
                colors = listOf(type.color, type.shadow), startY = 0f, endY = h))
        }
        drawPath(outer, brush = Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.55f),
                Color.White.copy(alpha = 0.05f),
                Color.Transparent
            ), startY = 0f, endY = h * 0.55f))
        drawPath(table, brush = Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.45f),
                Color.White.copy(alpha = 0.08f)
            ), startY = cy - r * 0.55f, endY = cy + r * 0.55f))
        drawPath(table, color = Color.White.copy(alpha = 0.5f), style = Stroke(width = w * 0.02f))
        drawCircle(
            color = Color.White.copy(alpha = 0.9f),
            radius = w * 0.055f,
            center = Offset(cx - w * 0.15f, cy - h * 0.18f)
        )
    }
}

// ============================================================
// HEART OVERLAY
// ============================================================

fun buildHeartShape(cx: Float, cy: Float, size: Float): Path {
    val path = Path()
    val r = size / 2f
    val topY = cy - r * 0.7f
    val bottomY = cy + r * 0.9f
    path.moveTo(cx, bottomY)
    path.cubicTo(
        cx - r * 1.3f, cy + r * 0.2f,
        cx - r * 1.0f, topY,
        cx, cy - r * 0.15f
    )
    path.cubicTo(
        cx + r * 1.0f, topY,
        cx + r * 1.3f, cy + r * 0.2f,
        cx, bottomY
    )
    path.close()
    return path
}

@Composable
fun HeartOverlay(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width; val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas
        val cx = w / 2f; val cy = h / 2f
        val s = min(w, h) * 0.55f

        val heartPath = buildHeartShape(cx, cy, s)

        drawPath(
            heartPath,
            color = Color.White.copy(alpha = 0.95f),
            style = Stroke(width = s * 0.18f, join = StrokeJoin.Round)
        )
        drawPath(heartPath, brush = Brush.verticalGradient(
            colors = listOf(Color(0xFFFF5252), Color(0xFFC62828)),
            startY = cy - s / 2f, endY = cy + s / 2f
        ))
        drawCircle(
            color = Color.White.copy(alpha = 0.85f),
            radius = s * 0.09f,
            center = Offset(cx - s * 0.18f, cy - s * 0.15f)
        )
    }
}

// ============================================================
// STONE, ICE, LOCK
// ============================================================

@Composable
fun StoneTile(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width; val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas
        val pad = min(w, h) * 0.05f
        val path = Path().apply {
            moveTo(pad * 1.5f, pad * 2f)
            lineTo(w * 0.30f, pad)
            lineTo(w * 0.72f, pad * 1.6f)
            lineTo(w - pad, h * 0.30f)
            lineTo(w - pad * 1.5f, h - pad * 1.5f)
            lineTo(w * 0.42f, h - pad)
            lineTo(pad, h * 0.75f)
            close()
        }
        drawPath(path, color = Color.Black.copy(alpha = 0.5f), style = Stroke(width = w * 0.10f))
        drawPath(path, brush = Brush.verticalGradient(
            listOf(Color(0xFF9E9E9E), Color(0xFF424242)), 0f, h))
        val rng = Random(7)
        repeat(6) {
            val px = pad + rng.nextFloat() * (w - pad * 2f)
            val py = pad + rng.nextFloat() * (h - pad * 2f)
            drawCircle(Color.White.copy(alpha = 0.18f), radius = w * 0.04f, center = Offset(px, py))
        }
        drawLine(
            color = Color.Black.copy(alpha = 0.35f),
            start = Offset(w * 0.30f, h * 0.25f),
            end = Offset(w * 0.60f, h * 0.75f),
            strokeWidth = w * 0.02f
        )
    }
}

@Composable
fun IceOverlay(layers: Int, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width; val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas
        val alpha = if (layers >= 2) 0.60f else 0.40f
        drawRoundRect(
            color = Color(0xFFB3E5FC).copy(alpha = alpha),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.16f)
        )
        val crackCount = if (layers >= 2) 4 else 2
        val rng = Random(if (layers >= 2) 11 else 3)
        repeat(crackCount) {
            val x1 = rng.nextFloat() * w
            val y1 = rng.nextFloat() * h
            val x2 = (x1 + (rng.nextFloat() - 0.5f) * w * 0.7f).coerceIn(0f, w)
            val y2 = (y1 + (rng.nextFloat() - 0.5f) * h * 0.7f).coerceIn(0f, h)
            drawLine(
                color = Color.White.copy(alpha = 0.80f),
                start = Offset(x1, y1), end = Offset(x2, y2),
                strokeWidth = w * 0.03f
            )
        }
        drawRoundRect(
            color = Color.White.copy(alpha = 0.90f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.16f),
            style = Stroke(width = w * 0.03f)
        )
    }
}

@Composable
fun LockOverlay(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width; val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas
        drawRoundRect(
            color = Color.Black.copy(alpha = 0.35f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.16f)
        )
        val bodyW = w * 0.42f
        val bodyH = h * 0.30f
        val bodyX = (w - bodyW) / 2f
        val bodyY = h * 0.52f
        drawRoundRect(
            color = Color(0xFFFFC107),
            topLeft = Offset(bodyX, bodyY),
            size = androidx.compose.ui.geometry.Size(bodyW, bodyH),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.05f)
        )
        drawRoundRect(
            color = Color(0xFF8D6E00),
            topLeft = Offset(bodyX, bodyY),
            size = androidx.compose.ui.geometry.Size(bodyW, bodyH),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.05f),
            style = Stroke(width = w * 0.02f)
        )
        val strokeW = w * 0.07f
        val arcLeft = bodyX + bodyW * 0.20f
        val arcRight = bodyX + bodyW * 0.80f
        val arcTop = bodyY - bodyH * 0.65f
        val arcBottom = bodyY + bodyH * 0.05f
        val path = Path().apply {
            moveTo(arcLeft, arcBottom)
            lineTo(arcLeft, (arcTop + arcBottom) / 2f)
            quadraticBezierTo((arcLeft + arcRight) / 2f, arcTop,
                arcRight, (arcTop + arcBottom) / 2f)
            lineTo(arcRight, arcBottom)
        }
        drawPath(path, color = Color(0xFFFFC107),
            style = Stroke(width = strokeW, cap = androidx.compose.ui.graphics.StrokeCap.Round))
        drawCircle(Color(0xFF3E2723), radius = w * 0.045f,
            center = Offset(w / 2f, bodyY + bodyH * 0.42f))
    }
}

// ============================================================
// BOARD LOGIC
// ============================================================

data class LevelState(
    val grid: List<List<Tile>>,
    val iceGrid: List<List<Int>>
)

object Board {
    const val SIZE = 8

    fun generate(types: Int, nextId: () -> Int, rng: Random = Random.Default): List<List<Tile>> {
        var grid: List<List<Tile>>
        var safety = 0
        do {
            grid = List(SIZE) { List(SIZE) { Tile(nextId(), rng.nextInt(types)) } }
            safety++
        } while ((findMatches(grid).isNotEmpty() || !hasAnyMove(grid)) && safety < 30)
        return grid
    }

    fun generateWithObstacles(
        types: Int,
        obstacles: List<String>,
        heartCount: Int,
        nextId: () -> Int,
        rng: Random = Random.Default
    ): LevelState {
        var state: LevelState
        var safety = 0
        do {
            val base = generate(types, nextId, rng)
            state = if (obstacles.isEmpty()) LevelState(base, emptyIce())
            else applyObstacles(base, obstacles)
            if (heartCount > 0) {
                val withHearts = plantHearts(state.grid, heartCount, state.iceGrid, rng)
                state = LevelState(withHearts, state.iceGrid)
            }
            safety++
        } while ((findMatches(state.grid).isNotEmpty() || !hasAnyMove(state.grid)) && safety < 40)
        return state
    }

    fun emptyIce(): List<List<Int>> = List(SIZE) { List(SIZE) { 0 } }

    private fun applyObstacles(grid: List<List<Tile>>, obstacles: List<String>): LevelState {
        val ice = MutableList(SIZE) { MutableList(SIZE) { 0 } }
        val newGrid = grid.mapIndexed { r, row ->
            row.mapIndexed { c, tile ->
                when (obstacles.getOrNull(r)?.getOrNull(c) ?: '.') {
                    '#' -> tile.copy(type = -1, stone = true)
                    'L' -> tile.copy(locked = true)
                    '1' -> { ice[r][c] = 1; tile }
                    '2' -> { ice[r][c] = 2; tile }
                    else -> tile
                }
            }
        }
        return LevelState(newGrid, ice.map { it.toList() })
    }

    private fun plantHearts(
        grid: List<List<Tile>>,
        count: Int,
        iceGrid: List<List<Int>>,
        rng: Random
    ): List<List<Tile>> {
        val candidates = mutableListOf<Pair<Int, Int>>()
        for (r in 0 until SIZE) for (c in 0 until SIZE) {
            val t = grid[r][c]
            if (!t.stone && !t.locked && iceGrid[r][c] == 0) candidates.add(r to c)
        }
        candidates.shuffle(rng)
        val chosen = candidates.take(count).toSet()
        return grid.mapIndexed { r, row ->
            row.mapIndexed { c, tile ->
                if (r to c in chosen) tile.copy(hasHeart = true) else tile
            }
        }
    }

    fun totalIce(iceGrid: List<List<Int>>): Int = iceGrid.sumOf { row -> row.sum() }

    fun findMatches(grid: List<List<Tile>>): Set<Pair<Int, Int>> {
        val matches = mutableSetOf<Pair<Int, Int>>()
        for (r in 0 until SIZE) {
            var c = 0
            while (c < SIZE) {
                val tile = grid[r][c]
                if (tile.stone) { c++; continue }
                val v = tile.type
                var c2 = c
                while (c2 < SIZE && !grid[r][c2].stone && grid[r][c2].type == v) c2++
                if (c2 - c >= 3) for (k in c until c2) matches.add(r to k)
                c = c2
            }
        }
        for (c in 0 until SIZE) {
            var r = 0
            while (r < SIZE) {
                val tile = grid[r][c]
                if (tile.stone) { r++; continue }
                val v = tile.type
                var r2 = r
                while (r2 < SIZE && !grid[r2][c].stone && grid[r2][c].type == v) r2++
                if (r2 - r >= 3) for (k in r until r2) matches.add(k to c)
                r = r2
            }
        }
        return matches
    }

    fun findFiveInRow(grid: List<List<Tile>>): Pair<Int, Int>? {
        for (r in 0 until SIZE) {
            var c = 0
            while (c < SIZE) {
                val tile = grid[r][c]
                if (tile.stone) { c++; continue }
                val v = tile.type
                var c2 = c
                while (c2 < SIZE && !grid[r][c2].stone && grid[r][c2].type == v) c2++
                if (c2 - c >= 5) return r to ((c + c2 - 1) / 2)
                c = c2
            }
        }
        for (c in 0 until SIZE) {
            var r = 0
            while (r < SIZE) {
                val tile = grid[r][c]
                if (tile.stone) { r++; continue }
                val v = tile.type
                var r2 = r
                while (r2 < SIZE && !grid[r2][c].stone && grid[r2][c].type == v) r2++
                if (r2 - r >= 5) return ((r + r2 - 1) / 2) to c
                r = r2
            }
        }
        return null
    }

    fun areAdjacent(a: Pair<Int, Int>, b: Pair<Int, Int>): Boolean =
        abs(a.first - b.first) + abs(a.second - b.second) == 1

    fun canSwap(grid: List<List<Tile>>, a: Pair<Int, Int>, b: Pair<Int, Int>): Boolean {
        if (!areAdjacent(a, b)) return false
        return grid[a.first][a.second].isMovable && grid[b.first][b.second].isMovable
    }

    fun swap(grid: List<List<Tile>>, a: Pair<Int, Int>, b: Pair<Int, Int>): List<List<Tile>> {
        val g = grid.map { it.toMutableList() }.toMutableList()
        val tmp = g[a.first][a.second]
        g[a.first][a.second] = g[b.first][b.second]
        g[b.first][b.second] = tmp
        return g.map { it.toList() }
    }

    fun hasAnyMove(grid: List<List<Tile>>): Boolean {
        val t = Array(SIZE) { r -> IntArray(SIZE) { c -> grid[r][c].type } }
        val movable = Array(SIZE) { r -> BooleanArray(SIZE) { c -> grid[r][c].isMovable } }
        val rainbow = Array(SIZE) { r -> BooleanArray(SIZE) { c -> grid[r][c].rainbow } }
        for (r in 0 until SIZE) for (c in 0 until SIZE) {
            if (!movable[r][c]) continue
            if (rainbow[r][c]) {
                if (c + 1 < SIZE && movable[r][c + 1]) return true
                if (r + 1 < SIZE && movable[r + 1][c]) return true
            }
        }
        for (r in 0 until SIZE) {
            for (c in 0 until SIZE) {
                if (!movable[r][c]) continue
                if (c + 1 < SIZE && movable[r][c + 1] && trySwapAndMatch(t, r, c, r, c + 1)) return true
                if (r + 1 < SIZE && movable[r + 1][c] && trySwapAndMatch(t, r, c, r + 1, c)) return true
            }
        }
        return false
    }

    fun findHint(grid: List<List<Tile>>): Pair<Pair<Int, Int>, Pair<Int, Int>>? {
        val t = Array(SIZE) { r -> IntArray(SIZE) { c -> grid[r][c].type } }
        val movable = Array(SIZE) { r -> BooleanArray(SIZE) { c -> grid[r][c].isMovable } }
        for (r in 0 until SIZE) for (c in 0 until SIZE) {
            if (grid[r][c].rainbow && movable[r][c]) {
                if (c + 1 < SIZE && movable[r][c + 1]) return (r to c) to (r to (c + 1))
                if (r + 1 < SIZE && movable[r + 1][c]) return (r to c) to ((r + 1) to c)
            }
        }
        for (r in 0 until SIZE) {
            for (c in 0 until SIZE) {
                if (!movable[r][c]) continue
                if (c + 1 < SIZE && movable[r][c + 1] && trySwapAndMatch(t, r, c, r, c + 1)) {
                    return (r to c) to (r to (c + 1))
                }
                if (r + 1 < SIZE && movable[r + 1][c] && trySwapAndMatch(t, r, c, r + 1, c)) {
                    return (r to c) to ((r + 1) to c)
                }
            }
        }
        return null
    }

    private fun trySwapAndMatch(t: Array<IntArray>, r1: Int, c1: Int, r2: Int, c2: Int): Boolean {
        val tmp = t[r1][c1]; t[r1][c1] = t[r2][c2]; t[r2][c2] = tmp
        val ok = hasMatchAt(t, r1, c1) || hasMatchAt(t, r2, c2)
        val tmp2 = t[r1][c1]; t[r1][c1] = t[r2][c2]; t[r2][c2] = tmp2
        return ok
    }

    private fun hasMatchAt(t: Array<IntArray>, r: Int, c: Int): Boolean {
        val v = t[r][c]
        if (v < 0) return false
        var count = 1
        var cc = c - 1
        while (cc >= 0 && t[r][cc] == v) { count++; cc-- }
        cc = c + 1
        while (cc < SIZE && t[r][cc] == v) { count++; cc++ }
        if (count >= 3) return true
        count = 1
        var rr = r - 1
        while (rr >= 0 && t[rr][c] == v) { count++; rr-- }
        rr = r + 1
        while (rr < SIZE && t[rr][c] == v) { count++; rr++ }
        return count >= 3
    }

    fun markMatching(grid: List<List<Tile>>, matches: Set<Pair<Int, Int>>): List<List<Tile>> =
        grid.mapIndexed { r, row ->
            row.mapIndexed { c, tile ->
                if (r to c in matches) tile.copy(matching = true) else tile
            }
        }

    fun unlockCells(grid: List<List<Tile>>, cells: Set<Pair<Int, Int>>): List<List<Tile>> =
        grid.mapIndexed { r, row ->
            row.mapIndexed { c, tile ->
                if (r to c in cells) tile.copy(locked = false) else tile
            }
        }

    fun dropAndRefill(
        grid: List<List<Tile>>,
        matches: Set<Pair<Int, Int>>,
        types: Int,
        nextId: () -> Int,
        rng: Random = Random.Default
    ): List<List<Tile>> {
        val g: MutableList<MutableList<Tile?>> =
            grid.map { it.toMutableList<Tile?>() }.toMutableList()
        for ((r, c) in matches) g[r][c] = null
        for (c in 0 until SIZE) {
            var segEnd = SIZE - 1
            var r = SIZE - 1
            while (r >= -1) {
                val isStone = r >= 0 && g[r][c]?.stone == true
                if (r < 0 || isStone) {
                    dropSegment(g, c, r + 1, segEnd, types, nextId, rng)
                    segEnd = r - 1
                }
                r--
            }
        }
        return g.map { it.filterNotNull() }
    }

    private fun dropSegment(
        g: MutableList<MutableList<Tile?>>,
        c: Int, from: Int, to: Int,
        types: Int, nextId: () -> Int, rng: Random
    ) {
        if (from > to) return
        val existing = mutableListOf<Tile>()
        for (r in from..to) {
            val t = g[r][c]
            if (t != null && !t.stone) existing.add(t)
        }
        var write = to
        for (i in existing.indices.reversed()) {
            g[write][c] = existing[i]
            write--
        }
        while (write >= from) {
            g[write][c] = Tile(nextId(), rng.nextInt(types))
            write--
        }
    }

    fun reshuffle(
        grid: List<List<Tile>>,
        types: Int,
        nextId: () -> Int,
        rng: Random = Random.Default
    ): List<List<Tile>> {
        val movable = mutableListOf<Pair<Int, Int>>()
        val colors = mutableListOf<Int>()
        for (r in 0 until SIZE) {
            for (c in 0 until SIZE) {
                val t = grid[r][c]
                if (!t.stone && !t.rainbow) {
                    movable.add(r to c)
                    colors.add(t.type)
                }
            }
        }
        colors.shuffle(rng)
        val newGrid = grid.map { it.toMutableList() }.toMutableList()
        movable.forEachIndexed { i, (r, c) ->
            newGrid[r][c] = grid[r][c].copy(type = colors[i], matching = false)
        }
        val result = newGrid.map { it.toList() }
        return if (findMatches(result).isEmpty()) result else grid
    }
}

// ============================================================
// LEVELS — 60 уровней
// ============================================================

enum class Difficulty(val label: String) {
    EASY("Лёгкий"), NORMAL("Обычный"), HARD("Сложный"), SUPER_HARD("Эпик")
}

enum class GoalType { SCORE, COLLECT_COLOR, BREAK_ICE, HEART }

data class LevelConfig(
    val id: Int,
    val moves: Int,
    val types: Int,
    val targetScore: Int,
    val difficulty: Difficulty,
    val rewardCoins: Int,
    val goalType: GoalType = GoalType.SCORE,
    val goalColor: Int? = null,
    val goalCount: Int = 0,
    val heartCount: Int = 0,
    val obstacles: List<String> = emptyList(),
    val continueCostCoins: Int = 50,
    val continueExtraMoves: Int = 5
)

object Levels {
    val all: List<LevelConfig> = listOf(
        // === МИР 1: обучение ===
        LevelConfig(1, 30, 5, 500, Difficulty.EASY, 10),
        LevelConfig(2, 30, 5, 800, Difficulty.EASY, 10),
        LevelConfig(3, 30, 5, 0, Difficulty.EASY, 12,
            goalType = GoalType.COLLECT_COLOR, goalColor = 0, goalCount = 15),
        LevelConfig(4, 28, 6, 1000, Difficulty.NORMAL, 15),
        LevelConfig(5, 30, 6, 0, Difficulty.NORMAL, 15,
            goalType = GoalType.HEART, heartCount = 8),
        LevelConfig(6, 30, 6, 1200, Difficulty.NORMAL, 18),
        LevelConfig(7, 30, 6, 0, Difficulty.NORMAL, 18,
            goalType = GoalType.COLLECT_COLOR, goalColor = 1, goalCount = 20),
        LevelConfig(8, 28, 6, 1400, Difficulty.NORMAL, 20),
        LevelConfig(9, 30, 6, 0, Difficulty.NORMAL, 22,
            goalType = GoalType.HEART, heartCount = 10),
        LevelConfig(10, 30, 6, 1600, Difficulty.NORMAL, 25),

        // === МИР 2: первые препятствия ===
        LevelConfig(11, 30, 6, 1300, Difficulty.NORMAL, 22,
            obstacles = listOf(
                "........", "........", "..#.....", "........",
                ".....#..", "........", "........", "........")),
        LevelConfig(12, 30, 6, 0, Difficulty.NORMAL, 22,
            goalType = GoalType.BREAK_ICE,
            obstacles = listOf(
                "........", "..1111..", "..1111..", "........",
                "........", "........", "........", "........")),
        LevelConfig(13, 28, 6, 1500, Difficulty.HARD, 24,
            obstacles = listOf(
                "........", "........", "..LL....", "..LL....",
                "........", "........", "........", "........")),
        LevelConfig(14, 30, 7, 0, Difficulty.HARD, 26,
            goalType = GoalType.BREAK_ICE,
            obstacles = listOf(
                "........", "........", "..1111..", "..1..1..",
                "..1..1..", "..1111..", "........", "........")),
        LevelConfig(15, 32, 7, 0, Difficulty.HARD, 28,
            goalType = GoalType.HEART, heartCount = 10,
            obstacles = listOf(
                "#......#", "........", "........", "........",
                "........", "........", "........", "#......#")),
        LevelConfig(16, 30, 7, 1800, Difficulty.HARD, 28),
        LevelConfig(17, 30, 7, 0, Difficulty.HARD, 30,
            goalType = GoalType.COLLECT_COLOR, goalColor = 4, goalCount = 25),
        LevelConfig(18, 28, 7, 2000, Difficulty.HARD, 30,
            obstacles = listOf(
                "........", "........", "..LL....", "..LL....",
                "........", "..LL....", "..LL....", "........")),
        LevelConfig(19, 32, 7, 0, Difficulty.HARD, 32,
            goalType = GoalType.HEART, heartCount = 12),
        LevelConfig(20, 30, 7, 2200, Difficulty.HARD, 35),

        // === МИР 3: комбинации ===
        LevelConfig(21, 30, 7, 0, Difficulty.HARD, 30,
            goalType = GoalType.BREAK_ICE,
            obstacles = listOf(
                "........", ".#....#.", ".222222.", "........",
                "........", ".222222.", ".#....#.", "........")),
        LevelConfig(22, 30, 7, 2400, Difficulty.HARD, 32,
            obstacles = listOf(
                "........", "..2222..", "........", "..L..L..",
                "........", "..2222..", "........", "........")),
        LevelConfig(23, 32, 7, 0, Difficulty.SUPER_HARD, 34,
            goalType = GoalType.COLLECT_COLOR, goalColor = 5, goalCount = 28),
        LevelConfig(24, 30, 7, 0, Difficulty.SUPER_HARD, 35,
            goalType = GoalType.BREAK_ICE,
            obstacles = listOf(
                "........", "..#..#..", ".111111.", "........",
                "........", ".111111.", "..#..#..", "........")),
        LevelConfig(25, 32, 7, 0, Difficulty.SUPER_HARD, 38,
            goalType = GoalType.HEART, heartCount = 14,
            obstacles = listOf(
                "L......L", "........", "........", "........",
                "........", "........", "........", "L......L")),
        LevelConfig(26, 32, 7, 2800, Difficulty.SUPER_HARD, 40),
        LevelConfig(27, 30, 7, 0, Difficulty.SUPER_HARD, 40,
            goalType = GoalType.BREAK_ICE,
            obstacles = listOf(
                "........", ".111111.", "........", "..#..#..",
                "........", ".111111.", "........", "........")),
        LevelConfig(28, 32, 7, 0, Difficulty.SUPER_HARD, 42,
            goalType = GoalType.HEART, heartCount = 14,
            obstacles = listOf(
                "........", ".222222.", "........", "..#..#..",
                "........", ".222222.", "........", "........")),
        LevelConfig(29, 32, 7, 3200, Difficulty.SUPER_HARD, 45,
            obstacles = listOf(
                "..#..#..", "........", ".2....2.", "........",
                "........", ".2....2.", "........", "..#..#..")),
        LevelConfig(30, 32, 7, 0, Difficulty.SUPER_HARD, 48,
            goalType = GoalType.COLLECT_COLOR, goalColor = 6, goalCount = 32),

        // === МИР 4: финал ===
        LevelConfig(31, 32, 7, 3600, Difficulty.SUPER_HARD, 50,
            obstacles = listOf(
                "##....##", "........", "........", "..L..L..",
                "..L..L..", "........", "........", "##....##")),
        LevelConfig(32, 32, 7, 0, Difficulty.SUPER_HARD, 52,
            goalType = GoalType.BREAK_ICE,
            obstacles = listOf(
                "........", "L.2222.L", "........", "..2..2..",
                "..2..2..", "........", "L.2222.L", "........")),
        LevelConfig(33, 34, 7, 0, Difficulty.SUPER_HARD, 55,
            goalType = GoalType.HEART, heartCount = 16),
        LevelConfig(34, 32, 7, 4000, Difficulty.SUPER_HARD, 58,
            obstacles = listOf(
                "........", "..#..#..", ".2....2.", "..L..L..",
                "..L..L..", ".2....2.", "..#..#..", "........")),
        LevelConfig(35, 32, 7, 0, Difficulty.SUPER_HARD, 60,
            goalType = GoalType.BREAK_ICE,
            obstacles = listOf(
                "........", ".#2222#.", ".2....2.", "..2..2..",
                "..2..2..", ".2....2.", ".#2222#.", "........")),
        LevelConfig(36, 34, 7, 4500, Difficulty.SUPER_HARD, 65,
            obstacles = listOf(
                "L......L", ".222222.", "........", "..#..#..",
                "..#..#..", "........", ".222222.", "L......L")),
        LevelConfig(37, 34, 7, 0, Difficulty.SUPER_HARD, 70,
            goalType = GoalType.HEART, heartCount = 18),
        LevelConfig(38, 34, 7, 5000, Difficulty.SUPER_HARD, 75,
            obstacles = listOf(
                "#......#", ".222222.", "..#..#..", "..2..2..",
                "..2..2..", "..#..#..", ".222222.", "#......#")),
        LevelConfig(39, 36, 7, 0, Difficulty.SUPER_HARD, 80,
            goalType = GoalType.BREAK_ICE,
            obstacles = listOf(
                "22222222", "22222222", "22222222", "22222222",
                "22222222", "22222222", "22222222", "22222222")),
        LevelConfig(40, 40, 7, 0, Difficulty.SUPER_HARD, 100,
            goalType = GoalType.HEART, heartCount = 20,
            obstacles = listOf(
                "#......#", ".222222.", "..#..#..", "..2LL2..",
                "..2LL2..", "..#..#..", ".222222.", "#......#")),

        // === МИР 5: после финала ===
        LevelConfig(41, 32, 7, 5500, Difficulty.SUPER_HARD, 105,
            obstacles = listOf(
                "##....##", ".2....2.", "..2222..", "........",
                "........", "..2222..", ".2....2.", "##....##")),
        LevelConfig(42, 34, 7, 0, Difficulty.SUPER_HARD, 110,
            goalType = GoalType.COLLECT_COLOR, goalColor = 3, goalCount = 35,
            obstacles = listOf(
                "........", "L222222L", "........", "..2222..",
                "..2222..", "........", "L222222L", "........")),
        LevelConfig(43, 34, 7, 0, Difficulty.SUPER_HARD, 115,
            goalType = GoalType.BREAK_ICE,
            obstacles = listOf(
                "2......2", ".2....2.", "..2222..", "...22...",
                "...22...", "..2222..", ".2....2.", "2......2")),
        LevelConfig(44, 36, 7, 6000, Difficulty.SUPER_HARD, 120,
            obstacles = listOf(
                "........", "..#..#..", ".2....2.", "..2222..",
                "..2222..", ".2....2.", "..#..#..", "........")),
        LevelConfig(45, 36, 7, 0, Difficulty.SUPER_HARD, 125,
            goalType = GoalType.HEART, heartCount = 22,
            obstacles = listOf(
                "L......L", ".222222.", "..2..2..", "..2..2..",
                "..2..2..", "..2..2..", ".222222.", "L......L")),

        // === МИР 6: хардкор ===
        LevelConfig(46, 32, 7, 6500, Difficulty.SUPER_HARD, 130,
            obstacles = listOf(
                "#......#", ".222222.", "..#..#..", "..2LL2..",
                "..2LL2..", "..#..#..", ".222222.", "#......#")),
        LevelConfig(47, 34, 7, 0, Difficulty.SUPER_HARD, 135,
            goalType = GoalType.COLLECT_COLOR, goalColor = 5, goalCount = 40,
            obstacles = listOf(
                "22222222", "2......2", "2......2", "2..LL..2",
                "2..LL..2", "2......2", "2......2", "22222222")),
        LevelConfig(48, 34, 7, 0, Difficulty.SUPER_HARD, 140,
            goalType = GoalType.BREAK_ICE,
            obstacles = listOf(
                "22222222", "21111112", "21111112", "21111112",
                "21111112", "21111112", "21111112", "22222222")),
        LevelConfig(49, 36, 7, 7000, Difficulty.SUPER_HARD, 150,
            obstacles = listOf(
                "#2....2#", "22....22", "........", "........",
                "........", "........", "22....22", "#2....2#")),
        LevelConfig(50, 40, 7, 0, Difficulty.SUPER_HARD, 200,
            goalType = GoalType.HEART, heartCount = 25,
            obstacles = listOf(
                "#......#", ".222222.", "..#..#..", "..2LL2..",
                "..2LL2..", "..#..#..", ".222222.", "#......#")),

        // === МИР 7: марафон ===
        LevelConfig(51, 34, 7, 7500, Difficulty.SUPER_HARD, 100),
        LevelConfig(52, 34, 7, 0, Difficulty.SUPER_HARD, 110,
            goalType = GoalType.COLLECT_COLOR, goalColor = 0, goalCount = 40),
        LevelConfig(53, 34, 7, 0, Difficulty.SUPER_HARD, 120,
            goalType = GoalType.BREAK_ICE,
            obstacles = listOf(
                "22222222", "2......2", "2.2222.2", "2.2..2.2",
                "2.2..2.2", "2.2222.2", "2......2", "22222222")),
        LevelConfig(54, 36, 7, 8000, Difficulty.SUPER_HARD, 130),
        LevelConfig(55, 36, 7, 0, Difficulty.SUPER_HARD, 140,
            goalType = GoalType.HEART, heartCount = 24),

        // === МИР 8: финальный ===
        LevelConfig(56, 34, 7, 8500, Difficulty.SUPER_HARD, 150,
            obstacles = listOf(
                "L......L", "2222222.", "........", "..2..2..",
                "..2..2..", "........", ".2222222", "L......L")),
        LevelConfig(57, 34, 7, 0, Difficulty.SUPER_HARD, 160,
            goalType = GoalType.COLLECT_COLOR, goalColor = 6, goalCount = 45),
        LevelConfig(58, 36, 7, 0, Difficulty.SUPER_HARD, 170,
            goalType = GoalType.BREAK_ICE,
            obstacles = listOf(
                "11111111", "12222221", "12222221", "12222221",
                "12222221", "12222221", "12222221", "11111111")),
        LevelConfig(59, 38, 7, 9000, Difficulty.SUPER_HARD, 180),
        LevelConfig(60, 45, 7, 0, Difficulty.SUPER_HARD, 300,
            goalType = GoalType.HEART, heartCount = 30,
            obstacles = listOf(
                "#222222#", "22222222", "22LLLL22", "22LLLL22",
                "22LLLL22", "22LLLL22", "22222222", "#222222#"))
    )
}

// ============================================================
// ACHIEVEMENTS
// ============================================================

data class Achievement(
    val id: Int,
    val title: String,
    val description: String,
    val target: Int,
    val emoji: String
)

object Achievements {
    val all: List<Achievement> = listOf(
        Achievement(1, "Первая победа", "Пройди 1 уровень", 1, "🎯"),
        Achievement(2, "Новичок", "Пройди 5 уровней", 5, "🌱"),
        Achievement(3, "Опытный", "Пройди 15 уровней", 15, "⭐"),
        Achievement(4, "Ветеран", "Пройди 30 уровней", 30, "🏅"),
        Achievement(5, "Мастер", "Пройди 50 уровней", 50, "👑"),
        Achievement(6, "Богач", "Накопи 500 монет за всё время", 500, "💰"),
        Achievement(7, "Миллионер", "Накопи 2000 монет за всё время", 2000, "💎"),
        Achievement(8, "Сердцеед", "Собери 50 сердечек", 50, "❤"),
        Achievement(9, "Ледокол", "Разбей 100 льда", 100, "❄"),
        Achievement(10, "Радужный мастер", "Создай 10 радужных камней", 10, "🌈"),
        Achievement(11, "Комбо", "Сделай каскад ×3 (5+ матчей подряд)", 3, "⚡"),
        Achievement(12, "Без бустеров", "Пройди 10 уровней без бустеров", 10, "🚫")
    )
}

// ============================================================
// PROGRESS
// ============================================================

class ProgressStore(context: Context) {
    private val prefs = context.getSharedPreferences("match3_progress", Context.MODE_PRIVATE)
    var unlockedLevel by mutableIntStateOf(prefs.getInt("unlocked", 1))
    var coins by mutableIntStateOf(prefs.getInt("coins", 100))
    var bestScores by mutableStateOf(loadScores())
    var lastDailyBonusTime by mutableStateOf(prefs.getLong("dailyTime", 0L))
    var dailyBonusStreak by mutableIntStateOf(prefs.getInt("dailyStreak", 0))

    // Достижения — прогресс по каждому
    var totalWins by mutableIntStateOf(prefs.getInt("totalWins", 0))
    var totalCoinsEarned by mutableIntStateOf(prefs.getInt("totalCoinsEarned", 0))
    var totalHeartsCollected by mutableIntStateOf(prefs.getInt("totalHearts", 0))
    var totalIceBroken by mutableIntStateOf(prefs.getInt("totalIce", 0))
    var totalRainbows by mutableIntStateOf(prefs.getInt("totalRainbows", 0))
    var bestCascade by mutableIntStateOf(prefs.getInt("bestCascade", 0))
    var noBoosterWins by mutableIntStateOf(prefs.getInt("noBoosterWins", 0))
    var unlockedAchievements by mutableStateOf(loadAchievements())

    // Испытание дня
    var lastDailyChallengeDate by mutableStateOf(prefs.getString("dailyChallengeDate", "") ?: "")

    private fun loadScores(): Map<Int, Int> {
        val s = prefs.getString("scores", "") ?: ""
        return s.split(",").filter { it.isNotBlank() }.mapNotNull { part ->
            val kv = part.split(":")
            if (kv.size == 2) {
                val k = kv[0].toIntOrNull(); val v = kv[1].toIntOrNull()
                if (k != null && v != null) k to v else null
            } else null
        }.toMap()
    }

    private fun loadAchievements(): Set<Int> {
        val s = prefs.getString("achievements", "") ?: ""
        return s.split(",").filter { it.isNotBlank() }.mapNotNull { it.toIntOrNull() }.toSet()
    }

    fun recordWin(levelId: Int, score: Int, rewardCoins: Int) {
        val prev = bestScores[levelId] ?: 0
        if (score > prev) bestScores = bestScores + (levelId to score)
        coins += rewardCoins
        totalCoinsEarned += rewardCoins
        totalWins += 1
        if (levelId + 1 > unlockedLevel) unlockedLevel = levelId + 1
        save()
    }

    fun addCoins(amount: Int) {
        coins += amount
        totalCoinsEarned += amount
        save()
    }

    fun spendCoins(amount: Int): Boolean {
        if (coins < amount) return false
        coins -= amount
        save()
        return true
    }

    fun canClaimDaily(): Boolean {
        val now = System.currentTimeMillis()
        return now - lastDailyBonusTime > 20L * 60 * 60 * 1000
    }

    fun nextDailyBonusAmount(): Int {
        val day = (dailyBonusStreak % 7) + 1
        return when (day) {
            1 -> 20; 2 -> 30; 3 -> 50; 4 -> 75; 5 -> 100; 6 -> 150; else -> 300
        }
    }

    fun claimDaily(): Int {
        val amount = nextDailyBonusAmount()
        coins += amount
        totalCoinsEarned += amount
        lastDailyBonusTime = System.currentTimeMillis()
        dailyBonusStreak = (dailyBonusStreak + 1) % 7
        save()
        return amount
    }

    // === Ежедневное испытание ===
    fun todayDateKey(): String {
        val cal = Calendar.getInstance()
        return "%04d-%02d-%02d".format(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
    }

    fun canPlayDailyChallenge(): Boolean = lastDailyChallengeDate != todayDateKey()

    fun markDailyChallengeCompleted() {
        lastDailyChallengeDate = todayDateKey()
        save()
    }

    fun dailyChallengeSeed(): Long {
        val cal = Calendar.getInstance()
        return cal.get(Calendar.YEAR) * 10000L +
                (cal.get(Calendar.MONTH) + 1) * 100L +
                cal.get(Calendar.DAY_OF_MONTH)
    }

    // === Достижения ===
    fun checkAchievements(): List<Achievement> {
        val newlyUnlocked = mutableListOf<Achievement>()
        for (a in Achievements.all) {
            if (a.id in unlockedAchievements) continue
            val progress = progressFor(a.id)
            if (progress >= a.target) {
                unlockedAchievements = unlockedAchievements + a.id
                newlyUnlocked.add(a)
            }
        }
        if (newlyUnlocked.isNotEmpty()) save()
        return newlyUnlocked
    }

    fun progressFor(achievementId: Int): Int = when (achievementId) {
        1, 2, 3, 4, 5 -> totalWins
        6, 7 -> totalCoinsEarned
        8 -> totalHeartsCollected
        9 -> totalIceBroken
        10 -> totalRainbows
        11 -> bestCascade
        12 -> noBoosterWins
        else -> 0
    }

    fun recordHeartsCollected(n: Int) { totalHeartsCollected += n; save() }
    fun recordIceBroken(n: Int) { totalIceBroken += n; save() }
    fun recordRainbowCreated() { totalRainbows += 1; save() }
    fun recordCascade(level: Int) {
        if (level > bestCascade) { bestCascade = level; save() }
    }
    fun recordNoBoosterWin() { noBoosterWins += 1; save() }

    private fun save() {
        val scoresStr = bestScores.entries.joinToString(",") { "${it.key}:${it.value}" }
        val achStr = unlockedAchievements.joinToString(",") { it.toString() }
        prefs.edit()
            .putInt("unlocked", unlockedLevel)
            .putInt("coins", coins)
            .putString("scores", scoresStr)
            .putLong("dailyTime", lastDailyBonusTime)
            .putInt("dailyStreak", dailyBonusStreak)
            .putInt("totalWins", totalWins)
            .putInt("totalCoinsEarned", totalCoinsEarned)
            .putInt("totalHearts", totalHeartsCollected)
            .putInt("totalIce", totalIceBroken)
            .putInt("totalRainbows", totalRainbows)
            .putInt("bestCascade", bestCascade)
            .putInt("noBoosterWins", noBoosterWins)
            .putString("achievements", achStr)
            .putString("dailyChallengeDate", lastDailyChallengeDate)
            .apply()
    }
}

// ============================================================
// ADS
// ============================================================

interface AdsController {
    fun showRewarded(onReward: () -> Unit, onFailed: (String) -> Unit = {})
}
class NoOpAdsController : AdsController {
    override fun showRewarded(onReward: () -> Unit, onFailed: (String) -> Unit) { onReward() }
}

// ============================================================
// BOOSTERS
// ============================================================

enum class BoosterType(val emoji: String, val label: String) {
    BOMB("💣", "Бомба"),
    ROCKET_H("➡️", "Ракета →"),
    ROCKET_V("⬇️", "Ракета ↓"),
    SHUFFLE("🔀", "Перемешать")
}

// ============================================================
// GAME ENGINE
// ============================================================

enum class GamePhase { PLAYING, WON, LOST }

class GameEngine(
    val level: LevelConfig,
    private val rng: Random = Random.Default,
    val isDailyChallenge: Boolean = false
) {
    private var idCounter = 0
    private fun nextId(): Int = ++idCounter

    private val initial: LevelState = Board.generateWithObstacles(
        level.types, level.obstacles, level.heartCount, ::nextId, rng
    )

    var grid by mutableStateOf(initial.grid)
        private set
    var iceGrid by mutableStateOf(initial.iceGrid)
        private set
    var movesLeft by mutableIntStateOf(level.moves)
        private set
    var score by mutableIntStateOf(0)
        private set
    var goalProgress by mutableIntStateOf(0)
        private set
    val totalIceCount: Int = Board.totalIce(initial.iceGrid)
    var selected by mutableStateOf<Pair<Int, Int>?>(null)
        private set
    var phase by mutableStateOf(GamePhase.PLAYING)
        private set
    var isAnimating by mutableStateOf(false)
        private set
    var activeBooster by mutableStateOf<BoosterType?>(null)
        private set

    // Для достижений
    var heartsThisSession by mutableIntStateOf(0)
        private set
    var iceBrokenThisSession by mutableIntStateOf(0)
        private set
    var rainbowsCreatedThisSession by mutableIntStateOf(0)
        private set
    var maxCascadeThisSession by mutableIntStateOf(0)
        private set
    var usedBooster by mutableStateOf(false)
        private set

    suspend fun trySwap(a: Pair<Int, Int>, b: Pair<Int, Int>) {
        if (isAnimating || phase != GamePhase.PLAYING) return
        if (!Board.canSwap(grid, a, b)) return
        isAnimating = true
        selected = null
        try {
            val tileA = grid[a.first][a.second]
            val tileB = grid[b.first][b.second]

            if (tileA.rainbow || tileB.rainbow) {
                movesLeft -= 1
                val rainbowPos = if (tileA.rainbow) a else b
                val targetPos = if (tileA.rainbow) b else a
                val targetType = grid[targetPos.first][targetPos.second].type
                activateRainbow(rainbowPos, targetType)
                return
            }

            grid = Board.swap(grid, a, b)
            delay(200)
            if (Board.findMatches(grid).isEmpty()) {
                grid = Board.swap(grid, a, b)
                delay(200)
                return
            }
            movesLeft -= 1
            resolveCascades()
            checkEnd()
        } finally { isAnimating = false }
    }

    private suspend fun activateRainbow(pos: Pair<Int, Int>, targetType: Int) {
        val affected = mutableSetOf<Pair<Int, Int>>()
        for (r in 0 until Board.SIZE) for (c in 0 until Board.SIZE) {
            val t = grid[r][c]
            if (!t.stone && (t.type == targetType || (r to c) == pos)) {
                affected.add(r to c)
            }
        }
        if (affected.isEmpty()) return

        val newIce = iceGrid.map { it.toMutableList() }
        var iceBroken = 0
        for ((rr, cc) in affected) if (newIce[rr][cc] > 0) { newIce[rr][cc] = 0; iceBroken++ }
        if (iceBroken > 0) {
            iceGrid = newIce.map { it.toList() }
            iceBrokenThisSession += iceBroken
            if (level.goalType == GoalType.BREAK_ICE) goalProgress += iceBroken
        }

        val heartsCleared = affected.count { (rr, cc) -> grid[rr][cc].hasHeart }
        if (heartsCleared > 0) {
            heartsThisSession += heartsCleared
            if (level.goalType == GoalType.HEART) goalProgress += heartsCleared
        }

        if (level.goalType == GoalType.COLLECT_COLOR && level.goalColor != null) {
            val count = affected.count { (rr, cc) -> grid[rr][cc].type == level.goalColor }
            goalProgress += count
        }

        grid = Board.markMatching(grid, affected)
        delay(280)
        score += affected.size * 25
        grid = Board.dropAndRefill(grid, affected, level.types, ::nextId, rng)
        delay(320)
        resolveCascades()
        checkEnd()
    }

    private suspend fun resolveCascades() {
        var safety = 0
        var cascadeLevel = 0
        while (safety++ < 30) {
            val matches = Board.findMatches(grid)
            if (matches.isEmpty()) break

            cascadeLevel++
            if (cascadeLevel > maxCascadeThisSession) maxCascadeThisSession = cascadeLevel
            val multiplier = min(3f, 1f + (cascadeLevel - 1) * 0.5f)

            val rainbowAt = Board.findFiveInRow(grid)
            if (rainbowAt != null) rainbowsCreatedThisSession += 1

            val newIce = iceGrid.map { it.toMutableList() }
            var iceBroken = 0
            for ((r, c) in matches) {
                if (newIce[r][c] > 0) { newIce[r][c] -= 1; iceBroken++ }
            }
            if (iceBroken > 0) {
                iceGrid = newIce.map { it.toList() }
                iceBrokenThisSession += iceBroken
                if (level.goalType == GoalType.BREAK_ICE) goalProgress += iceBroken
            }

            val heartsCleared = matches.count { (r, c) ->
                grid[r][c].hasHeart && newIce[r][c] == 0
            }
            if (heartsCleared > 0) {
                heartsThisSession += heartsCleared
                if (level.goalType == GoalType.HEART) goalProgress += heartsCleared
            }

            if (level.goalType == GoalType.COLLECT_COLOR && level.goalColor != null) {
                val target = level.goalColor
                val count = matches.count { (r, c) -> grid[r][c].type == target }
                goalProgress += count
            }

            val unlocks = mutableSetOf<Pair<Int, Int>>()
            for ((r, c) in matches) {
                for (d in listOf(0 to 1, 0 to -1, 1 to 0, -1 to 0)) {
                    val nr = r + d.first; val nc = c + d.second
                    if (nr in 0 until Board.SIZE && nc in 0 until Board.SIZE) {
                        if (grid[nr][nc].locked) unlocks.add(nr to nc)
                    }
                }
            }

            val basePoints = matches.size * 10 + iceBroken * 15 + unlocks.size * 5 + heartsCleared * 20
            score += (basePoints * multiplier).roundToInt()

            grid = Board.markMatching(grid, matches)
            if (unlocks.isNotEmpty()) grid = Board.unlockCells(grid, unlocks)
            delay(240)

            val toRemove = if (rainbowAt != null && rainbowAt in matches) {
                matches - rainbowAt
            } else matches

            grid = Board.dropAndRefill(grid, toRemove, level.types, ::nextId, rng)

            if (rainbowAt != null && rainbowAt in matches) {
                val g = grid.map { it.toMutableList() }.toMutableList()
                val existing = g[rainbowAt.first][rainbowAt.second]
                g[rainbowAt.first][rainbowAt.second] = existing.copy(
                    rainbow = true, matching = false
                )
                grid = g.map { it.toList() }
            }

            delay(280)

            var reshuffleSafety = 0
            while (!Board.hasAnyMove(grid) && reshuffleSafety < 8) {
                grid = Board.reshuffle(grid, level.types, ::nextId, rng)
                reshuffleSafety++
            }
        }
    }

    fun tapTile(r: Int, c: Int, scope: CoroutineScope) {
        if (phase != GamePhase.PLAYING || isAnimating) return
        if (activeBooster != null && activeBooster != BoosterType.SHUFFLE) {
            val booster = activeBooster!!
            activeBooster = null
            usedBooster = true
            scope.launch { applyBooster(booster, r, c) }
            return
        }
        val sel = selected
        if (sel == null) { selected = r to c; return }
        if (sel == (r to c)) { selected = null; return }
        if (!Board.canSwap(grid, sel, r to c)) { selected = r to c; return }
        val a = sel
        selected = null
        scope.launch { trySwap(a, r to c) }
    }

    fun swipe(from: Pair<Int, Int>, to: Pair<Int, Int>, scope: CoroutineScope) {
        if (phase != GamePhase.PLAYING || isAnimating) return
        if (activeBooster != null) return
        if (!Board.canSwap(grid, from, to)) return
        selected = null
        scope.launch { trySwap(from, to) }
    }

    fun requestBooster(type: BoosterType, scope: CoroutineScope) {
        if (phase != GamePhase.PLAYING || isAnimating) return
        usedBooster = true
        if (type == BoosterType.SHUFFLE) scope.launch { applyShuffle() }
        else activeBooster = type
    }

    fun cancelBooster() { activeBooster = null }

    private suspend fun applyShuffle() {
        isAnimating = true
        try {
            grid = Board.reshuffle(grid, level.types, ::nextId, rng)
            delay(300)
        } finally { isAnimating = false }
    }

    private suspend fun applyBooster(type: BoosterType, r: Int, c: Int) {
        isAnimating = true
        try {
            val affected: Set<Pair<Int, Int>> = when (type) {
                BoosterType.BOMB -> {
                    val cells = mutableSetOf<Pair<Int, Int>>()
                    for (rr in (r - 1)..(r + 1)) for (cc in (c - 1)..(c + 1)) {
                        if (rr in 0 until Board.SIZE && cc in 0 until Board.SIZE) cells.add(rr to cc)
                    }
                    cells
                }
                BoosterType.ROCKET_H -> (0 until Board.SIZE).map { r to it }.toSet()
                BoosterType.ROCKET_V -> (0 until Board.SIZE).map { it to c }.toSet()
                BoosterType.SHUFFLE -> emptySet()
            }
            if (affected.isEmpty()) return

            val newIce = iceGrid.map { it.toMutableList() }
            var iceBroken = 0
            for ((rr, cc) in affected) if (newIce[rr][cc] > 0) { newIce[rr][cc] = 0; iceBroken++ }
            if (iceBroken > 0) {
                iceGrid = newIce.map { it.toList() }
                iceBrokenThisSession += iceBroken
                if (level.goalType == GoalType.BREAK_ICE) goalProgress += iceBroken
            }

            val heartsCleared = affected.count { (rr, cc) -> grid[rr][cc].hasHeart && !grid[rr][cc].stone }
            if (heartsCleared > 0) {
                heartsThisSession += heartsCleared
                if (level.goalType == GoalType.HEART) goalProgress += heartsCleared
            }

            if (level.goalType == GoalType.COLLECT_COLOR && level.goalColor != null) {
                val count = affected.count { (rr, cc) ->
                    val t = grid[rr][cc]; !t.stone && t.type == level.goalColor
                }
                goalProgress += count
            }

            grid = Board.markMatching(grid, affected)
            delay(240)
            score += affected.size * 15 + heartsCleared * 20
            grid = Board.dropAndRefill(grid, affected, level.types, ::nextId, rng)
            delay(280)
            resolveCascades()
            checkEnd()
        } finally { isAnimating = false }
    }

    private fun checkEnd() {
        val reached = when (level.goalType) {
            GoalType.SCORE -> score >= level.targetScore
            GoalType.COLLECT_COLOR -> goalProgress >= level.goalCount
            GoalType.BREAK_ICE -> Board.totalIce(iceGrid) == 0
            GoalType.HEART -> goalProgress >= level.heartCount
        }
        if (reached) phase = GamePhase.WON
        else if (movesLeft <= 0) phase = GamePhase.LOST
    }

    fun continueWithExtraMoves(extra: Int) {
        if (phase != GamePhase.LOST) return
        movesLeft += extra
        phase = GamePhase.PLAYING
    }

    fun retry() {
        idCounter = 0
        val s = Board.generateWithObstacles(
            level.types, level.obstacles, level.heartCount, ::nextId, rng
        )
        grid = s.grid
        iceGrid = s.iceGrid
        movesLeft = level.moves
        score = 0
        goalProgress = 0
        selected = null
        phase = GamePhase.PLAYING
        isAnimating = false
        activeBooster = null
        usedBooster = false
        heartsThisSession = 0
        iceBrokenThisSession = 0
        rainbowsCreatedThisSession = 0
        maxCascadeThisSession = 0
    }
}

// ============================================================
// APP ROOT
// ============================================================

enum class Screen { MAP, GAME, DAILY, ACHIEVEMENTS }

@Composable
fun AppRoot() {
    val context = LocalContext.current
    val progress = remember { ProgressStore(context) }
    val ads: AdsController = remember { NoOpAdsController() }
    var screen by remember { mutableStateOf(Screen.MAP) }
    var currentLevelId by remember { mutableStateOf<Int?>(null) }
    var showDaily by remember { mutableStateOf(false) }
    var newAchievements by remember { mutableStateOf<List<Achievement>>(emptyList()) }

    GradientBg {
        when (screen) {
            Screen.MAP -> {
                LevelMapScreen(
                    levels = Levels.all,
                    progress = progress,
                    onPlay = { currentLevelId = it; screen = Screen.GAME },
                    canClaimDaily = progress.canClaimDaily(),
                    onClaimDaily = { showDaily = true },
                    canPlayDailyChallenge = progress.canPlayDailyChallenge(),
                    onPlayDailyChallenge = { screen = Screen.DAILY },
                    onOpenAchievements = { screen = Screen.ACHIEVEMENTS }
                )
            }
            Screen.GAME -> {
                val lvlId = currentLevelId
                val level = Levels.all.firstOrNull { it.id == lvlId } ?: Levels.all.first()
                key(lvlId) {
                    GameScreen(
                        level = level,
                        progress = progress,
                        ads = ads,
                        onExitToMap = { screen = Screen.MAP },
                        onNext = {
                            if (lvlId != null && lvlId < Levels.all.size) currentLevelId = lvlId + 1
                            else screen = Screen.MAP
                        },
                        onNewAchievements = { newAchievements = it }
                    )
                }
            }
            Screen.DAILY -> {
                DailyChallengeScreen(
                    progress = progress,
                    ads = ads,
                    onExit = { screen = Screen.MAP }
                )
            }
            Screen.ACHIEVEMENTS -> {
                AchievementsScreen(
                    progress = progress,
                    onBack = { screen = Screen.MAP }
                )
            }
        }

        if (showDaily && progress.canClaimDaily()) {
            DailyBonusDialog(
                progress = progress,
                onClaim = {
                    progress.claimDaily()
                    showDaily = false
                },
                onSkip = { showDaily = false }
            )
        }

        if (newAchievements.isNotEmpty()) {
            NewAchievementsDialog(
                achievements = newAchievements,
                onClose = { newAchievements = emptyList() }
            )
        }
    }
}

// ============================================================
// LEVEL MAP
// ============================================================

@Composable
fun LevelMapScreen(
    levels: List<LevelConfig>,
    progress: ProgressStore,
    onPlay: (Int) -> Unit,
    canClaimDaily: Boolean,
    onClaimDaily: () -> Unit,
    canPlayDailyChallenge: Boolean,
    onPlayDailyChallenge: () -> Unit,
    onOpenAchievements: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("MATCH 3", fontSize = 28.sp, fontWeight = FontWeight.Black,
                color = Accent, letterSpacing = 4.sp)
            Box(
                modifier = Modifier.clip(RoundedCornerShape(20.dp))
                    .background(CardBg).clickable { onOpenAchievements() }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text("🏆", fontSize = 20.sp)
            }
        }
        Spacer(Modifier.height(12.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.clip(RoundedCornerShape(30.dp)).background(CardBg)
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🪙", fontSize = 22.sp)
                Spacer(Modifier.size(8.dp))
                Text("${progress.coins}", fontSize = 20.sp,
                    fontWeight = FontWeight.Bold, color = Gold)
            }
            if (canClaimDaily) {
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(30.dp))
                        .background(Gold.copy(alpha = 0.85f))
                        .clickable { onClaimDaily() }
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Text("🎁", fontSize = 20.sp,
                        fontWeight = FontWeight.Bold, color = Color(0xFF3E2723))
                }
            }
            Box(
                modifier = Modifier.clip(RoundedCornerShape(30.dp))
                    .background(if (canPlayDailyChallenge) Accent.copy(alpha = 0.85f) else CardBg)
                    .clickable(enabled = canPlayDailyChallenge) { onPlayDailyChallenge() }
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    if (canPlayDailyChallenge) "⚔ Испытание" else "⚔ ✓",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (canPlayDailyChallenge) Color(0xFF3E2723) else Color.White.copy(alpha = 0.5f)
                )
            }
        }
        Spacer(Modifier.height(20.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(levels) { level ->
                val unlocked = level.id <= progress.unlockedLevel
                val best = progress.bestScores[level.id] ?: 0
                LevelCard(level, unlocked, best) { if (unlocked) onPlay(level.id) }
            }
        }
    }
}

@Composable
fun LevelCard(level: LevelConfig, unlocked: Boolean, bestScore: Int, onClick: () -> Unit) {
    val gradient = if (unlocked) {
        val c = when (level.difficulty) {
            Difficulty.EASY -> listOf(Color(0xFF66BB6A), Color(0xFF2E7D32))
            Difficulty.NORMAL -> listOf(Color(0xFF42A5F5), Color(0xFF1565C0))
            Difficulty.HARD -> listOf(Color(0xFFFFA726), Color(0xFFE65100))
            Difficulty.SUPER_HARD -> listOf(Color(0xFFEF5350), Color(0xFFB71C1C))
        }
        Brush.linearGradient(c)
    } else Brush.linearGradient(listOf(Color(0xFF424242), Color(0xFF212121)))

    val stars = when {
        !unlocked -> 0
        bestScore >= level.targetScore * 3 / 2 -> 3
        bestScore >= level.targetScore * 5 / 4 -> 2
        bestScore > 0 -> 1
        else -> 0
    }

    Box(
        modifier = Modifier.fillMaxWidth().aspectRatio(1f)
            .clip(RoundedCornerShape(18.dp)).background(gradient)
            .clickable(enabled = unlocked) { onClick() }.padding(8.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (unlocked) {
                Text("${level.id}", fontSize = 30.sp, color = Color.White, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(2.dp))
                Text("★".repeat(stars) + "☆".repeat(3 - stars), fontSize = 13.sp, color = Gold)

                when {
                    level.goalType == GoalType.COLLECT_COLOR && level.goalColor != null -> {
                        Spacer(Modifier.height(2.dp))
                        Text("◆ ${level.goalCount}", fontSize = 11.sp,
                            color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    level.goalType == GoalType.BREAK_ICE -> {
                        Spacer(Modifier.height(2.dp))
                        Text("❄ лёд", fontSize = 11.sp,
                            color = Color(0xFFB3E5FC), fontWeight = FontWeight.Bold)
                    }
                    level.goalType == GoalType.HEART -> {
                        Spacer(Modifier.height(2.dp))
                        Text("❤ ${level.heartCount}", fontSize = 11.sp,
                            color = Color(0xFFFF8A80), fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                Text("🔒", fontSize = 34.sp)
            }
        }
    }
}

// ============================================================
// DAILY BONUS DIALOG
// ============================================================

@Composable
fun DailyBonusDialog(progress: ProgressStore, onClaim: () -> Unit, onSkip: () -> Unit) {
    val amount = progress.nextDailyBonusAmount()
    AlertDialog(
        onDismissRequest = onSkip,
        title = { Text("🎁 Ежедневный бонус", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Заходи каждый день и получай всё больше!",
                    fontSize = 13.sp, color = Color.White.copy(alpha = 0.7f))
                Spacer(Modifier.height(14.dp))
                Text("🪙", fontSize = 44.sp)
                Spacer(Modifier.height(8.dp))
                Text("+$amount монет", fontSize = 26.sp,
                    fontWeight = FontWeight.Bold, color = Gold)
                Spacer(Modifier.height(6.dp))
                Text("Серия: ${progress.dailyBonusStreak % 7 + 1} / 7",
                    fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f))
            }
        },
        confirmButton = {
            Button(onClick = onClaim) { Text("Забрать!") }
        }
    )
}

// ============================================================
// ACHIEVEMENTS SCREEN
// ============================================================

@Composable
fun AchievementsScreen(progress: ProgressStore, onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("🏆 Достижения", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Accent)
            Box(
                modifier = Modifier.clip(RoundedCornerShape(20.dp))
                    .background(CardBg).clickable { onBack() }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text("← Назад", color = Color.White, fontSize = 14.sp)
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "${progress.unlockedAchievements.size} / ${Achievements.all.size} получено",
            fontSize = 13.sp, color = Color.White.copy(alpha = 0.6f)
        )
        Spacer(Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(Achievements.all) { a ->
                val unlocked = a.id in progress.unlockedAchievements
                val current = progress.progressFor(a.id).coerceAtMost(a.target)
                AchievementRow(a, unlocked, current)
            }
        }
    }
}

@Composable
fun AchievementRow(a: Achievement, unlocked: Boolean, current: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (unlocked) CardBg else CardBg.copy(alpha = 0.5f))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (unlocked) a.emoji else "🔒",
            fontSize = 28.sp
        )
        Spacer(Modifier.size(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                a.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = if (unlocked) Color.White else Color.White.copy(alpha = 0.5f)
            )
            Text(
                a.description,
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.6f)
            )
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { current.toFloat() / a.target },
                modifier = Modifier.fillMaxWidth().height(5.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = if (unlocked) Gold else Accent.copy(alpha = 0.5f),
                trackColor = Color.White.copy(alpha = 0.1f)
            )
            Spacer(Modifier.height(3.dp))
            Text(
                "$current / ${a.target}",
                fontSize = 10.sp,
                color = Color.White.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
fun NewAchievementsDialog(achievements: List<Achievement>, onClose: () -> Unit) {
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("🏆 Новое достижение!", fontWeight = FontWeight.Bold) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()) {
                achievements.forEach { a ->
                    Spacer(Modifier.height(8.dp))
                    Text(a.emoji, fontSize = 42.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(a.title, fontSize = 20.sp,
                        fontWeight = FontWeight.Bold, color = Gold)
                    Text(a.description, fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.7f))
                    Spacer(Modifier.height(8.dp))
                }
            }
        },
        confirmButton = {
            Button(onClick = onClose) { Text("Круто!") }
        }
    )
}

// ============================================================
// DAILY CHALLENGE SCREEN
// ============================================================

@Composable
fun DailyChallengeScreen(
    progress: ProgressStore,
    ads: AdsController,
    onExit: () -> Unit
) {
    // Генерируем уровень с сидом по дате
    val seed = progress.dailyChallengeSeed()
    val rng = remember { Random(seed) }
    val dailyLevel = remember {
        val difficulty = listOf(Difficulty.NORMAL, Difficulty.HARD, Difficulty.SUPER_HARD).random(rng)
        val goalType = listOf(GoalType.SCORE, GoalType.COLLECT_COLOR, GoalType.HEART).random(rng)
        val types = 6 + rng.nextInt(2)
        val moves = 22 + rng.nextInt(8)
        val goalColor = if (goalType == GoalType.COLLECT_COLOR) rng.nextInt(types) else null
        val goalCount = if (goalType == GoalType.COLLECT_COLOR) 20 + rng.nextInt(15) else 0
        val heartCount = if (goalType == GoalType.HEART) 10 + rng.nextInt(8) else 0
        LevelConfig(
            id = 1000,
            moves = moves,
            types = types,
            targetScore = 2000 + rng.nextInt(1500),
            difficulty = difficulty,
            rewardCoins = 50 + rng.nextInt(50),
            goalType = goalType,
            goalColor = goalColor,
            goalCount = goalCount,
            heartCount = heartCount
        )
    }

    var completed by remember { mutableStateOf(false) }

    GameScreen(
        level = dailyLevel,
        progress = progress,
        ads = ads,
        onExitToMap = onExit,
        onNext = onExit,
        onNewAchievements = {},
        isDailyChallenge = true,
        onDailyCompleted = {
            if (!completed) {
                progress.markDailyChallengeCompleted()
                completed = true
            }
            onExit()
        }
    )
}

// ============================================================
// GAME SCREEN
// ============================================================

@Composable
fun GameScreen(
    level: LevelConfig,
    progress: ProgressStore,
    ads: AdsController,
    onExitToMap: () -> Unit,
    onNext: () -> Unit,
    onNewAchievements: (List<Achievement>) -> Unit,
    isDailyChallenge: Boolean = false,
    onDailyCompleted: () -> Unit = {}
) {
    val engine = remember { GameEngine(level, isDailyChallenge = isDailyChallenge) }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    var boosterToBuy by remember { mutableStateOf<BoosterType?>(null) }
    var showPause by remember { mutableStateOf(false) }
    var hint by remember { mutableStateOf<Pair<Pair<Int, Int>, Pair<Int, Int>>?>(null) }

    BackHandler(enabled = engine.phase == GamePhase.PLAYING && !showPause) { showPause = true }
    BackHandler(enabled = showPause) { showPause = false }

    LaunchedEffect(engine.grid, engine.isAnimating, engine.phase, engine.activeBooster, showPause) {
        hint = null
        if (engine.phase != GamePhase.PLAYING) return@LaunchedEffect
        if (engine.isAnimating) return@LaunchedEffect
        if (engine.activeBooster != null) return@LaunchedEffect
        if (showPause) return@LaunchedEffect
        delay(4000)
        if (!engine.isAnimating && engine.phase == GamePhase.PLAYING
            && engine.activeBooster == null && !showPause) {
            hint = Board.findHint(engine.grid)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ChipButton("⏸") { showPause = true }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    if (isDailyChallenge) "⚔ Испытание дня" else "Уровень ${level.id}",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White, fontWeight = FontWeight.Bold
                )
                Text(level.difficulty.label, fontSize = 11.sp, color = Accent)
            }
            ChipButton("🪙 ${progress.coins}") { }
        }

        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                .background(CardBg).padding(12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatBlock("Ходы", "${engine.movesLeft}", Color.White)
            StatBlock("Очки", "${engine.score}", Color.White)
            GoalStat(level, engine)
        }

        Spacer(Modifier.height(8.dp))
        val progressValue = when (level.goalType) {
            GoalType.SCORE -> (engine.score.toFloat() / level.targetScore)
            GoalType.COLLECT_COLOR -> (engine.goalProgress.toFloat() / level.goalCount)
            GoalType.BREAK_ICE -> {
                if (engine.totalIceCount == 0) 1f
                else 1f - (Board.totalIce(engine.iceGrid).toFloat() / engine.totalIceCount)
            }
            GoalType.HEART -> (engine.goalProgress.toFloat() / level.heartCount)
        }.coerceIn(0f, 1f)

        LinearProgressIndicator(
            progress = { progressValue },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
            color = Accent, trackColor = Color.White.copy(alpha = 0.1f)
        )

        Spacer(Modifier.height(14.dp))

        if (engine.activeBooster != null) {
            Box(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                    .background(Accent.copy(alpha = 0.25f))
                    .clickable { engine.cancelBooster() }.padding(10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("${engine.activeBooster!!.emoji} Выбери клетку (тап — отмена)",
                    color = Accent, fontSize = 13.sp, textAlign = TextAlign.Center)
            }
            Spacer(Modifier.height(8.dp))
        }

        BoardView(
            grid = engine.grid,
            iceGrid = engine.iceGrid,
            selected = engine.selected,
            hint = hint,
            onTap = { r, c ->
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                engine.tapTile(r, c, scope)
            },
            onSwipe = { a, b ->
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                engine.swipe(a, b, scope)
            }
        )

        Spacer(Modifier.weight(1f))
        Text("БУСТЕРЫ", fontSize = 11.sp, color = Color.White.copy(alpha = 0.5f), letterSpacing = 2.sp)
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for (bt in BoosterType.entries) {
                BoosterButton(
                    type = bt,
                    enabled = engine.phase == GamePhase.PLAYING && !engine.isAnimating,
                    modifier = Modifier.weight(1f),
                    onClick = { boosterToBuy = bt }
                )
            }
        }
        Spacer(Modifier.height(8.dp))
    }

    if (showPause) {
        PauseDialog(level, { showPause = false }, { engine.retry(); showPause = false }, onExitToMap)
    }

    boosterToBuy?.let { booster ->
        BoosterBuyDialog(
            booster = booster, coins = progress.coins, cost = 50,
            onSpendCoins = {
                if (progress.spendCoins(50)) { engine.requestBooster(booster, scope); boosterToBuy = null }
            },
            onWatchAd = {
                ads.showRewarded(onReward = { engine.requestBooster(booster, scope); boosterToBuy = null })
            },
            onCancel = { boosterToBuy = null }
        )
    }

    when (engine.phase) {
        GamePhase.WON -> {
            LaunchedEffect(Unit) {
                // Учёт для достижений
                if (engine.heartsThisSession > 0) progress.recordHeartsCollected(engine.heartsThisSession)
                if (engine.iceBrokenThisSession > 0) progress.recordIceBroken(engine.iceBrokenThisSession)
                repeat(engine.rainbowsCreatedThisSession) { progress.recordRainbowCreated() }
                if (engine.maxCascadeThisSession > 0) progress.recordCascade(engine.maxCascadeThisSession)
                if (!engine.usedBooster) progress.recordNoBoosterWin()

                if (isDailyChallenge) {
                    // Награда за испытание без записи в unlockedLevel
                    progress.addCoins(level.rewardCoins)
                } else {
                    progress.recordWin(level.id, engine.score, level.rewardCoins)
                }

                val newAch = progress.checkAchievements()
                if (newAch.isNotEmpty()) onNewAchievements(newAch)
            }
            WinDialog(
                reward = level.rewardCoins,
                score = engine.score,
                target = level.targetScore,
                hasNext = !isDailyChallenge && level.id < Levels.all.size,
                onNext = {
                    if (isDailyChallenge) onDailyCompleted() else onNext()
                },
                onMap = {
                    if (isDailyChallenge) onDailyCompleted() else onExitToMap()
                },
                isDaily = isDailyChallenge
            )
        }
        GamePhase.LOST -> {
            AlertDialog(
                onDismissRequest = {},
                title = { Text("Ходы закончились") },
                text = { Text("Можно продолжить за монеты, посмотреть рекламу или переиграть.") },
                confirmButton = {
                    Button(
                        onClick = {
                            if (progress.spendCoins(level.continueCostCoins))
                                engine.continueWithExtraMoves(level.continueExtraMoves)
                        },
                        enabled = progress.coins >= level.continueCostCoins
                    ) { Text("+${level.continueExtraMoves} за ${level.continueCostCoins} монет") }
                },
                dismissButton = {
                    Column(horizontalAlignment = Alignment.End) {
                        TextButton({
                            ads.showRewarded(onReward = {
                                engine.continueWithExtraMoves(level.continueExtraMoves)
                            })
                        }) { Text("Реклама: +${level.continueExtraMoves} ходов") }
                        TextButton({ engine.retry() }) { Text("Заново") }
                        TextButton(onExitToMap) { Text("К карте") }
                    }
                }
            )
        }
        GamePhase.PLAYING -> Unit
    }
}

@Composable
fun GoalStat(level: LevelConfig, engine: GameEngine) {
    when (level.goalType) {
        GoalType.SCORE -> StatBlock("Цель", "${engine.score} / ${level.targetScore}", Color.White)
        GoalType.COLLECT_COLOR -> {
            val idx = level.goalColor ?: 0
            val tt = TileType.fromOrdinal(idx)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Цель", fontSize = 11.sp, color = Color.White.copy(alpha = 0.6f))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Canvas(modifier = Modifier.size(20.dp)) {
                        val cx = size.width / 2f; val cy = size.height / 2f
                        val r = min(size.width, size.height) / 2f * 0.94f
                        val p = buildPolygon(cx, cy, r, tt.sides, tt.rotation)
                        drawPath(p, brush = Brush.verticalGradient(listOf(tt.color, tt.shadow)))
                    }
                    Spacer(Modifier.size(4.dp))
                    Text("${engine.goalProgress} / ${level.goalCount}",
                        fontSize = 18.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
        GoalType.BREAK_ICE -> {
            val remaining = Board.totalIce(engine.iceGrid)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Цель", fontSize = 11.sp, color = Color.White.copy(alpha = 0.6f))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("❄", fontSize = 18.sp, color = Color(0xFFB3E5FC))
                    Spacer(Modifier.size(4.dp))
                    Text("${engine.totalIceCount - remaining} / ${engine.totalIceCount}",
                        fontSize = 18.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
        GoalType.HEART -> {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Цель", fontSize = 11.sp, color = Color.White.copy(alpha = 0.6f))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("❤", fontSize = 18.sp, color = Color(0xFFFF5252))
                    Spacer(Modifier.size(4.dp))
                    Text("${engine.goalProgress} / ${level.heartCount}",
                        fontSize = 18.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ============================================================
// DIALOGS
// ============================================================

@Composable
fun PauseDialog(level: LevelConfig, onResume: () -> Unit, onRetry: () -> Unit, onMap: () -> Unit) {
    AlertDialog(
        onDismissRequest = onResume,
        title = { Text("Пауза", fontWeight = FontWeight.Bold) },
        text = { Text("Уровень ${level.id} • ${level.difficulty.label}") },
        confirmButton = { Button(onClick = onResume) { Text("Продолжить") } },
        dismissButton = {
            Column(horizontalAlignment = Alignment.End) {
                TextButton(onClick = onRetry) { Text("🔄 Заново") }
                TextButton(onClick = onMap) { Text("🗺 К карте") }
            }
        }
    )
}

@Composable
fun WinDialog(
    reward: Int,
    score: Int,
    target: Int,
    hasNext: Boolean,
    onNext: () -> Unit,
    onMap: () -> Unit,
    isDaily: Boolean = false
) {
    val stars = when {
        score >= target * 3 / 2 -> 3
        score >= target * 5 / 4 -> 2
        else -> 1
    }
    var visibleStars by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { repeat(stars) { delay(280); visibleStars++ } }

    AlertDialog(
        onDismissRequest = {},
        title = {
            Text(
                if (isDaily) "⚔ Испытание пройдено!" else "Победа!",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Box(modifier = Modifier.fillMaxWidth()) {
                ConfettiEffect()
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("★".repeat(visibleStars) + "☆".repeat(3 - visibleStars),
                        fontSize = 44.sp, color = Gold, letterSpacing = 4.sp)
                    Spacer(Modifier.height(10.dp))
                    Text("Очки: $score")
                    Text("🪙 +$reward монет")
                }
            }
        },
        confirmButton = {
            if (hasNext) Button(onClick = onNext) { Text("Дальше") }
            else Button(onClick = onMap) { Text("К карте") }
        },
        dismissButton = { if (hasNext) TextButton(onClick = onMap) { Text("К карте") } }
    )
}

@Composable
fun ConfettiEffect() {
    val particles = remember {
        val rng = Random(System.currentTimeMillis())
        List(30) {
            ConfettiParticle(
                startX = rng.nextFloat(),
                speed = 0.6f + rng.nextFloat() * 0.8f,
                size = 4f + rng.nextFloat() * 6f,
                colorIdx = rng.nextInt(6),
                delay = rng.nextFloat() * 0.4f
            )
        }
    }
    val colors = listOf(
        Color(0xFFFF5252), Color(0xFFFFD740), Color(0xFF69F0AE),
        Color(0xFF40E0D0), Color(0xFF448AFF), Color(0xFFE040FB)
    )
    val transition = rememberInfiniteTransition(label = "confetti")
    val phase by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing)),
        label = "confettiPhase"
    )
    Canvas(modifier = Modifier.fillMaxSize().height(160.dp)) {
        particles.forEach { p ->
            val t = ((phase + p.delay) % 1f)
            val y = t * size.height
            val x = p.startX * size.width + sin(t * 6.28f) * 20f
            drawCircle(
                color = colors[p.colorIdx].copy(alpha = 1f - t),
                radius = p.size,
                center = Offset(x, y)
            )
        }
    }
}

data class ConfettiParticle(
    val startX: Float,
    val speed: Float,
    val size: Float,
    val colorIdx: Int,
    val delay: Float
)

@Composable
fun BoosterBuyDialog(booster: BoosterType, coins: Int, cost: Int,
                     onSpendCoins: () -> Unit, onWatchAd: () -> Unit, onCancel: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("${booster.emoji} ${booster.label}") },
        text = { Text("Активировать бустер за $cost монет или посмотреть рекламу?") },
        confirmButton = {
            Button(onClick = onSpendCoins, enabled = coins >= cost) { Text("🪙 $cost монет") }
        },
        dismissButton = {
            Column(horizontalAlignment = Alignment.End) {
                TextButton(onClick = onWatchAd) { Text("📺 Реклама") }
                TextButton(onClick = onCancel) { Text("Отмена") }
            }
        }
    )
}

// ============================================================
// SMALL COMPONENTS
// ============================================================

@Composable
fun ChipButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(CardBg)
            .clickable { onClick() }.padding(horizontal = 14.dp, vertical = 8.dp)
    ) { Text(text = text, color = Color.White, fontSize = 14.sp) }
}

@Composable
fun StatBlock(label: String, value: String, valueColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 11.sp, color = Color.White.copy(alpha = 0.6f))
        Text(value, fontSize = 20.sp, color = valueColor, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun BoosterButton(type: BoosterType, enabled: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        modifier = modifier.clip(RoundedCornerShape(12.dp))
            .background(if (enabled) CardBg else CardBg.copy(alpha = 0.4f))
            .clickable(enabled = enabled) { onClick() }.padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(type.emoji, fontSize = 22.sp)
        Spacer(Modifier.height(2.dp))
        Text("50🪙", fontSize = 10.sp, color = if (enabled) Gold else Gold.copy(alpha = 0.4f))
    }
}

// ============================================================
// BOARD VIEW
// ============================================================

@Composable
fun BoardView(
    grid: List<List<Tile>>,
    iceGrid: List<List<Int>>,
    selected: Pair<Int, Int>?,
    hint: Pair<Pair<Int, Int>, Pair<Int, Int>>?,
    onTap: (Int, Int) -> Unit,
    onSwipe: (Pair<Int, Int>, Pair<Int, Int>) -> Unit
) {
    val hintPulse by rememberInfiniteTransition(label = "hintPulse").animateFloat(
        initialValue = 0.20f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(650, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "hintPulseValue"
    )

    val hintCells = remember(hint) {
        hint?.let { setOf(it.first, it.second) } ?: emptySet()
    }

    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth().aspectRatio(1f)
            .clip(RoundedCornerShape(18.dp)).background(Color(0xFF241646))
    ) {
        val tileSizeDp = maxWidth / Board.SIZE
        val tileSizePx = with(LocalDensity.current) { tileSizeDp.toPx() }
        var dragStart by remember { mutableStateOf<Pair<Int, Int>?>(null) }
        var lastPos by remember { mutableStateOf(Offset.Zero) }

        fun posToCell(pos: Offset): Pair<Int, Int>? {
            val r = (pos.y / tileSizePx).toInt()
            val c = (pos.x / tileSizePx).toInt()
            return if (r in 0 until Board.SIZE && c in 0 until Board.SIZE) r to c else null
        }

        Box(
            modifier = Modifier.size(maxWidth)
                .pointerInput(tileSizePx) {
                    detectDragGestures(
                        onDragStart = { pos -> lastPos = pos; dragStart = posToCell(pos) },
                        onDragEnd = {
                            val s = dragStart; val e = posToCell(lastPos)
                            if (s != null && e != null && s != e) onSwipe(s, e)
                            dragStart = null
                        },
                        onDrag = { change, _ -> lastPos = change.position }
                    )
                }
                .pointerInput(tileSizePx) {
                    detectTapGestures { pos -> posToCell(pos)?.let { (r, c) -> onTap(r, c) } }
                }
        ) {
            for (r in 0 until Board.SIZE) for (c in 0 until Board.SIZE) {
                val layers = iceGrid[r][c]
                if (layers > 0) {
                    key("ice-$r-$c-$layers") {
                        Box(
                            modifier = Modifier
                                .offset {
                                    IntOffset(
                                        (c * tileSizePx).roundToInt(),
                                        (r * tileSizePx).roundToInt()
                                    )
                                }
                                .size(tileSizeDp)
                                .padding(2.dp)
                        ) { IceOverlay(layers, Modifier.fillMaxSize()) }
                    }
                }
            }

            for (r in 0 until Board.SIZE) for (c in 0 until Board.SIZE) {
                val tile = grid[r][c]
                key(tile.id) {
                    TileView(
                        tile = tile,
                        row = r,
                        col = c,
                        tileSizePx = tileSizePx,
                        tileSizeDp = tileSizeDp,
                        isSelected = selected == (r to c),
                        isHinted = hintCells.contains(r to c),
                        hintPulse = hintPulse
                    )
                }
            }
        }
    }
}

@Composable
fun TileView(
    tile: Tile,
    row: Int,
    col: Int,
    tileSizePx: Float,
    tileSizeDp: Dp,
    isSelected: Boolean,
    isHinted: Boolean,
    hintPulse: Float
) {
    val targetOffset = IntOffset((col * tileSizePx).roundToInt(), (row * tileSizePx).roundToInt())
    val offset by animateIntOffsetAsState(targetOffset, tween(240))

    val scale = remember { Animatable(0f) }
    val explosionProgress = remember { Animatable(1f) }

    LaunchedEffect(tile.matching) {
        if (tile.matching) {
            explosionProgress.snapTo(0f)
            launch { explosionProgress.animateTo(1f, tween(500)) }
            scale.animateTo(0f, tween(200))
        } else {
            if (scale.value < 1f) scale.animateTo(1f, tween(220))
        }
    }

    val selectedScale by animateFloatAsState(
        targetValue = if (isSelected) 1.12f else 1f,
        animationSpec = tween(150)
    )

    val explodeColor = if (tile.stone) Color(0xFF9E9E9E)
    else if (tile.rainbow) Color(0xFFFFD740)
    else TileType.fromOrdinal(tile.type).color

    Box(modifier = Modifier.offset { offset }.size(tileSizeDp)) {
        if (explosionProgress.value < 1f) {
            ExplosionParticles(explodeColor, explosionProgress.value)
        }

        if (isHinted) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val r = min(size.width, size.height) / 2f * 0.92f
                drawCircle(
                    color = Color.White.copy(alpha = 0.35f * hintPulse),
                    radius = r,
                    center = Offset(cx, cy)
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.55f + 0.45f * hintPulse),
                    radius = r * 0.92f,
                    center = Offset(cx, cy),
                    style = Stroke(width = size.minDimension * 0.10f)
                )
                drawCircle(
                    color = Color(0xFFFFF176).copy(alpha = 0.7f * hintPulse),
                    radius = r * 0.80f,
                    center = Offset(cx, cy),
                    style = Stroke(width = size.minDimension * 0.04f)
                )
            }
        }

        Box(
            modifier = Modifier.fillMaxSize().padding(2.dp)
                .graphicsLayer(
                    scaleX = scale.value * selectedScale,
                    scaleY = scale.value * selectedScale
                )
        ) {
            if (tile.stone) {
                StoneTile(Modifier.fillMaxSize())
            } else {
                GemTile(
                    TileType.fromOrdinal(tile.type),
                    Modifier.fillMaxSize(),
                    rainbow = tile.rainbow
                )
                if (tile.hasHeart) {
                    HeartOverlay(Modifier.fillMaxSize())
                }
                if (tile.locked) {
                    LockOverlay(Modifier.fillMaxSize())
                }
            }
        }
    }
}

@Composable
fun ExplosionParticles(color: Color, progress: Float) {
    val angles = remember { List(8) { i -> i * 45f } }
    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val maxDistance = size.minDimension * 0.55f
        val baseRadius = size.minDimension * 0.08f
        val alpha = (1f - progress).coerceIn(0f, 1f)

        drawCircle(color.copy(alpha = alpha * 0.5f),
            radius = size.minDimension * 0.5f * (1f - progress), center = center)

        for (angle in angles) {
            val rad = angle * PI / 180.0
            val dx = cos(rad).toFloat(); val dy = sin(rad).toFloat()
            val distance = maxDistance * progress
            val pos = Offset(center.x + dx * distance, center.y + dy * distance)
            val radius = baseRadius * (1f - progress * 0.6f)
            drawCircle(color.copy(alpha = alpha), radius = radius, center = pos)
            drawCircle(Color.White.copy(alpha = alpha * 0.7f), radius = radius * 0.4f, center = pos)
        }
    }
}