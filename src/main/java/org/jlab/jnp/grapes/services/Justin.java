package org.jlab.jnp.grapes.services;

import java.util.List;
import java.util.ArrayList;

import org.jlab.jnp.hipo4.data.Bank;
import org.jlab.jnp.hipo4.data.Event;
import org.jlab.jnp.hipo4.data.SchemaFactory;
import org.jlab.jnp.pdg.PDGDatabase;
import org.jlab.jnp.physics.LorentzVector;

/**
 * targetPDG and beamEnergy are defined in YAML
 *
 * return from processDataEvent as early as possible
 */
public class Justin extends BeamTargetWagon {

    public Justin() {
        super("Justin","Bobcat","0.0");
    }

    private boolean checkMissing(LorentzVector electron, LorentzVector proton, List<LorentzVector> protons) {
        for (int j=0; j<protons.size(); ++j) {
            LorentzVector missing = getMissingVector(electron, proton, protons.get(j));
            if (Math.abs(missing.mass()) < 0.1 && Math.abs(missing.p()) < 0.1) {
                return true;
            }
        }
        return false;
    }
    
    @Override
    public boolean processDataEvent(Event event, SchemaFactory factory) {

        Bank particles = new Bank(factory.getSchema("REC::Particle"));

        // get scattered electron, requiring it to be the trigger particle:
        if (particles.getInt("pid",0) != 11) return false;
        if (particles.getInt("status",0)/1000 != -2) return false;
        LorentzVector electron = Util.getLorentzVector(particles,0);

        // check Q2:
        LorentzVector q = new LorentzVector(electron);
        q.sub(beamVector);
        final double Q2 = Math.abs(q.mass2());
        if (Q2 > 1.0) return false;

        // check xB:
        final double xB = Q2 / 2 / PDGDatabase.getParticleById(targetPDG).mass()
            / beamEnergy / electron.e();
        if (xB < 666) return false;

        // check all 2-proton combinations:
        List<LorentzVector> protons = new ArrayList<>();
        for (int i=1; i<particles.getRows(); ++i) {
            if (particles.getInt("pid",i) == 2212) {
                // check this proton with all previous ones:
                LorentzVector proton = Util.getLorentzVector(particles, i);
                if (checkMissing(electron, proton, protons)) {
                    return true;
                }
                // add this proton to the previous ones:
                protons.add(proton);
            }
        }

        return false;
    }
}
