package sc.fiji.i5d.plugin;
//
// Z_Project.java
//

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;
import ij.plugin.ZProjector;
import ij.process.ColorProcessor;
import ij.process.TypeConverter;
import sc.fiji.i5d.Image5D;
import sc.fiji.i5d.gui.ChannelControl;

public class Z_Project implements PlugIn {

	/** Projection starts from this slice. */
	private int startSlice = 1;
	/** Projection ends at this slice. */
	private int stopSlice = 1;
	private static int method = ZProjector.MAX_METHOD;
	private static boolean bDisplayedChannelsOnly = false;
	private static boolean bAllTimeFrames = false;
	private static boolean bOutputImage5D = false;
	private static boolean bDoScaling = true;

	private ImagePlus imp;
	private Image5D i5d;

	public Z_Project() {}

	public Z_Project(final Image5D i5d) {
		setImage(i5d);
	}

	@Override
	public void run(final String arg) {
		IJ.register(Z_Project.class);
		imp = WindowManager.getCurrentImage();
		if (imp == null) {
			IJ.noImage();
			return;
		}

		// Check if image is an Image5D.
		if (!(imp instanceof Image5D)) {
			IJ.error("Z Projection", "Image5D required");
			return;
		}
		setImage((Image5D) imp);

		// User Dialog
		final GenericDialog gd = new GenericDialog("ZProjection", IJ.getInstance());
		gd.addNumericField("Start slice:", startSlice, 0/*digits*/);
		gd.addNumericField("Stop slice:", stopSlice, 0/*digits*/);
		// Different kinds of projections.
		gd.addChoice("Projection Type", ZProjector.METHODS,
			ZProjector.METHODS[method]);
		gd.addCheckbox("Displayed Channels only", bDisplayedChannelsOnly);
		gd.addCheckbox("All Time Frames", bAllTimeFrames);
		gd.addCheckbox("Output as Image5D", bOutputImage5D);
		gd.addCheckbox("Copy Contrast and Brightness", bDoScaling);

		gd.showDialog();
		if (gd.wasCanceled()) return;

		setStartSlice((int) gd.getNextNumber());
		setStopSlice((int) gd.getNextNumber());
		setMethod(gd.getNextChoiceIndex());
		setDisplayedChannelsOnly(gd.getNextBoolean());
		setAllTimeFrames(gd.getNextBoolean());
		setOutputImage5D(gd.getNextBoolean());
		setDoScaling(gd.getNextBoolean());

		if (stopSlice < startSlice) return;

		final long tstart = System.currentTimeMillis();

		// Do Projection
		if (bOutputImage5D) {
			final Image5D resultI5D = doI5DProjection();
			if (resultI5D != null) resultI5D.show();
		}
		else {
			final ImagePlus resultImage = doProjection();
			if (resultImage != null) resultImage.show();
		}
		final long tstop = System.currentTimeMillis();
		IJ.showStatus("ZProject: " + IJ.d2s((tstop - tstart) / 1000.0, 2) +
			" seconds");

	}

	/**
	 * Explicitly set image to be projected. This is useful if ZProjection_ object
	 * is to be used not as a plugin but as a stand alone processing object.
	 */
	public void setImage(final Image5D i5d) {
		this.i5d = i5d;
		this.imp = i5d;
		startSlice = 1;
		stopSlice = imp.getStackSize();
	}

	public void setStartSlice(final int slice) {
		if (imp == null || slice < 1 || slice > imp.getStackSize()) return;
		startSlice = slice;
	}

	public void setStopSlice(final int slice) {
		if (imp == null || slice < 1 || slice > imp.getStackSize()) return;
		stopSlice = slice;
	}

	public void setMethod(final int projMethod) {
		method = projMethod;
	}

	public void setDisplayedChannelsOnly(final boolean displayedChannelsOnly) {
		bDisplayedChannelsOnly = displayedChannelsOnly;
	}

	public void setAllTimeFrames(final boolean allTimeFrames) {
		bAllTimeFrames = allTimeFrames;
	}

	public void setOutputImage5D(final boolean outputImage5D) {
		bOutputImage5D = outputImage5D;
	}

