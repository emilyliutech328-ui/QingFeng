package io.legado.app.model.cover

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import kotlin.math.cos
import kotlin.math.sin

/**
 * 封面装饰图案：8 种自绘单色矢量（Canvas 直接画，零资源文件）。
 *
 * 选型规则：同一书名永远同一图案（哈希确定性），
 * 在「图案区」居中绘制，单色 alpha 由调用方控制。
 */
object CoverMotif {

    /** 图案总数（CoverStyle/选型共用） */
    const val COUNT = 8

    /**
     * 按书名哈希选图案并绘制。
     *
     * @param size 绘制区域边长（正方形内切）
     */
    fun draw(canvas: Canvas, name: String, cx: Float, cy: Float, size: Float, paint: Paint) {
        val kind = (fnv(name) % COUNT).toInt()
        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeJoin = Paint.Join.ROUND
        val r = size / 2f
        when (kind) {
            0 -> vine(canvas, cx, cy, r, paint)
            1 -> concentricCircles(canvas, cx, cy, r, paint)
            2 -> starMoon(canvas, cx, cy, r, paint)
            3 -> ripples(canvas, cx, cy, r, paint)
            4 -> diamonds(canvas, cx, cy, r, paint)
            5 -> pageFan(canvas, cx, cy, r, paint)
            6 -> sword(canvas, cx, cy, r, paint)
            7 -> magicCircle(canvas, cx, cy, r, paint)
        }
    }

    /** FNV-1a，与 CoverAuthorPool 同一哈希族，保证跨设备一致 */
    private fun fnv(s: String): Long {
        var h = 0x811C9DC5L
        for (ch in s) {
            h = h xor ch.code.toLong()
            h *= 0x01000193L
            h = h and 0xFFFFFFFFL
        }
        return h
    }

    private fun baseStroke(paint: Paint, r: Float, factor: Float = 0.045f) {
        paint.strokeWidth = (r * factor).coerceAtLeast(1.2f)
    }

