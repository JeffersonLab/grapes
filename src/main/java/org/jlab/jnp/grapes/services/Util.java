package org.jlab.jnp.grapes.services;

import java.util.ArrayList;
import java.util.HashMap;
import org.jlab.jnp.hipo4.data.Bank;
import org.jlab.jnp.physics.LorentzVector;
import org.jlab.jnp.pdg.PDGDatabase;

/**
 *
 * @author baltzell
 */
public class Util {

    /**
     * @param particles the particle bank, e.g. REC::Particle
     * @param index the index of the particle in the bank
     * @param pid the pid to assume for the mass
     * @return the corresponding Lorentz vector
     */
    public static LorentzVector getLorentzVector(Bank particles, int index, int pid) {
        final double px = particles.getFloat("px", index);
        final double py = particles.getFloat("py", index);
        final double pz = particles.getFloat("pz", index);
        LorentzVector v = new LorentzVector();
        v.setPxPyPzM(px,py,pz,PDGDatabase.getParticleById(pid).mass());
        return v;
    }

    /**
     * @param particles the particle bank, e.g. REC::Particle
     * @param index the index of the particle in the bank
     * @return the corresponding Lorentz vector assuming the bank's pid for mass
     */
    public static LorentzVector getLorentzVector(Bank particles, int index) {
        return getLorentzVector(particles, index, particles.getInt("pid",index));
    }

    /**
     * @param pindexBank the bank containing the pindex variable
     * @return map of pindex to rows in the bank
     */
    public static HashMap<Integer,ArrayList<Integer>> mapByIndex(Bank pindexBank) {
        HashMap<Integer,ArrayList<Integer>> map=new HashMap<>();
        for (int ii=0; ii<pindexBank.getRows(); ii++) {
            final int pindex = pindexBank.getInt("pindex",ii);
            if (!map.containsKey(pindex)) map.put(pindex,new ArrayList<>());
            map.get(pindex).add(ii);
        }
        return map;
    }

}
