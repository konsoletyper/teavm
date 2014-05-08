/*
 *  Copyright 2014 Alexey Andreev.
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
/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.teavm.classlib.java.util.regex;

/**
 * This class gives us a hashtable that contains canonical
 * classes that are generated from
 * http://www.unicode.org/Public/4.0-Update/UnicodeData-4.0.0.txt.
 */
class CanClasses{

    private static IntHash table = null;

    private CanClasses() {
    }

    public static IntHash getHashCanClasses() {
        if (table != null) {
            return table;
        } else {
            table = new IntHash(384);
            table.put(0x20D0, 230);
            table.put(0x0A4D, 9);
            table.put(0x0E49, 107);
            table.put(0x0954, 230);
            table.put(0x0E48, 107);
            table.put(0x0953, 230);
            table.put(0x0952, 220);
            table.put(0x0951, 230);
            table.put(0x094D, 9);
            table.put(0x0E3A, 9);
            table.put(0x17DD, 230);
            table.put(0x0A3C, 7);
            table.put(0x0E39, 103);
            table.put(0x0E38, 103);
            table.put(0x17D2, 9);
            table.put(0x093C, 7);
            table.put(0x06ED, 220);
            table.put(0x06EC, 230);
            table.put(0x06EB, 230);
            table.put(0x06EA, 220);
            table.put(0x06E8, 230);
            table.put(0x06E7, 230);
            table.put(0x06E4, 230);
            table.put(0x06E3, 220);
            table.put(0x06E2, 230);
            table.put(0x06E1, 230);
            table.put(0x06E0, 230);
            table.put(0x06DF, 230);
            table.put(0x06DC, 230);
            table.put(0x06DB, 230);
            table.put(0x06DA, 230);
            table.put(0x0BCD, 9);
            table.put(0x0486, 230);
            table.put(0x0485, 230);
            table.put(0x0484, 230);
            table.put(0x0FC6, 220);
            table.put(0x0483, 230);
            table.put(0x06D9, 230);
            table.put(0x06D8, 230);
            table.put(0x06D7, 230);
            table.put(0x06D6, 230);
            table.put(0xA806, 9);
            table.put(0x193B, 220);
            table.put(0x193A, 230);
            table.put(0x1939, 222);
            table.put(0x0D4D, 9);
            table.put(0x1A18, 220);
            table.put(0x1A17, 230);
            table.put(0x1D1AD, 230);
            table.put(0x1D1AC, 230);
            table.put(0x1D1AB, 230);
            table.put(0x1D1AA, 230);
            table.put(0xFB1E, 26);
            table.put(0x0ECB, 122);
            table.put(0x0ECA, 122);
            table.put(0x0ACD, 9);
            table.put(0x0EC9, 122);
            table.put(0x0EC8, 122);
            table.put(0x09CD, 9);
            table.put(0x0ABC, 7);
            table.put(0x0EB9, 118);
            table.put(0x0EB8, 118);
            table.put(0x05C7, 18);
            table.put(0x05C5, 220);
            table.put(0x05C4, 230);
            table.put(0x05C2, 25);
            table.put(0x05C1, 24);
            table.put(0x036F, 230);
            table.put(0x036E, 230);
            table.put(0x09BC, 7);
            table.put(0x036D, 230);
            table.put(0x036C, 230);
            table.put(0x036B, 230);
            table.put(0x036A, 230);
            table.put(0x05BF, 23);
            table.put(0x05BD, 22);
            table.put(0x05BC, 21);
            table.put(0x05BB, 20);
            table.put(0x0C56, 91);
            table.put(0x0C55, 84);
            table.put(0x0369, 230);
            table.put(0x0368, 230);
            table.put(0x0367, 230);
            table.put(0x0366, 230);
            table.put(0x0365, 230);
            table.put(0x0364, 230);
            table.put(0x0363, 230);
            table.put(0x0362, 233);
            table.put(0x05B9, 19);
            table.put(0x0361, 234);
            table.put(0x05B8, 18);
            table.put(0x0360, 234);
            table.put(0x05B7, 17);
            table.put(0x05B6, 16);
            table.put(0x05B5, 15);
            table.put(0x05B4, 14);
            table.put(0x05B3, 13);
            table.put(0x05B2, 12);
            table.put(0x05B1, 11);
            table.put(0x0C4D, 9);
            table.put(0x05B0, 10);
            table.put(0x035F, 233);
            table.put(0x035E, 234);
            table.put(0x035D, 234);
            table.put(0x035C, 233);
            table.put(0x035B, 230);
            table.put(0x035A, 220);
            table.put(0x05AF, 230);
            table.put(0x05AE, 228);
            table.put(0x05AD, 222);
            table.put(0x05AC, 230);
            table.put(0x05AB, 230);
            table.put(0x05AA, 220);
            table.put(0x1039, 9);
            table.put(0x0359, 220);
            table.put(0x0358, 232);
            table.put(0x1037, 7);
            table.put(0x0357, 230);
            table.put(0x0356, 220);
            table.put(0x0355, 220);
            table.put(0x0354, 220);
            table.put(0x0353, 220);
            table.put(0x0352, 230);
            table.put(0x05A9, 230);
            table.put(0x0351, 230);
            table.put(0x05A8, 230);
            table.put(0x0350, 230);
            table.put(0x05A7, 220);
            table.put(0x05A6, 220);
            table.put(0x05A5, 220);
            table.put(0x05A4, 220);
            table.put(0x05A3, 220);
            table.put(0x05A2, 220);
            table.put(0x074A, 230);
            table.put(0x05A1, 230);
            table.put(0x05A0, 230);
            table.put(0x034E, 220);
            table.put(0x034D, 220);
            table.put(0x034C, 230);
            table.put(0x034B, 230);
            table.put(0x0749, 230);
            table.put(0x034A, 230);
            table.put(0x0748, 220);
            table.put(0x0747, 230);
            table.put(0x0746, 220);
            table.put(0x0745, 230);
            table.put(0x0744, 220);
            table.put(0x0743, 230);
            table.put(0x0742, 220);
            table.put(0x0741, 230);
            table.put(0x0349, 220);
            table.put(0x0740, 230);
            table.put(0x0348, 220);
            table.put(0x0347, 220);
            table.put(0x0346, 230);
            table.put(0x0345, 240);
            table.put(0x0344, 230);
            table.put(0x0343, 230);
            table.put(0x0342, 230);
            table.put(0x0341, 230);
            table.put(0x0340, 230);
            table.put(0x073F, 230);
            table.put(0x073E, 220);
            table.put(0x073D, 230);
            table.put(0x073C, 220);
            table.put(0x073B, 220);
            table.put(0x073A, 230);
            table.put(0x309A, 8);
            table.put(0x033F, 230);
            table.put(0x033E, 230);
            table.put(0x033D, 230);
            table.put(0x033C, 220);
            table.put(0x033B, 220);
            table.put(0x0739, 220);
            table.put(0x033A, 220);
            table.put(0x0738, 220);
            table.put(0x0737, 220);
            table.put(0x0736, 230);
            table.put(0x3099, 8);
            table.put(0x0735, 230);
            table.put(0xFE23, 230);
            table.put(0x0734, 220);
            table.put(0x0F87, 230);
            table.put(0xFE22, 230);
            table.put(0x0733, 230);
            table.put(0x0F86, 230);
            table.put(0xFE21, 230);
            table.put(0x0732, 230);
            table.put(0xFE20, 230);
            table.put(0x0731, 220);
            table.put(0x0F84, 9);
            table.put(0x0339, 220);
            table.put(0x0730, 230);
            table.put(0x0F83, 230);
            table.put(0x0338, 1);
            table.put(0x0F82, 230);
            table.put(0x0337, 1);
            table.put(0x0336, 1);
            table.put(0x0F80, 130);
            table.put(0x0335, 1);
            table.put(0x0334, 1);
            table.put(0x0333, 220);
            table.put(0x0332, 220);
            table.put(0x0331, 220);
            table.put(0x0330, 220);
            table.put(0x1D244, 230);
            table.put(0x1D243, 230);
            table.put(0x1D242, 230);
            table.put(0x0F7D, 130);
            table.put(0x0F7C, 130);
            table.put(0x0F7B, 130);
            table.put(0x0F7A, 130);
            table.put(0x032F, 220);
            table.put(0x032E, 220);
            table.put(0x032D, 220);
            table.put(0x032C, 220);
            table.put(0x032B, 220);
            table.put(0x032A, 220);
            table.put(0x0F74, 132);
            table.put(0x0329, 220);
            table.put(0x0328, 202);
            table.put(0x0F72, 130);
            table.put(0x0327, 202);
            table.put(0x0DCA, 9);
            table.put(0x0F71, 129);
            table.put(0x0326, 220);
            table.put(0x0325, 220);
            table.put(0x0324, 220);
            table.put(0x0323, 220);
            table.put(0x0322, 202);
            table.put(0x0321, 202);
            table.put(0x0320, 220);
            table.put(0x10A3F, 9);
            table.put(0x135F, 230);
            table.put(0x10A3A, 220);
            table.put(0x031F, 220);
            table.put(0x031E, 220);
            table.put(0x031D, 220);
            table.put(0x031C, 220);
            table.put(0x031B, 216);
            table.put(0x031A, 232);
            table.put(0x10A39, 1);
            table.put(0x10A38, 230);
            table.put(0x0711, 36);
            table.put(0x0319, 220);
            table.put(0x0318, 220);
            table.put(0x0317, 220);
            table.put(0x0316, 220);
            table.put(0x0315, 232);
            table.put(0x0314, 230);
            table.put(0x1D18B, 220);
            table.put(0x0313, 230);
            table.put(0x1D18A, 220);
            table.put(0x0312, 230);
            table.put(0x0311, 230);
            table.put(0x0670, 35);
            table.put(0x0310, 230);
            table.put(0x1D189, 230);
            table.put(0x1D188, 230);
            table.put(0x1D187, 230);
            table.put(0x1D186, 230);
            table.put(0x030F, 230);
            table.put(0x1D185, 230);
            table.put(0x030E, 230);
            table.put(0x030D, 230);
            table.put(0x030C, 230);
            table.put(0x1D182, 220);
            table.put(0x030B, 230);
            table.put(0x1D181, 220);
            table.put(0x030A, 230);
            table.put(0x1D180, 220);
            table.put(0x0309, 230);
            table.put(0x0308, 230);
            table.put(0x1D17F, 220);
            table.put(0x0307, 230);
            table.put(0x1D17E, 220);
            table.put(0x0306, 230);
            table.put(0x1D17D, 220);
            table.put(0x0305, 230);
            table.put(0x1D17C, 220);
            table.put(0x0304, 230);
            table.put(0x1D17B, 220);
            table.put(0x0303, 230);
            table.put(0x0302, 230);
            table.put(0x0301, 230);
            table.put(0x0300, 230);
            table.put(0x065E, 230);
            table.put(0x065D, 230);
            table.put(0x065C, 220);
            table.put(0x065B, 230);
            table.put(0x1D172, 216);
            table.put(0x065A, 230);
            table.put(0x1D171, 216);
            table.put(0x0B4D, 9);
            table.put(0x1D170, 216);
            table.put(0x1734, 9);
            table.put(0x0659, 230);
            table.put(0x0658, 230);
            table.put(0x0657, 230);
            table.put(0x1D16F, 216);
            table.put(0x0656, 220);
            table.put(0x1D16E, 216);
            table.put(0x0655, 220);
            table.put(0x1D16D, 226);
            table.put(0x0654, 230);
            table.put(0x0653, 230);
            table.put(0x0652, 34);
            table.put(0x0651, 33);
            table.put(0x0650, 32);
            table.put(0x10A0F, 230);
            table.put(0x10A0D, 220);
            table.put(0x1D169, 1);
            table.put(0x1D168, 1);
            table.put(0x1D167, 1);
            table.put(0x064F, 31);
            table.put(0x1D166, 216);
            table.put(0x064E, 30);
            table.put(0x1D165, 216);
            table.put(0x064D, 29);
            table.put(0x064C, 28);
            table.put(0x064B, 27);
            table.put(0x0B3C, 7);
            table.put(0x0F39, 216);
            table.put(0x0F37, 220);
            table.put(0x0F35, 220);
            table.put(0x1DC3, 230);
            table.put(0x1DC2, 220);
            table.put(0x1DC1, 230);
            table.put(0x1DC0, 230);
            table.put(0x059F, 230);
            table.put(0x1714, 9);
            table.put(0x059E, 230);
            table.put(0x059D, 230);
            table.put(0x059C, 230);
            table.put(0x059B, 220);
            table.put(0x059A, 222);
            table.put(0x0599, 230);
            table.put(0x0598, 230);
            table.put(0x0597, 230);
            table.put(0x0596, 220);
            table.put(0x0595, 230);
            table.put(0x0594, 230);
            table.put(0x0593, 230);
            table.put(0x302F, 224);
            table.put(0x0592, 230);
            table.put(0x302E, 224);
            table.put(0x0591, 220);
            table.put(0x302D, 222);
            table.put(0x302C, 232);
            table.put(0x302B, 228);
            table.put(0x302A, 218);
            table.put(0x0F19, 220);
            table.put(0x0F18, 220);
            table.put(0x0CCD, 9);
            table.put(0x0615, 230);
            table.put(0x0614, 230);
            table.put(0x18A9, 228);
            table.put(0x0613, 230);
            table.put(0x0612, 230);
            table.put(0x0611, 230);
            table.put(0x0CBC, 7);
            table.put(0x0610, 230);
            table.put(0x20EB, 1);
            table.put(0x20EA, 1);
            table.put(0x20E9, 230);
            table.put(0x20E8, 220);
            table.put(0x20E7, 230);
            table.put(0x20E6, 1);
            table.put(0x20E5, 1);
            table.put(0x20E1, 230);
            table.put(0x20DC, 230);
            table.put(0x20DB, 230);
            table.put(0x20DA, 1);
            table.put(0x20D9, 1);
            table.put(0x20D8, 1);
            table.put(0x20D7, 230);
            table.put(0x20D6, 230);
            table.put(0x0E4B, 107);
            table.put(0x20D5, 230);
            table.put(0x0E4A, 107);
            table.put(0x20D4, 230);
            table.put(0x20D3, 1);
            table.put(0x20D2, 1);
            table.put(0x20D1, 230);
            return table;
        }
    }
}
