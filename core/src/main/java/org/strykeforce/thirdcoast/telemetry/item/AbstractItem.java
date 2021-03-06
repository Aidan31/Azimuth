package org.strykeforce.thirdcoast.telemetry.item;

import com.squareup.moshi.JsonWriter;
import java.io.IOException;
import java.util.Set;
import org.strykeforce.thirdcoast.telemetry.grapher.Measure;

/**
 * Abstract base class for Items.
 */
public abstract class AbstractItem implements Item {

  public final String type;
  public final String description;
  public final Set<Measure> measures;

  public AbstractItem(String type, String description,
      Set<Measure> measures) {
    this.type = type;
    this.description = description;
    this.measures = measures;
  }

  @Override
  public String type() {
    return type;
  }

  @Override
  public String description() {
    return description;
  }

  @Override
  public Set<Measure> measures() {
    return measures;
  }

  @Override
  public void toJson(JsonWriter writer) throws IOException {
    writer.beginObject();
    writer.name("not").value("implemented");
    writer.endObject();
  }

  @Override
  public String toString() {
    return "AbstractItem{" +
        "type='" + type + '\'' +
        ", description='" + description + '\'' +
        ", measures=" + measures +
        '}';
  }
}
