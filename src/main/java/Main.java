//
// Main.java
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

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;

import java.awt.Component;
import java.awt.Container;
import java.awt.Scrollbar;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

/**
 * Simple class with main method for testing Image5D behavior.
 * 
 * @author Curtis Rueden
 */
public class Main {

	public static void main(final String... args) {
		if (IJ.getInstance() == null) new ImageJ();
		final Open_Image5D openImage5D = new Open_Image5D();
		openImage5D.run("");

		final ImagePlus imp = IJ.getImage();

		final MyListener listener = new MyListener(imp);
		addScrollListener(imp, listener, listener);
	}

	public static void addScrollListener(final ImagePlus img,
		final AdjustmentListener al, final MouseWheelListener ml)
	{
		for (final Component c : img.getWindow().getComponents()) {
			if (c instanceof Scrollbar) ((Scrollbar) c).addAdjustmentListener(al);
			else if (c instanceof Container) {
				for (final Component c2 : ((Container) c).getComponents()) {
					if (c2 instanceof Scrollbar) {
						((Scrollbar) c2).addAdjustmentListener(al);
					}
				}
			}
		}
		img.getWindow().addMouseWheelListener(ml);
	}

	private static class MyListener implements AdjustmentListener,
		MouseWheelListener
	{

		private final ImagePlus imp;

		public MyListener(final ImagePlus imp) {
			this.imp = imp;
		}

		@Override
		public void mouseWheelMoved(final MouseWheelEvent e) {
			printPositionInfo();
		}

		@Override
		public void adjustmentValueChanged(final AdjustmentEvent e) {
			printPositionInfo();
		}

		private void printPositionInfo() {
			final int channel = imp.getChannel();
			final int slice = imp.getSlice();
			final int frame = imp.getFrame();
			IJ.log("Position: c=" + channel + ", z=" + slice + ", t=" + frame);
		}
	}

}