	public void setDoScaling(final boolean doScaling) {
		bDoScaling = doScaling;
	}

	ImagePlus doProjection() {
		if (!imp.lock()) return null; // exit if in use

		final int currentChannel = i5d.getCurrentChannel();
		final int currentSlice = i5d.getCurrentSlice();
		final int currentFrame = i5d.getCurrentFrame();

		// Do Projection.
		// Check, which channels are projected and store channel changes to keep
		// data consistent
		int nProjectedChannels = 0;
		final int[] projectedChannels = new int[i5d.getNChannels()];
		for (int c = 1; c <= i5d.getNChannels(); c++) {
			i5d.storeChannelProperties(c);
			if (bDisplayedChannelsOnly &&
				((i5d.getDisplayMode() == ChannelControl.OVERLAY && !i5d
					.getChannelDisplayProperties(c).isDisplayedInOverlay()) || i5d
					.getDisplayMode() != ChannelControl.OVERLAY &&
					c != currentChannel)) continue;
			projectedChannels[nProjectedChannels] = c;
			nProjectedChannels++;
		}
		if (bDisplayedChannelsOnly && nProjectedChannels == 0) {
			return null;
		}

		int startFrame = i5d.getCurrentFrame();
		int nFrames = 1;
		if (bAllTimeFrames) {
			startFrame = 1;
			nFrames = i5d.getNFrames();
		}

		// Allocate output arrays
		final byte[] reds = new byte[i5d.getWidth() * i5d.getHeight()];
		final byte[] greens = new byte[i5d.getWidth() * i5d.getHeight()];
		final byte[] blues = new byte[i5d.getWidth() * i5d.getHeight()];
		final String newTitle =
			WindowManager.makeUniqueName(imp.getTitle() + " Projection");
		final ImagePlus resultImp =
			IJ.createImage(newTitle, "rgb black", i5d.getWidth(), i5d.getHeight(),
				nFrames);
		resultImp.setCalibration(imp.getCalibration().copy());

		for (int frame = startFrame; frame < startFrame + nFrames; frame++) {
			for (int destChannel = 1; destChannel <= nProjectedChannels; destChannel++)
			{
				final int srcChannel = projectedChannels[destChannel - 1];

				// Do projection for each channel separately
				i5d.setCurrentPosition(0, 0, srcChannel - 1, currentSlice - 1,
					frame - 1);
				final ImagePlus tempImg =
					new ImagePlus(imp.getTitle() + " Projection", i5d.getStack());
				final ZProjector zp = new ZProjector(tempImg);
				zp.setStartSlice(startSlice);
				zp.setStopSlice(stopSlice);
				zp.setMethod(method);
				zp.doProjection();
				final ImagePlus proj = zp.getProjection();
				if (bDoScaling) {
					proj.getProcessor().setMinAndMax(
						i5d.getChannelDisplayProperties(srcChannel).getMinValue(),
						i5d.getChannelDisplayProperties(srcChannel).getMaxValue());
				}
				else {
					proj.getProcessor().resetMinAndMax();
				}

				// Sort projections into color channels
				final ColorProcessor proc =
					(ColorProcessor) (new TypeConverter(proj.getProcessor(), bDoScaling))
						.convertToRGB();

				final int[] rgb = new int[3];
				for (int x = 0; x < imp.getWidth(); x++) {
					for (int y = 0; y < imp.getHeight(); y++) {
						final int pos = x + imp.getWidth() * y;
						proc.getPixel(x, y, rgb);

						int newval = rgb[0] + (0xff & reds[pos]);
						if (newval < 256) {
							reds[pos] = (byte) newval;
						}
						else {
							reds[pos] = (byte) 0xff;
						}
						newval = rgb[1] + (0xff & greens[pos]);
						if (newval < 256) {
							greens[pos] = (byte) newval;
						}
						else {
							greens[pos] = (byte) 0xff;
						}
						newval = rgb[2] + (0xff & blues[pos]);
						if (newval < 256) {
							blues[pos] = (byte) newval;
						}
						else {
							blues[pos] = (byte) 0xff;
						}
					}
				}
			}
			final ColorProcessor cp =
				new ColorProcessor(imp.getWidth(), imp.getHeight());
			cp.setRGB(reds, greens, blues);
			resultImp.setSlice(frame - startFrame + 1);
			resultImp.setProcessor(null, cp);
			for (int i = 0; i < imp.getWidth() * imp.getHeight(); i++) {
				reds[i] = 0;
				greens[i] = 0;
				blues[i] = 0;
			}
		}

		i5d.setCurrentPosition(0, 0, currentChannel - 1, currentSlice - 1,
			currentFrame - 1);
		imp.unlock();

		return resultImp;
	}

