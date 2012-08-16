//
// Transfer_Channel_Settings.java
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
import i5d.cal.ChannelCalibration;
import ij.*;
import ij.gui.GenericDialog;
import ij.plugin.*;
/** Transfers channels colormaps from one Image5D to the current Image5D */
public class Transfer_Channel_Settings implements PlugIn {

    static int choiceID;
    static boolean transferColors = true;
    static boolean transferLabels = true;
    static boolean transferCalibrations = true;
    
    @Override
    public void run(String arg) {
        ImagePlus imp = WindowManager.getCurrentImage();
        
        if (imp==null) {
            IJ.noImage();
            return;
        }
        
        if (!(imp instanceof Image5D)) {
            IJ.error("Image is not an Image5D.");
            return;
        }
        IJ.register(Transfer_Channel_Settings.class);
        
        // Get ID list from WindowManager and sort IDs of all Image5Ds in a list.
        int[] idList = WindowManager.getIDList();
        Image5D[] i5dList = new Image5D[idList.length];
        int nI5Ds=0;
        String choiceTitle=null;
        for (int n=0; n<idList.length; n++) {
            if ((WindowManager.getImage(idList[n]) instanceof Image5D) && 
                    idList[n]!=imp.getID()) {
                i5dList[nI5Ds] = (Image5D)WindowManager.getImage(idList[n]);
                if(idList[n]==choiceID)
                    choiceTitle = WindowManager.getImage(idList[n]).getTitle();
                nI5Ds++;
            }
        }
        
        if (nI5Ds<1) {
            IJ.error("No Image5Ds to transfer from.");
            return;
        }
        
        if (choiceTitle==null) {
            choiceTitle=i5dList[0].getTitle();
        }

        String[] i5dTitles = new String[nI5Ds];
        for (int n=0; n<nI5Ds; n++) {
            i5dTitles[n] = i5dList[n].getTitle();
        }
        
        
        GenericDialog gd = new GenericDialog("Transfer Channel Settings");
        gd.addChoice("Transfer_Settings_from", i5dTitles, choiceTitle);
        gd.addCheckbox("ColorMaps", transferColors);
        gd.addCheckbox("Labels", transferLabels);
        gd.addCheckbox("Density_Calibrations", transferCalibrations);
        gd.showDialog();
        
        if(gd.wasCanceled()) {
            return;
        }

        transferColors = gd.getNextBoolean();
        transferLabels = gd.getNextBoolean();
        transferCalibrations = gd.getNextBoolean();
        
        Image5D src = i5dList[gd.getNextChoiceIndex()];
        Image5D dest = (Image5D)imp;
        
        choiceID = src.getID();
        
        int nChannels = Math.min(src.getNChannels(), dest.getNChannels());
        
        src.storeCurrentChannelProperties();
        dest.storeCurrentChannelProperties();
        for (int c=1; c<=nChannels; c++) {
            if (transferColors) {
                dest.setChannelColorModel(c, src.getChannelDisplayProperties(c).getColorModel());
            }
            if (transferLabels) {
                dest.getChannelCalibration(c).setLabel(src.getChannelCalibration(c).getLabel());
            }
            if (transferCalibrations) {
                // Make deep copy of ChannelCalibration to avoid messing up the function of the src.
                ChannelCalibration chCal = src.getChannelCalibration(c).copy();
                dest.getChannelCalibration(c).setFunction(chCal.getFunction(), chCal.getCoefficients(),
                                                            chCal.getValueUnit(), chCal.isZeroClip());
            }
        }
        
        dest.restoreCurrentChannelProperties();
        
        dest.updateAndRepaintWindow();
        dest.updateWindowControls();
    }

}
