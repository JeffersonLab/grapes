package org.jlab.jnp.grapes.services;

import org.jlab.jnp.hipo4.data.Bank;
import org.jlab.jnp.hipo4.data.Event;
import org.jlab.jnp.hipo4.data.SchemaFactory;

/**
 * keep events with opposite-sector, opposite-charge tracks
 * @author baltzell
 */
public class PairTriggerWagon extends Wagon {

    static final byte DC=6;
    
    public PairTriggerWagon() {
        super("PairTriggerWagon","baltzell","0.1");
    }

    @Override
    public boolean init(String jsonString) {
        System.out.println("PairTriggerWagon READY.");
        return true;
    }

    @Override
    public boolean processDataEvent(Event event, SchemaFactory factory) {

        Bank tracks = new Bank(factory.getSchema("REC::Track"));
        event.read(tracks);

        for (int it1=0; it1<tracks.getRows()-1; it1++) {

            if (tracks.getByte("q",it1) == 0) continue;

            if (tracks.getByte("detector",it1) != DC) continue;

            for (int it2=it1+1; it2<tracks.getRows(); it2++) {

                if (tracks.getByte("detector",it2) != DC) continue;

                if (tracks.getByte("q",it1) == tracks.getByte("q",it2)) continue;

                if (Math.abs(tracks.getByte("sector",it2)-
                             tracks.getByte("sector",it1)) % 6 == 3) {
                    return true;
                }
            }
        }

        return false;
    }

}
