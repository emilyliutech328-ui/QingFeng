package io.legado.app.model.cover

import android.graphics.Paint

/**
 * 封面版式计算：垂直分区、横排字号试算、切行与标点避头。
 *
 * 纯计算，不碰 Canvas，便于单测。
 */
object CoverLayout {

    /** 行高系数：中文字距紧，1.1 会糊在一起 */
    const val LINE_HEIGHT_RATIO = 1.28f

    /** 竖排/横排共用的呼吸边距比例 */
    const val INSET_RATIO = 0.04f

    /** 内容区左右额外留白 */
    const val SIDE_PADDING_RATIO = 0.08f

    /**
     * 行首禁排标点（避头）：这些字符不能出现在行首，
     * 遇到就挤到上一行末尾。
     */
    private const val LINE_HEAD_UNSAFE = "，。、；：？！）】》”’·…—,.!?:;)]}"

    /** 行尾禁排标点：这些字符不能出现在行尾 */
    private const val LINE_TAIL_UNSAFE = "（【《“‘([{"

    /** 垂直分区结果（绝对像素） */
    data class Zones(
        val topPad: Float,
        val titleTop: Float,
        val titleHeight: Float,   // 标题区最大可用高
        val motifTop: Float,
        val motifSize: Float,     // 图案区边长
        val authorBottom: Float
    )

    /**
     * 按设计稿比例划分区块（副标题取消后标题上限 56%）。
     * 上边距 12% / 标题 ≤56% / 间隔 4% / 图案 12% / 间隔 3% / 作者 10%
     */
    fun zones(width: Float, height: Float): Zones {
        val topPad = height * 0.12f
        val titleHeight = height * 0.56f
        val titleTop = topPad
        val motifTop = titleTop + titleHeight + height * 0.04f
        val motifSize = width * 0.16f
        val authorBottom = height * 0.90f
        return Zones(topPad, titleTop, titleHeight, motifTop, motifSize, authorBottom)
    }

    /**
     * 把书名切成「排版单元」序列：
     *  - 中文/全角字符 -> 单字成元（可任意断行）
     *  - 连续拉丁字母/数字/`.`/`-`/`'` -> 整词成元（不拆 `Vol.3`）
     */
    fun splitUnits(text: String): List<String> {
        val units = ArrayList<String>(text.length)
        val buf = StringBuilder()
        for (ch in text) {
            if (isLatinRun(ch)) {
                buf.append(ch)
            } else {
                if (buf.isNotEmpty()) {
                    units.add(buf.toString())
                    buf.setLength(0)
                }
                units.add(ch.toString())
            }
        }
        if (buf.isNotEmpty()) units.add(buf.toString())
        return units
    }

    private fun isLatinRun(ch: Char): Boolean =
        ch in 'a'..'z' || ch in 'A'..'Z' || ch in '0'..'9' ||
                ch == '.' || ch == '-' || ch == '\'' || ch == '_' ||
                // 西里尔/希腊/拉丁扩展字母也按词切，避免逐字母断行
                ch in '\u00C0'..'\u024F' || ch in '\u0400'..'\u04FF' ||
                ch in '\u0370'..'\u03FF'

    /**
     * 按可用宽度切行（最多 [maxLines] 行），带标点避头。
     *
     * @return 每行的文本
     */
    fun wrap(
        paint: Paint,
        units: List<String>,
        maxWidth: Float,
        maxLines: Int
    ): List<String> {
        if (units.isEmpty()) return emptyList()
        val lines = ArrayList<String>(maxLines)
        var current = StringBuilder()
        var currentWidth = 0f

        for (unit in units) {
            val w = paint.measureText(unit)
            // 行首禁排：若当前行为空且该单元是行首禁排标点，
            // 说明它本该属于上一行末尾 —— 但上一行已收尾，
            // 这里把它并进上一行（不新开一行）。
            if (current.isEmpty() && lines.isNotEmpty() && unit.length == 1 &&
                LINE_HEAD_UNSAFE.indexOf(unit[0]) >= 0
            ) {
                val last = lines.removeAt(lines.size - 1)
                // 上一行加上这个标点；若因此超宽，则从上一行末再挪一个单元
                lines.add(last + unit)
                continue
            }
            // 行尾禁排：若加上该单元后行首禁排字符被推到下一行行首，
            // 则提前收尾当前行（把禁排字符留在今行）。
            if (currentWidth + w > maxWidth && current.isNotEmpty()) {
                lines.add(current.toString())
                current = StringBuilder()
                currentWidth = 0f
                if (lines.size == maxLines) break
            }
            if (currentWidth + w > maxWidth && current.isEmpty()) {
                // 单元本身超宽（超长英文词），只能硬塞
                lines.add(unit)
                currentWidth = 0f
                if (lines.size == maxLines) break
                continue
            }
            current.append(unit)
            currentWidth += w
        }
        if (current.isNotEmpty() && lines.size < maxLines) {
            lines.add(current.toString())
        }
        return lines
    }

    /**
     * 横排标题字号试算：从大到小试，直到放得下。
     *
     * 起始 w*0.185，每次 *0.92，下限 w*0.072；最多 3 行。
     * 最小字号仍放不下 -> 3 行 + 末行省略号。
     *
     * @param maxWidth 可用宽
     * @param maxHeight 可用高（标题区）
     * @return 每行文本 + 采用的字号
     */
    data class TitleLayout(val lines: List<String>, val textSize: Float, val lineCount: Int)

    fun fitTitle(
        paint: Paint,
        name: String,
        width: Float,
        maxWidth: Float,
        maxHeight: Float,
        maxLines: Int = 3
    ): TitleLayout {
        val units = splitUnits(name)
        var size = width * 0.185f
        val minSize = width * 0.072f
        val ellipsis = "…"
        while (size >= minSize) {
            paint.textSize = size
            val lines = wrap(paint, units, maxWidth, maxLines)
            val lineHeight = size * LINE_HEIGHT_RATIO
            // 采用条件：所有单元都被排出（没有因行数上限被丢弃）且高度放得下。
            // 只按行数/高度判断会漏掉「wrap 提前 break 丢了单元」的情况，
            // 导致标题被无声截断。
            val consumed = lines.sumOf { splitUnits(it).size }
            if (consumed >= units.size && lines.size * lineHeight <= maxHeight) {
                return TitleLayout(lines, size, lines.size)
            }
            size *= 0.92f
        }
        // 最小字号仍放不下：截断 + 省略号
        paint.textSize = minSize
        val lineHeight = minSize * LINE_HEIGHT_RATIO
        val capacity = maxLines
        var lines = wrap(paint, units, maxWidth, capacity)
        if (lines.size > capacity) {
            lines = lines.subList(0, capacity)
        }
        // 若还有剩余单元没排进去，末行加省略号并回退到放得下为止
        val used = lines.sumOf { line -> splitUnits(line).size }
        if (used < units.size && lines.isNotEmpty()) {
            val last = lines[lines.size - 1]
            var trimmed = last
            while (paint.measureText(trimmed + ellipsis) > maxWidth && trimmed.isNotEmpty()) {
                trimmed = trimmed.substring(0, trimmed.length - 1)
            }
            lines = ArrayList(lines)
            lines[lines.size - 1] = trimmed + ellipsis
        }
        return TitleLayout(lines, minSize, lines.size)
    }

    /** 小封面阈值：宽小于此值只画标题+细边框（列表项看不清细节，省性能） */
    const val SMALL_WIDTH_PX = 120f

    fun isSmall(width: Float): Boolean = width < SMALL_WIDTH_PX
}
