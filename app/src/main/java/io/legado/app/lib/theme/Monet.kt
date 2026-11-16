package io.legado.app.lib.theme

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import androidx.annotation.ColorInt
import io.legado.app.constant.AppLog
import io.legado.app.constant.PreferKey
import io.legado.app.help.config.AppConfig
import io.legado.app.help.config.ThemeConfig
import io.legado.app.lib.theme.m3.QuantizerCelebi
import io.legado.app.lib.theme.m3.Score
import io.legado.app.lib.theme.m3.Scheme as M3Scheme
import io.legado.app.lib.theme.m3.TonalPalette
import io.legado.app.utils.getPrefString
import io.legado.app.utils.printOnDebug
import java.io.File

/**
 * 莫奈（Monet）动态取色 —— Material You / Material Design 3 引擎。
 *
 * 2026-11 重构：算法从「HSV 逐桶打分 + HSL 校正出 4 色」升级为
 * Google material-color-utilities 官方流水线（初版 2021/2022 规范，Apache-2.0）：
 *
 *   壁纸/图片 → 降采样 → QuantizerCelebi 量化(128 色)
 *             → Score 官方打分选种子色
 *             → Hct(CAM16 感知均匀色彩空间) → TonalPalette 色调调色板
 *             → Scheme 生成 27 个语义色 token（primary/onPrimary/container/
 *               surface/outline/inverse…，明暗两套自动保证对比度）
 *
 * 与旧实现的差别：
 * - HCT 取代 HSV：色度/明度在感知均匀的空间里选取，产物更接近系统 Material You
 * - tone 档位固定（primary=40/80，container=90/30 等），任何种子色都能保证可读性
 * - 完整 [M3Scheme] 缓存在 [lastScheme]，新界面可直接取 container/outline 等 token
 */
object Monet {

    @ColorInt
    private val FALLBACK_PRIMARY = Color.parseColor("#455A64")

    /** 与旧版兼容的 4 色结果（primaryDark/accent 是对 M3 token 的映射） */
    data class Scheme(
        @ColorInt val primary: Int,
        @ColorInt val primaryDark: Int,
        @ColorInt val accent: Int,
        @ColorInt val background: Int
    )

    /** 最近一次生成的 M3 完整配色（供新界面取 container/outline 等 token） */
    var lastScheme: M3Scheme? = null
        private set

    /** 取色统计的采样边长（采样 96x96 ≈ 9000 像素，足够稳定） */
    private const val SAMPLE_SIZE = 96

    /** QuantizerCelebi 的目标色数（M3 官方壁纸流程同款参数） */
    private const val MAX_COLORS = 128

