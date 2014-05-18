/*
 * Gray8LinComb.java
 *   Forms the linear combination of two gray images
 *
 * Created on September 9, 2006, 10:25 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 *
 * Copyright 2007 by Jon A. Webb
 *     This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Lesser General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Lesser General Public License for more details.
 *
 *    You should have received a copy of the Lesser GNU General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package jjil.algorithm;
import jjil.core.Error;
import jjil.core.Gray8Image;
import jjil.core.Image;
import jjil.core.Ladder;

/**
 * Computes linear combination of two Gray8Images. Result is 
 * (a*first image + b*second image) / c. The signed image values are shifted so
 * the minimum value is 0 and the result is then shifted back to the signed
 * range.<br>
 * Intended to be used as a combination stage in a ladder operation.
 * @author webb
 */
public class Gray8LinComb implements Ladder.Join {
    private int nA;
    private int nB;
    private int nC;
    
    /**
     * Creates a new instance of Gray8LinComb
     * @param a Multiplier for first image.
     * @param b Multiplier for second image.
     * @param c Divisor for linear combination.
     * @throws jjil.core.Error if the divisor (c) is 0.
     */
    public Gray8LinComb(int a, int b, int c) throws jjil.core.Error {
        if (c == 0) {
            throw new Error(
            				Error.PACKAGE.CORE,
            				jjil.core.ErrorCodes.MATH_DIVISION_ZERO,
            				null,
            				null,
            				null);
        }
        this.nA = a;
        this.nB = b;
        this.nC = c;
    }
    
    /**
     * Computes the linear combination of the two images, forming a*the first image
     * + b *the second image, all divided by c.
     * @param imageFirst the first image (and output)
     * @param imageSecond the second image
     * @return the linear combination of the two byte images, replacing the first.
     * @throws jjil.core.Error if either image is not a gray 8-bit
     * image, or they are of different sizes.
     */
    public Image doJoin(Image imageFirst, Image imageSecond)
        throws jjil.core.Error
    {
        if (!(imageFirst instanceof Gray8Image)) {
                throw new Error(
                				Error.PACKAGE.ALGORITHM,
                				ErrorCodes.IMAGE_NOT_GRAY8IMAGE,
                				imageFirst.toString(),
                				null,
                				null);
            }
        if (!(imageSecond instanceof Gray8Image)) {
                throw new Error(
                				Error.PACKAGE.ALGORITHM,
                				ErrorCodes.IMAGE_NOT_GRAY8IMAGE,
                				imageSecond.toString(),
                				null,
                				null);
            }
        if (imageFirst.getWidth() != imageSecond.getWidth() ||
            imageSecond.getHeight() != imageSecond.getHeight()) {
            throw new Error(
            				Error.PACKAGE.ALGORITHM,
            				ErrorCodes.IMAGE_SIZES_DIFFER,
            				imageFirst.toString(),
            				imageSecond.toString(),
            				null);
       }
        Gray8Image gray1 = (Gray8Image) imageFirst;
        Gray8Image gray2 = (Gray8Image) imageSecond;
        byte[] data1 = gray1.getData();
        byte[] data2 = gray2.getData();
        for (int i=0; i<data1.length; i++) {
            int nD1 = data1[i] - Byte.MIN_VALUE;
            int nD2 = data2[i] - Byte.MIN_VALUE;
            data1[i] = (byte) Math.min(
                    Byte.MAX_VALUE, 
                    Math.max(Byte.MIN_VALUE, 
                        ((nD1 * this.nA + nD2 * this.nB) / this.nC) + 
                    Byte.MIN_VALUE));
        }
        return gray1;
    }

}
