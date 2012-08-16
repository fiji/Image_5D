These plugins extend image stacks to 5 dimensions: x, y, channel (color), slice
(z), frame (t). Unlike the HyperVolume Browser, an Image5D has a "true" 5D
format. That way plugins working on stacks should usually work as expected on
the currently displayed stack of an Image5D (just try a z-projection to see
what this means). Image5Ds are displayed in a window with two scrollbars for
slice and time below the image and a panel with controls to change the current
channel and its color to the right of the image. A dropdown menu allows to
change the display of the channels. Options are one channel in gray, one
channel in color and an overlay of selected channels.

For further details, see the Image5D web page at:
    http://developer.imagej.net/plugins/image5d
