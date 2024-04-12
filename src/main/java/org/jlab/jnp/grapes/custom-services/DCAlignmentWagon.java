package org.jlab.jnp.grapes.services;

import org.jlab.jnp.hipo4.data.Bank;
import org.jlab.jnp.hipo4.data.Event;
import org.jlab.jnp.hipo4.data.SchemaFactory;

/**
 * 
 * Skim for DC alignment
 *
 * Determine whether an electron candidate, defined as a track with associated 
 * Cherenkov and calorimter clusters, was detected.
 *
 * @author devita
 */
public class DCAlignmentWagon extends Wagon {

    static final double VZMIN = -35;
    static final double VZMAX =  35;
    static final double ECALMIN = 0;
    static final double NPHEMIN = 2.0;

    public DCAlignmentWagon(){
        super("DCAlignmentWagon","devita","0.9");
    }

    @Override
    public boolean init(String jsonString) {
        return true;
    }

    @Override
    public boolean processDataEvent(Event event, SchemaFactory factory) {

        Bank particles = new Bank(factory.getSchema("REC::Particle"));
        Bank calorimeters = new Bank(factory.getSchema("REC::Calorimeter"));
        Bank cherenkovs = new Bank(factory.getSchema("REC::Cherenkov"));
        event.read(particles);
        event.read(calorimeters);
        event.read(cherenkovs);
        if (particles.getRows()<1) return false;
        if (calorimeters.getRows()<1) return false;
        if (cherenkovs.getRows()<1) return false;
        
        for(int ipart=0; ipart<particles.getRows(); ipart++) {
            double beta    = particles.getFloat("beta",ipart);
            double vz      = particles.getFloat("vz",ipart);
            
            double nphe = 0;
            for(int i=0; i<cherenkovs.getRows(); i++) {
                if(cherenkovs.getShort("pindex", i)==ipart) {
                    nphe = cherenkovs.getFloat("nphe", i);
                    break;
                }
            }

            double energy = 0;
            for (int i=0; i<calorimeters.getRows(); i++) {
                if(calorimeters.getShort("pindex", i)==ipart) {
                    energy+=calorimeters.getFloat("energy", i);
                }
            }

            if(beta>0 && 
                    vz>VZMIN && vz<VZMAX &&
               nphe>NPHEMIN && 
               energy>ECALMIN) { 
                return true;
            }
        } 
                
        return false;
    }   
}
