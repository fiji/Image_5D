package sc.fiji.i5d.plugin;
//
// I5D_about.java
//

import ij.IJ;
import ij.plugin.PlugIn;
import sc.fiji.i5d.Image5D;

public class I5D_about implements PlugIn {

	@Override
	public void run(final String arg) {
		IJ.showMessage("Image5D " + Image5D.VERSION,
			"Viewing and handling 5D (x/y/channel/z/time) image-data.\n"
				+ "Author: Joachim Walter");

	}

}
