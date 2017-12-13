package org.firstinspires.ftc.team6220_2017;

import com.qualcomm.robotcore.hardware.DcMotor;

/**
 * Encapsulates functionalities for the glyph-scoring mechanism on our robot.
 */

public class GlyphMechanism
{
    MasterOpMode op;

    int[] glyphHeights;

    private boolean wasStickPressed = false;

    // We pass in MasterOpMode so that this class can access important functionalities such as telemetry
    public GlyphMechanism (MasterOpMode mode, int[] GlyphHeights)
    {
        this.op = mode;
        this.glyphHeights = GlyphHeights;
    }

    // Takes input used to move all parts of the glyph mechanism
    public void driveGlyphMech()
    {
        // Note:  REMEMBER to restart robot if program is stopped, then adjust position manually
        // Glyphter controls---------------------------------------------------
        if (Math.abs(op.gamepad2.right_stick_y) >= Constants.MINIMUM_JOYSTICK_POWER)
        {
            wasStickPressed = true;
            op.motorGlyphter.setMode(DcMotor.RunMode.RUN_USING_ENCODER);

            // Drive glyphter manually
            op.motorGlyphter.setPower(-op.gamepad2.right_stick_y);
            op.telemetry.addData("rightStickY: ", -op.gamepad2.right_stick_y);
            op.telemetry.update();
        }
        else if (Math.abs(op.gamepad2.right_stick_y) < Constants.MINIMUM_JOYSTICK_POWER && wasStickPressed)
        {
            wasStickPressed = false;
            op.motorGlyphter.setPower(0);
            op.motorGlyphter.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        }
        else if (Math.abs(op.gamepad2.right_stick_y) < Constants.MINIMUM_JOYSTICK_POWER && !wasStickPressed)
        {
            if (op.driver2.isButtonJustPressed(Button.A) && !op.driver2.isButtonPressed(Button.START))  // Protect against Start + A running
            {                                                                                           // gylph mechanism into ground
                op.motorGlyphter.setTargetPosition(glyphHeights[0]);
                op.motorGlyphter.setPower(1.0);
            }
            else if (op.driver2.isButtonJustPressed(Button.B) && !op.driver2.isButtonPressed(Button.START))
            {
                op.motorGlyphter.setTargetPosition(glyphHeights[1]);
                op.motorGlyphter.setPower(1.0);
            }
            else if (op.driver2.isButtonJustPressed(Button.Y) && !op.driver2.isButtonPressed(Button.START))
            {
                op.motorGlyphter.setTargetPosition(glyphHeights[2]);
                op.motorGlyphter.setPower(1.0);
            }
            else if (op.driver2.isButtonJustPressed(Button.X) && !op.driver2.isButtonPressed(Button.START))
            {
                op.motorGlyphter.setTargetPosition(glyphHeights[3]);
                op.motorGlyphter.setPower(1.0);
            }
            // Stow glyph mechanism
            else if (op.driver2.isButtonJustPressed(Button.START))
            {
                op.motorGlyphter.setTargetPosition(0);
                op.motorGlyphter.setPower(1.0);
            }

        }

        //----------------------------------------------------------------------


        // Collector controls------------------------------------------------
         // Collect glyphs
        if (op.driver1.isButtonJustPressed(Button.DPAD_DOWN))
        {
            op.motorCollectorRight.setPower(-0.7);
            op.motorCollectorLeft.setPower(0.7);
        }
         // Score glyphs
        else if (op.driver1.isButtonJustPressed(Button.DPAD_UP))
        {
            op.motorCollectorRight.setPower(0.7);
            op.motorCollectorLeft.setPower(-0.7);

        }
         // Stack glyphs (need a slower speed for this)
        else if (op.driver1.isButtonJustPressed(Button.DPAD_RIGHT))
        {
            op.motorCollectorRight.setPower(0.3);
            op.motorCollectorLeft.setPower(-0.3);

        }
         // Stop collector
        else if (op.driver1.isButtonJustPressed(Button.DPAD_LEFT))
        {
            op.motorCollectorRight.setPower(0);
            op.motorCollectorLeft.setPower(0);
        }
        //---------------------------------------------------------------------


        op.telemetry.addData("Glyphter Enc: ", op.motorGlyphter.getCurrentPosition());
        op.telemetry.update();
    }
}
