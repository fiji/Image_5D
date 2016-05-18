package sc.fiji.i5d.plugin;
//
// RGB_to_Image5D.java
//

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;
import ij.process.ColorProcessor;
import sc.fiji.i5d.Image5D;
import sc.fiji.i5d.cal.ChannelDisplayProperties;

import java.awt.Color;

/*
 * Created on 07.04.2005
 */

/**
 * Converts an RGB image or RGB stack to an Image5D with three channels
 * corresponding to the R, G and B components.
 * 
 * @author Joachim Walter
 */
public class RGB_to_Image5D implements PlugIn {

	/**
	 * 
	 */
	public RGB_to_Image5D() {}

	@Override
	public void run(final String arg) {
		if (IJ.versionLessThan("1.34p")) return;

		final ImagePlus imp = WindowManager.getCurrentImage();
		if (imp == null) {
			IJ.noImage();
			return;
		}
		if (imp instanceof Image5D) {
			IJ.error("Image is already an Image5D");
			return;
		}
		final int width = imp.getWidth();
		final int height = imp.getHeight();
		final int stackSize = imp.getStackSize();
		final String title = imp.getTitle();
		final int type = imp.getType();

		if (type != ImagePlus.COLOR_RGB) {
			IJ.error("Not an RGB image");
			return;
		}

		// The choices that are initially displayed:
		// z, t and the respective dimension sizes.
		int first = 0;
		int nFirst = imp.getNSlices();
		int nLast = imp.getNFrames();
		final String[] dimensions = new String[] { "z", "t" };
		boolean goOn = true;
		do {
			goOn = true;

			final GenericDialog gd =
				new GenericDialog("Convert RGB stack to Image5D");
			gd.addMessage("Stack has " + stackSize + " slices.");
			gd.addChoice("3rd dimension", dimensions, dimensions[first]);
			gd.addNumericField("3rd_dimension_size", nFirst, 0, 8, "");
			gd.showDialog();

			if (gd.wasCanceled()) {
				return;
			}

			first = gd.getNextChoiceIndex();
			nFirst = (int) gd.getNextNumber();

			final double dLast = (double) stackSize / (double) nFirst;
			nLast = (int) dLast;
			if (nLast != dLast) {
				IJ.error("channels*slices*frames!=stackSize");
				goOn = false;
			}

		}
		while (goOn == false);

		int nSlices, nFrames;
		int sliceIncrement, frameIncrement;
		if (first == 0) {
			nSlices = nFirst;
			nFrames = nLast;
			sliceIncrement = 1;
			frameIncrement = nSlices;
		}
		else {
			nSlices = nLast;
			nFrames = nFirst;
			sliceIncrement = nFrames;
			frameIncrement = 1;
		}
		final Image5D img5d =
			new Image5D(title, ImagePlus.GRAY8, width, height, 3, nSlices, nFrames,
				false);

		for (int iFrame = 0; iFrame < nFrames; iFrame++) {
			final int baseIndex = iFrame * frameIncrement;
			for (int iSlice = 0; iSlice < nSlices; ++iSlice) {
				final int stackIndex = baseIndex + iSlice * sliceIncrement + 1;
				final byte[] R = new byte[width * height];
				final byte[] G = new byte[width * height];
				final byte[] B = new byte[width * height];
				imp.setSlice(stackIndex);
				((ColorProcessor) imp.getProcessor()).getRGB(R, G, B);
				img5d.setCurrentPosition(0, 0, 0, iSlice, iFrame);
				img5d.setPixels(R);
				img5d.setCurrentPosition(0, 0, 1, iSlice, iFrame);
				img5d.setPixels(G);
				img5d.setCurrentPosition(0, 0, 2, iSlice, iFrame);
				img5d.setPixels(B);
			}
		}

		img5d.getChannelCalibration(1).setLabel("Red");
		img5d.getChannelCalibration(2).setLabel("Green");
		img5d.getChannelCalibration(3).setLabel("Blue");
		img5d.setChannelColorModel(1, ChannelDisplayProperties
			.createModelFromColor(Color.red));
		img5d.setChannelColorModel(2, ChannelDisplayProperties
			.createModelFromColor(Color.green));
		img5d.setChannelColorModel(3, ChannelDisplayProperties
			.createModelFromColor(Color.blue));

		img5d.setCurrentPosition(0, 0, 0, 0, 0);
		img5d.setCalibration(imp.getCalibration().copy());

		img5d.show();
		imp.getWindow().close();
	}

}
