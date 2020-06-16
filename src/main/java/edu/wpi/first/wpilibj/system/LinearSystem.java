/*----------------------------------------------------------------------------*/
/* Copyright (c) 2020 FIRST. All Rights Reserved.                             */
/* Open Source Software - may be modified and shared by FRC teams. The code   */
/* must be accompanied by the FIRST BSD license file in the root directory of */
/* the project.                                                               */
/*----------------------------------------------------------------------------*/

package edu.wpi.first.wpilibj.system;

import edu.wpi.first.wpiutil.math.Matrix;
import edu.wpi.first.conflicted.VecBuilder;
import edu.wpi.first.wpilibj.math.Discretization;
import edu.wpi.first.wpilibj.math.StateSpaceUtil;
import edu.wpi.first.wpilibj.system.plant.DCMotor;
import edu.wpi.first.wpiutil.math.*;
import edu.wpi.first.wpiutil.math.numbers.N1;
import edu.wpi.first.wpiutil.math.numbers.N2;

import java.util.function.Function;

@SuppressWarnings("PMD.TooManyMethods")
public class LinearSystem<S extends Num, I extends Num,
        O extends Num> {

  /**
   * Continuous system matrix.
   */
  @SuppressWarnings("MemberName")
  private final Matrix<S, S> m_A;

  /**
   * Continuous input matrix.
   */
  @SuppressWarnings("MemberName")
  private final Matrix<S, I> m_B;

  /**
   * Output matrix.
   */
  @SuppressWarnings("MemberName")
  private final Matrix<O, S> m_C;

  /**
   * Feedthrough matrix.
   */
  @SuppressWarnings("MemberName")
  private final Matrix<O, I> m_D;

  private Function<Matrix<I, N1>, Matrix<I, N1>> m_clampFunction;

  /**
   * The states of the system represented as a Nat.
   */
  private final Nat<S> m_states;

  /**
   * The inputs of the system represented as a Nat.
   */
  private final Nat<I> m_inputs;

  /**
   * The outputs of the system represented as a Nat.
   */
  private final Nat<O> m_outputs;

  /**
   * State vector.
   */
  @SuppressWarnings("MemberName")
  private Matrix<S, N1> m_x;

  /**
   * Output vector.
   */
  @SuppressWarnings("MemberName")
  private Matrix<O, N1> m_y;

  /**
   * Delayed u since predict and correct steps are run in reverse.
   */
  private Matrix<I, N1> m_delayedU;

  /**
   * Construct a new LinearSystem from the four system matrices.
   *
   * @param states  A Nat representing the states of the system.
   * @param inputs  A Nat representing the inputs to the system.
   * @param outputs A Nat representing the outputs of the system.
   * @param a             The system matrix A.
   * @param b             The input matrix B.
   * @param c             The output matrix C.
   * @param d             The feedthrough matrix D.
   * @param clampFunction The function used to clamp the input U.
   */
  @SuppressWarnings("ParameterName")
  public LinearSystem(Nat<S> states, Nat<I> inputs, Nat<O> outputs,
                      Matrix<S, S> a, Matrix<S, I> b,
                      Matrix<O, S> c, Matrix<O, I> d,
                      Function<Matrix<I, N1>, Matrix<I, N1>> clampFunction) {

    this.m_states = states;
    this.m_inputs = inputs;
    this.m_outputs = outputs;

    this.m_A = a;
    this.m_B = b;
    this.m_C = c;
    this.m_D = d;

    this.m_clampFunction = clampFunction;

    this.m_x = MatrixUtils.zeros(states);
    this.m_y = MatrixUtils.zeros(outputs);
    this.m_delayedU = MatrixUtils.zeros(inputs);

    reset();
  }

  /**
   * Resets the plant.
   */
  public void reset() {
    m_x = MatrixUtils.zeros(m_states);
    m_y = MatrixUtils.zeros(m_outputs);
    m_delayedU = MatrixUtils.zeros(m_inputs);
  }

  /**
   * Returns the system matrix A.
   *
   * @return the system matrix A.
   */
  public Matrix<S, S> getA() {
    return m_A;
  }

  /**
   * Returns an element of the system matrix A.
   *
   * @param row Row of A.
   * @param col Column of A.
   * @return the system matrix A at (i, j).
   */
  public double getA(int row, int col) {
    return m_A.get(row, col);
  }

  /**
   * Returns the input matrix B.
   *
   * @return the input matrix B.
   */
  public Matrix<S, I> getB() {
    return m_B;
  }

  /**
   * Returns an element of the input matrix B.
   *
   * @param row Row of B.
   * @param col Column of B.
   * @return The value of the input matrix B at (i, j).
   */
  public double getB(int row, int col) {
    return m_B.get(row, col);
  }

  /**
   * Returns the output matrix C.
   *
   * @return Output matrix C.
   */
  public Matrix<O, S> getC() {
    return m_C;
  }

  /**
   * Returns an element of the output matrix C.
   *
   * @param row Row of C.
   * @param col Column of C.
   * @return the double value of C at the given position.
   */
  public double getC(int row, int col) {
    return m_C.get(row, col);
  }

  /**
   * Returns the feedthrough matrix D.
   *
   * @return the feedthrough matrix D.
   */
  public Matrix<O, I> getD() {
    return m_D;
  }

  /**
   * Returns an element of the feedthrough matrix D.
   *
   * @param row Row of D.
   * @param col Column of D.
   * @return The feedthrough matrix D at (i, j).
   */
  public double getD(int row, int col) {
    return m_D.get(row, col);
  }

  /**
   * Get the function used to clamp the input u.
   * @return The clamping function.
   */
  public Function<Matrix<I, N1>, Matrix<I, N1>> getClampFunction() {
    return m_clampFunction;
  }

  /**
   * Set the clamping function used to clamp inputs.
   */
  public void setClampFunction(Function<Matrix<I, N1>, Matrix<I, N1>> clampFunction) {
    this.m_clampFunction = clampFunction;
  }

  /**
   * Returns the current state x.
   *
   * @return The current state x.
   */
  public Matrix<S, N1> getX() {
    return m_x;
  }

  /**
   * Returns an element of the current state x.
   *
   * @param row Row of x.
   * @return The i-th element of the current state x.
   */
  public double getX(int row) {
    return getX().get(row, 0);
  }

  /**
   * Set the initial state x.
   *
   * @param x The initial state.
   */
  @SuppressWarnings("ParameterName")
  public void setX(Matrix<S, N1> x) {
    m_x = x;
  }

  /**
   * Set an element of the initial state x.
   *
   * @param row   Row of x.
   * @param value Value of element of x.
   */
  public void setX(int row, double value) {
    m_x.set(row, 0, value);
  }

  /**
   * Returns the current measurement vector y.
   *
   * @return the current measurement vector y.
   */
  public Matrix<O, N1> getY() {
    return m_y;
  }

  /**
   * Returns an element of the current measurement vector y.
   *
   * @param row Row of y.
   * @return the output matrix Y at the given row i.
   */
  public double getY(int row) {
    return getY().get(row, 0);
  }

  /**
   * Set the current measurement y.
   *
   * @param y The current measurement.
   */
  @SuppressWarnings("ParameterName")
  public void setY(Matrix<O, N1> y) {
    m_y = y;
  }

  /**
   * Set an element of the current measurement y.
   *
   * @param row   Row of y.
   * @param value Value of element of y.
   */
  public void setY(int row, double value) {
    m_y.set(row, 0, value);
  }

  /**
   * Returns the control input vector u.
   *
   * @return the control input vector u.
   */
  public Matrix<I, N1> getU() {
    return clampInput(m_delayedU);
  }

  /**
   * Returns an element of the control input vector u.
   *
   * @param row Row of u.
   * @return The i-th element of control input vector u.
   */
  public double getU(int row) {
    return getU().get(row, 0);
  }

  /**
   * Computes the new x and y given the control input.
   *
   * @param x         The current state.
   * @param u         The control input.
   * @param dtSeconds Timestep for model update.
   */
  @SuppressWarnings("ParameterName")
  public void update(Matrix<S, N1> x, Matrix<I, N1> u, double dtSeconds) {
    m_x = calculateX(x, m_delayedU, dtSeconds);
    m_y = calculateY(m_x, m_delayedU);
    m_delayedU = u;
  }

  /**
   * Computes the new x given the old x and the control input.
   *
   * <p>This is used by state observers directly to run updates based on state
   * estimate.
   *
   * @param x         The current state.
   * @param u         The control input.
   * @param dtSeconds Timestep for model update.
   * @return the updated x.
   */
  @SuppressWarnings("ParameterName")
  public Matrix<S, N1> calculateX(Matrix<S, N1> x, Matrix<I, N1> u, double dtSeconds) {
    var discABpair = Discretization.discretizeAB(m_A, m_B, dtSeconds);

    return (discABpair.getFirst().times(x)).plus(discABpair.getSecond().times(clampInput(u)));
  }

  /**
   * Computes the new y given the control input.
   *
   * <p>This is used by state observers directly to run updates based on state
   * estimate.
   *
   * @param x The current state.
   * @param u The control input.
   * @return the updated output matrix Y.
   */
  @SuppressWarnings("ParameterName")
  public Matrix<O, N1> calculateY(
          Matrix<S, N1> x,
          Matrix<I, N1> u) {
    return m_C.times(x).plus(m_D.times(clampInput(u)));
  }

  /**
   * Clamp the input u to the min and max.
   *
   * @param u The input to clamp.
   * @return The clamped input.
   */
  @SuppressWarnings({"ParameterName", "LocalVariableName"})
  public Matrix<I, N1> clampInput(Matrix<I, N1> u) {
    return m_clampFunction.apply(u);
  }

  @Override
  public String toString() {
    return String.format("Linear System: A\n%s\n\nB:\n%s\n\nC:\n%s\n\nD:\n%s\n", m_A.getStorage(),
            m_B.getStorage(), m_C.getStorage(), m_D.getStorage());
  }


  /**
   * Create a state-space model of an elevator system.
   *
   * @param motor        The motor (or gearbox) attached to the arm.
   * @param massKg       The mass of the elevator carriage, in kilograms.
   * @param radiusMeters The radius of thd driving drum of the elevator, in meters.
   * @param G            The reduction between motor and drum, as a ratio of output to input.
   * @param maxVoltage   The max voltage that can be applied. Inputs greater than this will
   *                     be clamped to it.
   * @return A LinearSystem representing the given characterized constants.
   */
  @SuppressWarnings("ParameterName")
  public static LinearSystem<N2, N1, N1> createElevatorSystem(DCMotor motor, double massKg,
                                                              double radiusMeters, double G,
                                                              double maxVoltage) {
    return new LinearSystem<>(Nat.N2(), Nat.N1(), Nat.N1(),
            new MatBuilder<>(Nat.N2(), Nat.N2()).fill(0, 1,
                    0, -Math.pow(G, 2) * motor.m_KtNMPerAmp
                            / (motor.m_rOhms * radiusMeters * radiusMeters * massKg
                            * motor.m_KvRadPerSecPerVolt)),
            new MatBuilder<>(Nat.N2(), Nat.N1()).fill(
                    0, G * motor.m_KtNMPerAmp / (motor.m_rOhms * radiusMeters * massKg)),
            new MatBuilder<>(Nat.N1(), Nat.N2()).fill(1, 0),
            MatrixUtils.zeros(Nat.N1()),
        u -> StateSpaceUtil.normalizeInputVector(u, maxVoltage));
  }

  /**
   * Create a state-space model of a flywheel system.
   *
   * @param motor            The motor (or gearbox) attached to the arm.
   * @param jKgMetersSquared The momoent of inertia J of the flywheel.
   * @param G                The reduction between motor and drum, as a ratio of output to input.
   * @param maxVoltage       The max voltage that can be applied. Inputs greater than this will
   *                         be clamped to it.
   * @return A LinearSystem representing the given characterized constants.
   */
  @SuppressWarnings("ParameterName")
  public static LinearSystem<N1, N1, N1> createFlywheelSystem(DCMotor motor,
                                                              double jKgMetersSquared,
                                                              double G, double maxVoltage) {
    return new LinearSystem<>(Nat.N1(), Nat.N1(), Nat.N1(),
            VecBuilder.fill(
                    -G * G * motor.m_KtNMPerAmp
                            / (motor.m_KvRadPerSecPerVolt * motor.m_rOhms * jKgMetersSquared)),
            VecBuilder.fill(G * motor.m_KtNMPerAmp
                    / (motor.m_rOhms * jKgMetersSquared)),
            MatrixUtils.eye(Nat.N1()),
            MatrixUtils.zeros(Nat.N1()),
        u -> StateSpaceUtil.normalizeInputVector(u, maxVoltage));
  }

  /**
   * Create a state-space model of a differential drive drivetrain.
   *
   * @param motor            the gearbox representing the motors driving the drivetrain.
   * @param massKg           the mass of the robot.
   * @param rMeters          the radius of the wheels in meters.
   * @param rbMeters         the radius of the base (half the track width) in meters.
   * @param JKgMetersSquared the moment of inertia of the robot.
   * @param G                the gearing reduction as output over input.
   * @param maxVoltageVolts  the maximum voltage. Usually 12v. Must be positive.
   * @return A LinearSystem representing a differential drivetrain.
   */
  @SuppressWarnings({"LocalVariableName", "ParameterName"})
  public static LinearSystem<N2, N2, N2> createDrivetrainVelocitySystem(DCMotor motor,
                                                                        double massKg,
                                                                        double rMeters,
                                                                        double rbMeters,
                                                                        double JKgMetersSquared,
                                                                        double G,
                                                                        double maxVoltageVolts) {
    var C1 =
            -(G * G) * motor.m_KtNMPerAmp
                    / (motor.m_KvRadPerSecPerVolt * motor.m_rOhms * rMeters * rMeters);
    var C2 = G * motor.m_KtNMPerAmp / (motor.m_rOhms * rMeters);

    final double C3 = 1 / massKg + rbMeters * rbMeters / JKgMetersSquared;
    final double C4 = 1 / massKg - rbMeters * rbMeters / JKgMetersSquared;
    var A = new MatBuilder<>(Nat.N2(), Nat.N2()).fill(
            C3 * C1,
            C4 * C1,
            C4 * C1,
            C3 * C1);
    var B = new MatBuilder<>(Nat.N2(), Nat.N2()).fill(
            C3 * C2,
            C4 * C2,
            C4 * C2,
            C3 * C2);
    var C = new MatBuilder<>(Nat.N2(), Nat.N2()).fill(1.0, 0.0, 0.0, 1.0);
    var D = new MatBuilder<>(Nat.N2(), Nat.N2()).fill(0.0, 0.0, 0.0, 0.0);

    return new LinearSystem<>(Nat.N2(), Nat.N2(), Nat.N2(), A, B, C, D,
        u -> StateSpaceUtil.normalizeInputVector(u, maxVoltageVolts));
  }

  /**
   * Create a state-space model of a single jointed arm system.
   *
   * @param motor            The motor (or gearbox) attached to the arm.
   * @param jKgSquaredMeters The momoent of inertia J of the arm.
   * @param G                the gearing between the motor and arm, in output over input.
   *                         Most of the time this will be greater than 1.
   * @param maxVoltage       The max voltage that can be applied. Inputs greater than this will
   *                         be clamped to it.
   * @return A LinearSystem representing the given characterized constants.
   */
  @SuppressWarnings("ParameterName")
  public static LinearSystem<N2, N1, N1> createSingleJointedArmSystem(DCMotor motor,
                                                                      double jKgSquaredMeters,
                                                                      double G,
                                                                      double maxVoltage) {
    return new LinearSystem<>(Nat.N2(), Nat.N1(), Nat.N1(),
            new MatBuilder<>(Nat.N2(), Nat.N2()).fill(0, 1,
                    0, -Math.pow(G, 2) * motor.m_KtNMPerAmp
                            / (motor.m_KvRadPerSecPerVolt * motor.m_rOhms * jKgSquaredMeters)),
            new MatBuilder<>(Nat.N2(), Nat.N1()).fill(0, G * motor.m_KtNMPerAmp
                    / (motor.m_rOhms * jKgSquaredMeters)),
            new MatBuilder<>(Nat.N1(), Nat.N2()).fill(1, 0),
            MatrixUtils.zeros(Nat.N1()),
        u -> StateSpaceUtil.normalizeInputVector(u, maxVoltage));
  }

  /**
   * Identify a velocity system from it's kV (volts/(unit/sec)) and kA (volts/(unit/sec^2).
   * These constants cam be found using frc-characterization.
   *
   * @param kV         The velocity gain, in volts per (units per second)
   * @param kA         The acceleration gain, in volts per (units per second squared)
   * @param maxVoltage The max voltage that can be applied. Inputs greater than this will
   *                   be clamped to it.
   * @return A LinearSystem representing the given characterized constants.
   * @see <a href="https://github.com/wpilibsuite/frc-characterization">
   * https://github.com/wpilibsuite/frc-characterization</a>
   */
  @SuppressWarnings("ParameterName")
  public static LinearSystem<N1, N1, N1> identifyVelocitySystem(double kV, double kA,
                                                                double maxVoltage) {
    return new LinearSystem<>(Nat.N1(), Nat.N1(), Nat.N1(),
            VecBuilder.fill(-kV / kA),
            VecBuilder.fill(1.0 / kA),
            VecBuilder.fill(1.0),
            VecBuilder.fill(0.0),
        u -> StateSpaceUtil.normalizeInputVector(u, maxVoltage));
  }

  /**
   * Identify a position system from it's kV (volts/(unit/sec)) and kA (volts/(unit/sec^2).
   * These constants cam be found using frc-characterization.
   *
   * @param kV         The velocity gain, in volts per (units per second)
   * @param kA         The acceleration gain, in volts per (units per second squared)
   * @param maxVoltage The max voltage that can be applied. Control inputs above this will be
   *                   clamped to it.
   * @return A LinearSystem representing the given characterized constants.
   * @see <a href="https://github.com/wpilibsuite/frc-characterization">
   * https://github.com/wpilibsuite/frc-characterization</a>
   */
  @SuppressWarnings("ParameterName")
  public static LinearSystem<N2, N1, N1> identifyPositionSystem(double kV, double kA,
                                                                double maxVoltage) {
    return new LinearSystem<>(Nat.N2(), Nat.N1(), Nat.N1(),
            new MatBuilder<>(Nat.N2(), Nat.N2()).fill(0.0, 1.0, 0.0, -kV / kA),
            new MatBuilder<>(Nat.N2(), Nat.N1()).fill(0.0, 1.0 / kA),
            new MatBuilder<>(Nat.N1(), Nat.N2()).fill(1.0, 0.0),
            VecBuilder.fill(0.0),
        u -> StateSpaceUtil.normalizeInputVector(u, maxVoltage));
  }

  /**
   * Identify a standard differential drive drivetrain, given the drivetrain's
   * kV and kA in both linear (volts/(meter/sec) and volts/(meter/sec^2)) and
   * angular (volts/(radian/sec) and volts/(radian/sec^2)) cases. This can be
   * found using frc-characterization.
   *
   * @param kVLinear   The linear velocity gain, volts per (meter per second).
   * @param kALinear   The linear acceleration gain, volts per (meter per second squared).
   * @param kVAngular  The angular velocity gain, volts per (radians per second).
   * @param kAAngular  The angular acceleration gain, volts per (radians per second squared).
   * @param maxVoltage The max voltage that can be applied. Control inputs above this will be
   *                   clamped to it.
   * @return A LinearSystem representing the given characterized constants.
   * @see <a href="https://github.com/wpilibsuite/frc-characterization">
   * https://github.com/wpilibsuite/frc-characterization</a>
   */
  @SuppressWarnings("ParameterName")
  public static LinearSystem<N2, N2, N2> identifyDrivetrainSystem(
          double kVLinear, double kALinear, double kVAngular, double kAAngular,
          double maxVoltage) {

    final double c = 0.5 / (kALinear * kAAngular);
    final double A1 = c * (-kALinear * kVAngular - kVLinear * kAAngular);
    final double A2 = c * (kALinear * kVAngular - kVLinear * kAAngular);
    final double B1 = c * (kALinear + kAAngular);
    final double B2 = c * (kAAngular - kALinear);

    return new LinearSystem<>(Nat.N2(), Nat.N2(), Nat.N2(),
            new MatBuilder<>(Nat.N2(), Nat.N2()).fill(A1, A2, A2, A1),
            new MatBuilder<>(Nat.N2(), Nat.N2()).fill(B1, B2, B2, B1),
            new MatBuilder<>(Nat.N2(), Nat.N2()).fill(1, 0, 0, 1),
            new MatBuilder<>(Nat.N2(), Nat.N2()).fill(0, 0, 0, 0),
        u -> StateSpaceUtil.normalizeInputVector(u, maxVoltage));
  }

}