	Image5D doI5DProjection() {
		final int currentChannel = i5d.getCurrentChannel();
		final int currentSlice = i5d.getCurrentSlice();
		final int currentFrame = i5d.getCurrentFrame();

		// Do Projection.
		// Check, which channels are projected and store channel changes to keep
		// data consistent
		int nProjectedChannels = 0;
		final int[] projectedChannels = new int[i5d.getNChannels()];
		for (int c = 1; c <= i5d.getNChannels(); c++) {
			i5d.storeChannelProperties(c);
			if (bDisplayedChannelsOnly &&
				((i5d.getDisplayMode() == ChannelControl.OVERLAY && !i5d
					.getChannelDisplayProperties(c).isDisplayedInOverlay()) || i5d
					.getDisplayMode() != ChannelControl.OVERLAY &&
					c != currentChannel)) continue;
			projectedChannels[nProjectedChannels] = c;
			nProjectedChannels++;
		}
		if (bDisplayedChannelsOnly && nProjectedChannels == 0) {
			return null;
		}

		int startFrame = i5d.getCurrentFrame();
		int nFrames = 1;
		if (bAllTimeFrames) {
			startFrame = 1;
			nFrames = i5d.getNFrames();
		}

		// Allocate output Image
		final String newTitle =
			WindowManager.makeUniqueName(imp.getTitle() + " Projection");
		final Image5D resultI5D =
			new Image5D(newTitle, i5d.getType(), i5d.getWidth(), i5d.getHeight(),
				nProjectedChannels, 1, nFrames, false);
		resultI5D.setCalibration(i5d.getCalibration().copy());

		for (int frame = startFrame; frame < startFrame + nFrames; frame++) {
			for (int destChannel = 1; destChannel <= nProjectedChannels; destChannel++)
			{
				final int srcChannel = projectedChannels[destChannel - 1];

				// Do projection for each channel separately
				i5d.setCurrentPosition(0, 0, srcChannel - 1, currentSlice - 1,
					frame - 1);
				final ImagePlus tempImg =
					new ImagePlus(imp.getTitle() + " Projection", i5d.getStack());
				final ZProjector zp = new ZProjector(tempImg);
				zp.setStartSlice(startSlice);
				zp.setStopSlice(stopSlice);
				zp.setMethod(method);
				zp.doProjection();
				final ImagePlus proj = zp.getProjection();

				resultI5D.setPixels(proj.getProcessor().getPixels(), destChannel, 1,
					frame - startFrame + 1);
				if (frame == startFrame) {

					if (destChannel == resultI5D.getCurrentChannel()) {
						resultI5D.setChannelCalibration(destChannel, i5d
							.getChannelCalibration(srcChannel).copy());
						resultI5D.setChannelDisplayProperties(destChannel, i5d
							.getChannelDisplayProperties(srcChannel).copy());
						resultI5D.restoreCurrentChannelProperties();
					}
					else {
						resultI5D.setChannelCalibration(destChannel, i5d
							.getChannelCalibration(srcChannel).copy());
						resultI5D.setChannelDisplayProperties(destChannel, i5d
							.getChannelDisplayProperties(srcChannel).copy());
						resultI5D.restoreChannelProperties(destChannel);
					}
					if (!bDoScaling) {
						resultI5D.getProcessor(destChannel).resetMinAndMax();
					}
				}
			}
		}

		i5d.setCurrentPosition(0, 0, currentChannel - 1, currentSlice - 1,
			currentFrame - 1);
		imp.unlock();

		return resultI5D;
	}

}