    fun fromWallpaper(context: Context): M3Scheme? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return null
        return runCatching {
            val manager = WallpaperManager.getInstance(context)
            val drawable = manager.drawable ?: return null
            val bitmap = drawable.toBitmap() ?: return null
            val scheme = fromBitmap(bitmap)
            bitmap.recycleSafely()
            scheme
        }.onFailure {
            AppLog.put("壁纸取色失败\n${it.localizedMessage}", it)
        }.getOrNull()
    }

    fun fromImage(path: String): M3Scheme? = runCatching {
        val file = File(path)
        if (!file.exists()) return null
        val opts = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = false }
        val bitmap = android.graphics.BitmapFactory.decodeFile(path, opts) ?: return null
        val scheme = fromBitmap(bitmap)
        bitmap.recycleSafely()
        scheme
    }.onFailure { it.printOnDebug() }.getOrNull()

    /**
     * 从 Bitmap 提炼配色：M3 官方流水线
     */
    fun fromBitmap(source: Bitmap): M3Scheme? {
        if (source.width <= 0 || source.height <= 0) return null
        val sampled = scaleDown(source, SAMPLE_SIZE)
        val pixels = IntArray(sampled.width * sampled.height)
        sampled.getPixels(pixels, 0, sampled.width, 0, 0, sampled.width, sampled.height)
        if (sampled !== source) sampled.recycleSafely()

        val seed = runCatching { officialSeed(pixels) }.getOrNull()
            ?: FALLBACK_PRIMARY
        return schemeOf(seed)
    }

    /** 任意种子色 → M3 完整配色（公开给自定义主题/新 UI 使用） */
    fun schemeOf(@ColorInt seed: Int): M3Scheme {
        val dark = isDarkTheme()
        val s = if (dark) M3Scheme.dark(seed) else M3Scheme.light(seed)
        lastScheme = s
        return s
    }

    /**
     * M3 官方种子选择：QuantizerCelebi 量化 + Score 打分。
     * Score 会过滤近黑/近白/低饱和，并倾向「既足够饱和又不太扎眼」的颜色，
     * 与系统 Material You 从壁纸选种子的行为一致。
     */
    @ColorInt
    private fun officialSeed(pixels: IntArray): Int {
        // QuantizerCelebi 直接返回 Map<像素色, 计数>；Score 挑出适合做种子的候选
        val colorToCount = QuantizerCelebi.quantize(pixels, MAX_COLORS)
        val ranked = Score.score(colorToCount)
        return ranked.firstOrNull() ?: FALLBACK_PRIMARY
    }

    /** 从 HCT 直接取 tone 档（供状态栏等「同色相、固定明度」场景） */
    @ColorInt
    fun tone(@ColorInt seed: Int, tone: Int): Int {
        return runCatching { TonalPalette.fromInt(seed).tone(tone) }
            .getOrDefault(seed)
    }

    /**
     * 应用配色到全局主题（写入后立即提交）
     *
     * token 映射：
     * - primary        ← M3 primary（tone 40/80，保证对 surface 可读）
     * - primaryDark    ← primary 色相 tone 30（状态栏/深色工具栏，沿用旧语义）
     * - accent         ← M3 tertiary（色相自动偏移的对比强调色）
     * - background     ← M3 background
     */
    fun apply(context: Context, scheme: M3Scheme) {
        val seedDark = tone(scheme.primary, 30)
        ThemeStore.editTheme(context)
            .primaryColor(scheme.primary)
            .primaryColorDark(seedDark)
            .accentColor(scheme.tertiary)
            .backgroundColor(scheme.background)
            .apply()
    }

    /**
     * 按当前设置刷新；返回 false 表示没取到颜色（主题保持原样）。
     */
    fun refresh(context: Context): Boolean {
        if (!AppConfig.monetEnabled) return false
        val scheme = when (AppConfig.monetSource) {
            "1" -> {
                val bg = backgroundImagePath(context)
                bg?.takeIf { it.isNotBlank() }?.let { fromImage(it) }
            }

            else -> fromWallpaper(context)
        } ?: return false
        apply(context, scheme)
        return true
    }

    /**
     * 当前主题背景图路径（主题里选的那张），没有则返回 null
     */
    private fun backgroundImagePath(context: Context): String? {
        val key = if (ThemeConfig.isDarkTheme()) PreferKey.bgImageN else PreferKey.bgImage
        return context.getPrefString(key)?.takeIf { it.isNotBlank() }
    }

    private fun isDarkTheme(): Boolean {
        val mode = AppConfig.themeMode
        return when (mode) {
            "1" -> true
            "2" -> io.legado.app.utils.ColorUtils.isColorLight(
                ThemeStore.primaryColor(splitties.init.appCtx)
            ).not()
            else -> false
        }
    }

    private fun Bitmap.recycleSafely() {
        if (!this.isRecycled) runCatching { this.recycle() }
    }

    private fun scaleDown(source: Bitmap, maxSide: Int): Bitmap {
        val w = source.width
        val h = source.height
        val max = maxOf(w, h)
        if (max <= maxSide) return source
        val ratio = maxSide.toFloat() / max
        val scaled = Bitmap.createScaledBitmap(
            source,
            (w * ratio).toInt().coerceAtLeast(1),
            (h * ratio).toInt().coerceAtLeast(1),
            true
        )
        return if (scaled === source) source else scaled
    }

    private fun android.graphics.drawable.Drawable.toBitmap(): Bitmap? {
        if (this is android.graphics.drawable.BitmapDrawable) {
            return this.bitmap
        }
        val w = intrinsicWidth.coerceAtLeast(1)
        val h = intrinsicHeight.coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        setBounds(0, 0, w, h)
        draw(canvas)
        return bitmap
    }
}
