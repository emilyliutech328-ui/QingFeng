package io.legado.app.model.cover

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.text.TextPaint
import androidx.core.graphics.createBitmap
import io.legado.app.lib.theme.accentColor
import io.legado.app.lib.theme.backgroundColor
import io.legado.app.utils.textHeight
import io.legado.app.utils.toStringArray
import splitties.init.appCtx

/**
 * 封面生成总入口。
 *
 * - [generate]：按 [CoverStyle.layout] 分发到横排（新）或竖排（旧，原样保留）。
 * - [simple]：书架列表小封面（宽 < 120px）专用简版：标题 + 细边框，
 *   不画纹理/图案（可视面积太小看不出细节，白烧性能）。
 *
 * 颜色沿用主题链路：底色 backgroundColor、字色 accentColor（描边用底色）。
 */
object CoverGenerator {

    /**
     * 完整版封面。
     *
     * @param drawAuthor 是否画作者（沿用 coverShowName/coverShowAuthor 语义，
     *                   由调用方决定；false 时只画标题）
     */
    fun generate(
        name: String?,
        author: String?,
        width: Int,
        height: Int,
        drawAuthor: Boolean
    ): Bitmap = generateWithColors(
        name, author, width, height, drawAuthor,
        appCtx.backgroundColor, appCtx.accentColor
    )

    /**
     * 可注入底/字色的入口（渲染测试用，避免依赖 app 初始化）。
     */
    fun generateWithColors(
        name: String?,
        author: String?,
        width: Int,
        height: Int,
        drawAuthor: Boolean,
        bg: Int,
        accent: Int
    ): Bitmap {
        val bitmap = createBitmap(width.coerceAtLeast(1), height.coerceAtLeast(1))
        val canvas = Canvas(bitmap)
        canvas.drawColor(bg)
        if (name.isNullOrEmpty()) return bitmap

        when (CoverStyle.layout) {
            CoverStyle.Layout.Vertical -> drawVertical(canvas, name, author, width.toFloat(), height.toFloat(), bg, accent, drawAuthor)
            CoverStyle.Layout.Horizontal -> drawHorizontal(canvas, name, author, width.toFloat(), height.toFloat(), bg, accent, drawAuthor, full = true)
        }
        return bitmap
    }

    /**
     * 小封面简版：标题 + 细边框，无纹理/图案。
     */
    fun simple(
        name: String?,
        author: String?,
        width: Int,
        height: Int,
        drawAuthor: Boolean
    ): Bitmap {
        val bitmap = createBitmap(width.coerceAtLeast(1), height.coerceAtLeast(1))
        val canvas = Canvas(bitmap)
        val bg = appCtx.backgroundColor
        val accent = appCtx.accentColor
        canvas.drawColor(bg)
        if (name.isNullOrEmpty()) return bitmap

        when (CoverStyle.layout) {
            CoverStyle.Layout.Vertical -> drawVertical(canvas, name, author, width.toFloat(), height.toFloat(), bg, accent, drawAuthor)
            CoverStyle.Layout.Horizontal -> drawHorizontal(canvas, name, author, width.toFloat(), height.toFloat(), bg, accent, drawAuthor, full = false)
        }
        return bitmap
    }

    // ---------------------------------------------------------------- 横排（新）

    private fun drawHorizontal(
        canvas: Canvas,
        name: String,
        author: String?,
        w: Float,
        h: Float,
        bg: Int,
        accent: Int,
        drawAuthor: Boolean,
        full: Boolean
    ) {
        val zones = CoverLayout.zones(w, h)
        val inset = w * CoverLayout.INSET_RATIO
        val maxWidth = w - inset * 2 - w * CoverLayout.SIDE_PADDING_RATIO * 2

        // 纹理（完整版才画）
        if (full) {
            val texturePaint = Paint(Paint.ANTI_ALIAS_FLAG)
            CoverTexture.draw(canvas, name, w, h, accent, texturePaint, CoverStyle.texture)
        }

        // 标题：字号试算
        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.LEFT
        }
        val title = CoverLayout.fitTitle(titlePaint, name, w, maxWidth, zones.titleHeight)
        val lineHeight = title.textSize * CoverLayout.LINE_HEIGHT_RATIO
        titlePaint.strokeWidth = title.textSize / 6

        var ty = zones.titleTop + title.textSize * 0.9f
        for (line in title.lines) {
            // 描边(底色) + 填充(强调色)，与旧竖排同款双层字
            titlePaint.color = bg
            titlePaint.style = Paint.Style.STROKE
            canvas.drawText(line, inset + w * CoverLayout.SIDE_PADDING_RATIO, ty, titlePaint)
            titlePaint.color = accent
            titlePaint.style = Paint.Style.FILL
            canvas.drawText(line, inset + w * CoverLayout.SIDE_PADDING_RATIO, ty, titlePaint)
            ty += lineHeight
        }

