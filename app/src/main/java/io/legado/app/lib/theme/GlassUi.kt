package io.legado.app.lib.theme

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.widget.FrameLayout
import io.legado.app.help.PerfHelper
import io.legado.app.help.config.AppConfig

/**
 * 玻璃拟态（Glassmorphism）辅助。
 *
 * ⚠️ 为什么不用 RenderEffect（2026-11-17 修复）：
 *  Android 12+ 的 `View.setRenderEffect(createBlurEffect(...))` 模糊的是
 *  **该 View 及其整个子树**，不是「该 View 背后的内容」。
 *  我们把它用在阅读菜单容器（llBottomBg / titleBar）上，结果是面板里的
 *  图标和文字一起被糊掉（KNOWN_ISSUES P0）。
 *  系统没有公开「模糊窗口背景」的 View 级 API（Android 15 的
 *  setBlurBehindParameters 是窗口级且覆盖面窄；截图模糊在滚动场景掉帧）。
 *
 * 因此这里采用纯视觉近似：半透明底色透出阅读内容 + 顶部高光渐变 + 圆角，
 *  观感接近磨砂玻璃，零额外绘制开销、全版本行为一致。
 */
object GlassUi {

    /** 是否启用玻璃效果（用户开关 + 设备能力） */
    fun enabled(): Boolean = AppConfig.glassUiEnabled && PerfHelper.glassUiEnabled()

    /**
     * 给面板应用玻璃外观（半透明 + 高光渐变，不加任何模糊）。
     *
     * @param view 要应用的面板
     * @param tint 底色（通常是主题背景色）
     * @param alpha 底色不透明度；调低可透出背后内容，增强玻璃感
     * @param radiusPx 圆角半径
     */
    fun apply(view: View?, tint: Int, alpha: Float = 0.86f, radiusPx: Float = 16f) {
        view ?: return
        // 底色是半透明渐变，顶部一条高光模拟玻璃折射；
        // 夜间主题高光压暗，避免在深色底上泛白
        val night = isNight(tint)
        val base = adjustAlpha(tint, alpha)
        val edge = adjustAlpha(if (night) Color.WHITE else Color.WHITE,
            if (night) 0.05f else 0.26f)
        val drawable = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(edge, base, base)
        ).apply {
            cornerRadius = radiusPx
            // 1px 描边让面板边缘在浅色阅读页上仍可分辨
            setStroke(dp1(view), adjustAlpha(if (night) Color.BLACK else inkOf(tint), 0.16f))
        }
        view.background = drawable
    }

    /**
     * 去掉玻璃外观（恢复透明背景，由调用方自行设置实色底）。
     */
    fun clear(view: View?) {
        view ?: return
        view.background = ColorDrawable(Color.TRANSPARENT)
    }

    /**
     * 给一个容器加一层玻璃卡片（用于对话框、底部面板）
     */
    fun card(container: FrameLayout?, tint: Int) {
        container ?: return
        apply(container, tint, alpha = 0.9f, radiusPx = 24f)
    }

    private fun isNight(@androidx.annotation.ColorInt tint: Int): Boolean =
        io.legado.app.utils.ColorUtils.isColorLight(tint).not()

    /** 与底色对比的描边色（浅色主题取深墨、深色主题取浅墨） */
    @androidx.annotation.ColorInt
    private fun inkOf(@androidx.annotation.ColorInt tint: Int): Int =
        if (isNight(tint)) Color.parseColor("#B3E6E6E6") else Color.parseColor("#33222222")

    private fun dp1(view: View): Int =
        (1 * view.context.resources.displayMetrics.density).toInt().coerceAtLeast(1)

    private fun adjustAlpha(@androidx.annotation.ColorInt color: Int, factor: Float): Int =
        Color.argb(
            (factor.coerceIn(0f, 1f) * 255).toInt().coerceIn(0, 255),
            Color.red(color),
            Color.green(color),
            Color.blue(color)
        )
}
