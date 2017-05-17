package com.stradigi.stradigibot;

import android.support.test.rule.ActivityTestRule;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * Created by Mario Bruno on 2017-04-14.
 */

public class RobotTest {

  @Rule
  public ActivityTestRule rule = new ActivityTestRule<>(MainActivity.class);

  private static Robot robot;

  @BeforeClass
  public static void setup() {
    robot = new Robot();
  }

  @Test
  public void robotForward() {
    robot.stop();
    robot.forward();

    assertTrue("Robot is moving forwards.", robot.isMovingForward());
  }

  @Test
  public void robotBackward() {
    robot.stop();
    robot.backward();

    assertTrue("Robot is moving backwards.", robot.isMovingBackwards());
  }

  @Test
  public void robotLeft() {
    robot.stop();
    robot.left();

    assertTrue("Robot is turning left.", robot.isTurningLeft());
  }

  @Test
  public void robotRight() {
    robot.stop();
    robot.right();

    assertTrue("Robot is turning right.", robot.isTurningRight());
  }

  @Test
  public void robotStop() {
    robot.stop();

    assertTrue("Robot has stopped.", robot.isStopped());
  }

  @AfterClass
  public static void shutdownTest() {
    robot.shutDown();
  }

}
