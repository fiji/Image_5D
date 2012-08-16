//
// Make_Montage.java
//

/*
Image5D plugins for 5-dimensional image stacks in ImageJ.

Copyright (c) 2010, Joachim Walter and ImageJDev.org.
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:
    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer in the
      documentation and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.
*/

import i5d.Image5D;
import i5d.gui.ChannelControl;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.measure.Calibration;
import ij.plugin.PlugIn;
import ij.process.ColorProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import ij.process.ShortProcessor;
import ij.process.TypeConverter;

import java.awt.Color;

/**
 * Does Montages of Image5Ds with help of ij.plugin.Make_Montage The Methods
 * <code> 
 * public void doMontage(Image5D i5d, int columns, int rows, double scale, int first, int last, int inc, int borderWidth, boolean labels)
 * </code> and <code> 
 * public void doI5DMontage(Image5D i5d, int columns, int rows, double scale, int first, int last, int inc, int borderWidth, boolean labels)
 * </code> Can be called directly without GUI. To do this, first create a
 * Make_Montage object and set parameters via <code>
 *     public void setDisplayedChannelsOnly(boolean displayedChannelsOnly)
 *     public void setAllTimeFrames(boolean allTimeFrames)
 *     public void setOutputImage5D(boolean outputImage5D) 
 *     public void setDoScaling(boolean doScaling) 
 *  </code> J. Walter 2005-10-03
 */

public class Make_Montage implements PlugIn {

	private static int columns, rows, first, last, inc, borderWidth;
	private static double scale;
	private static boolean label;
	private static int saveID;

	private static boolean bDisplayedChannelsOnly = false;
	private static boolean bAllTimeFrames = false;
	private static boolean bOutputImage5D = false;
	private static boolean bDoScaling = true;

	public Make_Montage() {}

	@Override
	public void run(final String arg) {
		IJ.register(Make_Montage.class);
		final ImagePlus imp = WindowManager.getCurrentImage();
		if (imp == null) {
			IJ.noImage();
			return;
		}
		makeMontage(imp);
	}

