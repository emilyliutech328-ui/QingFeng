package io.legado.app.model.cover

import androidx.test.core.app.ApplicationProvider
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * 封面生成离屏渲染测试（开发辅助，默认跳过）。
 *
 * 用 Robolectric NATIVE 图形模式把 CoverGenerator 真实绘制到 Bitmap，
 * 输出拼图 PNG 供人眼验收：横排字号试算、图案哈希、边框、作者兜底。
 *
 * 运行：./gradlew testAppDebugUnitTest --tests "*CoverRenderTest*"
 * 产物：build/render_test/cover_sheet.png
 */
@Ignore("开发辅助渲染测试，默认跳过；需要时单独运行：./gradlew testAppDebugUnitTest --tests *CoverRenderTest*")

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], application = android.app.Application::class, qualifiers = "w390dp-h844dp-xxhdpi")
class CoverRenderTest {

    private val titles = listOf(
        "月光航线",
        "玻璃海岸纪事",
        "关于我在便利店打工这件事",
        "Ночной экспресс",
        "风",
        "星空邮局与第七封信的冒险物语",
        "残棠记事",
        "Vol.3 拱桥之下"
    )

    @Test
    fun renderCoverSheet() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        // Robolectric 裸 Application 不会跑 App Startup，注入 splitties appCtx。
        // injectAsAppCtx 被 splitties 标记为内部 API（Kotlin 侧不可见），走反射。
        Class.forName("splitties.init.AppCtxKt")
            .getMethod("injectAsAppCtx", android.content.Context::class.java)
            .invoke(null, context)

        val cellW = 300
        val cellH = 400
        val cols = 4
        val rows = 2
        val sheet = android.graphics.Bitmap.createBitmap(
            cellW * cols, cellH * rows, android.graphics.Bitmap.Config.ARGB_8888
        )
        val canvas = android.graphics.Canvas(sheet)

        titles.forEachIndexed { i, title ->
            val author = if (i % 3 == 0) null else "林见舟"
            val bmp = CoverGenerator.generate(title, author, cellW, cellH, true)
            val x = (i % cols) * cellW
            val y = (i / cols) * cellH
            canvas.drawBitmap(bmp, x.toFloat(), y.toFloat(), null)
        }

        val out = File("build/render_test")
        out.mkdirs()
        val file = File(out, "cover_sheet.png")
        file.outputStream().use { sheet.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        println("cover sheet written: " + file.absolutePath)
        assertTrue("sheet 应非空", sheet.height > 0)
    }

    private fun assertTrue(msg: String, cond: Boolean) {
        org.junit.Assert.assertTrue(msg, cond)
    }
}
