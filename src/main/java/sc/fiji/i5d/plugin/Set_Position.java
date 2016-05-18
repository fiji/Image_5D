package sc.fiji.i5d.plugin;
//
// Set_Position.java
//

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.gui.ImageWindow;
import ij.plugin.PlugIn;
import sc.fiji.i5d.Image5D;
import sc.fiji.i5d.gui.ChannelControl;

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
