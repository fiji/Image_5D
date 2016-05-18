package sc.fiji.i5d.plugin;
//
// Image5D_to_Stack.java
//

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.plugin.PlugIn;
import sc.fiji.i5d.Image5D;

public class Image5D_to_Stack implements PlugIn {

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

		final ImageStack currentImageStack = currentImage.getImageStack();

		// Copy references to pixel arrays to new image. Don't just copy the
		// reference to the stack,
		// because the stack is disassembled when the currentImage is flushed.
		final ImagePlus newImage =
			new ImagePlus(currentImage.getTitle(), currentImageStack.getProcessor(1));
		final ImageStack newStack = newImage.getStack();
		newStack.setSliceLabel(currentImageStack.getSliceLabel(1), 1);
		for (int i = 2; i <= currentImage.getImageStackSize(); i++) {
			newStack.addSlice(currentImageStack.getSliceLabel(i), currentImageStack
				.getPixels(i));
		}
		newImage.setStack(null, newStack);

		newImage.setDimensions(currentImage.getNChannels(), currentImage
			.getNSlices(), currentImage.getNFrames());
		newImage.setCalibration(currentImage.getCalibration().copy());

		newImage.getProcessor().resetMinAndMax();
		newImage.show();

		currentImage.getWindow().close();

		if (newImage.getWindow() != null) WindowManager.setCurrentWindow(newImage
			.getWindow());
	}

}
