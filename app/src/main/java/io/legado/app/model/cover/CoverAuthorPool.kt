package io.legado.app.model.cover

/**
 * 无作者时的占位笔名池。
 *
 * 分配必须确定性：用书名哈希选，同一本书永远同一个名字，
 * 否则滚动重绘时名字会变、缓存也会失效。
 * 只用于封面显示，不写数据库、不进详情页。
 */
object CoverAuthorPool {

    /** 20 个中性偏日轻风格的笔名（非真实作者） */
    private val POOL = arrayOf(
        "夜见川", "青木遥", "白鸟千里", "冬野雪", "三日月透",
        "雨宫栖", "空町九", "佐仓凛", "灰谷灯", "凉风真昼",
        "月冈澪", "星川渡", "花仓杏", "雾岛灯里", "常磐树",
        "有栖川萤", "雪代岬", "七濑真冬", "甘木奈奈", "柊木千岁"
    )

    /**
     * 书名 -> 确定性占位名。
     * 用字符串内容哈希（不用 String.hashCode 的平台差异风险，
     * 这里显式自己算，保证跨版本/跨设备一致）。
     */
    fun forName(name: String): String {
        var h = 0x811C9DC5.toInt() // FNV-1a 32bit
        for (ch in name) {
            h = h xor ch.code
            h *= 0x01000193
        }
        val idx = (h and 0x7FFFFFFF) % POOL.size
        return POOL[idx]
    }
}
