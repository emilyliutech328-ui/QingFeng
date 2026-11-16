/*
 * Material You (Material Design 3) 动态取色核心算法。
 *
 * 来源：material-foundation/material-color-utilities @ 4dbd965df6 (2022-06-10)
 * 许可：Apache License 2.0（原许可头保留于各文件正文）
 * 改动：包名统一为 io.legado.app.lib.theme.m3；内部 import 因同包而移除。
 * 参考文档：https://m3.material.io/styles/color/system
 */
/*
 * Copyright 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.legado.app.lib.theme.m3;

interface Quantizer {
  public QuantizerResult quantize(int[] pixels, int maxColors);
}
