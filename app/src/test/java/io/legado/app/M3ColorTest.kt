package io.legado.app

import io.legado.app.lib.theme.m3.Hct
import io.legado.app.lib.theme.m3.QuantizerCelebi
import io.legado.app.lib.theme.m3.Scheme
import io.legado.app.lib.theme.m3.Score
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Material You / M3 动态取色引擎的行为测试。
 *
 * 不锁定具体色值（上游算法微调 tone 档位不该弄坏测试），
 * 只验证结构性约束：tone 档位范围、明暗两套的差异、对比度方向、
 * 量化与打分的基本行为。
 */
class M3ColorTest {

    private val blueSeed = 0xFF1565C0.toInt()   // Material Blue 800
    private val redSeed = 0xFFC62828.toInt()    // Material Red 800

    @Test
    fun `hct tone 范围合法`() {
        val hct = Hct.fromInt(blueSeed)
        assertTrue("tone ∈ [0,100]", hct.tone in 0.0..100.0)
        assertTrue("hue ∈ [0,360)", hct.hue in 0.0..360.0)
        assertTrue("chroma >= 0", hct.chroma >= 0.0)
    }

    @Test
    fun `浅色主题 primary 是深档 对背景可读`() {
        val light = Scheme.light(blueSeed)
        val primaryTone = Hct.fromInt(light.primary).tone
        val backgroundTone = Hct.fromInt(light.background).tone
        // M3 浅色：primary 在 tone 30~50 区间，背景接近白（≥ 90）
        assertTrue("浅色 primary tone 实际=$primaryTone", primaryTone in 25.0..55.0)
        assertTrue("浅色 background tone 实际=$backgroundTone", backgroundTone >= 88.0)
        assertTrue("主色应比背景深", primaryTone < backgroundTone)
    }

    @Test
    fun `深色主题 primary 是亮档 对背景可读`() {
        val dark = Scheme.dark(blueSeed)
        val primaryTone = Hct.fromInt(dark.primary).tone
        val backgroundTone = Hct.fromInt(dark.background).tone
        // M3 深色：primary 在 tone 70~90 区间，背景接近黑（≤ 15）
        assertTrue("深色 primary tone 实际=$primaryTone", primaryTone in 65.0..95.0)
        assertTrue("深色 background tone 实际=$backgroundTone", backgroundTone <= 16.0)
        assertTrue("主色应比背景亮", primaryTone > backgroundTone)
    }

    @Test
    fun `明暗两套的背景明显不同`() {
        val light = Scheme.light(blueSeed)
        val dark = Scheme.dark(blueSeed)
        val lightTone = Hct.fromInt(light.background).tone
        val darkTone = Hct.fromInt(dark.background).tone
        assertTrue("亮暗背景 tone 差应 > 60，实际=${lightTone - darkTone}", lightTone - darkTone > 60.0)
    }

    @Test
    fun `种子的色相大体保留`() {
        // 蓝种子产出的 primary 色相应仍在蓝区（220~280°），不应偏到红/绿
        val primary = Scheme.light(blueSeed).primary
        val hue = Hct.fromInt(primary).hue
        assertTrue("蓝色种子的 primary hue 实际=$hue", hue in 200.0..300.0)

        val redPrimary = Scheme.light(redSeed).primary
        val redHue = Hct.fromInt(redPrimary).hue
        assertTrue("红色种子的 primary hue 实际=$redHue", redHue < 40.0 || redHue > 340.0)
    }

    @Test
    fun `量化与打分能选出主导色`() {
        // 构造一个明显的蓝色主导图（80% 蓝 + 20% 灰）
        val pixels = IntArray(1000) { i ->
            if (i < 800) 0xFF1565C0.toInt() else 0xFFEEEEEE.toInt()
        }
        // QuantizerCelebi 直接返回 Map<像素色, 计数>
        val colorToCount = QuantizerCelebi.quantize(pixels, 16)
        assertTrue("量化应产出颜色", colorToCount.isNotEmpty())

        val ranked = Score.score(colorToCount)
        assertTrue("打分应产出候选", ranked.isNotEmpty())
        // 第一名应落在蓝色主导区域（色相 200~300）
        val bestHue = Hct.fromInt(ranked[0]).hue
        assertTrue("首选种子色相实际=$bestHue", bestHue in 200.0..300.0)
    }

    @Test
    fun `container 色 tone 档介于主色与背景之间`() {
        val light = Scheme.light(blueSeed)
        val pTone = Hct.fromInt(light.primary).tone
        val cTone = Hct.fromInt(light.primaryContainer).tone
        val bgTone = Hct.fromInt(light.background).tone
        assertTrue(
            "浅色 container 应比 primary 浅、比背景深（p=$pTone c=$cTone bg=$bgTone）",
            cTone > pTone && cTone < bgTone
        )
    }
}
