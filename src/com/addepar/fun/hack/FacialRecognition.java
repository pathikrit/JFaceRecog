package com.addepar.fun.hack;


import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

import com.google.common.collect.Lists;
import com.googlecode.javacpp.Loader;
import com.googlecode.javacv.cpp.opencv_core.CvMemStorage;
import com.googlecode.javacv.cpp.opencv_core.CvRect;
import com.googlecode.javacv.cpp.opencv_core.CvSeq;
import com.googlecode.javacv.cpp.opencv_core.IplImage;
import com.googlecode.javacv.cpp.opencv_objdetect.CvHaarClassifierCascade;

import static com.googlecode.javacv.cpp.opencv_core.IPL_DEPTH_8U;
import static com.googlecode.javacv.cpp.opencv_core.cvClearMemStorage;
import static com.googlecode.javacv.cpp.opencv_core.cvGetSeqElem;
import static com.googlecode.javacv.cpp.opencv_core.cvLoad;
import static com.googlecode.javacv.cpp.opencv_imgproc.CV_BGR2GRAY;
import static com.googlecode.javacv.cpp.opencv_imgproc.CV_INTER_AREA;
import static com.googlecode.javacv.cpp.opencv_imgproc.cvCvtColor;
import static com.googlecode.javacv.cpp.opencv_imgproc.cvResize;
import static com.googlecode.javacv.cpp.opencv_objdetect.CV_HAAR_DO_CANNY_PRUNING;
import static com.googlecode.javacv.cpp.opencv_objdetect.cvHaarDetectObjects;

public class FacialRecognition {

  private final BufferedImage src;
  private final List<PotentialFace> faces = Lists.newArrayList();

  public FacialRecognition(BufferedImage src) {
    this.src = src;
  }

  public List<PotentialFace> getMatches() {
    return faces;
  }

  protected void add(Rectangle r, String name, double confidence) {
    faces.add(new PotentialFace(r, name, confidence));
  }

  public static class PotentialFace {
    private final Rectangle r;
    private final String name;
    private final double confidence;  // confidence that face at r is name

    public PotentialFace(Rectangle r, String name, double confidence) {
      this.r = r;
      this.name = name;
      this.confidence = confidence;
    }

    @Override
    public String toString() {
      return String.format("%s found at (%d,%d) with confidence = %s", name, r.x, r.y, confidence);
    }
  }

  @Override
  public String toString() {
    final StringBuilder out = new StringBuilder();
    for(PotentialFace f : faces) {
      out.append("\t").append(f).append("\n");
    }
    return out.toString();
  }

  private static final CvHaarClassifierCascade classifier;
  static {
    final File classifierFile;
    try {
      classifierFile = Loader.extractResource("haarcascade_frontalface_alt.xml", null, "classifier", ".xml");
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    // Loader.load(opencv_objdetect.class); // Preload the opencv_objdetect module to work around a known bug.
    classifier = new CvHaarClassifierCascade(cvLoad(classifierFile.getAbsolutePath()));
  }

  private final static CvMemStorage storage = CvMemStorage.create();
  private final static int F = 4; // scaling factor

  public static synchronized List<Rectangle> detectFaces(BufferedImage image) {
    cvClearMemStorage(storage);
    final CvSeq cvSeq = cvHaarDetectObjects(toTinyGray(image), classifier, storage, 1.1, 3, CV_HAAR_DO_CANNY_PRUNING);
    final int N = cvSeq.total();
    final List<Rectangle> ret = Lists.newArrayListWithCapacity(N);
    for (int i = 0; i < N; i++) {
      CvRect r = new CvRect(cvGetSeqElem(cvSeq, i));
      ret.add(new Rectangle(r.x()* F, r.y()* F, r.width()* F, r.height()* F));
    }
    return ret;
  }

  private static IplImage toTinyGray(BufferedImage bufferedImage) {
    final IplImage image = IplImage.createFrom(bufferedImage);
    final IplImage gray = IplImage.create(image.width(), image.height(), IPL_DEPTH_8U, 1);
    final IplImage tiny = IplImage.create(image.width()/F, image.height()/F, IPL_DEPTH_8U, 1);
    cvCvtColor(image, gray, CV_BGR2GRAY);
    cvResize(gray, tiny, CV_INTER_AREA);
    return tiny;
  }
}
