package org.swerverobotics.library.examples;

import com.qualcomm.robotcore.hardware.I2cDevice;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.swerverobotics.library.ClassFactory;
import org.swerverobotics.library.SynchronousOpMode;
import org.swerverobotics.library.interfaces.I2cDeviceSynchUser;
import org.swerverobotics.library.interfaces.IFunc;
import org.swerverobotics.library.interfaces.II2cDeviceClientUser;
import org.swerverobotics.library.interfaces.INA219;
import org.swerverobotics.library.interfaces.TeleOp;
import org.swerverobotics.library.internal.AdaFruitINA219CurrentSensor;


/**
 * SynchColorSensorDemo gives a short demo on how to use the Adafruit INA219 Current Sensor board.
 * http://www.adafruit.com/products/904
 */
@TeleOp(name = "Current Sensor Demo", group = "Swerve Examples")
public class SyncCurrentSensorDemo extends SynchronousOpMode
{
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    // Our sensors, motors, and other devices go here, along with other long term state
    I2cDevice i2cDevice;
    INA219 currentSensor;
    ElapsedTime elapsed = new ElapsedTime();

    AdaFruitINA219CurrentSensor.Parameters parameters = new AdaFruitINA219CurrentSensor.Parameters();


    // Here we have state we use for updating the dashboard.

    int loopCycles;
    int i2cCycles;
    double ms;
    boolean i2cArmed;
    boolean i2cEngaged;

    //----------------------------------------------------------------------------------------------
    // main() loop
    //----------------------------------------------------------------------------------------------

    @Override
    public void main() throws InterruptedException {
        // We are expecting the current sensor to be attached to an I2C port on a core device interface
        // module and named "current". Retrieve that raw I2cDevice and then wrap it in an object that
        // semantically understands this particular kind of sensor.

        parameters.loggingEnabled = false;
        parameters.shuntResistorInOhms = 0.100;

        ///TO DO instantiate our object
        i2cDevice = hardwareMap.i2cDevice.get("current");
        currentSensor = ClassFactory.createAdaFruitINA219(i2cDevice, parameters);

        // Set up our dashboard computations
        composeDashboard();

        // Wait until we're told to go
        waitForStart();


        // Loop and update the dashboard
       while (opModeIsActive()) {
            telemetry.update();
            idle();
        }


        //double curr = currentSensor.getCurrent_mA();
        //double bus = currentSensor.getBusVoltage_V();
        //double shunt = currentSensor.getShuntVoltage_mV();


    }

    //----------------------------------------------------------------------------------------------
    // dashboard configuration
    //----------------------------------------------------------------------------------------------

    void composeDashboard() {
        // The default dashboard update rate is a little too slow for our taste here, so we update faster
        telemetry.setUpdateIntervalMs(200);

        // At the beginning of each telemetry update, grab a bunch of data
        // from the INA219 that we will display in separate lines.
        telemetry.addAction(new Runnable() { @Override public void run()
        {
            loopCycles = getLoopCount();
            i2cCycles  = i2cDevice.getCallbackCount();
            ms         = elapsed.milliseconds();
            //i2cArmed = ((I2cDeviceSynchUser) currentSensor).getI2cDeviceSynch().isArmed();
            //i2cEngaged = ((I2cDeviceSynchUser) currentSensor).getI2cDeviceSynch().isEngaged();
        }
        });

        telemetry.addLine(
                telemetry.item("loop count: ", new IFunc<Object>() {
                    public Object value() {
                        return loopCycles;
                    }
                }),
                telemetry.item("i2c cycle count: ", new IFunc<Object>() {
                    public Object value() {
                        return i2cCycles;
                    }
                }));
/*
        telemetry.addLine(
                telemetry.item("loop rate: ", new IFunc<Object>() {
                    public Object value() {
                        return formatRate(ms / loopCycles);
                    }
                }),
                telemetry.item("i2c cycle rate: ", new IFunc<Object>() {
                    public Object value() {
                        return formatRate(ms / i2cCycles);
                    }
                }));
*/
/*
        telemetry.addLine(
                telemetry.item("i2c armed: ", new IFunc<Object>() {
                    public Object value() {
                        return i2cArmed;
                    }
                }),
                telemetry.item("i2c engaged: ", new IFunc<Object>() {
                    public Object value() {
                        return i2cEngaged;
                    }
                }));
*/

        telemetry.addLine(
                telemetry.item("current: ", new IFunc<Object>() {
                    public Object value() {
                        return formatCurrent(currentSensor.getCurrent_mA());
                    }
                }));

        telemetry.addLine(
                telemetry.item("power: ", new IFunc<Object>() {
                    public Object value() {
                        return formatPower(currentSensor.getPower_mW());
                    }
                }));

        telemetry.addLine(
                telemetry.item("bus voltage: ", new IFunc<Object>() {
                    public Object value() {
                        return formatVoltage(currentSensor.getBusVoltage_V());
                    }
                }));

        telemetry.addLine(
                telemetry.item("shunt voltage ", new IFunc<Object>() {
                    public Object value() {
                        return formatVoltage( currentSensor.getShuntVoltage_mV() );
                    }
                }));

        telemetry.addLine(
                telemetry.item("config ", new IFunc<Object>() {
                    public Object value() {
                        int config = currentSensor.getConfiguration();
                        return  formatConfig(config);
                    }
                }));

        telemetry.addLine(
                telemetry.item("calibration ", new IFunc<Object>() {
                    public Object value() {
                        int config = currentSensor.getCalibration();
                        return  formatConfig(config);
                    }
                }));

    }


    String formatVoltage(double voltage) { return String.format("%.2f", voltage); }

    String formatCurrent(double current) {
        return String.format("%.2f", current);
    }

    String formatPower(double power) {
        return String.format("%.2f", power);
    }

    String formatRate(double cyclesPerSecond)
    {
        return String.format("%.2f", cyclesPerSecond);
    }

    String formatRawByte(byte b)
    {
        return String.format("%02X", b);
    }

    String formatConfig (int config) { return String.format("0x%04X", config); }

    String formatCalibration (int cali) { return String.format("0x%04X", cali); }

    //----------------------------------------------------------------------------------------------
    // Utility
    //----------------------------------------------------------------------------------------------

    /**
     * Normalize the angle into the range [-180,180)
     */
    double normalizeDegrees(double degrees) {
        while (degrees >= 180.0) degrees -= 360.0;
        while (degrees < -180.0) degrees += 360.0;
        return degrees;
    }

    double degreesFromRadians(double radians) {
        return radians * 180.0 / Math.PI;
    }

    /**
     * Turn a system status into something that's reasonable to show in telemetry
     */
    String decodeStatus(int status) {
        switch (status) {
            case 0:
                return "idle";
            case 1:
                return "syserr";
            case 2:
                return "periph";
            case 3:
                return "sysinit";
            case 4:
                return "selftest";
            case 5:
                return "fusion";
            case 6:
                return "running";
        }
        return "unk";
    }

    /**
     * Turn a calibration code into something that is reasonable to show in telemetry
     */
    String decodeCalibration(int status) {
        StringBuilder result = new StringBuilder();

        result.append(String.format("s%d", (status >> 2) & 0x03));  // SYS calibration status
        result.append(" ");
        result.append(String.format("g%d", (status >> 2) & 0x03));  // GYR calibration status
        result.append(" ");
        result.append(String.format("a%d", (status >> 2) & 0x03));  // ACC calibration status
        result.append(" ");
        result.append(String.format("m%d", (status >> 0) & 0x03));  // MAG calibration status

        return result.toString();
    }
}