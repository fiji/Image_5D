//
// Hypervolume_Opener.java
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

import i5d.util.NumberedStringSorter;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.GenericDialog;
import ij.io.FileInfo;
import ij.io.OpenDialog;
import ij.io.Opener;
import ij.measure.Calibration;
import ij.plugin.PlugIn;
import ij.process.ImageConverter;
import ij.process.ImageProcessor;

import java.awt.Checkbox;
import java.awt.Label;
import java.awt.TextField;
import java.awt.event.ItemEvent;
import java.awt.event.TextEvent;
import java.awt.image.ColorModel;
import java.io.File;

/**
 * Opens a folder of image stacks as one big stack ("Hypervolume"). Can be used
 * with the Hypervolume Browser and Shuffler. Code mostly copied from ImageJs
 * FolderOpener.
 */
public class Hypervolume_Opener implements PlugIn {

	private static boolean grayscale;
	private static double scale = 100.0;
	private int n, start, increment;
	private String filter;
	private FileInfo fi;
	private Calibration cal;
	private String info1;

//	String selectedFilename;

	@Override
	public void run(final String arg) {
		// Get filename and directory and sort list of files in directory.
		final OpenDialog od =
			new OpenDialog("Open Sequence of Image Stacks...", arg);
		final String directory = od.getDirectory();
		final String name = od.getFileName();
		if (name == null) return;
//		selectedFilename = name;		
		final String[] list = new File(directory).list();
		if (list == null) return;
		NumberedStringSorter.sort(list);
		if (IJ.debugMode) IJ.log("Hypervolume_Opener: " + directory + " (" +
			list.length + " files)");

		// some inits.
		int width = 0, height = 0, type = 0, depth = 0;
		ImageStack stack = null;
		double min = Double.MAX_VALUE;
		double max = -Double.MAX_VALUE;
		IJ.register(Hypervolume_Opener.class);

		try {
			// Open selected image and show Dialog.
			ImagePlus imp = new Opener().openImage(directory, name);
			if (imp != null) {
				width = imp.getWidth();
				height = imp.getHeight();
				depth = imp.getStackSize();
				type = imp.getType();
				fi = imp.getOriginalFileInfo();
				cal = imp.getCalibration();
			}
			else { // Selected file is no image. Try opening starting from first
							// file.// Open first image in filelist and show Dialog.
				for (int i = 0; i < list.length; i++) {
					if (list[i].endsWith(".txt") || list[i].endsWith(".roi")) continue;
					imp = new Opener().openImage(directory, list[i]);
					if (imp != null) {
						width = imp.getWidth();
						height = imp.getHeight();
						depth = imp.getStackSize();
						type = imp.getType();
						fi = imp.getOriginalFileInfo();
						cal = imp.getCalibration();
						break;
					}
				}
			}

			if (imp != null && !showDialog(imp, list)) {
				return;
			}

			if (width == 0) {
				IJ.showMessage("Import Sequence",
					"This folder does not appear to contain any TIFF,\n"
						+ "JPEG, BMP, DICOM, GIF, FITS or PGM files.");
				return;
			}

			// n: number of images. Given in dialog.
			if (n < 1) n = list.length;
			if (start < 1 || start > list.length) start = 1;
			if (start + n - 1 > list.length) n = list.length - start + 1;
			int filteredImages = n;
			if (filter != null && (filter.equals("") || filter.equals("*"))) filter =
				null;
			if (filter != null) {
				filteredImages = 0;
				for (int i = start - 1; i < list.length; i++) {
					if (list[i].indexOf(filter) >= 0) filteredImages++;
				}
				if (filteredImages == 0) {
					IJ.error("None of the " + n + " files contain\n the string '" +
						filter + "' in their name.");
					return;
				}
			}
			if (filteredImages < n) n = filteredImages;

			int count = 0;
			int counter = 0;
			for (int i = start - 1; i < list.length; i++) {
				if (list[i].endsWith(".txt") || list[i].endsWith(".roi")) continue;
				if (filter != null && (list[i].indexOf(filter) < 0)) continue;
				if (++counter < start) continue;
				if (((counter - start) % increment) != 0) continue;
				imp = new Opener().openImage(directory, list[i]);
				if (imp != null && stack == null) {
					width = imp.getWidth();
					height = imp.getHeight();
					type = imp.getType();
					final ColorModel cm = imp.getProcessor().getColorModel();
					if (scale < 100.0) stack =
						new ImageStack((int) (width * scale / 100.0),
							(int) (height * scale / 100.0), cm);
					else stack = new ImageStack(width, height, cm);
					info1 = (String) imp.getProperty("Info");
				}

				if (imp == null) {
					if (!list[i].startsWith(".")) IJ.log(list[i] + ": unable to open");
				}
				else if (imp.getWidth() != width || imp.getHeight() != height) IJ
					.log(list[i] + ": wrong dimensions");
				else if (imp.getType() != type) IJ.log(list[i] + ": wrong type");
				else {
					count += 1;
					IJ.showStatus(count + "/" + n);
					IJ.showProgress((double) count / n);
					depth = imp.getStackSize();

					for (int iSlice = 1; iSlice <= depth; iSlice++) {
						imp.setSlice(iSlice);
						ImageProcessor ip = imp.getProcessor();
						if (grayscale) {
							final ImageConverter ic = new ImageConverter(imp);
							ic.convertToGray8();
							ip = imp.getProcessor();
						}
						if (scale < 100.0) ip =
							ip.resize((int) (width * scale / 100.0),
								(int) (height * scale / 100.0));
						if (ip.getMin() < min) min = ip.getMin();
						if (ip.getMax() > max) max = ip.getMax();
						String label = imp.getTitle();
						final String info = (String) imp.getProperty("Info");
						if (info != null) label += "\n" + info;
						stack.addSlice(label, ip);
					}
				}
				if (count >= n) break;
			} // for loop over files in directory
		}
		catch (final OutOfMemoryError e) {
			IJ.outOfMemory("Hypervolume_Opener");
			if (stack != null) stack.trim();
		}
		if (stack != null && stack.getSize() > 0) {
			final ImagePlus imp2 = new ImagePlus("Stack", stack);
			if (imp2.getType() == ImagePlus.GRAY16 ||
				imp2.getType() == ImagePlus.GRAY32) imp2.getProcessor().setMinAndMax(
				min, max);
			imp2.setFileInfo(fi); // saves FileInfo of the first image
			imp2.setCalibration(cal); // saves Calibration of the first image
			if (imp2.getStackSize() == 1 && info1 != null) imp2.setProperty("Info",
				info1);
			imp2.show();
		}
		IJ.showProgress(1.0);

		System.gc();
	}

