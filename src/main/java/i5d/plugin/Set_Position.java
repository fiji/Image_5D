package i5d.plugin;
//
// Set_Position.java
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

import i5d.Image5D;
import i5d.gui.ChannelControl;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.gui.ImageWindow;
import ij.plugin.PlugIn;

public class Set_Position implements PlugIn {

	@Override
	public void run(final String arg) {
		final ImagePlus imp = WindowManager.getCurrentImage();

		if (imp == null) {
			IJ.noImage();
			return;
		}
		if (!(imp instanceof Image5D)) {
			IJ.error("Image is not an Image5D.");
			return;
		}

		final Image5D i5d = (Image5D) imp;
		final ImageWindow win = i5d.getWindow();
		int displayMode = 0;
		boolean allGray = false;
		if (win != null) {
			displayMode = i5d.getDisplayMode();
			if (displayMode < 0 || displayMode >= ChannelControl.displayModes.length)
			{
				displayMode = 1;
			}
			allGray =
				(displayMode == ChannelControl.TILED && i5d.isDisplayGrayInTiles());
		}

		final GenericDialog gd = new GenericDialog("Image5D Set Position");
		gd.addNumericField("x-Position", 1, 0, 5, "");
		gd.addNumericField("y-Position", 1, 0, 5, "");
		gd.addNumericField("channel", i5d.getCurrentChannel(), 0, 5, "");
		gd.addNumericField("slice", i5d.getCurrentSlice(), 0, 5, "");
		gd.addNumericField("frame", i5d.getCurrentFrame(), 0, 5, "");
		gd.addChoice("Display Mode", ChannelControl.displayModes,
			ChannelControl.displayModes[displayMode]);
		gd.addCheckbox("All Gray when Tiled", allGray);
		gd.showDialog();

		if (gd.wasCanceled()) {
			return;
		}

		final int[] position = new int[5];
		for (int i = 0; i < 5; i++) {
			position[i] = (int) gd.getNextNumber();
			if (position[i] < 1 || position[i] > i5d.getDimensionSize(i)) {
				position[i] = 0;
			}
			else {
				position[i] -= 1;
			}
		}

		displayMode = gd.getNextChoiceIndex();
		allGray = gd.getNextBoolean();
		i5d.setDisplayGrayInTiles(allGray);
		i5d.setDisplayMode(displayMode);

		i5d.setCurrentPosition(position);

	}

}
