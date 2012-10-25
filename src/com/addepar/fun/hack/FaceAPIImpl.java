package com.addepar.fun.hack;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.googlecode.javacv.cpp.opencv_contrib;
import com.googlecode.javacv.cpp.opencv_contrib.FaceRecognizer;
import com.googlecode.javacv.cpp.opencv_core.IplImage;
import com.googlecode.javacv.cpp.opencv_core.MatVector;

import static com.googlecode.javacv.cpp.opencv_contrib.createEigenFaceRecognizer;
import static com.googlecode.javacv.cpp.opencv_contrib.createFisherFaceRecognizer;
import static com.googlecode.javacv.cpp.opencv_contrib.createLBPHFaceRecognizer;
import static com.googlecode.javacv.cpp.opencv_highgui.cvLoadImage;

public class FaceAPIImpl implements FaceAPI {

  // todo: we should persist this in db
  private final ArrayListMultimap<String, MugShot> faceDb = ArrayListMultimap.create();

  private List<FaceRecognizer> faceRecognizers; // lazy init this

  @Override
  public String save(String name, BufferedImage pic) {
    final MugShot shot = new MugShot(pic);
    faceDb.put(name, shot);
    undoTraining();
    return shot.getId();
  }

  @Override
  public Map<String, Double> recognize(BufferedImage pic) {
    return null;  //TODO
  }

  @Override
  public List<MugShot> get(String name) {
    return faceDb.get(name);
  }

  @Override
  public boolean delete(String id) {
    for (String name : faceDb.keys()) {
      for (MugShot m : get(name)) {
        if (id.equals(m.getId())) {
          undoTraining();
          return faceDb.remove(name, m);
        }
      }
    }
    return false;
  }

  private List<FaceRecognizer> getAlgorithms() {
    if (faceRecognizers == null) {
      final double THRESHHOLD = 1e5;
      // We use an array of algorithms to try out which one works
      // http://docs.opencv.org/trunk/modules/contrib/doc/facerec/facerec_api.html
      faceRecognizers = Lists.newArrayList(
          createLBPHFaceRecognizer(1, 8, 8, 8, THRESHHOLD).get(),
          createFisherFaceRecognizer(0, THRESHHOLD).get(),
          createEigenFaceRecognizer(0, THRESHHOLD).get()
      );
    }
    return faceRecognizers;
  }

  private void undoTraining() {
    faceRecognizers = null; // we undo our training - todo: we should ideally retrain asynchronously
  }

  private void train() {
    for(String name : faceDb.keys()) {
      final List<MugShot> pics = get(name);
      for(FaceRecognizer f : getAlgorithms()) {
//        MatVector images = new MatVector(pics.size());
//        for (int i = 0; i < pics.size(); i++) {
//          images.put(i, IplImage.creaF)
//        }
//
//        f.train();
      }
    }

  }



}