    /** 1. 藤蔓：正弦曲线 + 两侧小叶 */
    private fun vine(canvas: Canvas, cx: Float, cy: Float, r: Float, paint: Paint) {
        baseStroke(paint, r)
        val path = Path()
        val steps = 32
        for (i in 0..steps) {
            val t = i.toFloat() / steps
            val x = cx - r + t * 2 * r
            val y = cy + sin(t * Math.PI * 2).toFloat() * r * 0.35f
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        canvas.drawPath(path, paint)
        // 小叶：波峰处两枚
        for (sign in intArrayOf(-1, 1)) {
            val lx = cx + sign * r * 0.5f
            val ly = cy + sin((0.5f + sign * 0.25f) * Math.PI * 2).toFloat() * r * 0.35f
            val leaf = Path()
            leaf.moveTo(lx, ly)
            leaf.quadTo(lx + r * 0.12f, ly - r * 0.3f * sign, lx, ly - r * 0.45f * sign)
            leaf.quadTo(lx - r * 0.12f, ly - r * 0.3f * sign, lx, ly)
            canvas.drawPath(leaf, paint)
        }
    }

    /** 2. 同心圆 */
    private fun concentricCircles(canvas: Canvas, cx: Float, cy: Float, r: Float, paint: Paint) {
        baseStroke(paint, r, 0.04f)
        canvas.drawCircle(cx, cy, r * 0.9f, paint)
        canvas.drawCircle(cx, cy, r * 0.6f, paint)
        canvas.drawCircle(cx, cy, r * 0.3f, paint)
    }

    /** 3. 星月：月牙（外弧 + 内弧围合）+ 五角星 */
    private fun starMoon(canvas: Canvas, cx: Float, cy: Float, r: Float, paint: Paint) {
        baseStroke(paint, r, 0.04f)
        // Android 角度约定：0°=东，正角顺时针（y 轴向下）。
        // 外弧：从 55°（右下）顺时针扫 250° 经西/北到 305°（右上），
        // 得到月牙的左半外缘；再用内凹二次曲线从右上回接右下，围成月牙。
        val ocx = cx - r * 0.32f
        val ocy = cy
        val or = r * 0.72f
        fun pt(deg: Double): Pair<Float, Float> =
            (ocx + or * cos(Math.toRadians(deg))).toFloat() to
                    (ocy + or * sin(Math.toRadians(deg))).toFloat()
        val pStart = pt(55.0)    // 右下
        val pEnd = pt(305.0)     // 右上
        val moon = Path()
        moon.moveTo(pStart.first, pStart.second)
        moon.arcTo(ocx - or, ocy - or, ocx + or, ocy + or, 55f, 250f, false)
        // 内弧：向圆心右侧内凹
        moon.quadTo(ocx + or * 0.62f, ocy, pStart.first, pStart.second)
        moon.close()
        canvas.drawPath(moon, paint)
        // 五角星（右侧）
        val star = Path()
        val scx = cx + r * 0.55f
        val scy = cy - r * 0.35f
        val sr = r * 0.32f
        for (i in 0 until 5) {
            val outer = Math.PI / 2 + i * 2 * Math.PI / 5
            val inner = outer + Math.PI / 5
            val ox = (scx + sr * cos(outer)).toFloat()
            val oy = (scy - sr * sin(outer)).toFloat()
            val ix = (scx + sr * 0.45f * cos(inner)).toFloat()
            val iy = (scy - sr * 0.45f * sin(inner)).toFloat()
            if (i == 0) star.moveTo(ox, oy) else star.lineTo(ox, oy)
            star.lineTo(ix, iy)
        }
        star.close()
        canvas.drawPath(star, paint)
    }

    /** 4. 波纹：三条平行浅正弦 */
    private fun ripples(canvas: Canvas, cx: Float, cy: Float, r: Float, paint: Paint) {
        baseStroke(paint, r, 0.038f)
        for (row in -1..1) {
            val path = Path()
            val steps = 24
            for (i in 0..steps) {
                val t = i.toFloat() / steps
                val x = cx - r * 0.9f + t * r * 1.8f
                val y = cy + row * r * 0.42f + sin(t * Math.PI * 2).toFloat() * r * 0.12f
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            canvas.drawPath(path, paint)
        }
    }

    /** 5. 方菱：旋转 45° 菱形 + 内接小菱形 */
    private fun diamonds(canvas: Canvas, cx: Float, cy: Float, r: Float, paint: Paint) {
        baseStroke(paint, r, 0.04f)
        val outer = Path()
        outer.moveTo(cx, cy - r * 0.9f)
        outer.lineTo(cx + r * 0.9f, cy)
        outer.lineTo(cx, cy + r * 0.9f)
        outer.lineTo(cx - r * 0.9f, cy)
        outer.close()
        canvas.drawPath(outer, paint)
        val inner = Path()
        val k = 0.45f
        inner.moveTo(cx, cy - r * k)
        inner.lineTo(cx + r * k, cy)
        inner.lineTo(cx, cy + r * k)
        inner.lineTo(cx - r * k, cy)
        inner.close()
        canvas.drawPath(inner, paint)
    }

    /** 6. 书页扇：三片扇形叠排 */
    private fun pageFan(canvas: Canvas, cx: Float, cy: Float, r: Float, paint: Paint) {
        baseStroke(paint, r, 0.038f)
        val pivotY = cy + r * 0.75f
        for (i in -1..1) {
            val tilt = i * 0.38f
            val path = Path()
            path.moveTo(cx, pivotY)
            val tipX = cx + sin(tilt.toDouble()).toFloat() * r * 1.5f
            val tipY = pivotY - cos(tilt.toDouble()).toFloat() * r * 1.5f
            val leftX = cx + sin((tilt - 0.22f).toDouble()).toFloat() * r * 1.1f
            val leftY = pivotY - cos((tilt - 0.22f).toDouble()).toFloat() * r * 1.1f
            val rightX = cx + sin((tilt + 0.22f).toDouble()).toFloat() * r * 1.1f
            val rightY = pivotY - cos((tilt + 0.22f).toDouble()).toFloat() * r * 1.1f
            path.moveTo(leftX, leftY)
            path.quadTo(tipX, tipY, rightX, rightY)
            canvas.drawPath(path, paint)
        }
    }

    /** 7. 剑锋：锐角折线 + 短线 */
    private fun sword(canvas: Canvas, cx: Float, cy: Float, r: Float, paint: Paint) {
        baseStroke(paint, r, 0.05f)
        val blade = Path()
        blade.moveTo(cx - r * 0.7f, cy + r * 0.7f)
        blade.lineTo(cx + r * 0.55f, cy - r * 0.75f)
        blade.lineTo(cx + r * 0.75f, cy - r * 0.55f)
        blade.lineTo(cx - r * 0.5f, cy + r * 0.9f)
        blade.close()
        canvas.drawPath(blade, paint)
        // 护手短线
        canvas.drawLine(cx - r * 0.25f, cy + r * 0.15f, cx + r * 0.2f, cy - r * 0.3f, paint)
    }

    /** 8. 魔法阵：圆 + 八等分半径线 + 内圆 */
    private fun magicCircle(canvas: Canvas, cx: Float, cy: Float, r: Float, paint: Paint) {
        baseStroke(paint, r, 0.035f)
        canvas.drawCircle(cx, cy, r * 0.9f, paint)
        canvas.drawCircle(cx, cy, r * 0.45f, paint)
        for (i in 0 until 8) {
            val a = i * Math.PI / 4
            canvas.drawLine(
                cx, cy,
                cx + (r * 0.9f * cos(a)).toFloat(),
                cy + (r * 0.9f * sin(a)).toFloat(),
                paint
            )
        }
    }
}
