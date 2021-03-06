package org.firstinspires.ftc.team417;

import com.qualcomm.hardware.bosch.BNO055IMU;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.util.ElapsedTime;
import com.qualcomm.robotcore.util.Range;

import org.firstinspires.ftc.robotcore.external.Func;
import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.robotcore.external.matrices.OpenGLMatrix;
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.AxesOrder;
import org.firstinspires.ftc.robotcore.external.navigation.AxesReference;
import org.firstinspires.ftc.robotcore.external.navigation.Orientation;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaTrackable;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaTrackableDefaultListener;

//@Autonomous(name="Master Autonomous", group = "Swerve")
// @Disabled

abstract class MasterAutonomous extends MasterOpMode
{
    private ElapsedTime runtime = new ElapsedTime();
    public ElapsedTime autoRuntime = new ElapsedTime();

    final float mmPerInch = 25.4f;
    final float mmBotWidth = 18 * mmPerInch; // the robot width
    final float mmFTCFieldWidth = (12 * 12 - 2) * mmPerInch;   // the FTC field is ~11'10" center-to-center of the glass panels

    boolean isRedTeam; // are you on red team? (if not, you're blue)
    boolean isStartingPosOne;  // are you on starting position one? (if not, you're on position two)
    double startDist; // the distance traveled depending on pos one or two
    int startDelay; // the time to delay the start if our alliance needs us to delay
    int targetIndex; // specify what image target it is

    double pivotAngle; // depends on what team you're on
    double targetAngle; // the Vuforia angle to align to depending on what team you're on

    float[] targetPos = {1524, mmFTCFieldWidth}; // target position x and y with an origin right between the driver stations
    VuforiaNavigation VuforiaNav = new VuforiaNavigation();

    // speed is proportional to error
    double Kmove = 1.0f/1200.0f;
    double Kpivot = 1.0f/150.0f;

    double TOL = 100.0;
    double TOL_ANGLE = 5;

    double VUFORIA_TOL = 20;
    double VUFORIA_TOL_ANGLE = 1;

    double MINSPEED = 0.25;
    double PIVOT_MINSPEED = 0.2;


    // VARIABLES FOR MOVE/ALIGN METHODS
    int pivotDst;

    int newTargetFL;
    int newTargetBL;
    int newTargetFR;
    int newTargetBR;

    int errorFL;
    int errorFR;
    int errorBL;
    int errorBR;

    double speedFL;
    double speedFR;
    double speedBL;
    double speedBR;

    double speedAbsFL;
    double speedAbsFR;
    double speedAbsBL;
    double speedAbsBR;

    double startAngle;
    double curTurnAngle;
    double pivotSpeed;
    double errorAngle;

    double avgDistError;
    double avgSpeed;
    double speedAbsAvg;

    double refAngle;


    public void initializeRobot()
    {
        super.initializeHardware(); // call master op mode's init method

        // zero the motor controllers before running, don't know if motors start out at zero
        motorFrontLeft.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        motorFrontRight.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        motorBackLeft.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        motorBackRight.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);

        // run with encoder mode
        motorFrontLeft.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        motorFrontRight.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        motorBackLeft.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        motorBackRight.setMode(DcMotor.RunMode.RUN_USING_ENCODER);

        // treat back side (camera) as front
        // reverse left side motors instead of right side motors
        motorFrontLeft.setDirection(DcMotor.Direction.REVERSE);
        motorBackLeft.setDirection(DcMotor.Direction.REVERSE);

        motorFrontRight.setDirection(DcMotor.Direction.FORWARD);
        motorBackRight.setDirection(DcMotor.Direction.FORWARD);

