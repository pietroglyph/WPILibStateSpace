package edu.wpi.first.conflicted;

import edu.wpi.first.wpiutil.math.Matrix;
import edu.wpi.first.wpiutil.math.Num;
import org.ejml.MatrixDimensionException;
import org.ejml.simple.SimpleMatrix;

public class Exp {
  /**
   * Computes the matrix exponential using Eigen's solver.
   * This method only works for square matrices, and will
   * otherwise throw an {@link MatrixDimensionException}.
   *
   * @return the exponential of A.
   */
  public static final <R extends Num, C extends Num> Matrix<R, C> exp(Matrix<R, C> mat) {
    if (mat.getNumRows() != mat.getNumCols()) {
      throw new MatrixDimensionException("Non-square matrices cannot be exponentiated! "
              + "This matrix is " + mat.getNumRows() + " x " + mat.getNumCols());
    }
    Matrix<R, C> toReturn = new Matrix<>(new SimpleMatrix(mat.getNumRows(), mat.getNumCols()));
    WPIUtilJNI.exp(mat.getStorage().getDDRM().getData(), mat.getNumRows(),
            toReturn.getStorage().getDDRM().getData());
    return toReturn;
  }

}
