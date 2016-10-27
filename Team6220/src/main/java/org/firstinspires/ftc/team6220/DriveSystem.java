package org.firstinspires.ftc.team6220;

/*
    Contains the robot's drive assemblies, and gives them power or velocity targets based upon robot velocity, power, or position targets.

    INPUTS:
      -Target powers, velocities, or positions (e.g. a driver input)
      -Positions or velocities from sensors (e.g. the vuforia position)
      -Sensor errors (e.g. +/- 1cm)

    OUTPUTS:
      -Estimated robot location using combined sensor inputs
*/
public class DriveSystem
{
    private DriveAssembly[] assemblies;
    private PIDFilter[] LocationControlFilter = new PIDFilter[2];
    private PIDFilter RotationControlFilter;
    public Transform2D robotLocation;
    //TODO add Kalman filter for position estimation

    public DriveSystem(DriveAssembly[] driveAssemblyArray, Transform2D initialLocation, PIDFilter[] filter)
    {
        this.assemblies = driveAssemblyArray;
        this.robotLocation = initialLocation;
        this.LocationControlFilter[0] = filter[0];
        this.LocationControlFilter[1] = filter[1];
        this.RotationControlFilter = filter[3];
    }

    //TODO add updateRobotLocation() and getRobotLocation()

    //PID-driven navigation to a point
    //TODO add ability to use non "zig-zag" pathing
    //call once per loop
    //assumes robot position had already been updated
    public void navigateTo(Transform2D target)
    {
        //update error terms
        LocationControlFilter[0].roll(target.x-robotLocation.x);
        LocationControlFilter[1].roll(target.y-robotLocation.y);
        RotationControlFilter.roll(optimizeRotationTarget(target.rot-robotLocation.rot));
        double xRate = LocationControlFilter[0].getFilteredValue();
        double yRate = LocationControlFilter[1].getFilteredValue();
        double wRate = RotationControlFilter.getFilteredValue();
        moveRobot(xRate,yRate,wRate);
        getRobotMotionFromEncoders();
    }

    //TODO make this more configurable
    //TODO make assemblies.location represent their actual values

    //                   horizontal vertical  rotation
    public void moveRobot(double x, double y, double w)
    {
        double[] rawPowers = new double[]{0.0,0.0,0.0,0.0};

        //TODO see beginning of MasterOpMode
        //calculate motor powers proportionally
        for (int corner = 0; corner < 4; corner++)
        {
            rawPowers[corner] =
                    w
                    + assemblies[corner].location.y * x         //assemblies[corner].location.y works as sine of the angle of each motor
                    + assemblies[corner].location.x * y         //assemblies[corner].location.x works as cosine of the angle of each motor
                    ;
        }
        //scales values such that they will remain in proportion in the case that they would "overflow", e.g. [0.4,0.6,1.0,2.0] becomes [0.2,0.3,0.5,1.0]
        double scalingFactor = 1.0;
        double largest = SequenceUtilities.getLargestMagnitude(rawPowers);
        if (largest > 1.0)
        {
            scalingFactor = largest;
        }

        //write motor powers
        for (int corner = 0; corner < 4; corner++)
        {
            assemblies[corner].setPower(rawPowers[corner]/scalingFactor);
        }
    }

    //estimate the robot's last motion using encoders
    //returns an average, so more frequent calls will be noisier but will miss some events
    //note that this is a rate of change in deg/s and m/s, not a position and orientation difference
    public Transform2D getRobotMotionFromEncoders()
    {
        Transform2D diff = new Transform2D(0,0,0);
        double[] motion = new double[]{0,0};
        for (int corner = 0; corner < 4; corner++)
        {
            diff.rot += assemblies[corner].getRotationEncoderDifferential();
        }
        for(int corner = 0; corner < 4; corner++)
        {
            motion = SequenceUtilities.vectorAdd(motion,assemblies[corner].getVectorEncoderDifferential());
        }
        diff.x = motion[0];
        diff.y = motion[1];
        return diff;
    }

    //prevent an unlimited rotary system from rotating further than it needs
    double optimizeRotationTarget(double angle)
    {
        double q = Math.floor(angle/180);
        angle -= q*180;

        return angle;
    }

}