        // 图案（完整版 + 开关开启）
        if (full && CoverStyle.motif == CoverStyle.Motif.ByName) {
            val motifPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = accent
                alpha = 55
            }
            // 图案顶端距最后一行标题底部 = 字号 × 0.6
            val motifCx = w / 2f
            val motifCy = maxOf(zones.motifTop + zones.motifSize / 2f,
                ty - lineHeight + title.textSize * 0.6f + zones.motifSize / 2f)
            var motifSize = zones.motifSize
            // 图案宽超出标题最宽行时缩到 0.85 倍
            val widest = title.lines.maxOfOrNull { titlePaint.measureText(it) } ?: 0f
            if (motifSize > widest && widest > 0) motifSize *= 0.85f
            CoverMotif.draw(canvas, name, motifCx, motifCy.coerceAtMost(h - zones.motifSize), motifSize, motifPaint)
        }

        // 作者（右下角）
        if (drawAuthor) {
            val shown = author?.takeIf { it.isNotBlank() }
                ?: if (CoverStyle.fallbackAuthor) CoverAuthorPool.forName(name) else null
            if (!shown.isNullOrBlank()) {
                val authorPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                    typeface = Typeface.DEFAULT
                    textAlign = Paint.Align.RIGHT
                    textSize = w * (if (full) 0.045f else 0.06f)
                }
                val ax = w - inset - w * CoverLayout.SIDE_PADDING_RATIO
                val ay = zones.authorBottom + h * 0.04f
                authorPaint.color = bg
                authorPaint.style = Paint.Style.STROKE
                authorPaint.strokeWidth = authorPaint.textSize / 5
                canvas.drawText(shown, ax, ay, authorPaint)
                authorPaint.color = accent
                authorPaint.style = Paint.Style.FILL
                authorPaint.alpha = 200
                canvas.drawText(shown, ax, ay, authorPaint)
            }
        }

        // 边框
        if (CoverStyle.border) {
            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG)
            CoverBorder.draw(canvas, w, h, accent, borderPaint, small = !full)
        }
    }

    // ---------------------------------------------------------------- 竖排（旧版原样保留）

    /**
     * 旧 `CoverImageView.generateCoverBitmap()` 的竖排逻辑，逐行搬运未改动。
     * `coverLayout = 竖排` 时走这里，保证旧观感随时可回退。
     */
    private fun drawVertical(
        canvas: Canvas,
        name: String,
        author: String?,
        viewWidth: Float,
        viewHeight: Float,
        backgroundColor: Int,
        accentColor: Int,
        drawAuthor: Boolean
    ) {
        var startX = viewWidth * 0.2f
        var startY = viewHeight * 0.2f
        val namePaint = TextPaint().apply {
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        name.toStringArray().let { nameArray ->
            var line = 0
            namePaint.textSize = viewWidth / 7
            namePaint.strokeWidth = namePaint.textSize / 6
            nameArray.forEachIndexed { index, char ->
                namePaint.color = backgroundColor
                namePaint.style = Paint.Style.STROKE
                canvas.drawText(char, startX, startY, namePaint)
                namePaint.color = accentColor
                namePaint.style = Paint.Style.FILL
                canvas.drawText(char, startX, startY, namePaint)
                startY += namePaint.textHeight
                if (startY > viewHeight * 0.9) {
                    if ((nameArray.size - index - 1) == 1) { //只剩一个字
                        startY -= namePaint.textHeight / 5
                        namePaint.textSize = viewWidth / 9
                        return@forEachIndexed
                    }
                    startX += namePaint.textSize
                    line++
                    namePaint.textSize = viewWidth / 10
                    startY = viewHeight * 0.2f + namePaint.textHeight * line
                } else if (startY > viewHeight * 0.8 && (nameArray.size - index - 1) > 2) { //剩余字数大于2
                    startX += namePaint.textSize
                    line++
                    namePaint.textSize = viewWidth / 10
                    startY = viewHeight * 0.2f + namePaint.textHeight * line
                }
            }
        }
        if (!drawAuthor) {
            return
        }
        val authorPaint = TextPaint(namePaint).apply {
            typeface = Typeface.DEFAULT
        }
        val shownAuthor = author?.takeIf { it.isNotBlank() }
            ?: if (CoverStyle.fallbackAuthor) CoverAuthorPool.forName(name) else null
        shownAuthor?.toStringArray()?.let { authorArray ->
            authorPaint.textSize = viewWidth / 10
            authorPaint.strokeWidth = authorPaint.textSize / 5
            startX = viewWidth * 0.8f
            startY = viewHeight * 0.95f - authorArray.size * authorPaint.textHeight
            startY = maxOf(startY, viewHeight * 0.3f)
            authorArray.forEach {
                authorPaint.color = backgroundColor
                authorPaint.style = Paint.Style.STROKE
                canvas.drawText(it, startX, startY, authorPaint)
                authorPaint.color = accentColor
                authorPaint.style = Paint.Style.FILL
                canvas.drawText(it, startX, startY, authorPaint)
                startY += authorPaint.textHeight
                if (startY > viewHeight * 0.95) {
                    return@let
                }
            }
        }
    }
}
