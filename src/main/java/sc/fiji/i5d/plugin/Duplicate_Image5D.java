package sc.fiji.i5d.plugin;
//
// Duplicate_Image5D.java
//

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.plugin.PlugIn;
import sc.fiji.i5d.Image5D;

public class Duplicate_Image5D implements PlugIn {

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
		final Image5D i5d = (Image5D) currentImage;

		(i5d.duplicate()).show();

	}

}
