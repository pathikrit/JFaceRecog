package com.addepar.fun.hack;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.Map;

import com.google.common.collect.Maps;
import com.googlecode.javacv.cpp.opencv_contrib;
import com.googlecode.javacv.cpp.opencv_core.CvMat;
import com.googlecode.javacv.cpp.opencv_core.IplImage;
import com.googlecode.javacv.cpp.opencv_core.MatVector;

import static com.googlecode.javacv.cpp.opencv_contrib.createLBPHFaceRecognizer;
import static com.googlecode.javacv.cpp.opencv_core.CV_32SC1;
import static com.googlecode.javacv.cpp.opencv_core.IPL_DEPTH_8U;
import static com.googlecode.javacv.cpp.opencv_core.cvCreateMat;
import static com.googlecode.javacv.cpp.opencv_imgproc.CV_BGR2GRAY;
import static com.googlecode.javacv.cpp.opencv_imgproc.CV_INTER_AREA;
import static com.googlecode.javacv.cpp.opencv_imgproc.cvCvtColor;
import static com.googlecode.javacv.cpp.opencv_imgproc.cvResize;

public class FaceRecognizer {

  // We can try out different algorithms here: http://docs.opencv.org/trunk/modules/contrib/doc/facerec/facerec_api.html
  private final opencv_contrib.FaceRecognizer algorithm =
    createLBPHFaceRecognizer(1, 8, 8, 8, Double.MAX_VALUE).get();
    //createFisherFaceRecognizer(0, THRESHHOLD).get(),
    //createEigenFaceRecognizer(0, THRESHHOLD).get()

  private final Map<Integer, String> names = Maps.newHashMap();

  public FaceRecognizer(FaceDb db) {
    // code from here: http://stackoverflow.com/questions/11913980/
    final int numberOfImages = db.size();
    final MatVector images = new MatVector(numberOfImages);
    final CvMat labels = cvCreateMat(1, numberOfImages, CV_32SC1);

    int imgCount = 0, personCount = 0;
    for(String name : db.names()) {
      for (BufferedImage image : db.get(name)) {
        images.put(imgCount, toTinyGray(image));
        labels.put(imgCount, personCount);
        imgCount++;
      }
      names.put(personCount++, name);
    }

    algorithm.train(images, labels);  // expensive operation
    algorithm.save(algorithm.name() + ".yml");  // save the expensive hard work
  }

  public FacialRecognition identifyFaces(BufferedImage image) {
    final FacialRecognition result = new FacialRecognition(image);
    for(Rectangle r : FacialRecognition.detectFaces(image)) {
      final BufferedImage candidate = image.getSubimage(r.x, r.y, r.width, r.height);
      final IplImage iplImage = toTinyGray(candidate);
      final int[] prediction = new int[1];
      final double[] confidence = new double[1];
      algorithm.predict(iplImage, prediction, confidence);
      result.add(r, names.get(prediction[0]), confidence[0]);
    }
    return result;
  }

  private IplImage toTinyGray(BufferedImage image) {
    final int X = 100, Y = 100;
    final IplImage src = IplImage.createFrom(image);
    final IplImage tiny = IplImage.create(X, Y, IPL_DEPTH_8U, 1);
    final IplImage gray = IplImage.create(image.getWidth(), image.getHeight(), IPL_DEPTH_8U, 1);
    cvCvtColor(src, gray, CV_BGR2GRAY);
    cvResize(gray, tiny, CV_INTER_AREA);  //todo: do tiny before gray
    return tiny;
  }
}
