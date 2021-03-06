//
// Calibration5D.java
//

/*
Image5D plugins for 5-dimensional image stacks in ImageJ.

Copyright (c) 2010, Joachim Walter and ImageJDev.org.
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:
    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer in the
      documentation and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.
*/

package sc.fiji.i5d.cal;

import ij.measure.Calibration;
import sc.fiji.i5d.Image5D;

/*
 * Created on 10.04.2005
 */

/**
 * Extension of the Calibration class to 5 Dimensions.
 * 
 * @author Joachim
 */
public class Calibration5D extends Calibration {

	protected int nDimensions = 5;
	protected String[] dimensionLabels;

	/**
	 * @param imp
	 */
	public Calibration5D(final Image5D imp) {
		super(imp);

		dimensionLabels = new String[nDimensions];
		dimensionLabels[0] = "x";
		dimensionLabels[1] = "y";
		if (nDimensions >= 2) dimensionLabels[2] = "ch";
		if (nDimensions >= 3) dimensionLabels[3] = "z";
		if (nDimensions >= 4) dimensionLabels[4] = "t";
	}

	/**
	 * 
	 */
	public Calibration5D() {
		this(null);
		// TODO Auto-generated constructor stub
	}

	public String getDimensionLabel(final int dimension) {
		if (dimension < 0 || dimension >= nDimensions) throw new IllegalArgumentException(
			"Invalid Dimension: " + dimension);
		return dimensionLabels[dimension];
	}

	@Override
	public Calibration copy() {
		final Calibration5D copy = new Calibration5D();
		copy.pixelWidth = pixelWidth;
		copy.pixelHeight = pixelHeight;
		copy.pixelDepth = pixelDepth;
		copy.frameInterval = frameInterval;
		copy.xOrigin = xOrigin;
		copy.yOrigin = yOrigin;
		copy.zOrigin = zOrigin;
		copy.info = info;
		copy.setUnit(getUnit());
//		copy.setUnits(getUnits());
		copy.setValueUnit(getValueUnit());
// TODO: correct copying of calibration function, cal functs have to be one for each color dimension, anyway
//		copy.function = function;
//		copy.coefficients = coefficients;
//		copy.cTable = cTable;
//		copy.invertedLut = invertedLut;
//		copy.bitDepth = bitDepth;
//		copy.zeroClip = zeroClip;

		copy.nDimensions = nDimensions;
		copy.dimensionLabels = new String[nDimensions];
		for (int i = 0; i < nDimensions; ++i) {
			dimensionLabels[i] = getDimensionLabel(i);
		}
		return copy;
	}
}
