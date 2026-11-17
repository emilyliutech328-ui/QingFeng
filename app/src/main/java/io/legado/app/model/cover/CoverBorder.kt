package io.legado.app.model.cover

import android.graphics.Canvas
import android.graphics.Paint

/**
 * 封面边框（双线 + 四角 L 形装饰）与背景纹理。
 *
 * 颜色统一取 accentColor，按 alpha 层次递减（外重内轻）。
 */
object CoverBorder {

    /**
     * 双线边框 + 四角装饰。
     *
     * @param insetDp 由调用方换算成 px 后传入（外框 5dp、内框 11dp 的基准比例）
     */
    fun draw(canvas: Canvas, width: Float, height: Float, color: Int, paint: Paint, small: Boolean) {
        paint.style = Paint.Style.FILL
        paint.color = color
        val outerInset = width * 0.028f   // ≈5dp @300px
        val innerInset = width * 0.055f   // ≈11dp @300px
        val outerW = (width * 0.006f).coerceAtLeast(1f)   // 1.2dp
        val innerW = (width * 0.003f).coerceAtLeast(0.8f) // 0.6dp

        // 外框 alpha 60（小封面降到 80 增强存在感）
        paint.alpha = if (small) 80 else 60
        paint.strokeWidth = outerW
        paint.style = Paint.Style.STROKE
        canvas.drawRect(outerInset, outerInset, width - outerInset, height - outerInset, paint)

        if (small) return  // 小封面只留一条细边框

        // 内框 alpha 35
        paint.alpha = 35
        paint.strokeWidth = innerW
        canvas.drawRect(innerInset, innerInset, width - innerInset, height - innerInset, paint)

        // 四角 L 形装饰：长度 = 内框 inset × 1.6，alpha 90
        paint.alpha = 90
        paint.strokeWidth = innerW * 1.6f
        val len = innerInset * 1.6f
        val corners = arrayOf(
            // [x, y, dx, dy] 角点与两臂方向
            floatArrayOf(innerInset, innerInset, 1f, 1f),
            floatArrayOf(width - innerInset, innerInset, -1f, 1f),
            floatArrayOf(innerInset, height - innerInset, 1f, -1f),
            floatArrayOf(width - innerInset, height - innerInset, -1f, -1f)
        )
        for (c in corners) {
            val (x, y, dx, dy) = c
            canvas.drawLine(x, y, x + len * dx, y, paint)
            canvas.drawLine(x, y, x, y + len * dy, paint)
        }
    }
}

/**
 * 背景纹理。点/线位置全部由书名哈希决定（确定性），
 * 否则每次渲染纹理都会变、缓存失效。
 */
object CoverTexture {

    fun draw(canvas: Canvas, name: String, width: Float, height: Float, color: Int, paint: Paint, kind: CoverStyle.Texture) {
        when (kind) {
            CoverStyle.Texture.None -> return
            CoverStyle.Texture.Grid -> grid(canvas, width, height, color, paint)
            CoverStyle.Texture.Dots -> dots(canvas, name, width, height, color, paint)
        }
    }

    /** 细网格：每 4dp 一条 1px 线，alpha 6（类似书纸） */
    private fun grid(canvas: Canvas, width: Float, height: Float, color: Int, paint: Paint) {
        val step = (width * 0.022f).coerceAtLeast(6f) // ≈4dp @300px，下限保护
        paint.style = Paint.Style.FILL
        paint.color = color
        paint.alpha = 6
        val lineW = 1f
        var x = step
        while (x < width) {
            canvas.drawRect(x, 0f, x + lineW, height, paint)
            x += step
        }
        var y = step
        while (y < height) {
            canvas.drawRect(0f, y, width, y + lineW, paint)
            y += step
        }
    }

    /** 网点：按书名哈希撒点，alpha 8（类似漫画网点） */
    private fun dots(canvas: Canvas, name: String, width: Float, height: Float, color: Int, paint: Paint) {
        paint.style = Paint.Style.FILL
        paint.color = color
        paint.alpha = 8
        // 确定性伪随机：LCG，种子来自书名
        var state = CoverMotifSeed.of(name)
        val count = (width * height / 900f).toInt().coerceIn(40, 400)
        val r = (width * 0.004f).coerceAtLeast(0.8f)
        for (i in 0 until count) {
            state = (state * 6364136223846793005L + 1442695040888963407L) and 0xFFFFFFFFFFFFFFFL
            val x = ((state ushr 33).toInt() and 0xFFFF) / 65535f * width
            state = (state * 6364136223846793005L + 1442695040888963407L) and 0xFFFFFFFFFFFFFFFL
            val y = ((state ushr 33).toInt() and 0xFFFF) / 65535f * height
            canvas.drawCircle(x, y, r, paint)
        }
    }
}

/** 图案/纹理共用的书名哈希（与 CoverMotif 同族） */
object CoverMotifSeed {
    fun of(s: String): Long {
        var h = 0x811C9DC5L
        for (ch in s) {
            h = h xor ch.code.toLong()
            h *= 0x01000193L
            h = h and 0xFFFFFFFFL
        }
        return h
    }
}
