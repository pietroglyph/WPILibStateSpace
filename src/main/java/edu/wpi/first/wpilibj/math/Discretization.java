/*----------------------------------------------------------------------------*/
/* Copyright (c) 2020 FIRST. All Rights Reserved.                             */
/* Open Source Software - may be modified and shared by FRC teams. The code   */
/* must be accompanied by the FIRST BSD license file in the root directory of */
/* the project.                                                               */
/*----------------------------------------------------------------------------*/

package edu.wpi.first.wpilibj.math;

import edu.wpi.first.conflicted.Exp;
import edu.wpi.first.wpiutil.math.Matrix;
import edu.wpi.first.wpiutil.math.Num;
import edu.wpi.first.wpiutil.math.Pair;
import edu.wpi.first.conflicted.SimpleMatrixUtils;
import org.ejml.dense.row.CommonOps_DDRM;
import org.ejml.simple.SimpleMatrix;

@SuppressWarnings({"PMD.TooManyMethods", "ParameterName"})
public final class Discretization {
  private Discretization() {
    // Utility class
  }

  /**
   * Discretizes the given continuous A matrix.
   *
   * @param <S>       Num representing the number of states.
   * @param contA     Continuous system matrix.
   * @param dtSeconds Discretization timestep.
   * @return the discrete matrix system.
   */
  public static <S extends Num> Matrix<S, S> discretizeA(Matrix<S, S> contA, double dtSeconds) {
    return Exp.exp(contA.times(dtSeconds));
  }

  /**
   * Discretizes the given continuous A and B matrices.
   *
   * <p>Rather than solving a (States + Inputs) x (States + Inputs) matrix
   * exponential like in DiscretizeAB(), we take advantage of the structure of the
   * block matrix of A and B.
   *
   * <p>1) The exponential of A*t, which is only N x N, is relatively cheap.
   * 2) The upper-right quarter of the (States + Inputs) x (States + Inputs)
   * matrix, which we can approximate using a taylor series to several terms
   * and still be substantially cheaper than taking the big exponential.
   *
   * @param contA     Continuous system matrix.
   * @param contB     Continuous input matrix.
   * @param dtseconds Discretization timestep.
   */
  public static <S extends Num, I extends Num> Pair<Matrix<S, S>, Matrix<S, I>>
      discretizeABTaylor(Matrix<S, S> contA,
                         Matrix<S, I> contB,
                         double dtseconds) {
    var lastTerm = new Matrix<S, S>(SimpleMatrix.identity(contA.getNumRows()));
    double lastCoeff = dtseconds;

    var phi12 = lastTerm.times(lastCoeff);

    // i = 6 i.e. 5th order should be enough precision
    for (int i = 2; i < 6; ++i) {
      lastTerm = contA.times(lastTerm);
      lastCoeff *= dtseconds / ((double) i);

      phi12 = phi12.plus(lastTerm.times(lastCoeff));
    }

    var discB = phi12.times(contB);

    var discA = discretizeA(contA, dtseconds);

    return Pair.of(discA, discB);
  }

  /**
   * Discretizes the given continuous A and Q matrices.
   *
   * <p>Rather than solving a 2N x 2N matrix exponential like in DiscretizeQ() (which
   * is expensive), we take advantage of the structure of the block matrix of A
   * and Q.
   *
   * <p>The exponential of A*t, which is only N x N, is relatively cheap.
   * 2) The upper-right quarter of the 2N x 2N matrix, which we can approximate
   * using a taylor series to several terms and still be substantially cheaper
   * than taking the big exponential.
   *
   * @param <S>       Nat representing the number of states.
   * @param contA     Continuous system matrix.
   * @param contQ     Continuous process noise covariance matrix.
   * @param dtSeconds Discretization timestep.
   * @return a pair representing the discrete system matrix and process noise covariance matrix.
   */
  @SuppressWarnings("LocalVariableName")
  public static <S extends Num> Pair<Matrix<S, S>,
          Matrix<S, S>> discretizeAQTaylor(Matrix<S, S> contA, Matrix<S, S> contQ,
                                           double dtSeconds) {
    Matrix<S, S> Q = (contQ.plus(contQ.transpose())).div(2.0);


    Matrix<S, S> lastTerm = Q.copy();
    double lastCoeff = dtSeconds;

    // A^T^n
    Matrix<S, S> Atn = contA.transpose();
    Matrix<S, S> phi12 = lastTerm.times(lastCoeff);

    // i = 6 i.e. 6th order should be enough precision
    for (int i = 2; i < 6; ++i) {
      lastTerm = contA.times(-1).times(lastTerm).plus(Q.times(Atn));
      lastCoeff *= dtSeconds / ((double) i);

      phi12 = phi12.plus(lastTerm.times(lastCoeff));

      Atn = Atn.times(contA.transpose());
    }

    var discA = discretizeA(contA, dtSeconds);
    Q = discA.times(phi12);

    // Make Q symmetric if it isn't already
    var discQ = Q.plus(Q.transpose()).div(2.0);

    return new Pair<>(discA, discQ);
  }

  /**
   * Returns a discretized version of the provided continuous measurement noise
   * covariance matrix. Note that dt=0.0 divides R by zero.
   *
   * @param <O>       Nat representing the number of outputs.
   * @param R         Continuous measurement noise covariance matrix.
   * @param dtSeconds Discretization timestep.
   * @return Discretized version of the provided continuous measurement noise covariance matrix.
   */
  public static <O extends Num> Matrix<O, O> discretizeR(Matrix<O, O> R, double dtSeconds) {
    return R.div(dtSeconds);
  }

  /**
   * Discretizes the given continuous A and B matrices.
   *
   * @param <S>       Nat representing the states of the system.
   * @param <I>       Nat representing the inputs to the system.
   * @param contA     Continuous system matrix.
   * @param contB     Continuous input matrix.
   * @param dtSeconds Discretization timestep.
   * @return a Pair representing discA and diskB.
   */
  @SuppressWarnings("LocalVariableName")
  public static <S extends Num, I extends Num> Pair<Matrix<S, S>,
          Matrix<S, I>> discretizeAB(Matrix<S, S> contA,
                                     Matrix<S, I> contB,
                                     double dtSeconds) {
    var scaledA = contA.times(dtSeconds);
    var scaledB = contB.times(dtSeconds);

    var contSize = contB.getNumRows() + contB.getNumCols();
    SimpleMatrix Mcont = new SimpleMatrix(contSize, contSize);
    Mcont.insertIntoThis(0, 0, scaledA.getStorage());
    Mcont.insertIntoThis(0, scaledA.getNumCols(), scaledB.getStorage());
    var Mdisc = SimpleMatrixUtils.exp(Mcont);

    var discA = new Matrix<S, S>(new SimpleMatrix(contB.getNumRows(), contB.getNumRows()));
    var discB = new Matrix<S, I>(new SimpleMatrix(contB.getNumRows(), contB.getNumCols()));
    CommonOps_DDRM.extract(Mdisc.getDDRM(), 0, 0, discA.getStorage().getDDRM());
    CommonOps_DDRM.extract(Mdisc.getDDRM(), 0, contB.getNumRows(), discB.getStorage().getDDRM());

    return new Pair<>(discA, discB);
  }
}
