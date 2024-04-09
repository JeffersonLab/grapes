package org.jlab.jnp.grapes.services;

import java.util.Random;
import org.jlab.jnp.hipo4.data.Event;
import org.jlab.jnp.hipo4.data.SchemaFactory;
import org.jlab.jnp.utils.json.Json;
import org.jlab.jnp.utils.json.JsonObject;

/**
 *
 * @author baltzell
 */
public class RandomWagon extends Wagon {

    boolean includeTags = true;
    double prescale = 100.0f;
    Random random = new Random();
    
    public RandomWagon() {
        super("RandomWagon", "baltzell", "1.0");
    }

    @Override
    public boolean processDataEvent(Event event, SchemaFactory factory) {
        if (includeTags && event.getEventTag() != 0) return true;
        return random.nextFloat(1.0f) < 1./prescale;
    }

    @Override
    public boolean init(String jsonString) {
        JsonObject jsonObj = Json.parse(jsonString).asObject();
        prescale = jsonObj.getDouble("prescale",prescale);
        includeTags = jsonObj.getBoolean("includeTags",includeTags);
        return true;
    }
    
}
