package io.legado.app.model.cover

import io.legado.app.constant.PreferKey
import io.legado.app.utils.getPrefBoolean
import io.legado.app.utils.getPrefString
import splitties.init.appCtx

/**
 * 封面版式与装饰的配置读取。
 *
 * 全部走 SharedPreferences（PreferKey），与 coverShowName / coverShowAuthor 并列。
 * 读取集中在一个对象里，避免绘制路径反复查偏好。
 */
object CoverStyle {

    /** 版式 */
    enum class Layout { Horizontal, Vertical }

    /** 纹理 */
    enum class Texture { Grid, Dots, None }

    /** 图案 */
    enum class Motif { ByName, Off }

    /** 版式：默认横排；旧竖排保留可切 */
    val layout: Layout
        get() = when (appCtx.getPrefString(PreferKey.coverLayout, "0")) {
            "1" -> Layout.Vertical
            else -> Layout.Horizontal
        }

    /** 纹理：默认细网格 */
    val texture: Texture
        get() = when (appCtx.getPrefString(PreferKey.coverTexture, "0")) {
            "1" -> Texture.Dots
            "2" -> Texture.None
            else -> Texture.Grid
        }

    /** 图案：默认按书名哈希 */
    val motif: Motif
        get() = when (appCtx.getPrefString(PreferKey.coverMotif, "0")) {
            "1" -> Motif.Off
            else -> Motif.ByName
        }

    /** 无作者时是否用占位名（默认开） */
    val fallbackAuthor: Boolean
        get() = appCtx.getPrefBoolean(PreferKey.coverFallbackAuthor, true)

    /** 是否画边框与四角装饰（默认开） */
    val border: Boolean
        get() = appCtx.getPrefBoolean(PreferKey.coverBorder, true)

    /**
     * 配置指纹：参与缓存 key，保证改开关后不会命中旧封面。
     * 只包含影响画面的项，不含 drawBookName/drawBookAuthor（已在 key 里）。
     */
    val styleHash: Int
        get() {
            var h = layout.ordinal
            h = h * 31 + texture.ordinal
            h = h * 31 + motif.ordinal
            h = h * 31 + (if (fallbackAuthor) 1 else 0)
            h = h * 31 + (if (border) 1 else 0)
            return h
        }
}
