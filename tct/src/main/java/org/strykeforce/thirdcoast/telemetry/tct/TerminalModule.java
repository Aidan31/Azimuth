package org.strykeforce.thirdcoast.telemetry.tct;

import com.ctre.CANTalon;
import dagger.Module;
import dagger.Provides;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import javax.inject.Singleton;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

@Module
public abstract class TerminalModule {

  @Provides
  @Singleton
  public static Set<CANTalon> provideTalons() {
    return new HashSet<>();
  }

  @Provides
  @Singleton
  public static Terminal provideTerminal() {
    try {
      return TerminalBuilder.terminal();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