        //Set up telemetry data
        // We show the log in oldest-to-newest order, as that's better for poetry
        telemetry.log().setDisplayOrder(Telemetry.Log.DisplayOrder.OLDEST_FIRST);
        // We can control the number of lines shown in the log
        telemetry.log().setCapacity(4);
        //configureDashboard();
    }

    // drive forwards/backwards/horizontal left and right function
    public void move(double x, double y, double speed, double timeout) throws InterruptedException
    {
        newTargetFL = motorFrontLeft.getCurrentPosition() + (int) Math.round(COUNTS_PER_MM * y) + (int) Math.round(COUNTS_PER_MM * x * 1.15);
        newTargetFR = motorFrontRight.getCurrentPosition() + (int) Math.round(COUNTS_PER_MM * y) - (int) Math.round(COUNTS_PER_MM * x * 1.15);
        newTargetBL = motorBackLeft.getCurrentPosition() + (int) Math.round(COUNTS_PER_MM * y) - (int) Math.round(COUNTS_PER_MM * x * 1.15);
        newTargetBR = motorBackRight.getCurrentPosition() + (int) Math.round(COUNTS_PER_MM * y) + (int) Math.round(COUNTS_PER_MM * x * 1.15);

    runtime.reset(); // used for timeout

        // wait until the motors reach the position
        do
        {
            errorFL = newTargetFL - motorFrontLeft.getCurrentPosition();
            speedFL = Math.abs(errorFL * Kmove);
            speedFL = Range.clip(speedFL, MINSPEED, speed);
            speedFL = speedFL * Math.signum(errorFL);

            errorFR = newTargetFR - motorFrontRight.getCurrentPosition();
            speedFR = Math.abs(errorFR * Kmove);
            speedFR = Range.clip(speedFR, MINSPEED, speed);
            speedFR = speedFR * Math.signum(errorFR);

            errorBL = newTargetBL - motorBackLeft.getCurrentPosition();
            speedBL = Math.abs(errorBL * Kmove);
            speedBL = Range.clip(speedBL, MINSPEED, speed);
            speedBL = speedBL * Math.signum(errorBL);

            errorBR = newTargetBR - motorBackRight.getCurrentPosition();
            speedBR = Math.abs(errorBR * Kmove);
            speedBR = Range.clip(speedBR, MINSPEED, speed);
            speedBR = speedBR * Math.signum(errorBR);

            motorFrontLeft.setPower(speedFL);
            motorFrontRight.setPower(speedFR);
            motorBackLeft.setPower(speedBL);
            motorBackRight.setPower(speedBR);

            idle();
        }
        while (opModeIsActive() &&
                (runtime.seconds() < timeout) &&
                (Math.abs(errorFL) > TOL || Math.abs(errorFR) > TOL || Math.abs(errorBL) > TOL || Math.abs(errorBR) > TOL));

        // stop the motors
        motorFrontLeft.setPower(0);
        motorFrontRight.setPower(0);
        motorBackLeft.setPower(0);
        motorBackRight.setPower(0);
    }

    // aligns angularly with the image target
    public void pivotVuforia (double targetAngle, double speed)
    {
        double error;
        double curTurnAngle;
        double pivotSpeed;

        // run with encoder mode
        motorFrontLeft.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        motorFrontRight.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        motorBackLeft.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        motorBackRight.setMode(DcMotor.RunMode.RUN_USING_ENCODER);

        do
        {
            VuforiaNav.getLocation(targetIndex); // update target location and angle

            do
            {
                VuforiaNav.getLocation(targetIndex); // update target location and angle
                idle();
            }
            while (VuforiaNav.lastLocation == null && opModeIsActive());

            // now extract the angle out of "get location", and stores your location
            curTurnAngle = Orientation.getOrientation(VuforiaNav.lastLocation, AxesReference.EXTRINSIC, AxesOrder.XYZ, AngleUnit.DEGREES).thirdAngle;
            curTurnAngle = adjustAngles(curTurnAngle);
            error =  targetAngle - curTurnAngle;
            pivotSpeed = speed * Math.abs(error) * Kpivot;
            pivotSpeed = Range.clip(pivotSpeed, PIVOT_MINSPEED, speed); // limit abs speed
            pivotSpeed = pivotSpeed * Math.signum(error); // set the sign of speed

            pivot(error, pivotSpeed);

            if (isLogging) telemetry.log().add(String.format("CurAngle: %f, error: %f", curTurnAngle, error));
            idle();

        } while (opModeIsActive() && (Math.abs(error) > TOL_ANGLE));

        // stop motors
        motorFrontLeft.setPower(0);
        motorFrontRight.setPower(0);
        motorBackLeft.setPower(0);
        motorBackRight.setPower(0);
    }

    // a combination of both the align and pivot function (WITHOUT VUFORIA)
    // angle has to be small otherwise won't work, this function moves and pivots robot
    public void pivotMove(double x, double y, double pivotAngle, double speed, double timeout)
    {
        final double XSCALE = 1.1;
        MINSPEED = 0.35;
        // run with encoder mode
        motorFrontLeft.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        motorFrontRight.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        motorBackLeft.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        motorBackRight.setMode(DcMotor.RunMode.RUN_USING_ENCODER);

        final double ROBOT_DIAMETER_MM = 27.6 * 25.4;   // diagonal 17.6 inch FL to BR and FR to BL
        pivotDst = (int) ((pivotAngle / 360.0) * ROBOT_DIAMETER_MM * 3.1415 * COUNTS_PER_MM);

        int xTarget = (int) Math.round(COUNTS_PER_MM * (x * XSCALE));
        int yTarget = (int) Math.round(COUNTS_PER_MM * (y));
        newTargetFL = motorFrontLeft.getCurrentPosition() + xTarget + yTarget + pivotDst;
        newTargetFR = motorFrontRight.getCurrentPosition() - xTarget + yTarget - pivotDst;
        newTargetBL = motorBackLeft.getCurrentPosition() - xTarget + yTarget + pivotDst;
        newTargetBR = motorBackRight.getCurrentPosition() + xTarget + yTarget - pivotDst;

        runtime.reset(); // reset timer, which is used for loop timeout below

        // read starting angle
        startAngle = imu.getAngularOrientation().firstAngle;

        // wait until the motors reach the position
        // adjust robot angle during movement by adjusting speed of motors
        do
        {
            curTurnAngle = imu.getAngularOrientation().firstAngle - startAngle;
            curTurnAngle = adjustAngles(curTurnAngle);
            errorAngle =  pivotAngle - curTurnAngle;
            pivotSpeed = errorAngle * Kpivot;
            pivotSpeed = Range.clip(pivotSpeed, -0.3, 0.3); // limit max pivot speed
            // pivotSpeed is added to each motor's movement speed

            errorFL = newTargetFL - motorFrontLeft.getCurrentPosition();
            speedFL = Kmove * errorFL;  // movement speed proportional to error
            speedFL += pivotSpeed;  // combine movement and pivot speeds
            speedAbsFL = Math.abs(speedFL);
            speedAbsFL = Range.clip(speedAbsFL, MINSPEED, speed);  // clip abs(speed)
            speedFL = speedAbsFL * Math.signum(speedFL);  // set sign of speed

            errorFR = newTargetFR - motorFrontRight.getCurrentPosition();
            speedFR = Kmove * errorFR;
            speedFR -= pivotSpeed;  // combine movement and pivot speeds
            speedAbsFR = Math.abs(speedFR);
            speedAbsFR = Range.clip(speedAbsFR, MINSPEED, speed);  // clip abs(speed)
            speedFR = speedAbsFR * Math.signum(speedFR);

            errorBL = newTargetBL - motorBackLeft.getCurrentPosition();
            speedBL = Kmove * errorBL;
            speedBL += pivotSpeed;  // combine movement and pivot speeds
            speedAbsBL = Math.abs(speedBL);
            speedAbsBL = Range.clip(speedAbsBL, MINSPEED, speed);  // clip abs(speed)
            speedBL = speedAbsBL * Math.signum(speedBL);

            errorBR = newTargetBR - motorBackRight.getCurrentPosition();
            speedBR = Kmove * errorBR;
            speedBR -= pivotSpeed;  // combine movement and pivot speeds
            speedAbsBR = Math.abs(speedBR);
            speedAbsBR = Range.clip(speedAbsBR, MINSPEED, speed);
            speedBR = speedAbsBR * Math.signum(speedBR);

            motorFrontLeft.setPower(speedFL);
            motorFrontRight.setPower(speedFR);
            motorBackLeft.setPower(speedBL);
            motorBackRight.setPower(speedBR);

            idle();
        }
        while ( (opModeIsActive()) &&
                (runtime.seconds() < timeout) &&
                (
                        (Math.abs(errorFL) > TOL) || (Math.abs(errorFR) > TOL) || (Math.abs(errorBL) > TOL) || (Math.abs(errorBR) > TOL) ||
                                (Math.abs(errorAngle) > TOL_ANGLE)
                )
                );

        // stop the motors
        motorFrontLeft.setPower(0);
        motorFrontRight.setPower(0);
        motorBackLeft.setPower(0);
        motorBackRight.setPower(0);
    }


    // a combination of both the align and pivot function (WITHOUT VUFORIA)
    // angle has to be small otherwise won't work, this function moves and pivots robot
    public void moveAverage(double x, double y, double pivotAngle, double speed, double timeout)
    {
        final double XSCALE = 1.1;
        // run with encoder mode
        motorFrontLeft.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        motorFrontRight.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        motorBackLeft.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        motorBackRight.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        final double ROBOT_DIAMETER_MM = 27.6 * 25.4;   // diagonal 17.6 inch FL to BR and FR to BL
        pivotDst = (int) ((pivotAngle / 360.0) * ROBOT_DIAMETER_MM * 3.1415 * COUNTS_PER_MM);

        int xTarget = (int) Math.round(COUNTS_PER_MM * (x * XSCALE));
        int yTarget = (int) Math.round(COUNTS_PER_MM * (y));
        newTargetFL = motorFrontLeft.getCurrentPosition() + xTarget + yTarget + pivotDst;
        newTargetFR = motorFrontRight.getCurrentPosition() - xTarget + yTarget - pivotDst;
        newTargetBL = motorBackLeft.getCurrentPosition() - xTarget + yTarget + pivotDst;
        newTargetBR = motorBackRight.getCurrentPosition() + xTarget + yTarget - pivotDst;

        runtime.reset(); // reset timer, which is used for loop timeout below

        // read starting angle
        startAngle = imu.getAngularOrientation().firstAngle;

        // wait until the motors reach the position
        // adjust robot angle during movement by adjusting speed of motors
        do
        {
            curTurnAngle = imu.getAngularOrientation().firstAngle - startAngle;
            curTurnAngle = adjustAngles(curTurnAngle);
            errorAngle =  pivotAngle - curTurnAngle;
            pivotSpeed = errorAngle * Kpivot;
            pivotSpeed = Range.clip(pivotSpeed, -0.3, 0.3); // limit max pivot speed
            // pivotSpeed is added to each motor's movement speed

            errorFL = newTargetFL - motorFrontLeft.getCurrentPosition();
            speedFL = Kmove * errorFL;  // movement speed proportional to error
            speedFL += pivotSpeed;  // combine movement and pivot speeds
            speedAbsFL = Math.abs(speedFL);
            speedAbsFL = Range.clip(speedAbsFL, MINSPEED, speed);  // clip abs(speed)
            speedFL = speedAbsFL * Math.signum(speedFL);  // set sign of speed

            errorFR = newTargetFR - motorFrontRight.getCurrentPosition();
            speedFR = Kmove * errorFR;
            speedFR -= pivotSpeed;  // combine movement and pivot speeds
            speedAbsFR = Math.abs(speedFR);
            speedAbsFR = Range.clip(speedAbsFR, MINSPEED, speed);  // clip abs(speed)
            speedFR = speedAbsFR * Math.signum(speedFR);

            errorBL = newTargetBL - motorBackLeft.getCurrentPosition();
            speedBL = Kmove * errorBL;
            speedBL += pivotSpeed;  // combine movement and pivot speeds
            speedAbsBL = Math.abs(speedBL);
            speedAbsBL = Range.clip(speedAbsBL, MINSPEED, speed);  // clip abs(speed)
            speedBL = speedAbsBL * Math.signum(speedBL);

            errorBR = newTargetBR - motorBackRight.getCurrentPosition();
            speedBR = Kmove * errorBR;
            speedBR -= pivotSpeed;  // combine movement and pivot speeds
            speedAbsBR = Math.abs(speedBR);
            speedAbsBR = Range.clip(speedAbsBR, MINSPEED, speed);
            speedBR = speedAbsBR * Math.signum(speedBR);

            motorFrontLeft.setPower(speedFL);
            motorFrontRight.setPower(speedFR);
            motorBackLeft.setPower(speedBL);
            motorBackRight.setPower(speedBR);

            idle();
        }
        while ( (opModeIsActive()) &&
                (runtime.seconds() < timeout) &&
                (
                        ( (Math.abs(errorFL) > TOL) && (Math.abs(errorFR) > TOL) && (Math.abs(errorBL) > TOL) && (Math.abs(errorBR) > TOL) )
                        || (Math.abs(errorAngle) > TOL_ANGLE)
                )
                );

        // stop the motors
        motorFrontLeft.setPower(0);
        motorFrontRight.setPower(0);
        motorBackLeft.setPower(0);
        motorBackRight.setPower(0);
    }

    // a combination of both the align and pivot function (WITHOUT VUFORIA)
    // angle has to be small otherwise won't work, this function moves and pivots robot
    public void moveMaintainHeading(double x, double y, double pivotAngle, double refAngle, double speed, double timeout)
    {
        // run with encoder mode
        motorFrontLeft.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        motorFrontRight.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        motorBackLeft.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        motorBackRight.setMode(DcMotor.RunMode.RUN_USING_ENCODER);

        curTurnAngle = imu.getAngularOrientation().firstAngle - refAngle;
        curTurnAngle = adjustAngles(curTurnAngle);
        errorAngle =  pivotAngle - curTurnAngle;

        final double ROBOT_DIAMETER_MM = 22.6 * 25.4;   // diagonal 17.6 inch FL to BR and FR to BL
        pivotDst = (int) ((errorAngle / 360.0) * ROBOT_DIAMETER_MM * 3.1415 * COUNTS_PER_MM);

        final double XSCALE = 1.1;

        int xTarget = (int) Math.round(COUNTS_PER_MM * (x * XSCALE));
        int yTarget = (int) Math.round(COUNTS_PER_MM * (y));
        newTargetFL = motorFrontLeft.getCurrentPosition() + xTarget + yTarget + pivotDst;
        newTargetFR = motorFrontRight.getCurrentPosition() - xTarget + yTarget - pivotDst;
        newTargetBL = motorBackLeft.getCurrentPosition() - xTarget + yTarget + pivotDst;
        newTargetBR = motorBackRight.getCurrentPosition() + xTarget + yTarget - pivotDst;

        runtime.reset(); // reset timer, which is used for loop timeout below

        // wait until the motors reach the position
        // adjust robot angle during movement by adjusting speed of motors
        do
        {
            // read the real current angle and compute error compared to ref angle
            curTurnAngle = imu.getAngularOrientation().firstAngle - refAngle;
            curTurnAngle = adjustAngles(curTurnAngle);
            errorAngle =  pivotAngle - curTurnAngle;
            pivotSpeed = errorAngle * Kpivot;
            pivotSpeed = Range.clip(pivotSpeed, -0.3, 0.3); // limit max pivot speed
            // pivotSpeed is added to each motor's movement speed

            errorFL = newTargetFL - motorFrontLeft.getCurrentPosition();
            speedFL = Kmove * errorFL;  // movement speed proportional to error
            speedAbsFL = Math.abs(speedFL);
            // clip abs(speed) MAX speed minus 0.3 to leave room for pivot factor
            speedAbsFL = Range.clip(speedAbsFL, MINSPEED, speed - 0.3);
            speedFL = speedAbsFL * Math.signum(speedFL);  // set sign of speed
            speedFL += pivotSpeed;  // combine movement and pivot speeds

            errorFR = newTargetFR - motorFrontRight.getCurrentPosition();
            speedFR = Kmove * errorFR;
            speedAbsFR = Math.abs(speedFR);
            speedAbsFR = Range.clip(speedAbsFR, MINSPEED, speed - 0.3);  // clip abs(speed)
            speedFR = speedAbsFR * Math.signum(speedFR);
            speedFR -= pivotSpeed;  // combine movement and pivot speeds

            errorBL = newTargetBL - motorBackLeft.getCurrentPosition();
            speedBL = Kmove * errorBL;
            speedAbsBL = Math.abs(speedBL);
            speedAbsBL = Range.clip(speedAbsBL, MINSPEED, speed - 0.3);  // clip abs(speed)
            speedBL = speedAbsBL * Math.signum(speedBL);
            speedBL += pivotSpeed;  // combine movement and pivot speeds

            errorBR = newTargetBR - motorBackRight.getCurrentPosition();
            speedBR = Kmove * errorBR;
            speedAbsBR = Math.abs(speedBR);
            speedAbsBR = Range.clip(speedAbsBR, MINSPEED, speed - 0.3);
            speedBR = speedAbsBR * Math.signum(speedBR);
            speedBR -= pivotSpeed;  // combine movement and pivot speeds

            motorFrontLeft.setPower(speedFL);
            motorFrontRight.setPower(speedFR);
            motorBackLeft.setPower(speedBL);
            motorBackRight.setPower(speedBR);

            avgDistError = (Math.abs(errorFL) + Math.abs(errorFR) + Math.abs(errorBL) + Math.abs(errorBR)) / 4.0;
            idle();
        }
        while ( (opModeIsActive()) &&
                (runtime.seconds() < timeout) &&
                (
                     //   ( (Math.abs(errorFL) > TOL) && (Math.abs(errorFR) > TOL) && (Math.abs(errorBL) > TOL) && (Math.abs(errorBR) > TOL) )
                        avgDistError > TOL
                                || (Math.abs(errorAngle) > TOL_ANGLE)
                )
                );

        // stop the motors
        motorFrontLeft.setPower(0);
        motorFrontRight.setPower(0);
        motorBackLeft.setPower(0);
        motorBackRight.setPower(0);
    }


    public void moveKeepHeading(double x, double y, double pivotAngle, double refAngle, double speed, double timeout)
    {
        // run with encoder mode
        motorFrontLeft.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        motorFrontRight.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        motorBackLeft.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        motorBackRight.setMode(DcMotor.RunMode.RUN_USING_ENCODER);

        double avgDistError;

        curTurnAngle = imu.getAngularOrientation().firstAngle - refAngle;
        curTurnAngle = adjustAngles(curTurnAngle);
        errorAngle =  pivotAngle - curTurnAngle;

        final double ROBOT_DIAMETER_MM = 22.6 * 25.4;   // diagonal 17.6 inch FL to BR and FR to BL
        pivotDst = (int) ((errorAngle / 360.0) * ROBOT_DIAMETER_MM * 3.1415 * COUNTS_PER_MM);

        final double XSCALE = 1.1;

        int xTarget = (int) Math.round(COUNTS_PER_MM * (x * XSCALE));
        int yTarget = (int) Math.round(COUNTS_PER_MM * (y));
        newTargetFL = motorFrontLeft.getCurrentPosition() + xTarget + yTarget + pivotDst;
        newTargetFR = motorFrontRight.getCurrentPosition() - xTarget + yTarget - pivotDst;
        newTargetBL = motorBackLeft.getCurrentPosition() - xTarget + yTarget + pivotDst;
        newTargetBR = motorBackRight.getCurrentPosition() + xTarget + yTarget - pivotDst;

        runtime.reset(); // reset timer, which is used for loop timeout below

        // wait until the motors reach the position
        // adjust robot angle during movement by adjusting speed of motors
        do
        {
            // read the real current angle and compute error compared to ref angle
            curTurnAngle = imu.getAngularOrientation().firstAngle - refAngle;
            curTurnAngle = adjustAngles(curTurnAngle);
            errorAngle =  pivotAngle - curTurnAngle;
            pivotSpeed = errorAngle * Kpivot;
            pivotSpeed = Range.clip(pivotSpeed, -0.3, 0.3); // limit max pivot speed
            // pivotSpeed is added to each motor's movement speed

            errorFL = newTargetFL - motorFrontLeft.getCurrentPosition();
            errorFR = newTargetFR - motorFrontRight.getCurrentPosition();
            errorBL = newTargetBL - motorBackLeft.getCurrentPosition();
            errorBR = newTargetBR - motorBackRight.getCurrentPosition();
            avgDistError = (errorFL + errorFR + errorBL + errorBR) / 4.0;
            avgSpeed = Kmove * avgDistError;
            speedAbsAvg = Range.clip(Math.abs(avgSpeed), MINSPEED, speed - 0.3);
            avgSpeed = speedAbsAvg * Math.signum(avgSpeed);  // set sign of speed

            speedFL = avgSpeed + pivotSpeed; // combine movement and pivot speeds
            speedFR = avgSpeed - pivotSpeed;
            speedBL = avgSpeed + pivotSpeed;
            speedBR = avgSpeed - pivotSpeed;

            motorFrontLeft.setPower(speedFL);
            motorFrontRight.setPower(speedFR);
            motorBackLeft.setPower(speedBL);
            motorBackRight.setPower(speedBR);

            if (isLogging) telemetry.log().add(String.format("avgDistError: %f , EA: %f", avgDistError, errorAngle));

            idle();
        }
        while ( (opModeIsActive()) &&
                (runtime.seconds() < timeout) &&
                (
                        //   ( (Math.abs(errorFL) > TOL) && (Math.abs(errorFR) > TOL) && (Math.abs(errorBL) > TOL) && (Math.abs(errorBR) > TOL) )
                        Math.abs(avgDistError) > TOL
                                || (Math.abs(errorAngle) > TOL_ANGLE)
                )
                );

        if (isLogging) telemetry.log().add(String.format("avgDistError: %f , EA: %f", avgDistError, errorAngle));
        telemetry.update();

        // stop the motors
        motorFrontLeft.setPower(0);
        motorFrontRight.setPower(0);
        motorBackLeft.setPower(0);
        motorBackRight.setPower(0);
    }


    // a combination of both the align and pivot function (WITH VUFORIA) using pivot move
    public void alignPivotVuforia(double speed, double distAwayX, double distAwayY, double timeout) throws InterruptedException
    {
        float xPos;
        float yPos;
        float errorX;
        float errorY;
        double robotErrorX;
        double robotErrorY;

        double curRobotAngle;
        double pivotSpeed;
        double errorAngle;
        double refAngle;

        // run with encoder mode
        motorFrontLeft.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        motorFrontRight.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        motorBackLeft.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        motorBackRight.setMode(DcMotor.RunMode.RUN_USING_ENCODER);

        int loopCount = 0;
        do
        {
            while (!VuforiaNav.isVisible(targetIndex) && opModeIsActive())
                idle();
            VuforiaNav.lastLocation = null;
            do
            {
                VuforiaNav.getLocation(targetIndex); // update target location and angle
                idle();
            }
            while (VuforiaNav.lastLocation == null && opModeIsActive());

            xPos = VuforiaNav.lastLocation.getTranslation().getData()[0];
            yPos = VuforiaNav.lastLocation.getTranslation().getData()[1];
            errorX = targetPos[0] - xPos;
            errorY = targetPos[1] - yPos;

            curRobotAngle = Orientation.getOrientation(VuforiaNav.lastLocation, AxesReference.EXTRINSIC, AxesOrder.XYZ, AngleUnit.DEGREES).thirdAngle;
            curRobotAngle = adjustAngles(curRobotAngle);
            errorAngle =  targetAngle - curRobotAngle;

            // transform extrinsic field coordinate to robot intrinsic coordinate so robot knows where to go
            robotErrorX = errorX * Math.cos(Math.toRadians(targetAngle)) + errorY * Math.sin(Math.toRadians(targetAngle));
            robotErrorY = -errorX * Math.sin(Math.toRadians(targetAngle)) + errorY * Math.cos(Math.toRadians(targetAngle));
            // shift position back 25 inches away from target image
            robotErrorY -= distAwayY;
            robotErrorX += distAwayX;

// calls pivot move function here
            //pivotMove(robotErrorX, robotErrorY, errorAngle, speed, timeout); // speed, 3 second timeout

            if (Math.abs(errorAngle) > VUFORIA_TOL_ANGLE)
            {
                PIVOT_MINSPEED = 0.14;
                Kpivot = 1.0/100.0;
                TOL_ANGLE = 1.5;
                pivot(errorAngle, 0.5);
            }

            refAngle = imu.getAngularOrientation().firstAngle;

            if (Math.abs(robotErrorY) > VUFORIA_TOL)
            {
                TOL = 70;
                MINSPEED = 0.2;
                Kmove = 1.0/1200.0;
                move(0.0, robotErrorY, 0.3, 3);
            }

            if (Math.abs(robotErrorX) > VUFORIA_TOL)
            {
                TOL = 70;
                MINSPEED = 0.3;
                Kmove = 1.0/1250.0;
                move(robotErrorX, 0, 0.5, 3);
            }

            runtime.reset();
            if (isLogging) telemetry.log().add(String.format("cnt %d ErX:%.2f ErY:%.2f ErA:%.2f", loopCount, robotErrorX, robotErrorY, errorAngle)); // display each motor error as well
            telemetry.update();
            loopCount++;

            // stop the motors
            motorFrontLeft.setPower(0);
            motorFrontRight.setPower(0);
            motorBackLeft.setPower(0);
            motorBackRight.setPower(0);
            pause(250); // allow Vuforia to catch up

            // error is in mm
        } while ( opModeIsActive() && ( Math.abs(robotErrorX) > VUFORIA_TOL || Math.abs(robotErrorY) > VUFORIA_TOL
                || errorAngle > VUFORIA_TOL_ANGLE ) );

        // stop motors
        motorFrontLeft.setPower(0);
        motorFrontRight.setPower(0);
        motorBackLeft.setPower(0);
        motorBackRight.setPower(0);
    }


    // pivot using IMU
    public void pivot(double turnAngle, double speed)
    {
        double pivotSpeed;
        double startAngle;
        double curTurnAngle;

        // run with encoder mode
        motorFrontLeft.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        motorFrontRight.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        motorBackLeft.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        motorBackRight.setMode(DcMotor.RunMode.RUN_USING_ENCODER);

        startAngle = imu.getAngularOrientation().firstAngle;

        // read angle, record in starting angle variable
        // run motor
        // loop, current angle - start angle = error
        // if error is close to 0, stop motors

        double error = 100;
        double errorP1 = 100;
        double errorP2 = 100;

        do
        {
            errorP2 = errorP1;
            errorP1 = error;
            curTurnAngle = imu.getAngularOrientation().firstAngle - startAngle;
            curTurnAngle = adjustAngles(curTurnAngle);
            error =  turnAngle - curTurnAngle;
            //pivotSpeed = speed * Math.abs(error) * Kpivot;
            //pivotSpeed = Range.clip(pivotSpeed, 0.2, 0.7); // limit abs speed
            pivotSpeed = Math.abs(error) * Kpivot;
            pivotSpeed = Range.clip(pivotSpeed, PIVOT_MINSPEED, speed); // limit abs speed
            pivotSpeed = pivotSpeed * Math.signum(error); // set the sign of speed

            // positive angle means CCW rotation
            motorFrontLeft.setPower(pivotSpeed);
            motorFrontRight.setPower(-pivotSpeed);
            motorBackLeft.setPower(pivotSpeed);
            motorBackRight.setPower(-pivotSpeed);

            // allow some time for IMU to catch up
            if (Math.abs(error) < 2)
            {
                sleep(15);
                // stop motors
                motorFrontLeft.setPower(0);
                motorFrontRight.setPower(0);
                motorBackLeft.setPower(0);
                motorBackRight.setPower(0);
                sleep(150);
            }

            if (isLogging) telemetry.log().add(String.format("StartAngle: %f, CurAngle: %f, error: %f", startAngle, curTurnAngle, error));
            idle();

        } while (opModeIsActive() && (Math.abs(error) > TOL_ANGLE || Math.abs(errorP1) > TOL_ANGLE) );

        // stop motors
        motorFrontLeft.setPower(0);
        motorFrontRight.setPower(0);
        motorBackLeft.setPower(0);
        motorBackRight.setPower(0);
    }

    // pivot using IMU, but with a reference start angle, but this angle has to be determined (read) before this method is called
    public void pivotWithReference(double turnAngle, double refAngle, double speed)
    {
        double pivotSpeed;
        double curTurnAngle;

        // run with encoder mode
        motorFrontLeft.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        motorFrontRight.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        motorBackLeft.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        motorBackRight.setMode(DcMotor.RunMode.RUN_USING_ENCODER);

        // read angle, record in starting angle variable
        // run motor
        // loop, current angle - start angle = error
        // if error is close to 0, stop motors

        double error = 100;
        double errorP1 = 100;
        double errorP2 = 100;

        do
        {
            errorP2 = errorP1;
            errorP1 = error;
            curTurnAngle = imu.getAngularOrientation().firstAngle - refAngle;
            curTurnAngle = adjustAngles(curTurnAngle);
            error =  turnAngle - curTurnAngle;
            pivotSpeed = Math.abs(error) * Kpivot;
            pivotSpeed = Range.clip(pivotSpeed, PIVOT_MINSPEED, speed); // limit abs speed
            pivotSpeed = pivotSpeed * Math.signum(error); // set the sign of speed

            // positive angle means CCW rotation
            motorFrontLeft.setPower(pivotSpeed);
            motorFrontRight.setPower(-pivotSpeed);
            motorBackLeft.setPower(pivotSpeed);
            motorBackRight.setPower(-pivotSpeed);

            // allow some time for IMU to catch up
            if (Math.abs(error) < 2)
            {
                sleep(15);
                // stop motors
                motorFrontLeft.setPower(0);
                motorFrontRight.setPower(0);
                motorBackLeft.setPower(0);
                motorBackRight.setPower(0);
                sleep(150);
            }

            if (isLogging) telemetry.log().add(String.format("StartAngle: %f, CurAngle: %f, error: %f", refAngle, curTurnAngle, error));
            idle();

        } while (opModeIsActive() && (Math.abs(error) > TOL_ANGLE || Math.abs(errorP1) > TOL_ANGLE) );

        // stop motors
        motorFrontLeft.setPower(0);
        motorFrontRight.setPower(0);
        motorBackLeft.setPower(0);
        motorBackRight.setPower(0);
    }


    public void pivotDetectTarget (double span, int numIterations) throws InterruptedException
    {
        double angle = 0.0;
        int i;
        for (i = 0; i < numIterations; i++)
        {
            // allow Vuforia to catch up
            pause(300);
            if (VuforiaNav.isVisible(targetIndex))
            {
                return;
            }
            angle += span / (double) numIterations;
            //pivot(angle, 0.5);
            PivotForSeconds(0.5, 200);
        }
        PivotForSeconds(-0.5, 800);
        angle = 0.0;
        pause(300);
        for (i = 0; i < numIterations; i++)
        {
            pause(300);
            if (VuforiaNav.isVisible(targetIndex))
            {
                return;
            }
            angle -= span / (double) numIterations;
            //pivot(angle, 0.5);
            PivotForSeconds(-0.5, 200);
        }
    }

    public void WaitUntilTime(int milliseconds) throws InterruptedException
    {
        while (autoRuntime.milliseconds() < milliseconds) idle();
    }

    // this method drives for seconds, and it can only pivot
    public void PivotForSeconds(double speed, int milliSeconds) throws InterruptedException
    {
        // turn on power
        motorFrontLeft.setPower(speed);
        motorFrontRight.setPower(-speed);
        motorBackLeft.setPower(speed);
        motorBackRight.setPower(-speed);
        // let it run for x seconds
        pause(milliSeconds);
        // stop the motors after two seconds
        motorFrontLeft.setPower(0);
        motorFrontRight.setPower(0);
        motorBackLeft.setPower(0);
        motorBackRight.setPower(0);
    }


    public void shootParticlesAfterBeacons() throws InterruptedException
    {
        TOL_ANGLE = 3.0;
        VUFORIA_TOL_ANGLE = 3.0;
        Kpivot = 1/100.0;
        MINSPEED = 0.35;

        motorLauncher.setPower(0.85);
        if (isRedTeam)
        {
            pivot(-45, 0.8);
        }
        else
        {
            pivot(55, 0.8);
        }
        move(0, -100, 0.6, 2);

        motorCollector.setPower(1.0);
        servoParticle.setPosition(0.8);
        pause(300);
        servoParticle.setPosition(0.0);
        pause(1500);
        servoParticle.setPosition(0.8);
        pause(300);
        servoParticle.setPosition(0.0);
        pause(300);
        motorLauncher.setPower(0.0);
        motorCollector.setPower(0.0);
    }

    public void PushButton() throws InterruptedException
    {
        pause(70);
        telemetry.addData("Path", "pushing button");
        telemetry.update();
        move(0, 230, 0.3, 3); // push the button, used to be 325mm forwards
        if (isLogging) telemetry.log().add(String.format("pushed button"));
    }

    public void configureDashboard()
    {
        telemetry.addLine()
                .addData("Power | FrontLeft: ", new Func<String>() {
                    @Override public String value() {
                        return formatNumber(motorFrontLeft.getPower());
                    }
                })
                .addData("Power | FrontRight: ", new Func<String>() {
                    @Override public String value() {
                        return formatNumber(motorFrontRight.getPower());
                    }
                })
                .addData("Power | BackLeft: ", new Func<String>() {
                    @Override public String value() {
                        return formatNumber(motorBackLeft.getPower());
                    }
                })
                .addData("Power | BackRight: ", new Func<String>() {
                    @Override public String value() {
                        return formatNumber(motorBackRight.getPower());
                    }
                });
    }


    public String formatNumber(double d)
    {
        return String.format("%.2f", d);
    }

    String format(OpenGLMatrix transformationMatrix) {
        return transformationMatrix.formatAsTransform();

    }
}


/* TABLE:
                 FL      FR      BL      BR
    rotate CCW   +        -      +        -
    forward      +        +      +        +
    right        +        -      -        +
    d. left      0        +      +        0
    d. right     +        0      0
    */