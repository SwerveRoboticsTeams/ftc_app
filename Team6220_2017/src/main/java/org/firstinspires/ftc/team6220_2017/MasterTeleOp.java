package org.firstinspires.ftc.team6220_2017;

import com.qualcomm.robotcore.util.Range;

/*
    Contains methods for accepting and interpreting pilot and co-pilot input
*/
abstract public class MasterTeleOp extends MasterOpMode
{
    boolean slowMode = false;

    // Factor that adjusts magnitudes of vertical and horizontal movement
    double tFactor = Constants.T_FACTOR;
    // Factor that adjusts magnitude of rotational movement
    double rFactor = Constants.R_FACTOR;

    // Takes driver 1 stick input and uses it to give power and direction inputs to the drive
    void driveMecanumWithJoysticks()
    {
        /*
         Note: factors are different for translation and rotation
         Slow mode functionality.  ist driver presses right bumper to toggle slow mode
        */
        if (driver1.isButtonJustPressed(Button.RIGHT_BUMPER) && !slowMode)
        {
            tFactor = Constants.SLOW_MODE_T_FACTOR;
            rFactor = Constants.SLOW_MODE_R_FACTOR;
            slowMode = true;
        }
        else if (driver1.isButtonJustPressed(Button.RIGHT_BUMPER) && slowMode)
        {
            tFactor = Constants.T_FACTOR;
            rFactor = Constants.R_FACTOR;
            slowMode = false;
        }

        // Allows the driver to raise the vertical jewel servo if it fails during the match
        if(driver2.isButtonPressed(Button.LEFT_BUMPER))
        {
            verticalJewelServoToggler.retract();
        }

        double adjustedDriveMagnitude = driver1.getRightStickMagnitude();
        /*
         Scale right stick magnitude, since magnitudes are not uniform for all stick positions
         (they have been observed to be as large as 1.1)
        */
        if (adjustedDriveMagnitude > 1.0)
            adjustedDriveMagnitude = 1.0;

        // Stick inputs must be changed from x and y to angle and drive power
        double angle = driver1.getRightStickAngle();
        double power = tFactor * stickCurve.getOuput(adjustedDriveMagnitude);
        double rotationPower = rFactor * stickCurve.getOuput(gamepad1.left_stick_x);

        driveMecanum(angle, power, rotationPower);
    }


}
