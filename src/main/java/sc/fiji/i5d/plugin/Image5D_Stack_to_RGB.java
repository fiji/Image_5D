package sc.fiji.i5d.plugin;
//
// Image5D_Stack_to_RGB.java
//

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.gui.ImageCanvas;
import ij.measure.Calibration;
import ij.plugin.PlugIn;
import ij.process.ImageConverter;
import sc.fiji.i5d.Image5D;

import java.awt.Graphics;

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
