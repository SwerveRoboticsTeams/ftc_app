package org.firstinspires.ftc.team6220_2017;

import com.qualcomm.robotcore.util.Range;

/**
 *  Encapsulates driver input for operating relic arm
 */

public class ArmMechanism
{
    MasterOpMode op;

    // The triggers adjust this value to change the wrist servo's position
    double wristServoCount = Constants.WRIST_SERVO_INIT;

    // We pass in MasterOpMode so that this class can access important functionalities such as telemetry
    public ArmMechanism (MasterOpMode mode)
    {
        this.op = mode;
    }

    // Method for running entire arm system on robot
    public void driveArm()
    {
        // If the arm is not attached, don't try to drive it
        if (!op.isArmAttached)
        {
            op.telemetry.addLine("Arm is not attached!");
            op.telemetry.update();
            return;
        }


        // Run arm
        if (op.driver2.getLeftStickMagnitude() > Constants.MINIMUM_JOYSTICK_POWER)
        {
            // Adjust power inputs for the arm motor
            double armPower = Constants.ARM_POWER_CONSTANT * op.stickCurve.getOuput(op.gamepad2.left_stick_y);

            op.motorArm.setPower(armPower);

            // Set encoder limits for the arm's range of motion.  If it is retracted or extended
            // as far as it can be, we do not want to attempt to move beyond those points.
            /*if (op.motorArm.getCurrentPosition() >= 3000 && Math.signum(op.gamepad2.left_stick_y) < 0)  // todo Adjust encoder limit
                op.motorArm.setPower(0);
            else if (op.motorArm.getCurrentPosition() <= 10 && Math.signum(op.gamepad2.left_stick_y) > 0)   // todo Adjust encoder limit
                op.motorArm.setPower(0);
            else
                op.motorArm.setPower(armPower);*/
        }
        else
        {
            op.motorArm.setPower(0);
        }


        // Run wrist
        if (op.driver2.getLeftTriggerValue() >= Constants.MINIMUM_TRIGGER_VALUE)
        {
            wristServoCount -= Constants.WRIST_SERVO_INCREMENT;
            op.wristServo.setPosition(wristServoCount);
        }
        else if (op.driver2.getRightTriggerValue() >= Constants.MINIMUM_TRIGGER_VALUE)
        {
            wristServoCount += Constants.WRIST_SERVO_INCREMENT;
            op.wristServo.setPosition(wristServoCount);
        }


        // Run grabber
        if (op.driver2.isButtonJustPressed(Button.DPAD_UP))
            op.grabberServoToggler.toggle();


        //op.telemetry.addData("wristServoCount: ", wristServoCount);
        //op.telemetry.addData("armPower: ", armPower);
        op.telemetry.addData("encoder value: ", op.motorArm.getCurrentPosition());
        op.telemetry.update();
    }
}