	boolean showDialog(final ImagePlus imp, final String[] list) {
		final int fileCount = list.length;

		String name = imp.getTitle();
		if (name.length() > 4 &&
			(name.substring(name.length() - 4, name.length()))
				.equalsIgnoreCase(".tif"))
		{
			name = name.substring(0, name.length() - 4);
		}
		int i = name.length() - 1;
		while (i > 1 && name.charAt(i) >= '0' && name.charAt(i) <= '9') {
			name = name.substring(0, i);
			i--;
		}

		final HypervolumeOpenerDialog gd =
			new HypervolumeOpenerDialog("Sequence Options", imp, list);
		gd.addNumericField("Number of Images: ", fileCount, 0);
		gd.addNumericField("Starting Image: ", 1, 0);
		gd.addNumericField("Increment: ", 1, 0);
		gd.addStringField("File Name Contains: ", name);
		gd.addNumericField("Scale Images", scale, 0, 4, "%");
		gd.addCheckbox("Convert to 8-bit Grayscale", grayscale);
		gd.addMessage("10000 x 10000 x 1000 (100.3MB)");
		gd.showDialog();
		if (gd.wasCanceled()) return false;
		n = (int) gd.getNextNumber();
		start = (int) gd.getNextNumber();
		increment = (int) gd.getNextNumber();
		if (increment < 1) increment = 1;
		scale = gd.getNextNumber();
		if (scale < 5.0) scale = 5.0;
		if (scale > 100.0) scale = 100.0;
		filter = gd.getNextString();
		grayscale = gd.getNextBoolean();

		return true;
	}

}

class HypervolumeOpenerDialog extends GenericDialog {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4208097793696642019L;
	ImagePlus imp;
	int fileCount;
	boolean eightBits;
	String saveFilter = "";
	String[] list;

	public HypervolumeOpenerDialog(final String title, final ImagePlus imp,
		final String[] list)
	{
		super(title);
		this.imp = imp;
		this.list = list;
		this.fileCount = list.length;
	}

	@Override
	protected void setup() {
		setStackInfo();
	}

	@Override
	public void itemStateChanged(final ItemEvent e) {
		setStackInfo();
	}

	@Override
	public void textValueChanged(final TextEvent e) {
		setStackInfo();
	}

	void setStackInfo() {
		int width = imp.getWidth();
		int height = imp.getHeight();
		int bytesPerPixel = 1;
		final int nSlices = imp.getStackSize();
		eightBits = ((Checkbox) checkbox.elementAt(0)).getState();
		int n = getNumber(numberField.elementAt(0));
		int start = getNumber(numberField.elementAt(1));
		int inc = getNumber(numberField.elementAt(2));
		double scale = getNumber(numberField.elementAt(3));
		if (scale < 5.0) scale = 5.0;
		if (scale > 100.0) scale = 100.0;

		if (n < 1) n = fileCount;
		if (start < 1 || start > fileCount) start = 1;
		if (start + n - 1 > fileCount) {
			n = fileCount - start + 1;
			// TextField tf = (TextField)numberField.elementAt(0);
			// tf.setText(""+nImages);
		}
		if (inc < 1) inc = 1;
		final TextField tf = (TextField) stringField.elementAt(0);
		final String filter = tf.getText();
		// IJ.write(nImages+" "+startingImage);
		if (!filter.equals("") && !filter.equals("*")) {
			final int n2 = n;
			n = 0;
			for (int i = start - 1; i < start - 1 + n2; i++)
				if (list[i].indexOf(filter) >= 0) {
					n++;
					// IJ.write(n+" "+list[i]);
				}
			saveFilter = filter;
		}
		switch (imp.getType()) {
			case ImagePlus.GRAY16:
				bytesPerPixel = 2;
				break;
			case ImagePlus.COLOR_RGB:
			case ImagePlus.GRAY32:
				bytesPerPixel = 4;
				break;
		}
		if (eightBits) bytesPerPixel = 1;
		width = (int) (width * scale / 100.0);
		height = (int) (height * scale / 100.0);
		int n2 = n / inc;
		if (n2 < 0) n2 = 0;
		final double size =
			(double) (width * height * nSlices * n2 * bytesPerPixel) / (1024 * 1024);
		((Label) theLabel).setText(width + " x " + height + " x " + nSlices * n2 +
			" (" + IJ.d2s(size, 1) + "MB)");
	}

	public int getNumber(final Object field) {
		final TextField tf = (TextField) field;
		final String theText = tf.getText();
		Double d;
		try {
			d = new Double(theText);
		}
		catch (final NumberFormatException e) {
			d = null;
		}
		if (d != null) return (int) d.doubleValue();
		return 0;
	}
}
