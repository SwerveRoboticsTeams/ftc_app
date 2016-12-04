package org.firstinspires.ftc.team6220;

import com.qualcomm.hardware.adafruit.BNO055IMU;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.DcMotor;

import java.text.Normalizer;



/*

*/

abstract public class MasterOpMode extends LinearOpMode
{
    //Java Enums cannot act as integer indices as they do in other languages
    //To maintain compatabliity with with a FOR loop, we
    //TODO find if java supports an order-independent loop for each member of an array
    //CodeReview: How about this? Java lets you use a for statement with an array, like this:
    /*
    int [] numberArray = {100, 200, 300, 400, 500};

    for (int num: numberArray){    //num is a local variable. this will iterate through numberArray one value at a time.
        System.out.print( num );
        System.out.print(",");
    }

    //You can also do this with more complicated objects.
    for (DriveAssembly d : driveAssemblies) {
        d.motor.setPower(0);
    }

    */

    //e.g. in python that would be:   for assembly in driveAssemblies:
    //                                    assembly.doStuff()
    public final int FRONT_RIGHT = 0;
    public final int FRONT_LEFT  = 1;
    public final int BACK_LEFT   = 2;
    public final int BACK_RIGHT  = 3;

    DcMotor CollectorMotor;

    private int EncoderFR = 0;
    private int EncoderFL = 0;
    private int EncoderBL = 0;
    private int EncoderBR = 0;

    double robotXPos = 0;
    double robotYPos = 0;

    //TODO deal with angles at all starting positions
    double currentAngle = 0.0;

    //used to create global coordinates by adjusting the imu heading based on the robot's starting orientation
    private double headingOffset = 0.0;

    private BNO055IMU imu;

    DriveAssembly[] driveAssemblies;

    DriveSystem drive;

    VuforiaHelper vuforiaHelper;

    MotorToggler motorToggler;
    MotorToggler motorTogglerReverse;

    public void initializeHardware() {
        //create a driveAssembly array to allow for easy access to motors
        driveAssemblies = new DriveAssembly[4];

        //TODO adjust correction factor if necessary
        //TODO fix all switched front and back labels on motors
        //our robot uses an omni drive, so our motors are positioned at 45 degree angles to motor positions on a normal drive.
        //mtr,                                                                                                       x,   y,  rot, gear, radius, correction factor
        //CodeReview: please fix the config file so "front" and "back" are consistent between drive assemblies and hardwareMap
        driveAssemblies[BACK_RIGHT] = new DriveAssembly(hardwareMap.dcMotor.get("motorBackRight"), new Transform2D(1.0, 1.0, 135), 1.0, 0.1016, 1.0);
        driveAssemblies[BACK_LEFT] = new DriveAssembly(hardwareMap.dcMotor.get("motorBackLeft"), new Transform2D(-1.0, 1.0, 225), 1.0, 0.1016, 1.0);
        driveAssemblies[FRONT_LEFT] = new DriveAssembly(hardwareMap.dcMotor.get("motorFrontLeft"), new Transform2D(-1.0, -1.0, 315), 1.0, 0.1016, 1.0);
        driveAssemblies[FRONT_RIGHT] = new DriveAssembly(hardwareMap.dcMotor.get("motorFrontRight"), new Transform2D(1.0, -1.0, 45), 1.0, 0.1016, 1.0);

        CollectorMotor = hardwareMap.dcMotor.get("motorCollector");

        //TODO tune our own drive PID loop using DriveAssemblyPID instead of build-in P/step filter
        //TODO Must be disabled if motor encoders are not correctly reporting
        driveAssemblies[FRONT_RIGHT].motor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        driveAssemblies[FRONT_LEFT].motor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        driveAssemblies[BACK_LEFT].motor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        driveAssemblies[BACK_RIGHT].motor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);

