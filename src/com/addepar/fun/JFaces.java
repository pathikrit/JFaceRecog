package com.addepar.fun;

import java.applet.Applet;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JApplet;


import com.googlecode.javacpp.Loader;
import com.googlecode.javacv.FrameGrabber;
import com.googlecode.javacv.OpenCVFrameGrabber;

import static com.googlecode.javacv.cpp.opencv_core.CvMemStorage;
import static com.googlecode.javacv.cpp.opencv_core.CvRect;
import static com.googlecode.javacv.cpp.opencv_core.CvSeq;
import static com.googlecode.javacv.cpp.opencv_core.IPL_DEPTH_8U;
import static com.googlecode.javacv.cpp.opencv_core.IplImage;
import static com.googlecode.javacv.cpp.opencv_core.cvClearMemStorage;
import static com.googlecode.javacv.cpp.opencv_core.cvGetSeqElem;
import static com.googlecode.javacv.cpp.opencv_core.cvLoad;
import static com.googlecode.javacv.cpp.opencv_imgproc.CV_BGR2GRAY;
import static com.googlecode.javacv.cpp.opencv_imgproc.CV_INTER_AREA;
import static com.googlecode.javacv.cpp.opencv_imgproc.cvCvtColor;
import static com.googlecode.javacv.cpp.opencv_imgproc.cvResize;
import static com.googlecode.javacv.cpp.opencv_highgui.cvSaveImage;
import static com.googlecode.javacv.cpp.opencv_objdetect.CV_HAAR_DO_CANNY_PRUNING;
import static com.googlecode.javacv.cpp.opencv_objdetect.CvHaarClassifierCascade;
import static com.googlecode.javacv.cpp.opencv_objdetect.cvHaarDetectObjects;

public class JFaces extends JApplet implements Runnable {

  private CvHaarClassifierCascade classifier = null;
  private CvMemStorage storage = null;
  private FrameGrabber grabber = null;
  private IplImage grabbedImage = null;
  private CvSeq faces = null;
  private boolean stop = false;
  private BufferedImage currentFace = null;

  @Override public void init() {
    try {
      File classifierFile = Loader.extractResource("haarcascade_frontalface_alt.xml", null, "classifier", ".xml");
      //Loader.load(opencv_objdetect.class); // Preload the opencv_objdetect module to work around a known bug.
      classifier = new CvHaarClassifierCascade(cvLoad(classifierFile.getAbsolutePath()));
      classifierFile.delete();
      storage = CvMemStorage.create();
      setSize(1600, 900);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Override public void start() {
    new Thread(this).start();
  }

  public void run() {
    try {
      int CAMERA_NUMBER = 0;
      try {
        grabber = FrameGrabber.createDefault(CAMERA_NUMBER);
      } catch (Exception e) {
        if (grabber != null) grabber.release();
        grabber = new OpenCVFrameGrabber(CAMERA_NUMBER);
      }

      grabber.start();
      grabbedImage = grabber.grab();

      final IplImage grayImage = IplImage.create(grabbedImage.width(), grabbedImage.height(), IPL_DEPTH_8U, 1);
      final IplImage tinyImage = IplImage.create(grabbedImage.width()/4, grabbedImage.height()/4, IPL_DEPTH_8U, 1);
      stop = false;
      while (!stop && (grabbedImage = grabber.grab()) != null) {
        if (faces == null) {
          cvClearMemStorage(storage);
          cvCvtColor(grabbedImage, grayImage, CV_BGR2GRAY);
          cvResize(grayImage, tinyImage, CV_INTER_AREA);
          faces = cvHaarDetectObjects(tinyImage, classifier, storage, 1.1, 3, CV_HAAR_DO_CANNY_PRUNING);
          repaint();
        }
      }
      grabbedImage = null;
      grabber.stop();
      grabber.release();
      grabber = null;
    } catch (Exception e) {
     e.printStackTrace();
    }
  }

  @Override public void update(Graphics g) {
    paint(g);
  }

  @Override public void paint(Graphics g) {
    if (grabbedImage == null) {
      return;
    }
    BufferedImage image = grabbedImage.getBufferedImage(2.2 / grabber.getGamma());
    Graphics2D g2 = image.createGraphics();
    if (faces != null) {
      g2.setColor(Color.RED);
      g2.setStroke(new BasicStroke(2));
      int total = faces.total();
      for (int i = 0; i < total; i++) {
        CvRect r = new CvRect(cvGetSeqElem(faces, i));
        if (currentFace != null) {
          g.clearRect(image.getWidth(), 0, currentFace.getWidth(), currentFace.getHeight());
        }
        currentFace = image.getSubimage(r.x()*4, r.y()*4, r.width()*4, r.height()*4);
        g.drawImage(currentFace, image.getWidth(), 0, null);
        g2.drawRect(r.x() * 4, r.y() * 4, r.width() * 4, r.height() * 4);
      }
      faces = null;
    }
    g.drawImage(image, 0, 0, null);

  }


  @Override public void stop() {
    stop = true;
  }

  @Override public void destroy() {
    try {
      ImageIO.write(currentFace, "png", new File("capture.png"));
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public double getMatchProbability() {
    return 90 + 10*Math.random();
  }
}
