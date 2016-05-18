package sc.fiji.i5d.plugin;
//
// Image5D_Extensions.java
//

import ij.IJ;
import ij.macro.ExtensionDescriptor;
import ij.macro.Functions;
import ij.macro.MacroExtension;
import ij.plugin.PlugIn;
import sc.fiji.i5d.Image5D;
import sc.fiji.i5d.gui.ChannelControl;

/**
 * Macro extensions for Image5D.
 * 
 * @author Shannon Stewman
 */
public class Image5D_Extensions implements PlugIn, MacroExtension {

	@Override
	public void run(final String arg) {
		if (!IJ.macroRunning()) {
			IJ.error("Cannot install extensions from outside a macro!");
			return;
		}

		Functions.registerExtensions(this);
	}

	private final ExtensionDescriptor[] extensions = {
		ExtensionDescriptor.newDescriptor("getChannel", this, ARG_OUTPUT +
			ARG_NUMBER),
		ExtensionDescriptor
			.newDescriptor("getFrame", this, ARG_OUTPUT + ARG_NUMBER),
		ExtensionDescriptor.newDescriptor("setChannel", this, ARG_NUMBER),
		ExtensionDescriptor.newDescriptor("setFrame", this, ARG_NUMBER),
		ExtensionDescriptor.newDescriptor("getDisplayMode", this),
		ExtensionDescriptor.newDescriptor("setDisplayMode", this, ARG_STRING), };

	@Override
	public ExtensionDescriptor[] getExtensionFunctions() {
		return extensions;
	}

	@Override
	public String handleExtension(final String name, final Object[] args) {
		if (!(IJ.getImage() instanceof Image5D)) {
			IJ.error("Current image is not an Image5D");
		}

		final Image5D im5d = (Image5D) IJ.getImage();

		if (name.equals("getChannel")) {
			final int[] pos = im5d.getCurrentPosition();
			((Double[]) args[0])[0] = new Double(pos[im5d.getColorDimension()] + 1);
		}
		else if (name.equals("setChannel")) {
			final int ch = ((Double) args[0]).intValue();
			im5d.setChannel(ch);
		}
		else if (name.equals("getFrame")) {
			final int[] pos = im5d.getCurrentPosition();
			((Double[]) args[0])[0] = new Double(pos[4] + 1);
		}
		else if (name.equals("setFrame")) {
			final int fr = ((Double) args[0]).intValue();
			im5d.setFrame(fr);
		}
		else if (name.equals("getDisplayMode")) {
			final int mode = im5d.getDisplayMode();
			switch (mode) {
				case ChannelControl.ONE_CHANNEL_COLOR:
					return "color";
				case ChannelControl.ONE_CHANNEL_GRAY:
					return "gray";
				case ChannelControl.OVERLAY:
					return "overlay";
				default:
					return "unknown";
			}
		}
		else if (name.equals("setDisplayMode")) {
			final String arg = (String) args[0];
			if (arg.equals("color")) {
				im5d.setDisplayMode(ChannelControl.ONE_CHANNEL_COLOR);
			}
			else if (arg.equals("gray")) {
				im5d.setDisplayMode(ChannelControl.ONE_CHANNEL_GRAY);
			}
			else if (arg.equals("overlay")) {
				im5d.setDisplayMode(ChannelControl.OVERLAY);
			}
			else {
				IJ.error("Illegal display mode value: " + arg);
				return null;
			}
		}

		return null;
	}

}
