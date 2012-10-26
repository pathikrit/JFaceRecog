package com.addepar.fun;

import com.googlecode.javacpp.Loader;
import com.googlecode.javacv.CanvasFrame;
import com.googlecode.javacv.OpenCVFrameGrabber;

import static com.googlecode.javacv.cpp.opencv_core.CvContour;
import static com.googlecode.javacv.cpp.opencv_core.CvMemStorage;
import static com.googlecode.javacv.cpp.opencv_core.CvSeq;
import static com.googlecode.javacv.cpp.opencv_core.IPL_DEPTH_8U;
import static com.googlecode.javacv.cpp.opencv_core.IplImage;
import static com.googlecode.javacv.cpp.opencv_core.cvAbsDiff;
import static com.googlecode.javacv.cpp.opencv_imgproc.CV_CHAIN_APPROX_SIMPLE;
import static com.googlecode.javacv.cpp.opencv_imgproc.CV_GAUSSIAN;
import static com.googlecode.javacv.cpp.opencv_imgproc.CV_RETR_LIST;
import static com.googlecode.javacv.cpp.opencv_imgproc.CV_RGB2GRAY;
import static com.googlecode.javacv.cpp.opencv_imgproc.CV_THRESH_BINARY;
import static com.googlecode.javacv.cpp.opencv_imgproc.cvCvtColor;
import static com.googlecode.javacv.cpp.opencv_imgproc.cvFindContours;
import static com.googlecode.javacv.cpp.opencv_imgproc.cvSmooth;
import static com.googlecode.javacv.cpp.opencv_imgproc.cvThreshold;

public class MotionDetector {
   public static void main(String[] args) throws Exception {
    OpenCVFrameGrabber grabber = new OpenCVFrameGrabber(0);
    grabber.start();

    IplImage frame = grabber.grab();
    IplImage image = null;
    IplImage prevImage = null;
    IplImage diff = null;

    CanvasFrame canvasFrame = new CanvasFrame("Some Title");
    canvasFrame.setCanvasSize(frame.width(), frame.height());

    CvMemStorage storage = CvMemStorage.create();

    while (canvasFrame.isVisible() && (frame = grabber.grab()) != null) {
      cvSmooth(frame, frame, CV_GAUSSIAN, 9, 9, 2, 2);
      if (image == null) {
         image = IplImage.create(frame.width(), frame.height(), IPL_DEPTH_8U, 1);
         cvCvtColor(frame, image, CV_RGB2GRAY);
      } else {
         prevImage = image;
         image = IplImage.create(frame.width(), frame.height(), IPL_DEPTH_8U, 1);
         cvCvtColor(frame, image, CV_RGB2GRAY);
      }

      if (diff == null) {
         diff = IplImage.create(frame.width(), frame.height(), IPL_DEPTH_8U, 1);
      }

      if (prevImage != null) {
         cvAbsDiff(image, prevImage, diff);
         cvThreshold(diff, diff, 64, 255, CV_THRESH_BINARY);
         canvasFrame.showImage(diff);
         CvSeq contour = new CvSeq(null);
         cvFindContours(diff, storage, contour, Loader.sizeof(CvContour.class), CV_RETR_LIST, CV_CHAIN_APPROX_SIMPLE);
         while (contour != null && !contour.isNull()) {
          contour = contour.h_next();
         }
      }
    }
    grabber.stop();
    canvasFrame.dispose();
   }
}
