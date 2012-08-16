//
// Add_Empty_Channel.java
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
import i5d.gui.Image5DWindow;
import ij.*;
import ij.gui.GenericDialog;
import ij.plugin.*;
/*
 * Created on 09.08.2005
 *
 */

/**
 * @author Joachim Walter
 *
 */
public class Add_Empty_Channel implements PlugIn {
 
    static boolean sameSlices;
    static boolean sameFrames;
    
     @Override
     public void run(String arg) {
        ImagePlus imp = WindowManager.getCurrentImage();
        if (! (imp instanceof Image5D)) {
            IJ.error("Current Image is not an Image5D.");
            return;
        }
        IJ.register(Add_Empty_Channel.class);
        
        
        GenericDialog gd = new GenericDialog("Add Empty Channel");
        gd.addStringField("Channel Label","");
        gd.addCheckbox("Same data for all z-slices", sameSlices);
        gd.addCheckbox("Same data for all time-frames", sameFrames);
        gd.showDialog();
        if (gd.wasCanceled()) {
            return;
        }
        String channelLabel = gd.getNextString();
        sameSlices = gd.getNextBoolean();
        sameFrames = gd.getNextBoolean();
        
        Image5D i5d = (Image5D)imp;
        Image5DWindow win = ((Image5DWindow)i5d.getWindow());
        int n = i5d.getNChannels();
        
        if (!sameSlices && !sameFrames) {
            i5d.expandDimension(i5d.getColorDimension(), n+1, true);        
    		i5d.getChannelCalibration(n+1).setLabel(channelLabel);
        } else if (sameSlices && !sameFrames){
            i5d.expandDimension(i5d.getColorDimension(), n+1, false);
            for (int f=1; f<=i5d.getNFrames(); f++) {
                Object pixels = i5d.createEmptyPixels();
                for (int s=1; s<=i5d.getNSlices(); s++) {
                    i5d.setPixels(pixels, n+1, s, f);
                }
            }    
            i5d.getChannelCalibration(n+1).setLabel(channelLabel);
        } else if (!sameSlices && sameFrames){
            i5d.expandDimension(i5d.getColorDimension(), n+1, false);
            for (int s=1; s<=i5d.getNSlices(); s++) {
                Object pixels = i5d.createEmptyPixels();
                for (int f=1; f<=i5d.getNFrames(); f++) {
                    i5d.setPixels(pixels, n+1, s, f);
                }
            }    
            i5d.getChannelCalibration(n+1).setLabel(channelLabel);
        } else if (sameSlices && sameFrames){
            i5d.expandDimension(i5d.getColorDimension(), n+1, false);
            Object pixels = i5d.createEmptyPixels();
            for (int s=1; s<=i5d.getNSlices(); s++) {
                for (int f=1; f<=i5d.getNFrames(); f++) {
                    i5d.setPixels(pixels, n+1, s, f);
                }
            }    
            i5d.getChannelCalibration(n+1).setLabel(channelLabel);
        }
		
        win.updateSliceSelector();
    }

}
