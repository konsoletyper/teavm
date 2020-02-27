/*
 *  Copyright 2017 Alexey Andreev.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.teavm.classlib.java.nio.charset;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.util.Arrays;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.TeaVMTestRunner;
import org.teavm.junit.WholeClassCompilation;

@RunWith(TeaVMTestRunner.class)
@WholeClassCompilation
public class UTF8Test {
    private static char[] hexDigits = "0123456789ABCDEF".toCharArray();
    // Fragment from "The Idiot" by F. Dostoevsky
    private String text =
            "" + "Здесь в моем объяснении я отмечаю все эти цифры и числа. Мне, конечно, всё равно будет, но теперь "
                    + "(и, может быть, только в эту минуту) я желаю, чтобы те, которые будут судить мой поступок, "
                    + "могли ясно "
                    + "видеть, из какой логической цепи выводов вышло мое „последнее убеждение“. Я написал сейчас "
                    + "выше, что "
                    + "окончательная решимость, которой недоставало мне для исполнения моего „последнего убеждения“, "
                    + "произошла "
                    + "во мне, кажется, вовсе не из логического вывода, а от какого-то странного толчка, от одного "
                    + "странного " + "обстоятельства, может быть вовсе не связанного ничем с ходом дела. "
                    + "Дней десять назад зашел ко мне Рогожин, "
                    + "по одному своему делу, о котором здесь лишнее распространяться. Я никогда не видал Рогожина "
                    + "прежде, "
                    + "но слышал о нем очень многое. Я дал ему все нужные справки, и он скоро ушел, а так как он и "
                    + "приходил "
                    + "только за справками, то тем бы дело между нами и кончилось. Но он слишком заинтересовал меня, "
                    + "и весь этот день я был под влиянием странных мыслей, так что решился пойти к нему на другой "
                    + "день сам, "
                    + "отдать визит. Рогожин был мне очевидно не рад и даже „деликатно“ намекнул, что нам нечего "
                    + "продолжать " + "знакомство; но все-таки я провел очень любопытный час, как, вероятно, и он. "
                    + "Между нами был такой контраст, "
                    + "который не мог не сказаться нам обоим, особенно мне: я был человек, уже сосчитавший дни свои, "
                    + "а он - "
                    + "живущий самою полною, непосредственною жизнью, настоящею минутой, без всякой заботы о "
                    + "„последних“ "
                    + "выводах, цифрах или о чем бы то ни было, не касающемся того, на чем... на чем... ну хоть на "
                    + "чем он " + "помешан; пусть простит мне это выражение господин Рогожин, пожалуй хоть как плохому "
                    + "литератору, не "
                    + "умевшему выразить свою мысль. Несмотря на всю его нелюбезность, мне показалось, что он человек"
                    + " с умом и "
                    + "может многое понимать, хотя его мало что интересует из постороннего. Я не намекал ему о моем "
                    + "„последнем " + "убеждении“, но мне почему-то показалось, что он, слушая меня, угадал его. "
                    + "Он промолчал, он ужасно молчалив. "
                    + "Я намекнул ему, уходя, что, несмотря на всю между нами разницу и на все противоположности, - "
                    + "les extrémités se touchent 1 (я растолковал ему это по-русски), так что, может быть, он и сам "
                    + "вовсе не "
                    + "так далек от моего „последнего убеждения“, как кажется. На это он ответил мне очень угрюмою и "
                    + "кислою " + "гримасой, встал, сам сыскал мне мою фуражку, сделав вид, будто бы я сам ухожу, и "
                    + "просто-запросто вывел "
                    + "меня из своего мрачного дома под видом того, что провожает меня из учтивости. Дом его поразил "
                    + "меня; " + "похож на кладбище, а ему, кажется, нравится, что, впрочем, понятно: такая полная, "
                    + "непосредственная "
                    + "жизнь, которою он живет, слишком полна сама по себе, чтобы нуждаться в обстановке.";
    private String hex = ""
            +
            "D097D0B4D0B5D181D18C20D0B220D0BCD0BED0B5D0BC20D0BED0B1D18AD18FD181D0BDD0B5D0BDD0B8D0B820D18F20D0BED"
            + "182D0BCD0B5D187D0B0D18E20D0B2D181D0B520D18DD182D0B820D186D0B8D184D180D18B20D0B820D187D0B8D181D0BBD"
            + "0B02E20D09CD0BDD0B52C20D0BAD0BED0BDD0B5D187D0BDD0BE2C20D0B2D181D19120D180D0B0D0B2D0BDD0BE20D0B1D18"
            + "3D0B4D0B5D1822C20D0BDD0BE20D182D0B5D0BFD0B5D180D18C2028D0B82C20D0BCD0BED0B6D0B5D18220D0B1D18BD182D"
            + "18C2C20D182D0BED0BBD18CD0BAD0BE20D0B220D18DD182D18320D0BCD0B8D0BDD183D182D1832920D18F20D0B6D0B5D0B"
            + "BD0B0D18E2C20D187D182D0BED0B1D18B20D182D0B52C20D0BAD0BED182D0BED180D18BD0B520D0B1D183D0B4D183D1822"
            + "0D181D183D0B4D0B8D182D18C20D0BCD0BED0B920D0BFD0BED181D182D183D0BFD0BED0BA2C20D0BCD0BED0B3D0BBD0B8"
            + "20D18FD181D0BDD0BE20D0B2D0B8D0B4D0B5D182D18C2C20D0B8D0B720D0BAD0B0D0BAD0BED0B920D0BBD0BED0B3D0B8D"
            + "187D0B5D181D0BAD0BED0B920D186D0B5D0BFD0B820D0B2D18BD0B2D0BED0B4D0BED0B220D0B2D18BD188D0BBD0BE20D0"
            + "BCD0BED0B520E2809ED0BFD0BED181D0BBD0B5D0B4D0BDD0B5D0B520D183D0B1D0B5D0B6D0B4D0B5D0BDD0B8D0B5E2809C"
            + "2E20D0AF20D0BDD0B0D0BFD0B8D181D0B0D0BB20D181D0B5D0B9D187D0B0D18120D0B2D18BD188D0B52C20D187D182D0BE"
            + "20D0BED0BAD0BED0BDD187D0B0D182D0B5D0BBD18CD0BDD0B0D18F20D180D0B5D188D0B8D0BCD0BED181D182D18C2C20D0"
            + "BAD0BED182D0BED180D0BED0B920D0BDD0B5D0B4D0BED181D182D0B0D0B2D0B0D0BBD0BE20D0BCD0BDD0B520D0B4D0BBD1"
            + "8F20D0B8D181D0BFD0BED0BBD0BDD0B5D0BDD0B8D18F20D0BCD0BED0B5D0B3D0BE20E2809ED0BFD0BED181D0BBD0B5D0B4"
            + "D0BDD0B5D0B3D0BE20D183D0B1D0B5D0B6D0B4D0B5D0BDD0B8D18FE2809C2C20D0BFD180D0BED0B8D0B7D0BED188D0BBD0"
            + "B020D0B2D0BE20D0BCD0BDD0B52C20D0BAD0B0D0B6D0B5D182D181D18F2C20D0B2D0BED0B2D181D0B520D0BDD0B520D0B8"
            + "D0B720D0BBD0BED0B3D0B8D187D0B5D181D0BAD0BED0B3D0BE20D0B2D18BD0B2D0BED0B4D0B02C20D0B020D0BED18220D0"
            + "BAD0B0D0BAD0BED0B3D0BE2DD182D0BE20D181D182D180D0B0D0BDD0BDD0BED0B3D0BE20D182D0BED0BBD187D0BAD0B02C"
            + "20D0BED18220D0BED0B4D0BDD0BED0B3D0BE20D181D182D180D0B0D0BDD0BDD0BED0B3D0BE20D0BED0B1D181D182D0BED18"
            + "FD182D0B5D0BBD18CD181D182D0B2D0B02C20D0BCD0BED0B6D0B5D18220D0B1D18BD182D18C20D0B2D0BED0B2D181D0B52"
            + "0D0BDD0B520D181D0B2D18FD0B7D0B0D0BDD0BDD0BED0B3D0BE20D0BDD0B8D187D0B5D0BC20D18120D185D0BED0B4D0BE"
            + "D0BC20D0B4D0B5D0BBD0B02E20D094D0BDD0B5D0B920D0B4D0B5D181D18FD182D18C20D0BDD0B0D0B7D0B0D0B420D0B7D0"
            + "B0D188D0B5D0BB20D0BAD0BE20D0BCD0BDD0B520D0A0D0BED0B3D0BED0B6D0B8D0BD2C20D0BFD0BE20D0BED0B4D0BDD0BE"
            + "D0BCD18320D181D0B2D0BED0B5D0BCD18320D0B4D0B5D0BBD1832C20D0BE20D0BAD0BED182D0BED180D0BED0BC20D0B7D0"
            + "B4D0B5D181D18C20D0BBD0B8D188D0BDD0B5D0B520D180D0B0D181D0BFD180D0BED181D182D180D0B0D0BDD18FD182D18CD"
            + "181D18F2E20D0AF20D0BDD0B8D0BAD0BED0B3D0B4D0B020D0BDD0B520D0B2D0B8D0B4D0B0D0BB20D0A0D0BED0B3D0BED0B"
            + "6D0B8D0BDD0B020D0BFD180D0B5D0B6D0B4D0B52C20D0BDD0BE20D181D0BBD18BD188D0B0D0BB20D0BE20D0BDD0B5D0BC20"
            + "D0BED187D0B5D0BDD18C20D0BCD0BDD0BED0B3D0BED0B52E20D0AF20D0B4D0B0D0BB20D0B5D0BCD18320D0B2D181D0B520"
            + "D0BDD183D0B6D0BDD18BD0B520D181D0BFD180D0B0D0B2D0BAD0B82C20D0B820D0BED0BD20D181D0BAD0BED180D0BE20D1"
            + "83D188D0B5D0BB2C20D0B020D182D0B0D0BA20D0BAD0B0D0BA20D0BED0BD20D0B820D0BFD180D0B8D185D0BED0B4D0B8D0"
            + "BB20D182D0BED0BBD18CD0BAD0BE20D0B7D0B020D181D0BFD180D0B0D0B2D0BAD0B0D0BCD0B82C20D182D0BE20D182D0B5"
            + "D0BC20D0B1D18B20D0B4D0B5D0BBD0BE20D0BCD0B5D0B6D0B4D18320D0BDD0B0D0BCD0B820D0B820D0BAD0BED0BDD187D0B"
            + "8D0BBD0BED181D18C2E20D09DD0BE20D0BED0BD20D181D0BBD0B8D188D0BAD0BED0BC20D0B7D0B0D0B8D0BDD182D0B5D18"
            + "0D0B5D181D0BED0B2D0B0D0BB20D0BCD0B5D0BDD18F2C20D0B820D0B2D0B5D181D18C20D18DD182D0BED18220D0B4D0B5D"
            + "0BDD18C20D18F20D0B1D18BD0BB20D0BFD0BED0B420D0B2D0BBD0B8D18FD0BDD0B8D0B5D0BC20D181D182D180D0B0D0BDD"
            + "0BDD18BD18520D0BCD18BD181D0BBD0B5D0B92C20D182D0B0D0BA20D187D182D0BE20D180D0B5D188D0B8D0BBD181D18F2"
            + "0D0BFD0BED0B9D182D0B820D0BA20D0BDD0B5D0BCD18320D0BDD0B020D0B4D180D183D0B3D0BED0B920D0B4D0B5D0BDD1"
            + "8C20D181D0B0D0BC2C20D0BED182D0B4D0B0D182D18C20D0B2D0B8D0B7D0B8D1822E20D0A0D0BED0B3D0BED0B6D0B8D0B"
            + "D20D0B1D18BD0BB20D0BCD0BDD0B520D0BED187D0B5D0B2D0B8D0B4D0BDD0BE20D0BDD0B520D180D0B0D0B420D0B820D0B4"
            + "D0B0D0B6D0B520E2809ED0B4D0B5D0BBD0B8D0BAD0B0D182D0BDD0BEE2809C20D0BDD0B0D0BCD0B5D0BAD0BDD183D0BB2"
            + "C20D187D182D0BE20D0BDD0B0D0BC20D0BDD0B5D187D0B5D0B3D0BE20D0BFD180D0BED0B4D0BED0BBD0B6D0B0D182D18C2"
            + "0D0B7D0BDD0B0D0BAD0BED0BCD181D182D0B2D0BE3B20D0BDD0BE20D0B2D181D0B52DD182D0B0D0BAD0B820D18F20D0BF"
            + "D180D0BED0B2D0B5D0BB20D0BED187D0B5D0BDD18C20D0BBD18ED0B1D0BED0BFD18BD182D0BDD18BD0B920D187D0B0D181"
            + "2C20D0BAD0B0D0BA2C20D0B2D0B5D180D0BED18FD182D0BDD0BE2C20D0B820D0BED0BD2E20D09CD0B5D0B6D0B4D18320D0B"
            + "DD0B0D0BCD0B820D0B1D18BD0BB20D182D0B0D0BAD0BED0B920D0BAD0BED0BDD182D180D0B0D181D1822C20D0BAD0BED182"
            + "D0BED180D18BD0B920D0BDD0B520D0BCD0BED0B320D0BDD0B520D181D0BAD0B0D0B7D0B0D182D18CD181D18F20D0BDD0B0D0"
            + "BC20D0BED0B1D0BED0B8D0BC2C20D0BED181D0BED0B1D0B5D0BDD0BDD0BE20D0BCD0BDD0B53A20D18F20D0B1D18BD0BB20D18"
            + "7D0B5D0BBD0BED0B2D0B5D0BA2C20D183D0B6D0B520D181D0BED181D187D0B8D182D0B0D0B2D188D0B8D0B920D0B4D0BDD0"
            + "B820D181D0B2D0BED0B82C20D0B020D0BED0BD202D20D0B6D0B8D0B2D183D189D0B8D0B920D181D0B0D0BCD0BED18E20D"
            + "0BFD0BED0BBD0BDD0BED18E2C20D0BDD0B5D0BFD0BED181D180D0B5D0B4D181D182D0B2D0B5D0BDD0BDD0BED18E20D0B6"
            + "D0B8D0B7D0BDD18CD18E2C20D0BDD0B0D181D182D0BED18FD189D0B5D18E20D0BCD0B8D0BDD183D182D0BED0B92C20D0"
            + "B1D0B5D0B720D0B2D181D18FD0BAD0BED0B920D0B7D0B0D0B1D0BED182D18B20D0BE20E2809ED0BFD0BED181D0BBD0B5"
            + "D0B4D0BDD0B8D185E2809C20D0B2D18BD0B2D0BED0B4D0B0D1852C20D186D0B8D184D180D0B0D18520D0B8D0BBD0B820"
            + "D0BE20D187D0B5D0BC20D0B1D18B20D182D0BE20D0BDD0B820D0B1D18BD0BBD0BE2C20D0BDD0B520D0BAD0B0D181D0B0D"
            + "18ED189D0B5D0BCD181D18F20D182D0BED0B3D0BE2C20D0BDD0B020D187D0B5D0BC2E2E2E20D0BDD0B020D187D0B5D0B"
            + "C2E2E2E20D0BDD18320D185D0BED182D18C20D0BDD0B020D187D0B5D0BC20D0BED0BD20D0BFD0BED0BCD0B5D188D0B0D"
            + "0BD3B20D0BFD183D181D182D18C20D0BFD180D0BED181D182D0B8D18220D0BCD0BDD0B520D18DD182D0BE20D0B2D18B"
            + "D180D0B0D0B6D0B5D0BDD0B8D0B520D0B3D0BED181D0BFD0BED0B4D0B8D0BD20D0A0D0BED0B3D0BED0B6D0B8D0BD2C20"
            + "D0BFD0BED0B6D0B0D0BBD183D0B920D185D0BED182D18C20D0BAD0B0D0BA20D0BFD0BBD0BED185D0BED0BCD18320D0B"
            + "BD0B8D182D0B5D180D0B0D182D0BED180D1832C20D0BDD0B520D183D0BCD0B5D0B2D188D0B5D0BCD18320D0B2D18BD18"
            + "0D0B0D0B7D0B8D182D18C20D181D0B2D0BED18E20D0BCD18BD181D0BBD18C2E20D09DD0B5D181D0BCD0BED182D180D18"
            + "F20D0BDD0B020D0B2D181D18E20D0B5D0B3D0BE20D0BDD0B5D0BBD18ED0B1D0B5D0B7D0BDD0BED181D182D18C2C20D0B"
            + "CD0BDD0B520D0BFD0BED0BAD0B0D0B7D0B0D0BBD0BED181D18C2C20D187D182D0BE20D0BED0BD20D187D0B5D0BBD0BED"
            + "0B2D0B5D0BA20D18120D183D0BCD0BED0BC20D0B820D0BCD0BED0B6D0B5D18220D0BCD0BDD0BED0B3D0BED0B520D0BF"
            + "D0BED0BDD0B8D0BCD0B0D182D18C2C20D185D0BED182D18F20D0B5D0B3D0BE20D0BCD0B0D0BBD0BE20D187D182D0BE2"
            + "0D0B8D0BDD182D0B5D180D0B5D181D183D0B5D18220D0B8D0B720D0BFD0BED181D182D0BED180D0BED0BDD0BDD0B5D"
            + "0B3D0BE2E20D0AF20D0BDD0B520D0BDD0B0D0BCD0B5D0BAD0B0D0BB20D0B5D0BCD18320D0BE20D0BCD0BED0B5D0BC20"
            + "E2809ED0BFD0BED181D0BBD0B5D0B4D0BDD0B5D0BC20D183D0B1D0B5D0B6D0B4D0B5D0BDD0B8D0B8E2809C2C20D0BD"
            + "D0BE20D0BCD0BDD0B520D0BFD0BED187D0B5D0BCD1832DD182D0BE20D0BFD0BED0BAD0B0D0B7D0B0D0BBD0BED181D18"
            + "C2C20D187D182D0BE20D0BED0BD2C20D181D0BBD183D188D0B0D18F20D0BCD0B5D0BDD18F2C20D183D0B3D0B0D0B4D0"
            + "B0D0BB20D0B5D0B3D0BE2E20D09ED0BD20D0BFD180D0BED0BCD0BED0BBD187D0B0D0BB2C20D0BED0BD20D183D0B6D0B"
            + "0D181D0BDD0BE20D0BCD0BED0BBD187D0B0D0BBD0B8D0B22E20D0AF20D0BDD0B0D0BCD0B5D0BAD0BDD183D0BB20D0B5"
            + "D0BCD1832C20D183D185D0BED0B4D18F2C20D187D182D0BE2C20D0BDD0B5D181D0BCD0BED182D180D18F20D0BDD0B02"
            + "0D0B2D181D18E20D0BCD0B5D0B6D0B4D18320D0BDD0B0D0BCD0B820D180D0B0D0B7D0BDD0B8D186D18320D0B820D0BD"
            + "D0B020D0B2D181D0B520D0BFD180D0BED182D0B8D0B2D0BED0BFD0BED0BBD0BED0B6D0BDD0BED181D182D0B82C202D20"
            + "6C65732065787472C3A96D6974C3A97320736520746F756368656E7420312028D18F20D180D0B0D181D182D0BED0BBD0"
            + "BAD0BED0B2D0B0D0BB20D0B5D0BCD18320D18DD182D0BE20D0BFD0BE2DD180D183D181D181D0BAD0B8292C20D182D0B"
            + "0D0BA20D187D182D0BE2C20D0BCD0BED0B6D0B5D18220D0B1D18BD182D18C2C20D0BED0BD20D0B820D181D0B0D0BC20"
            + "D0B2D0BED0B2D181D0B520D0BDD0B520D182D0B0D0BA20D0B4D0B0D0BBD0B5D0BA20D0BED18220D0BCD0BED0B5D0B3D0"
            + "BE20E2809ED0BFD0BED181D0BBD0B5D0B4D0BDD0B5D0B3D0BE20D183D0B1D0B5D0B6D0B4D0B5D0BDD0B8D18FE2809C2C"
            + "20D0BAD0B0D0BA20D0BAD0B0D0B6D0B5D182D181D18F2E20D09DD0B020D18DD182D0BE20D0BED0BD20D0BED182D0B2D0"
            + "B5D182D0B8D0BB20D0BCD0BDD0B520D0BED187D0B5D0BDD18C20D183D0B3D180D18ED0BCD0BED18E20D0B820D0BAD0B8"
            + "D181D0BBD0BED18E20D0B3D180D0B8D0BCD0B0D181D0BED0B92C20D0B2D181D182D0B0D0BB2C20D181D0B0D0BC20D181D"
            + "18BD181D0BAD0B0D0BB20D0BCD0BDD0B520D0BCD0BED18E20D184D183D180D0B0D0B6D0BAD1832C20D181D0B4D0B5D0BB"
            + "D0B0D0B220D0B2D0B8D0B42C20D0B1D183D0B4D182D0BE20D0B1D18B20D18F20D181D0B0D0BC20D183D185D0BED0B6D1"
            + "832C20D0B820D0BFD180D0BED181D182D0BE2DD0B7D0B0D0BFD180D0BED181D182D0BE20D0B2D18BD0B2D0B5D0BB20D0"
            + "BCD0B5D0BDD18F20D0B8D0B720D181D0B2D0BED0B5D0B3D0BE20D0BCD180D0B0D187D0BDD0BED0B3D0BE20D0B4D0BED0"
            + "BCD0B020D0BFD0BED0B420D0B2D0B8D0B4D0BED0BC20D182D0BED0B3D0BE2C20D187D182D0BE20D0BFD180D0BED0B2D0"
            + "BED0B6D0B0D0B5D18220D0BCD0B5D0BDD18F20D0B8D0B720D183D187D182D0B8D0B2D0BED181D182D0B82E20D094D0BE"
            + "D0BC20D0B5D0B3D0BE20D0BFD0BED180D0B0D0B7D0B8D0BB20D0BCD0B5D0BDD18F3B20D0BFD0BED185D0BED0B620D0BD"
            + "D0B020D0BAD0BBD0B0D0B4D0B1D0B8D189D0B52C20D0B020D0B5D0BCD1832C20D0BAD0B0D0B6D0B5D182D181D18F2C20D"
            + "0BDD180D0B0D0B2D0B8D182D181D18F2C20D187D182D0BE2C20D0B2D0BFD180D0BED187D0B5D0BC2C20D0BFD0BED0BDD1"
            + "8FD182D0BDD0BE3A20D182D0B0D0BAD0B0D18F20D0BFD0BED0BBD0BDD0B0D18F2C20D0BDD0B5D0BFD0BED181D180D0B5D"
            + "0B4D181D182D0B2D0B5D0BDD0BDD0B0D18F20D0B6D0B8D0B7D0BDD18C2C20D0BAD0BED182D0BED180D0BED18E20D0BED"
            + "0BD20D0B6D0B8D0B2D0B5D1822C20D181D0BBD0B8D188D0BAD0BED0BC20D0BFD0BED0BBD0BDD0B020D181D0B0D0BCD0B"
            + "020D0BFD0BE20D181D0B5D0B1D0B52C20D187D182D0BED0B1D18B20D0BDD183D0B6D0B4D0B0D182D18CD181D18F20D0B"
            + "220D0BED0B1D181D182D0B0D0BDD0BED0B2D0BAD0B52E";

    @Test
    public void encode1() {
        runEncode(600, 600);
    }

    @Test
    public void encode2() {
        runEncode(600, 100);
    }

    @Test
    public void encode3() {
        runEncode(100, 600);
    }

    @Test
    public void decode1() {
        runDecode(600, 600);
    }

    @Test
    public void decode2() {
        runDecode(600, 100);
    }

    @Test
    public void decode3() {
        runDecode(100, 600);
    }

    @Test
    public void replaceMalformedSurrogatePair() {
        Charset charset = Charset.forName("UTF-8");
        ByteBuffer buffer = charset.encode("a\uD800\uD800b");
        byte[] result = new byte[buffer.remaining()];
        buffer.get(result);
        assertArrayEquals(new byte[] { 97, 63, 63, 98 }, result);
    }

    @Test
    public void encodeSurrogate() {
        Charset charset = Charset.forName("UTF-8");
        ByteBuffer buffer = charset.encode("a\uD800\uDC00b");
        byte[] result = new byte[buffer.remaining()];
        buffer.get(result);
        assertArrayEquals(new byte[] { 97, -16, -112, -128, -128, 98 }, result);
    }

    @Test
    public void encodeSupplementary() {
        Charset charset = Charset.forName("UTF-8");
        StringBuilder sb = new StringBuilder();
        sb.appendCodePoint(0xfedcb);
        ByteBuffer buffer = charset.encode(sb.toString());
        byte[] result = new byte[buffer.remaining()];
        buffer.get(result);
        assertArrayEquals(new byte[] { -13, -66, -73, -117 }, result);
    }

    @Test
    public void replaceMalformedFirstByte() {
        Charset charset = Charset.forName("UTF-8");
        CharBuffer buffer = charset.decode(ByteBuffer.wrap(new byte[] { 97, (byte) 0xFF, 98 }));
        char[] result = new char[buffer.remaining()];
        buffer.get(result);
        assertEquals("a\uFFFDb", new String(result));
    }

    @Test
    public void replaceMalformedMidByte() {
        Charset charset = Charset.forName("UTF-8");
        CharBuffer buffer = charset.decode(ByteBuffer.wrap(new byte[] { 97, (byte) 0xC0, 98, 98 }));
        char[] result = new char[buffer.remaining()];
        buffer.get(result);
        assertEquals("a\uFFFDbb", new String(result));
    }

    @Test
    public void decodeLongUTF8ByteArray() {
        byte[] bytes = new byte[16384];
        int i = 0;
        while (i < bytes.length) {
            bytes[i++] = -16;
            bytes[i++] = -66;
            bytes[i++] = -78;
            bytes[i++] = -69;
        }
        Charset charset = Charset.forName("UTF-8");
        CharBuffer buffer = charset.decode(ByteBuffer.wrap(bytes));
        assertEquals('\uD8BB', buffer.get(8190));
        assertEquals('\uDCBB', buffer.get(8191));
    }

    private void runEncode(int inSize, int outSize) {
        char[] input = text.toCharArray();
        byte[] output = new byte[16384];
        int inPos = 0;
        int outPos = 0;
        CharsetEncoder encoder = Charset.forName("UTF-8").newEncoder();
        CoderResult result = CoderResult.UNDERFLOW;

        while (true) {
            int inLen = Math.min(inSize, input.length - inPos);
            CharBuffer in = CharBuffer.wrap(input, inPos, inLen);
            int outLen = Math.min(outSize, output.length - outPos);
            ByteBuffer out = ByteBuffer.wrap(output, outPos, outLen);
            result = encoder.encode(in, out, inPos + inLen >= input.length);
            inPos = in.position();
            outPos = out.position();
            if (result.isError() || inPos >= input.length) {
                break;
            }
        }

        assertTrue("Should be UNDERFLOW after encoding", result.isUnderflow());

        while (true) {
            int outLen = Math.min(outSize, output.length - outPos);
            ByteBuffer out = ByteBuffer.wrap(output, outPos, outLen);
            result = encoder.flush(out);
            outPos = out.position();
            if (result.isUnderflow()) {
                break;
            }
        }

        assertTrue("Should be UNDERFLOW after flushing", result.isUnderflow());
        output = Arrays.copyOf(output, outPos);
        assertEquals(hex, bytesToHex(output));
    }

    private void runDecode(int inSize, int outSize) {
        byte[] input = hexToBytes(hex);
        char[] output = new char[16384];
        int inPos = 0;
        int outPos = 0;
        CharsetDecoder decoder = Charset.forName("UTF-8").newDecoder();
        CoderResult result = CoderResult.UNDERFLOW;

        while (true) {
            int inLen = Math.min(inSize, input.length - inPos);
            ByteBuffer in = ByteBuffer.wrap(input, inPos, inLen);
            int outLen = Math.min(outSize, output.length - outPos);
            CharBuffer out = CharBuffer.wrap(output, outPos, outLen);
            result = decoder.decode(in, out, inPos + inLen >= input.length);
            inPos = in.position();
            outPos = out.position();
            if (result.isError() || inPos >= input.length) {
                break;
            }
        }

        assertTrue("Should be UNDERFLOW after encoding", result.isUnderflow());

        while (true) {
            int outLen = Math.min(outSize, output.length - outPos);
            CharBuffer out = CharBuffer.wrap(output, outPos, outLen);
            result = decoder.flush(out);
            outPos = out.position();
            if (result.isUnderflow()) {
                break;
            }
        }

        assertTrue("Should be UNDERFLOW after flushing", result.isUnderflow());
        output = Arrays.copyOf(output, outPos);
        assertEquals(text, new String(output));
    }

    private String bytesToHex(byte[] bytes) {
        char[] result = new char[bytes.length * 2];
        int j = 0;
        for (int i = 0; i < bytes.length; ++i) {
            int b = bytes[i] & 0xFF;
            result[j++] = hexDigits[b >> 4];
            result[j++] = hexDigits[b & 0xF];
        }
        return new String(result);
    }

    private byte[] hexToBytes(String hex) {
        char[] chars = hex.toCharArray();
        byte[] result = new byte[chars.length / 2];
        int j = 0;
        for (int i = 0; i < chars.length; i += 2) {
            char hi = chars[i];
            char lo = chars[i + 1];
            result[j++] = (byte) ((digit(hi) << 4) | digit(lo));
        }
        return result;
    }

    private static int digit(char c) {
        if (c >= '0' && c <= '9') {
            return c - '0';
        }
        return c - 'A' + 10;
    }
}
