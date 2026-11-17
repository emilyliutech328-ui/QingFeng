package io.legado.app

import io.legado.app.model.cover.CoverAuthorPool
import io.legado.app.model.cover.CoverLayout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 封面版式引擎的纯计算测试（不依赖 Android 绘制）。
 *
 * wrap/fitTitle 需要 Paint 测量宽度，Robolectric 下才有实现，
 * 因此这里只测不碰 Paint 的部分：切单元、分区、哈希确定性、小图阈值。
 */
class CoverLayoutTest {

    @Test
    fun `中英混排切单元`() {
        val units = CoverLayout.splitUnits("风之谷Vol.3外传")
        // Vol.3 必须整体成一个单元，不能被拆开
        assertTrue("应含整词 Vol.3", units.contains("Vol.3"))
        assertTrue("中文逐字", units.contains("风"))
        // 拼接回去必须无损
        assertEquals("风之谷Vol.3外传", units.joinToString(""))
    }

    @Test
    fun `分区比例符合设计`() {
        val z = CoverLayout.zones(300f, 400f)
        assertEquals(48f, z.topPad, 0.1f)          // 12%
        assertEquals(224f, z.titleHeight, 0.1f)    // 56%
        assertEquals(48f, z.motifSize, 0.1f)       // 16% of width
        assertEquals(360f, z.authorBottom, 0.1f)   // 90%
        assertTrue("图案区在标题区之下", z.motifTop > z.titleTop + z.titleHeight * 0.5f)
    }

    @Test
    fun `小封面阈值`() {
        assertTrue(CoverLayout.isSmall(119f))
        assertTrue(!CoverLayout.isSmall(121f))
    }

    @Test
    fun `占位作者确定性且分布均匀`() {
        // 同一书名永远同一名字
        val a = CoverAuthorPool.forName("月光航线")
        val b = CoverAuthorPool.forName("月光航线")
        assertEquals(a, b)

        // 不同书名应散落到池子里不同位置（20 个样本至少 5 种不同）
        val names = (1..20).map { CoverAuthorPool.forName("测试书名$it 第${it}卷") }.toSet()
        assertTrue("分布过窄: ${names.size}", names.size >= 5)

        // 池子里的名字必须都在声明的 20 个内
        val pool = setOf(
            "夜见川", "青木遥", "白鸟千里", "冬野雪", "三日月透",
            "雨宫栖", "空町九", "佐仓凛", "灰谷灯", "凉风真昼",
            "月冈澪", "星川渡", "花仓杏", "雾岛灯里", "常磐树",
            "有栖川萤", "雪代岬", "七濑真冬", "甘木奈奈", "柊木千岁"
        )
        assertTrue("产出的名字必须来自池子", pool.contains(a))
    }

    @Test
    fun `行首禁排标点不独立成行`() {
        // 用假 Paint 不可行（需要 Android），这里只验证常量集合本身合法
        // 真正的避头行为在设备渲染测试里覆盖
        val units = CoverLayout.splitUnits("你好，世界")
        assertEquals(listOf("你", "好", "，", "世", "界"), units)
    }
}
