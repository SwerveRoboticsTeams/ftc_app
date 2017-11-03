package org.firstinspires.ftc.team6220_2017;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;

import org.firstinspires.ftc.robotcore.external.Const;

/**
 * Created by Mridula on 10/29/2017.
 */
@Autonomous(name = "Auto Red Right", group = "Autonomous")

public class AutoRedRight extends MasterAutonomous
{
    @Override
    public void runOpMode() throws InterruptedException
    {
        boolean isBlueSide = false;
        // must initialize to prevent errors; not necessarily true
        boolean isLeftBlue = true;

        vuforiaHelper.getVumark();

        //if the vuMark is not visible, vuforia will tell us
        if (vuforiaHelper.isVisible())
        {
            isLeftBlue = vuforiaHelper.getLeftJewelColor();
            telemetry.addData("leftColor ", vuforiaHelper.avgLeftJewelColor);
            telemetry.addData("RightColor ", vuforiaHelper.avgRightJewelColor);
        }
        else
        {
            moveRobot(270, 1, 1000);
            telemetry.addData("vuMark: ", "not visible");
        }

        telemetry.update();

        initializeAuto();

        waitForStart();

        // auto code-----------------------------
        knockJewel(isLeftBlue, isBlueSide);
        //---------------------------------------
    }

    //todo modify for jewels rather than beacons
    //we use this function to determine the color of jewels and knock them
    public void knockJewel (boolean isLeftBlue, boolean isBlueSide) throws InterruptedException
    {
        jewelJostlerServo.setPosition(Constants.JEWEL_JOSTLER_DEPLOYED);

        if(isBlueSide)
        {
            if(isLeftBlue)
            {
                turnTo(-90);
                moveRobot(0, 1, 1000);
            }
            else
            {
                turnTo(90);
                moveRobot(180, 1, 1000);
            }
        }
        else
        {
            if(isLeftBlue)
            {
                turnTo(-90);
                moveRobot(0, 1, 1000);
            }
            else
            {
                turnTo(90);
                moveRobot(180, 1, 1000);
            }
        }
    }
}