	/** To make montages via a GUI */
	public void makeMontage(final ImagePlus imp) {
		// Check if image is an Image5D.
		if (!(imp instanceof Image5D)) {
			IJ.error("Make Montage", "Image5D required");
			return;
		}

		final Image5D i5d = (Image5D) imp;

		final int nSlices = imp.getStackSize();
		if (columns == 0 || imp.getID() != saveID) {
			columns = (int) Math.sqrt(nSlices);
			rows = columns;
			final int n = nSlices - columns * rows;
			if (n > 0) columns += (int) Math.ceil((double) n / rows);
			scale = 1.0;
			if (imp.getWidth() * columns > 800) scale = 0.5;
			if (imp.getWidth() * columns > 1600) scale = 0.25;
			inc = 1;
			first = 1;
			last = nSlices;
		}

		final GenericDialog gd =
			new GenericDialog("Make Montage", IJ.getInstance());
		gd.addNumericField("Columns:", columns, 0);
		gd.addNumericField("Rows:", rows, 0);
		gd.addNumericField("Scale Factor:", scale, 2);
		gd.addNumericField("First Slice:", first, 0);
		gd.addNumericField("Last Slice:", last, 0);
		gd.addNumericField("Increment:", inc, 0);
		gd.addNumericField("Border Width:", borderWidth, 0);
		gd.addCheckbox("Label Slices", label);

		gd.addCheckbox("Displayed Channels only", bDisplayedChannelsOnly);
		gd.addCheckbox("All Time Frames", bAllTimeFrames);
		gd.addCheckbox("Output as Image5D", bOutputImage5D);
		gd.addCheckbox("Copy Contrast and Brightness", bDoScaling);
		gd.showDialog();
		if (gd.wasCanceled()) return;
		columns = (int) gd.getNextNumber();
		rows = (int) gd.getNextNumber();
		scale = gd.getNextNumber();
		first = (int) gd.getNextNumber();
		last = (int) gd.getNextNumber();
		inc = (int) gd.getNextNumber();
		borderWidth = (int) gd.getNextNumber();
		if (borderWidth < 0) borderWidth = 0;
		if (first < 1) first = 1;
		if (last > nSlices) last = nSlices;
		if (inc < 1) inc = 1;
		if (gd.invalidNumber()) {
			IJ.error("Invalid number");
			return;
		}
		label = gd.getNextBoolean();

		setDisplayedChannelsOnly(gd.getNextBoolean());
		setAllTimeFrames(gd.getNextBoolean());
		setOutputImage5D(gd.getNextBoolean());
		setDoScaling(gd.getNextBoolean());

		saveID = imp.getID();

		final long tstart = System.currentTimeMillis();

		// Do Montage
		if (bOutputImage5D) {
			final Image5D resultI5D =
				doI5DMontage(i5d, columns, rows, scale, first, last, inc, borderWidth,
					label);
			if (resultI5D != null) resultI5D.show();
		}
		else {
			final ImagePlus resultImage =
				doMontage(i5d, columns, rows, scale, first, last, inc, borderWidth,
					label);
			if (resultImage != null) resultImage.show();
		}

		final long tstop = System.currentTimeMillis();
		IJ.showStatus("Montage: " + IJ.d2s((tstop - tstart) / 1000.0, 2) +
			" seconds");
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

	ImagePlus doMontage(final Image5D i5d, final int columns, final int rows,
		final double scale, final int first, final int last, final int inc,
		final int borderWidth, final boolean labels)
	{
		if (!i5d.lock()) return null; // exit if in use

		final ImagePlus imp = i5d;

		final int width = (int) (i5d.getWidth() * scale);
		final int height = (int) (i5d.getHeight() * scale);
		final int montageImageWidth = width * columns + borderWidth / 2;
		final int montageImageHeight = height * rows + borderWidth / 2;

		final int currentChannel = i5d.getCurrentChannel();
		final int currentSlice = i5d.getCurrentSlice();
		final int currentFrame = i5d.getCurrentFrame();

		// Do Projection.
		// Check, which channels are projected and store channel changes to keep
		// data consistent
		int nMontagedChannels = 0;
		final int[] montagedChannels = new int[i5d.getNChannels()];
		for (int c = 1; c <= i5d.getNChannels(); c++) {
			i5d.storeChannelProperties(c);
			if (bDisplayedChannelsOnly &&
				((i5d.getDisplayMode() == ChannelControl.OVERLAY && !i5d
					.getChannelDisplayProperties(c).isDisplayedInOverlay()) || i5d
					.getDisplayMode() != ChannelControl.OVERLAY &&
					c != currentChannel)) continue;
			montagedChannels[nMontagedChannels] = c;
			nMontagedChannels++;
		}
		if (bDisplayedChannelsOnly && nMontagedChannels == 0) {
			return null;
		}

		int startFrame = i5d.getCurrentFrame();
		int nFrames = 1;
		if (bAllTimeFrames) {
			startFrame = 1;
			nFrames = i5d.getNFrames();
		}

		// Allocate output arrays
		final byte[] reds = new byte[montageImageWidth * montageImageHeight];
		final byte[] greens = new byte[montageImageWidth * montageImageHeight];
		final byte[] blues = new byte[montageImageWidth * montageImageHeight];
		final String newTitle =
			WindowManager.makeUniqueName(imp.getTitle() + " Montage");
		final ImagePlus resultImp =
			IJ.createImage(newTitle, "rgb black", montageImageWidth,
				montageImageHeight, nFrames);
		resultImp.setCalibration(imp.getCalibration().copy());

		for (int frame = startFrame; frame < startFrame + nFrames; frame++) {
			for (int destChannel = 1; destChannel <= nMontagedChannels; destChannel++)
			{
				final int srcChannel = montagedChannels[destChannel - 1];

				// Do montage for each channel separately
				i5d.setCurrentPosition(0, 0, srcChannel - 1, currentSlice - 1,
					frame - 1);
				final ImageStack tmp = i5d.getStack();
				final ImagePlus tempImg =
					new ImagePlus(imp.getTitle() + " Montage", tmp);
//                ImagePlus tempImg = new ImagePlus(imp.getTitle()+" Montage", i5d.getStack());
				final ImagePlus montage =
					makeMontage(tempImg, columns, rows, scale, first, last, inc,
						borderWidth, label);
				tempImg.flush();
				if (bDoScaling) {
					montage.getProcessor().setMinAndMax(
						i5d.getChannelDisplayProperties(srcChannel).getMinValue(),
						i5d.getChannelDisplayProperties(srcChannel).getMaxValue());
				}
				else {
					montage.getProcessor().resetMinAndMax();
				}

				// Sort projections into color channels
				final ColorProcessor proc =
					(ColorProcessor) (new TypeConverter(montage.getProcessor(),
						bDoScaling)).convertToRGB();
				final int[] rgb = new int[3];
				for (int x = 0; x < montageImageWidth; x++) {
					for (int y = 0; y < montageImageHeight; y++) {
						final int pos = x + montageImageWidth * y;
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
				montage.flush();
			}
			final ColorProcessor cp =
				new ColorProcessor(montageImageWidth, montageImageHeight);
			cp.setRGB(reds, greens, blues);
			resultImp.setSlice(frame - startFrame + 1);
			resultImp.setProcessor(null, cp);
			for (int i = 0; i < montageImageWidth * montageImageHeight; i++) {
				reds[i] = 0;
				greens[i] = 0;
				blues[i] = 0;
			}
		}

		i5d.setCurrentPosition(0, 0, currentChannel - 1, currentSlice - 1,
			currentFrame - 1);
		i5d.unlock();

		return resultImp;
	}

	Image5D doI5DMontage(final Image5D i5d, final int columns, final int rows,
		final double scale, final int first, final int last, final int inc,
		final int borderWidth, final boolean labels)
	{
		if (!i5d.lock()) return null; // exit if in use

		final ImagePlus imp = i5d;

		final int width = (int) (i5d.getWidth() * scale);
		final int height = (int) (i5d.getHeight() * scale);
		final int montageImageWidth = width * columns + borderWidth / 2;
		final int montageImageHeight = height * rows + borderWidth / 2;

		final int currentChannel = i5d.getCurrentChannel();
		final int currentSlice = i5d.getCurrentSlice();
		final int currentFrame = i5d.getCurrentFrame();

		// Do Montage.
		// Check, which channels are montaged and store channel changes to keep data
		// consistent
		int nMontagedChannels = 0;
		final int[] montagedChannels = new int[i5d.getNChannels()];
		for (int c = 1; c <= i5d.getNChannels(); c++) {
			i5d.storeChannelProperties(c);
			if (bDisplayedChannelsOnly &&
				((i5d.getDisplayMode() == ChannelControl.OVERLAY && !i5d
					.getChannelDisplayProperties(c).isDisplayedInOverlay()) || i5d
					.getDisplayMode() != ChannelControl.OVERLAY &&
					c != currentChannel)) continue;
			montagedChannels[nMontagedChannels] = c;
			nMontagedChannels++;
		}
		if (bDisplayedChannelsOnly && nMontagedChannels == 0) {
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
			WindowManager.makeUniqueName(imp.getTitle() + " Montage");
		final Image5D resultI5D =
			new Image5D(newTitle, i5d.getType(), montageImageWidth,
				montageImageHeight, nMontagedChannels, 1, nFrames, false);
		resultI5D.setCalibration(i5d.getCalibration().copy());

		for (int frame = startFrame; frame < startFrame + nFrames; frame++) {
			for (int destChannel = 1; destChannel <= nMontagedChannels; destChannel++)
			{
				final int srcChannel = montagedChannels[destChannel - 1];

				// Do montage for each channel separately
				i5d.setCurrentPosition(0, 0, srcChannel - 1, currentSlice - 1,
					frame - 1);
				final ImagePlus tempImg =
					new ImagePlus(imp.getTitle() + " Montage", i5d.getStack());
				final ImagePlus montage =
					makeMontage(tempImg, columns, rows, scale, first, last, inc,
						borderWidth, label);
				tempImg.flush();

				resultI5D.setPixels(montage.getProcessor().getPixels(), destChannel, 1,
					frame - startFrame + 1);
				montage.flush();
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
		i5d.unlock();

		return resultI5D;
	}

	/**
	 * Copied fr0m ij.plugin.MontageMaker, because there the Montage image is not
	 * returned but displayed.
	 */
	public ImagePlus makeMontage(final ImagePlus imp, final int columns,
		final int rows, final double scale, final int first, final int last,
		final int inc, final int borderWidth, final boolean labels)
	{
		final int stackWidth = imp.getWidth();
		final int stackHeight = imp.getHeight();
		final int width = (int) (stackWidth * scale);
		final int height = (int) (stackHeight * scale);
		final int montageWidth = width * columns;
		final int montageHeight = height * rows;
		final ImageProcessor ip = imp.getProcessor();
		final ImageProcessor montage =
			ip.createProcessor(montageWidth + borderWidth / 2, montageHeight +
				borderWidth / 2);
		final ImageStatistics is = imp.getStatistics();
		boolean blackBackground = is.mode < 200;
		if (imp.isInvertedLut()) blackBackground = !blackBackground;
		if ((ip instanceof ShortProcessor) || (ip instanceof FloatProcessor)) blackBackground =
			true;
		if (blackBackground) {
			final float[] cTable = imp.getCalibration().getCTable();
			final boolean signed16Bit = cTable != null && cTable[0] == -32768;
			if (signed16Bit) montage.setValue(32768);
			else montage.setColor(Color.black);
			montage.fill();
			montage.setColor(Color.white);
		}
		else {
			montage.setColor(Color.white);
			montage.fill();
			montage.setColor(Color.black);
		}
		final ImageStack stack = imp.getStack();
		int x = 0;
		int y = 0;
		ImageProcessor aSlice;
		int slice = first;
		while (slice <= last) {
			aSlice = stack.getProcessor(slice);
			if (scale != 1.0) aSlice = aSlice.resize(width, height);
			montage.insert(aSlice, x, y);
			final String label = stack.getShortSliceLabel(slice);
			if (borderWidth > 0) drawBorder(montage, x, y, width, height, borderWidth);
			if (labels) drawLabel(montage, slice, label, x, y, width, height);
			x += width;
			if (x >= montageWidth) {
				x = 0;
				y += height;
				if (y >= montageHeight) break;
			}
			IJ.showProgress((double) (slice - first) / (last - first));
			slice += inc;
		}
		if (borderWidth > 0) {
			final int w2 = borderWidth / 2;
			drawBorder(montage, w2, w2, montageWidth - w2, montageHeight - w2,
				borderWidth);
		}
		IJ.showProgress(1.0);
		final ImagePlus imp2 = new ImagePlus("Montage", montage);
		imp2.setCalibration(imp.getCalibration());
		final Calibration cal = imp2.getCalibration();
		if (cal.scaled()) {
			cal.pixelWidth /= scale;
			cal.pixelHeight /= scale;
		}
		return imp2;
	}

	void drawBorder(final ImageProcessor montage, final int x, final int y,
		final int width, final int height, final int borderWidth)
	{
		montage.setLineWidth(borderWidth);
		montage.moveTo(x, y);
		montage.lineTo(x + width, y);
		montage.lineTo(x + width, y + height);
		montage.lineTo(x, y + height);
		montage.lineTo(x, y);
	}

	void drawLabel(final ImageProcessor montage, final int slice, String label,
		int x, int y, final int width, final int height)
	{
		if (label != null && !label.equals("") &&
			montage.getStringWidth(label) >= width)
		{
			do {
				label = label.substring(0, label.length() - 1);
			}
			while (label.length() > 1 && montage.getStringWidth(label) >= width);
		}
		if (label == null || label.equals("")) label = "" + slice;
		final int swidth = montage.getStringWidth(label);
		x += width / 2 - swidth / 2;
		y += height;
		montage.moveTo(x, y);
		montage.drawString(label);
	}

}
