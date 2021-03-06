package org.strykeforce.thirdcoast.swerve

import com.ctre.CANTalon
import com.electronwill.nightconfig.core.file.FileConfig
import org.strykeforce.thirdcoast.talon.TalonProvisioner
import spock.lang.Shared
import spock.lang.Specification

class WheelTest extends Specification {

    static final EPSILON = 1e-15

    def azimuth = Mock(CANTalon)
    def drive = Mock(CANTalon)
    @Shared
    TalonProvisioner provisioner

    void setupSpec() {
        URL url = this.getClass().getResource("/org/strykeforce/thirdcoast/talon/testdata/talons.toml")
        FileConfig config = FileConfig.of(url.file)
        config.load()
        config.close()
        provisioner = new TalonProvisioner(config.unmodifiable())
    }

    def "configures azimuth and drive talons"() {
        when:
        def wheel = new Wheel(provisioner, azimuth, drive)

        then:
        1 * drive.SetVelocityMeasurementPeriod(CANTalon.VelocityMeasurementPeriod.Period_100Ms)
        1 * drive.reverseOutput(false)
        1 * drive.changeControlMode(CANTalon.TalonControlMode.Voltage)
        1 * drive.setFeedbackDevice(CANTalon.FeedbackDevice.QuadEncoder)
        1 * drive.enableLimitSwitch(false, false)
        1 * drive.setSafetyEnabled(false)
        1 * drive.isSensorPresent(CANTalon.FeedbackDevice.QuadEncoder)
        1 * drive.reverseSensor(false)
        1 * drive.enableBrakeMode(true)
        1 * drive.SetVelocityMeasurementWindow(64)
        1 * azimuth.setSafetyEnabled(false)
        1 * azimuth.SetVelocityMeasurementWindow(64)
        1 * azimuth.configPeakOutputVoltage(6.0, -6.0)
        1 * azimuth.setPID(1.0, 2.0, 3.0)
        1 * azimuth.setF(4.0)
        1 * azimuth.setAllowableClosedLoopErr(0)
        1 * azimuth.setNominalClosedLoopVoltage(0.0)
        1 * azimuth.reverseSensor(false)
        1 * azimuth.enableBrakeMode(false)
        1 * azimuth.isSensorPresent(CANTalon.FeedbackDevice.CtreMagEncoder_Relative)
        1 * azimuth.reverseOutput(false)
        1 * azimuth.changeControlMode(CANTalon.TalonControlMode.Position)
        1 * azimuth.setIZone(0)
        1 * azimuth.configNominalOutputVoltage(0.0, 0.0)
        1 * azimuth.enableLimitSwitch(false, false)
        1 * azimuth.setFeedbackDevice(CANTalon.FeedbackDevice.CtreMagEncoder_Relative)
        1 * azimuth.SetVelocityMeasurementPeriod(CANTalon.VelocityMeasurementPeriod.Period_100Ms)

        when:
        wheel.azimuthParameters = "speed"

        then:
        1 * azimuth.changeControlMode(CANTalon.TalonControlMode.Speed)
        0 * azimuth.changeControlMode(_)
        0 * drive.changeControlMode(_)
    }

    def "gets azimuth absolution position"() {
        setup:
        azimuth.getPulseWidthPosition() >> 0x1000

        when:
        def wheel = new Wheel(provisioner, azimuth, drive)
        def zeroPosition = 2767

        then:
        wheel.azimuthAbsolutePosition == 0

        when:
        wheel.setAzimuthZero(zeroPosition)

        then:
        1 * azimuth.setPosition((double) -zeroPosition / 0xFFF)
    }

