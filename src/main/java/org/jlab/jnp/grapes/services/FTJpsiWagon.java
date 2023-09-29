package org.jlab.jnp.grapes.services;

import org.jlab.jnp.hipo4.data.Bank;
import org.jlab.jnp.hipo4.data.Event;
import org.jlab.jnp.hipo4.data.SchemaFactory;
import org.jlab.jnp.physics.LorentzVector;

/**
 *
 * @author baltzell
 */
public class FTJpsiWagon extends BeamTargetWagon {
    
    static final float MIN_MISS_MASS = 2.50f; 

    public FTJpsiWagon(){
        super("JFTJPsiWagon","baltzell","0.6");
    }
    
    public boolean processDataEvent(Event event, SchemaFactory factory) {

        Bank particles = new Bank(factory.getSchema("RECFT::Particle"));
        event.read(particles);

        final int nrows = particles.getRows();
        int ielec = -1;
        for (int i=0; i<nrows; ++i) {
            if (particles.getInt("pid",i) != 11) continue;
            if (particles.getInt("status",i)/1000 != -1) continue;
            ielec = i;
            break;
        }
        if (ielec < 0) return false;

        LorentzVector electron = Util.getLorentzVector(particles, ielec);
        for (int i=0; i<nrows; ++i) {
            if (particles.getInt("pid",i) != 2212) continue;
            if (particles.getInt("status",i)/2000 != 1) continue;
            LorentzVector proton = Util.getLorentzVector(particles, i);
            if (getMissingVector(proton,electron).mass() > MIN_MISS_MASS) {
                return true;
            }
        }
        return false;
    }

}
