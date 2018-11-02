package org.firstinspires.ftc.team417_2018;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import org.corningrobotics.enderbots.endercv.ActivityViewDisplay;

import java.util.Locale;

@TeleOp(name="OpenCV Test")
public class OpenCVTest extends LinearOpMode
{
    private OpenCVDetect goldVision;
    boolean isLeftGold = false;
    boolean isCenterGold = false;
    boolean isRightGold = false;
    float goldX;
    float goldY;

    @Override
    public void runOpMode()
    {
        goldVision = new OpenCVDetect();
        // can replace with ActivityViewDisplay.getInstance() for fullscreen
        goldVision.init(hardwareMap.appContext, ActivityViewDisplay.getInstance());
        goldVision.setShowCountours(false);
        // start the vision system
        goldVision.enable();

        waitForStart();
        goldVision.setShowCountours(true);

        goldX = (goldVision.getGoldRect().x + goldVision.getGoldRect().width) / 2;
        goldY = (goldVision.getGoldRect().y + goldVision.getGoldRect().height) / 2;


        while(opModeIsActive())
        {
            telemetry.addData("Gold",
                    String.format(Locale.getDefault(), "(%d, %d)", ((goldVision.getGoldRect().x + goldVision.getGoldRect().width) / 2), (goldVision.getGoldRect().y + goldVision.getGoldRect().height) / 2));

            if( ((goldVision.getGoldRect().y + goldVision.getGoldRect().height / 2) <= 120) && ((goldVision.getGoldRect().x + goldVision.getGoldRect().width / 2) >= 140) )
            {
                //goldLocation = sampleFieldLocations.right;
                isLeftGold = false;
                isCenterGold = false;
                isRightGold = true;
                telemetry.addLine("Right");
            }
            else if( ((goldVision.getGoldRect().y + goldVision.getGoldRect().height / 2) >= 140) && ((goldVision.getGoldRect().x + goldVision.getGoldRect().width / 2) >= 140))
            {
                //goldLocation = sampleFieldLocations.right;
                isLeftGold = false;
                isCenterGold = true;
                isRightGold = false;
                telemetry.addLine("Center");
            }
            // gold is towards middle of phone screen in horizontal position (rotated counter clockwise 90 degrees looking at it from the front)
            else if ( ((goldVision.getGoldRect().y + goldVision.getGoldRect().height / 2)==0) && ((goldVision.getGoldRect().x + goldVision.getGoldRect().width / 2) >= 140))
            {
                //goldLocation = sampleFieldLocations.center;
                isCenterGold = false;
                isRightGold = false;
                isLeftGold = true;
                telemetry.addLine("Left");
            }

            int midpoint = goldVision.getGoldRect().y + goldVision.getGoldRect().height / 2;


            telemetry.update();
        }

        goldVision.disable();
    }
}