        CollectorMotor.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);

        //CodeReview: This isn't currently used in teleop, so we might want to move it
        //            into the MasterAutonomous initializeAuto method. It might be the reason that
        //            we occasionally get control delays in teleop. As far as I can tell, it's ok
        //            for this to be null in the drive system in teleop.
        vuforiaHelper = new VuforiaHelper();

        //TODO remove "magic numbers"
        //CodeReview: please don't use magic numbers (0.8, 1/150). Instead use named constants and
        //            put a comment next to those names explaining where the value comes from (how you derived it)
        //                                          drive assemblies  initial loc:     x    y    w
        drive = new DriveSystem(this, vuforiaHelper, driveAssemblies, new Transform2D(0.0, 0.0, 0.0),
                new PIDFilter[]{
                        new PIDFilter(0.8, 0.0, 0.0),    //x location control
                        new PIDFilter(0.8, 0.0, 0.0),    //y location control
                        new PIDFilter(Constants.turningPowerFactor, 0.0, 0.0)}); //rotation control

        //CodeReview: It seems like your MotorToggler class should handle both of these cases.
        //            You shouldn't have to create two variables (one for forwards, one for backwards).
        //            E.g. your MotorToggler could have setDirection(Direction.Forwards), setDirection(Direction.Backwards)
        //            and then turnOn() would start the motor in that direction.
        motorToggler = new MotorToggler(CollectorMotor, 1.0);
        motorTogglerReverse = new MotorToggler(CollectorMotor, -1.0);

        BNO055IMU.Parameters parameters = new BNO055IMU.Parameters();
        parameters.angleUnit = BNO055IMU.AngleUnit.DEGREES;
        parameters.accelUnit = BNO055IMU.AccelUnit.METERS_PERSEC_PERSEC;
        parameters.calibrationDataFile = "AdafruitIMUCalibration.json"; // see the calibration sample opmode
        //CodeReview: Did we include the calibration json somewhere so it can be found in our program?
        //            If we are going to reference this file, it has to exist, and has to be where the
        //            calibration sample opmode puts it (so it can be found)
        parameters.loggingEnabled = true;
        parameters.loggingTag = "IMU";

        // Retrieve and initialize the IMU. We expect the IMU to be attached to an I2C port
        // on a Core Device Interface Module, configured to be a sensor of type "AdaFruit IMU",
        // and named "imu".
        imu = hardwareMap.get(BNO055IMU.class, "imu");
        imu.initialize(parameters);
    }

    //TODO test encoder function; likely has errors
    //keeps track of the robot's location on the field based on Encoders and IMU; make sure to call once each loop
    public Transform2D updateLocationUsingEncoders()
    {

        //CodeReview: since you intend this to be called every loop, you should eliminate things
        //            that you aren't using (e.g. the elapsed time stuff?), because they will slow
        //            down your main loop and thus slow the responsiveness of your bot.
        //            You can comment them out for now so you have them for later, if you do intend to use them eventually.

        //stands for elapsed time
        double eTime;

        //get time when loop starts
        double startTime = System.nanoTime() / 1000 / 1000 / 1000;
        double finalTime = 0;

        eTime = finalTime - startTime; //CodeReview: Can it be correct that this starts as a negative number?

        //x and y positions not considering robot rotation
        double xRawPosition = 0.0;
        double yRawPosition = 0.0;

        //x and y positions taking into account robot rotation
        double xLocation;
        double yLocation;

        //angles relative to starting angle used to determine our x and y positions
        double xAngle;
        double yAngle;

        Transform2D location;

        //angle in degrees for return value
        double currentAngleDegrees = getAngularOrientationWithOffset();

        //converted to radians for Math.sin() function
        currentAngle = currentAngleDegrees * Math.PI / 180;

        EncoderFR = driveAssemblies[FRONT_RIGHT].motor.getCurrentPosition();
        EncoderFL = driveAssemblies[FRONT_LEFT].motor.getCurrentPosition();
        EncoderBL = driveAssemblies[BACK_LEFT].motor.getCurrentPosition();
        EncoderBR = driveAssemblies[BACK_RIGHT].motor.getCurrentPosition();

        //math to calculate x and y positions based on encoder ticks and robot angle
        //factors after EncoderFL are equivalent to circumference / encoder ticks per rotation
        //robotXPos = Math.cos(currentAngle) * (EncoderFL + EncoderFR) * 2 * Math.PI * 0.1016 / (1120 * Math.pow(2, 0.5));
        //robotYPos = Math.sin(currentAngle) * (EncoderFL - EncoderFR) * 2 * Math.PI * 0.1016 / (1120 * Math.pow(2, 0.5));

        //not currently in use
        Transform2D motion = drive.getRobotMotionFromEncoders();

        //these are shorthand for the encoder derivative for each motor and will be plugged into our encoder function
        double FLencDerivative = driveAssemblies[BACK_RIGHT].getLinearEncoderDerivative();
        double FRencDerivative = driveAssemblies[BACK_RIGHT].getLinearEncoderDerivative();
        double BLencDerivative = driveAssemblies[BACK_RIGHT].getLinearEncoderDerivative();
        double BRencDerivative = driveAssemblies[BACK_RIGHT].getLinearEncoderDerivative();


        yAngle = Math.PI / 2 + currentAngle;
        xAngle = Math.PI / 2 - yAngle;

        //math to calculate x and y positions based on encoder ticks and not accounting for angle
        xRawPosition += eTime * ((FLencDerivative + FRencDerivative -(BLencDerivative + BRencDerivative)) / (4 * Math.sqrt(2)));
        yRawPosition += eTime * ((FLencDerivative + BLencDerivative -(FRencDerivative + BRencDerivative)) / (4 * Math.sqrt(2)));

        //final location utilizing angles
        xLocation = yRawPosition * Math.cos(yAngle) + xRawPosition * Math.cos(xAngle);
        yLocation = yRawPosition * Math.sin(yAngle) - xRawPosition * Math.sin(xAngle);

        location = new Transform2D(xLocation, yLocation, currentAngleDegrees);

        telemetry.addData("X:", xLocation);
        telemetry.addData("Y:", yLocation);
        telemetry.addData("W:", currentAngleDegrees);
        telemetry.update();

        //get time at end of loop
        finalTime = System.nanoTime()/1000/1000/1000;

        //CodeReview: do you need this return value? (does any caller need it?)
        //            Seems like all that's needed is to update location in this method.
        return location;
    }

    //uses solely encoders to move the robot to a desired location
    public void navigateUsingEncoders(Transform2D Target)
    {
        Transform2D newLocation = updateLocationUsingEncoders();

        while ((Math.abs(Target.x - drive.robotLocation.x) > Constants.xTolerance) || (Math.abs(Target.y - drive.robotLocation.y) > Constants.yTolerance)|| (Math.abs(Target.rot - drive.robotLocation.rot) > Constants.wTolerance))
        {
            drive.navigateTo(Target);

            newLocation = updateLocationUsingEncoders();

            idle();
        }
    }

    //CodeReview: Might be a little clearer to rename this method "setRobotStartingOrientation" since that is what it means in your opmodes
    //other opmodes must go through this method to prevent others from blithely changing headingOffset
    public void setRobotStartingOrientation(double newValue)
    {
        headingOffset = newValue;
    }

    //takes into account headingOffset to utilize global orientation
    public double getAngularOrientationWithOffset()
    {
        double correctedHeading = imu.getAngularOrientation().firstAngle + headingOffset;

        return correctedHeading;
    }

    //wait a number of milliseconds
    public void pause(int t) throws InterruptedException
    {
        //we don't use System.currentTimeMillis() because it can be inconsistent
        long initialTime = System.nanoTime();
        while((System.nanoTime() - initialTime)/1000/1000 < t)
        {
            idle();
        }
    }

    public void stopAllDriveMotors()
    {
        driveAssemblies[FRONT_RIGHT].setPower(0.0);
        driveAssemblies[FRONT_LEFT].setPower(0.0);
        driveAssemblies[BACK_LEFT].setPower(0.0);
        driveAssemblies[BACK_RIGHT].setPower(0.0);
    }

    //tells our robot to turn to a specified angle
    public void turnTo(double targetAngle)
    {
        double currentAngle = getAngularOrientationWithOffset();
        double angleDiff = drive.normalizeRotationTarget(targetAngle, currentAngle);
        double turningPower;

        while(Math.abs(angleDiff) > Constants.minimumAngleDiff)
        {
            currentAngle = getAngularOrientationWithOffset();
            angleDiff = drive.normalizeRotationTarget(targetAngle, currentAngle);
            turningPower = angleDiff * Constants.turningPowerFactor;

            if (Math.abs(turningPower) > 1.0)
            {
                turningPower = Math.signum(turningPower);
            }

            // Makes sure turn power doesn't go below minimum power
            if(turningPower > 0 && turningPower < Constants.minimumTurningPower)
            {
                turningPower = Constants.minimumTurningPower;
            }
            else if (turningPower < 0 && turningPower > -Constants.minimumTurningPower)
            {
                turningPower = -Constants.minimumTurningPower;
            }
            else
            {

            }

            telemetry.addData("current angle: ", getAngularOrientationWithOffset());
            telemetry.update();

            drive.moveRobot(0.0, 0.0, -turningPower);

            idle();
        }

        stopAllDriveMotors();
    }
}
