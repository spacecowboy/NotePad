/*
The MIT License (MIT)

Copyright (c) 2014 Amulya Khare

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
 */

package com.amulyakhare.textdrawable.util;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * @author amulya
 * @datetime 14 Oct 2014, 5:20 PM
 * <p/>
 * MIT LICENSE: https://github.com/amulyakhare/TextDrawable/blob/master/LICENSE
 */
public class ColorGenerator {

    public static ColorGenerator DEFAULT;

    public static ColorGenerator MATERIAL;

    static {
        DEFAULT = create(Arrays.asList(0xfff16364, 0xfff58559, 0xfff9a43e, 0xffe4c62e,
                0xff67bf74, 0xff59a2be, 0xff2093cd, 0xffad62a7, 0xff805781));
        MATERIAL = create(Arrays.asList(0xffe57373, 0xfff06292, 0xffba68c8, 0xff9575cd,
                0xff7986cb, 0xff64b5f6, 0xff4fc3f7, 0xff4dd0e1, 0xff4db6ac, 0xff81c784,
                0xffaed581, 0xffff8a65, 0xffd4e157, 0xffffd54f, 0xffffb74d, 0xffa1887f,
                0xff90a4ae));
    }

    private final List<Integer> mColors;
    private final Random mRandom;

    private ColorGenerator(List<Integer> colorList) {
        mColors = colorList;
        mRandom = new Random(System.currentTimeMillis());
    }

    public static ColorGenerator create(List<Integer> colorList) {
        return new ColorGenerator(colorList);
    }

    public int getRandomColor() {
        return mColors.get(mRandom.nextInt(mColors.size()));
    }

    public int getColor(Object key) {
        return mColors.get(Math.abs(key.hashCode()) % mColors.size());
    }
}