    def "azimuth changes are optimized"() {
        when:
        azimuth.getPosition() >> start_position
        def wheel = new Wheel(provisioner, azimuth, drive)
        wheel.set(setpoint, 1)

        then:
        Math.abs(wheel.azimuthSetpoint - end_position) < EPSILON
        wheel.isDriveReversed() == is_reversed

        where:
        start_position | setpoint || end_position | is_reversed
        0              | 0        || 0            | false
        0              | 0.1      || -0.1         | false
        -0.1           | 0.2      || -0.2         | false
        -0.2           | 0.3      || -0.3         | false
        -0.3           | 0.4      || -0.4         | false
        -0.4           | 0.45     || -0.45        | false
        -0.45          | 0.5      || -0.5         | false
        -0.5           | 0.5      || -0.5         | false
        -0.5           | 0.45     || -0.45        | false
        -0.45          | -0.4     || -0.6         | false
        -0.6           | -0.3     || -0.7         | false
        -0.7           | -0.2     || -0.8         | false
        -0.8           | -0.1     || -0.9         | false
        -0.9           | -0.0     || -1.0         | false

        -0.1           | 0.1      || -0.1         | false
        0.1            | -0.1     || 0.1          | false

        -0.4           | 0.4      || -0.4         | false
        -0.6           | 0.3      || -0.8         | true
        -0.7           | 0.25     || -0.75        | true
        -0.75          | 0.1      || -0.6         | true
        -0.9           | -0.1     || -0.9         | false

        0              | 0.5      || 0            | true
        0              | -0.5     || 0            | true
        -0.5           | 0.5      || -0.5         | false

        -0.1           | -0.4     || -0.1         | true
        -0.1           | 0.4      || 0.1          | true
        -1.1           | -0.4     || -1.1         | true
        -1.1           | 0.4      || -0.9         | true

        2767.4         | -0.2     || 2767.2       | false
        -2767.4        | 0.2      || -2767.2      | false
        2767.4         | -10.2    || 2767.2       | false
        -2767.4        | 10.2     || -2767.2      | false

        0.25           | -0.25    || 0.25         | false
    }

    def "drive output is scaled"() {
        when:
        def wheel = new Wheel(provisioner, azimuth, drive)
        wheel.set(0, 1)

        then:
        1 * azimuth.set(0.0)
        1 * drive.set(12.0)
    }

    // check some wheel-related math
    def "calculates error between current azimuth and setpoint"() {

        expect:
        Math.abs(Math.IEEEremainder(setpoint - position, 1.0) - error) < EPSILON

        where:
        position | setpoint || error
        0        | 0        || 0
        0.25     | 0.25     || 0
        0.25     | 0.5      || 0.25
        0.25     | -0.25    || -0.5
        0.25     | -0.5     || 0.25
        -0.4     | 0.4      || -0.2
        0.5      | -0.5     || 0
        -0.5     | 0.5      || 0
        -0.01    | 0.01     || 0.02
        -0.4     | 0.2      || -0.4
        -2.4     | 0.2      || -0.4
    }

    def "calculate minimal azimuth error with drive direction"() {
        when:
        def error = Math.IEEEremainder(setpoint - position, 1.0)
        def isReversed = false
        if (Math.abs(error) > 0.25) {
            error -= Math.copySign(0.5, error)
            isReversed = true
        }

        then:
        Math.abs(error - expected_error) < EPSILON
        isReversed == expected_reverse
        Math.abs(position + error - expected_position) < EPSILON

        where:
        setpoint | position || expected_error | expected_position | expected_reverse
        0        | 0        || 0              | 0                 | false
        0.25     | 0.5      || -0.25          | 0.25              | false
        -0.5     | -0.25    || -0.25          | -0.5              | false
        0.25     | -0.1     || -0.15          | -0.25             | true
        -0.5     | 0.5      || 0              | 0.5               | false
        -0.5     | 0.5      || 0              | 0.5               | false
        0.49     | -0.5     || -0.01          | -0.51             | false
        0        | 1.0      || 0              | 1.0               | false
        0        | 1.1      || -0.1           | 1.0               | false
        0.4      | -2.4     || -0.2           | -2.6              | false
        0        | -0.4     || -0.1           | -0.5              | true
        0.2      | -0.4     || 0.1            | -0.3              | true
        0.2      | -2.4     || 0.1            | -2.3              | true
        -0.2     | 0.4      || -0.1           | 0.3               | true
        -0.2     | 2.4      || -0.1           | 2.3               | true
        0.6      | 0        || 0.1            | 0.1               | true
        -1.0     | 0.5      || 0.0            | 0.5               | true
        1.5      | 0.5      || 0.0            | 0.5               | false

    }
}
