package org.strykeforce.thirdcoast.swerve

import com.electronwill.nightconfig.core.file.FileConfig
import org.strykeforce.thirdcoast.talon.TalonProvisioner
import spock.lang.Shared

class SwerveDriveTest extends spock.lang.Specification {

    @Shared
    TalonProvisioner provisioner

    void setupSpec() {
        URL url = this.getClass().getResource("/org/strykeforce/thirdcoast/talon/testdata/talons.toml")
        FileConfig config = FileConfig.of(url.file)
        config.load()
        config.close()
        provisioner = new TalonProvisioner(config.unmodifiable())
    }

    def "calculates inverse kinematics"() {
        Wheel[] wheels = [Mock(Wheel), Mock(Wheel), Mock(Wheel), Mock(Wheel)]
        SwerveDrive swerve = new SwerveDrive(null, wheels)

        when:
        for (Wheel w : wheels) {
            w.azimuthTalon.getPosition() >> 0
        }
        swerve.drive(f, s, a)

        then:
        1 * wheels[0].set({ Math.abs(it - w0a) < 1e-3 }, { Math.abs(it - w0d) < 1e-4 })
        1 * wheels[1].set({ Math.abs(it - w1a) < 1e-3 }, { Math.abs(it - w1d) < 1e-4 })
        1 * wheels[2].set({ Math.abs(it - w2a) < 1e-3 }, { Math.abs(it - w2d) < 1e-4 })
        1 * wheels[3].set({ Math.abs(it - w3a) < 1e-3 }, { Math.abs(it - w3d) < 1e-4 })

        where:
        f     | s   | a    || w0a    | w0d    | w1a    | w1d    | w2a     | w2d    | w3a     | w3d
        0     | 0   | 0    || 0      | 0      | 0      | 0      | 0       | 0      | 0       | 0
        1     | 0   | 0    || 0      | 1      | 0      | 1      | 0       | 1      | 0       | 1
        -1    | 0   | 0    || 0.5    | 1      | 0.5    | 1      | 0.5     | 1      | 0.5     | 1
        0     | 1   | 0    || 0.25   | 1      | 0.25   | 1      | 0.25    | 1      | 0.25    | 1
        0     | -1  | 0    || -0.25  | 1      | -0.25  | 1      | -0.25   | 1      | -0.25   | 1

        1     | 1   | 0    || 0.125  | 1      | 0.125  | 1      | 0.125   | 1      | 0.125   | 1
        -1    | 1   | 0    || 0.375  | 1      | 0.375  | 1      | 0.375   | 1      | 0.375   | 1
        1     | -1  | 0    || -0.125 | 1      | -0.125 | 1      | -0.125  | 1      | -0.125  | 1
        -1    | -1  | 0    || -0.375 | 1      | -0.375 | 1      | -0.375  | 1      | -0.375  | 1

        0     | 0   | 1    || 0.125  | 1      | 0.375  | 1      | -0.125  | 1      | -0.375  | 1
        0     | 0   | -1   || -0.375 | 1      | -0.125 | 1      | 0.375   | 1      | 0.125   | 1

        // expected values from Ether's swerve tester spreadsheet
        0.5   | 0   | 1    || 0.0844 | 1      | 0.2953 | 0.5267 | -0.0844 | 1      | -0.2953 | 0.5267
        1     | 1   | 1    || 0.125  | 1      | 0.2231 | 0.7174 | 0.0269  | 0.7174 | 0.125   | 0.1716
        -0.25 | 0.5 | -0.2 || 0.3819 | 0.5308 | 0.2967 | 0.3746 | 0.3372  | 0.7514 | 0.2767  | 0.6505
    }

    def "sets preference key for wheel zero data"() {
        expect:
        key == SwerveDrive.getPreferenceKeyForWheel(i)

        where:
        i || key
        0 || "SwerveDrive/wheel.0"
        1 || "SwerveDrive/wheel.1"
    }

    // check some swerve related math
    def "calculate max of 4 doubles"() {
        expect:
        Math.max(Math.max(a, b), Math.max(c, d)) == max

        where:
        a   | b   | c   | d   || max
        0.1 | 0.2 | 1.1 | 0.2 || 1.1
        1.1 | 0.2 | 0.1 | 0.2 || 1.1
        0.1 | 1.2 | 0.1 | 0.2 || 1.2
        0.1 | 1.2 | 0.1 | 1.2 || 1.2
    }
}
