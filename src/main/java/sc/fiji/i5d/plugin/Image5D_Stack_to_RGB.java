package sc.fiji.i5d.plugin;
//
// Image5D_Stack_to_RGB.java
//

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.gui.ImageCanvas;
import ij.measure.Calibration;
import ij.plugin.PlugIn;
import ij.process.ImageConverter;
import sc.fiji.i5d.Image5D;
import sc.fiji.i5d.cal.ChannelDisplayProperties;
import ij.measure.*;
import java.awt.Graphics;
import java.awt.image.ColorModel;
import java.util.Vector;

/**
 * Converts the current timeframe of an Image5D to an RGB stack using the
 * current view settings.
 * 
 * @author Joachim Walter
 */
public class Image5D_Stack_to_RGB implements PlugIn {

	@Override
	public void run(final String arg) {
		final ImagePlus currentImage = WindowManager.getCurrentImage();
		if (currentImage == null) {
			IJ.noImage();
			return;
		}
		if (!(currentImage instanceof Image5D)) {
			IJ.error("Image is not an Image5D.");
			return;
		}

		final GenericDialog gd = new GenericDialog("Store active channel data");
		gd.addMessage("Store the labels and RGB values of active channels in the results table?");
		gd.addCheckbox("Store active channel data", true);
		gd.addCheckbox("Show the results table", false);
		gd.showDialog();

		if (gd.wasCanceled()) {
			return;
		}
		
		final Boolean storeChannelData = gd.getNextBoolean();
		final Boolean displayResultsTable = gd.getNextBoolean();
		
		// Hijack the results table to print a table with display colors
		// for each active channel
		if (storeChannelData) {	
			final Image5D image = (Image5D) currentImage;
			final ResultsTable results = ResultsTable.getResultsTable();
			
			// Check if there are pre-existing results in the results table.
			// If yes, ask whether they should be discarded. 
			if (results.getCounter() != 0 && !(results.getLastColumn() == 3 && results.getColumnHeading(3) == "blue")) {
				final GenericDialog errorgd = new GenericDialog("Pre-existing results table");
				errorgd.addMessage("There already is a results table! Do you want to discard it?");
				errorgd.addCheckbox("Discard", false);
				errorgd.showDialog();
				
				if (!errorgd.getNextBoolean()) {
					IJ.error("Please empty your results table before you continue.");
					return;
				}
			}
			
			// Empty the results table if its not empty
			if (results.getCounter() != 0) {
				IJ.run("Clear Results");
			}
			
			int rownumber = 0;
			
			for (int i = 1; i <= image.getNChannels(); i++) {
				final ChannelDisplayProperties channel = image.getChannelDisplayProperties(i);
				
				if (channel.isDisplayedInOverlay()) {
					final ColorModel colormod = channel.getColorModel();
					final String label = image.getChannelCalibration(i).getLabel();
					results.setValue("label", rownumber, label);
					results.setValue("red", rownumber, colormod.getRed(255));
					results.setValue("green", rownumber, colormod.getGreen(255));
					results.setValue("blue", rownumber, colormod.getBlue(255));
					rownumber++;
				}
			}
			if (displayResultsTable) {	
				results.show("Results");
			}
			
		}

		final String title = currentImage.getTitle();
		final int width = currentImage.getWidth();
		final int height = currentImage.getHeight();
		final int depth = currentImage.getNSlices();
		final int currentSlice = currentImage.getCurrentSlice();
		final Calibration cal = currentImage.getCalibration().copy();

		currentImage.killRoi();
		
		final ImagePlus rgbImage =
			IJ.createImage(title + "-RGB", "RGB black", width, height, 1);
		final ImageStack rgbStack = rgbImage.getStack();

		final ImageCanvas canvas = currentImage.getCanvas();
		final Graphics imageGfx = canvas.getGraphics();
		for (int i = 1; i <= depth; i++) {
			currentImage.setSlice(i);

			// HACK: force immediate (synchronous) repaint of image canvas
			if (currentImage instanceof Image5D) {
				((Image5D) currentImage).updateImageAndDraw();
			}
			
			currentImage.updateAndDraw();
			canvas.paint(imageGfx);

			currentImage.copy(false);

			final ImagePlus rgbClip = ImagePlus.getClipboard();
			if (rgbClip.getType() != ImagePlus.COLOR_RGB) new ImageConverter(rgbClip)
				.convertToRGB();
			if (i > 1) {
				rgbStack.addSlice(currentImage.getStack().getSliceLabel(i), rgbClip
					.getProcessor().getPixels());
			}
			else {
				rgbStack.setPixels(rgbClip.getProcessor().getPixels(), 1);
				rgbStack.setSliceLabel(currentImage.getStack().getSliceLabel(1), 1);
			}
		}
		imageGfx.dispose();

		currentImage.setSlice(currentSlice);
		rgbImage.setStack(null, rgbStack);
		rgbImage.setSlice(currentSlice);
		rgbImage.setCalibration(cal);

		rgbImage.killRoi();
		rgbImage.show();
	}

}
