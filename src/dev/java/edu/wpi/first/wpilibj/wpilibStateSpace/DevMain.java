/*----------------------------------------------------------------------------*/
/* Copyright (c) 2017-2019 FIRST. All Rights Reserved.                        */
/* Open Source Software - may be modified and shared by FRC teams. The code   */
/* must be accompanied by the FIRST BSD license file in the root directory of */
/* the project.                                                               */
/*----------------------------------------------------------------------------*/

package edu.wpi.first.wpilibj.wpilibStateSpace;

import edu.wpi.first.hal.HALUtil;
import edu.wpi.first.networktables.NetworkTablesJNI;
import edu.wpi.first.wpilibj.math.DrakeJNI;
import edu.wpi.first.wpiutil.RuntimeDetector;

public final class DevMain {
  /**
   * Main entry point.
   */
  public static void main(String[] args) {
    System.out.println("Hello World!");
    try {
      DrakeJNI.forceLoad();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private DevMain() {
  }
}
