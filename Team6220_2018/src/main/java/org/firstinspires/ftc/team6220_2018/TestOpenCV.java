package org.firstinspires.ftc.team6220_2018;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;

import org.corningrobotics.enderbots.endercv.ActivityViewDisplay;

import java.util.Locale;

/**
*  This is a test class for scoring the sampling field using OpenCV.
*/

@Autonomous(name="TestOpenCV")
//@Disabled

public class TestOpenCV extends MasterAutonomous
{
    @Override
    public void runOpMode() throws InterruptedException
    {
        OpenCVVision = new OpenCVGold();

        OpenCVVision.init(hardwareMap.appContext, ActivityViewDisplay.getInstance());
        OpenCVVision.enable();
        OpenCVVision.setShowCountours(true);

        initializeRobot();

        waitForStart();
        // Accounts for delay between initializing the program and starting TeleOp
        lTime = timer.seconds();

        while(opModeIsActive())
        {
            // Finds the time elapsed each loop
            double eTime = timer.seconds() - lTime;
            lTime = timer.seconds();

            // Gives driver ability to adjust yellow color threshold that OpenCV uses.
            if (driver1.isButtonJustPressed(Button.DPAD_UP))
                OpenCVVision.thresholdVal++;
            else if (driver1.isButtonJustPressed(Button.DPAD_DOWN))
                OpenCVVision.thresholdVal--;

            // Gold is towards left of phone screen in horizontal  (rotated counter clockwise 90 degrees looking at it from the front).
            if (((OpenCVVision.getGoldRect().y + (OpenCVVision.getGoldRect().height / 2)) < Constants.GOLD_DIVIDING_LINE) && (OpenCVVision.getGoldRect().y > Constants.OPENCV_TOLERANCE_PIX))
            {
                goldLocation = sampleFieldLocations.center;
                telemetry.addLine("Center");
            }
            // Gold is towards right of phone screen in horizontal position (rotated counter clockwise 90 degrees looking at it from the front).
            else if (((OpenCVVision.getGoldRect().y + (OpenCVVision.getGoldRect().height / 2))) > Constants.GOLD_DIVIDING_LINE)
            {
                goldLocation = sampleFieldLocations.right;
                telemetry.addLine("Right");
            }
            // Gold is towards middle of phone screen in horizontal position (rotated counter clockwise 90 degrees looking at it from the front).
            else
            {
                goldLocation = sampleFieldLocations.left;
                telemetry.addLine("Left (default)");
            }
            telemetry.addData("Gold",
                    String.format(Locale.getDefault(), "(%d, %d)", (OpenCVVision.getGoldRect().x + (OpenCVVision.getGoldRect().width) / 2), (OpenCVVision.getGoldRect().y + (OpenCVVision.getGoldRect().height / 2))));
            telemetry.addData("ThresholdVal: ", OpenCVVision.thresholdVal);
            telemetry.update();
            updateCallback(eTime);
            idle();
        }


        // stop the vision system
        OpenCVVision.disable();
    }
}
