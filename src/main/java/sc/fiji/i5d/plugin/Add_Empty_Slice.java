package sc.fiji.i5d.plugin;
//
// Add_Empty_Slice.java
//

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
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
public class Add_Empty_Slice implements PlugIn {

	@Override
	public void run(final String arg) {
		final ImagePlus imp = WindowManager.getCurrentImage();
		if (!(imp instanceof Image5D)) {
			IJ.error("Current Image is not an Image5D.");
			return;
		}
		IJ.register(Add_Empty_Slice.class);

		final Image5D i5d = (Image5D) imp;
		final Image5DWindow win = ((Image5DWindow) i5d.getWindow());
		final int n = i5d.getNSlices();

		i5d.expandDimension(3, n + 1, true);

		win.updateSliceSelector();
	}

}
