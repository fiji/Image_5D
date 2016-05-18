package sc.fiji.i5d.plugin;
//
// Add_Empty_Channel.java
//

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;
import sc.fiji.i5d.Image5D;
import sc.fiji.i5d.gui.Image5DWindow;

/*
 * Created on 09.08.2005
 *
 */

/**
 * @author Joachim Walter
 */
public class Add_Empty_Channel implements PlugIn {

	static boolean sameSlices;
	static boolean sameFrames;

	@Override
	public void run(final String arg) {
		final ImagePlus imp = WindowManager.getCurrentImage();
		if (!(imp instanceof Image5D)) {
			IJ.error("Current Image is not an Image5D.");
			return;
		}
		IJ.register(Add_Empty_Channel.class);

		final GenericDialog gd = new GenericDialog("Add Empty Channel");
		gd.addStringField("Channel Label", "");
		gd.addCheckbox("Same data for all z-slices", sameSlices);
		gd.addCheckbox("Same data for all time-frames", sameFrames);
		gd.showDialog();
		if (gd.wasCanceled()) {
			return;
		}
		final String channelLabel = gd.getNextString();
		sameSlices = gd.getNextBoolean();
		sameFrames = gd.getNextBoolean();

		final Image5D i5d = (Image5D) imp;
		final Image5DWindow win = ((Image5DWindow) i5d.getWindow());
		final int n = i5d.getNChannels();

		if (!sameSlices && !sameFrames) {
			i5d.expandDimension(i5d.getColorDimension(), n + 1, true);
			i5d.getChannelCalibration(n + 1).setLabel(channelLabel);
		}
		else if (sameSlices && !sameFrames) {
			i5d.expandDimension(i5d.getColorDimension(), n + 1, false);
			for (int f = 1; f <= i5d.getNFrames(); f++) {
				final Object pixels = i5d.createEmptyPixels();
				for (int s = 1; s <= i5d.getNSlices(); s++) {
					i5d.setPixels(pixels, n + 1, s, f);
				}
			}
			i5d.getChannelCalibration(n + 1).setLabel(channelLabel);
		}
		else if (!sameSlices && sameFrames) {
			i5d.expandDimension(i5d.getColorDimension(), n + 1, false);
			for (int s = 1; s <= i5d.getNSlices(); s++) {
				final Object pixels = i5d.createEmptyPixels();
				for (int f = 1; f <= i5d.getNFrames(); f++) {
					i5d.setPixels(pixels, n + 1, s, f);
				}
			}
			i5d.getChannelCalibration(n + 1).setLabel(channelLabel);
		}
		else if (sameSlices && sameFrames) {
			i5d.expandDimension(i5d.getColorDimension(), n + 1, false);
			final Object pixels = i5d.createEmptyPixels();
			for (int s = 1; s <= i5d.getNSlices(); s++) {
				for (int f = 1; f <= i5d.getNFrames(); f++) {
					i5d.setPixels(pixels, n + 1, s, f);
				}
			}
			i5d.getChannelCalibration(n + 1).setLabel(channelLabel);
		}

		win.updateSliceSelector();
	}

}
