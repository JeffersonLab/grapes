package org.jlab.jnp.grapes.services;

import org.jlab.jnp.hipo4.data.Bank;
import org.jlab.jnp.hipo4.data.Event;
import org.jlab.jnp.hipo4.data.SchemaFactory;
import java.util.ArrayList;

/**
 * 
 * Skim for studying high-energy pion rejection for improving electron identification 
 *
 * Until standard filters provide access to momentum, ECAL energy, invariant/missing masses.
 *
 * @author jnewton
 * @author baltzell
 */
public class MissingNeutronWagon extends Wagon {

    static final double BEAM_ENERGY = 10.6f;
    static final double PROTON_MASS = 0.938f;
    static final double ELE_MOM_LOW = 0.5f;
    static final double ELE_MOM_HIGH = 4.5f;
    static final double OTHER_MOM_LOW = 5.0f;
    static final double NEUTRON_MASS_LOW = 0.7f;
    static final double NEUTRON_MASS_HIGH = 1.3f;
    
    public MissingNeutronWagon() {
        super("MissingNeutronWagon","jnewton","0.2");
    }

    @Override
    public boolean init(String jsonString) {
        System.out.println("MissingNeutronWagon READY.");
        return true;
    }

    private double getMass(final int pid) {
        double mass=-1;
        switch (Math.abs(pid)) {
            case 11:
                mass = 0.000511;
                break;
            case 211:
                mass = 0.139570;
                break;
            case 321:
                mass = 0.493677;
                break;
            case 2112:
                mass = 0.939272;
                break;
            case 2212:
                mass = 0.939565;
                break;
            default:
                throw new RuntimeException("unknown pid: "+pid);
        }
        return mass;
    }

    private double getMomentum(final int ipart, Bank particles) {
        final double px = particles.getFloat("px",ipart);
        final double py = particles.getFloat("py",ipart);
        final double pz = particles.getFloat("pz",ipart);
        return Math.sqrt(px*px + py*py + pz*pz);
    }

    double getMissingMass(final int ipart1, final int ipart2, final double m1, final double m2, Bank particles) {
	
        final double px1 = particles.getFloat("px",ipart1);
        final double py1 = particles.getFloat("py",ipart1);
        final double pz1 = particles.getFloat("pz",ipart1);
        final double e1 = Math.sqrt(px1*px1 + py1*py1 + pz1*pz1 + m1*m1);

        final double px2 = particles.getFloat("px",ipart2);
        final double py2 = particles.getFloat("py",ipart2);
        final double pz2 = particles.getFloat("pz",ipart2);
        final double e2 = Math.sqrt(px2*px2 + py2*py2 + pz2*pz2 + m2*m2);

        double missing_energy = BEAM_ENERGY + PROTON_MASS - (e1 + e2);
        double missing_px = px1 + px2;
        double missing_py = py1 + py2;
        double missing_pz = BEAM_ENERGY - (pz1 + pz2);
        double missing_p = Math.sqrt(missing_px*missing_px + missing_py*missing_py + missing_pz*missing_pz);
        double missing_mass = Math.sqrt(missing_energy*missing_energy - missing_p*missing_p);
        
        return missing_mass;
    }

    @Override
    public boolean processDataEvent(Event event, SchemaFactory factory) {

        Bank particles = new Bank(factory.getSchema("REC::Particle"));
        event.read(particles);
        if (particles.getRows()<1) return false;
        
        // electrons, negatives, and positives within momentum cuts:
        ArrayList<Integer> negHiCandi = new ArrayList<>();
        ArrayList<Integer> posHiCandi = new ArrayList<>();
        
        for (int ipart=0; ipart<particles.getRows(); ipart++) {
           
            final int pid = particles.getInt("pid",ipart);
            final int status = particles.getInt("status",ipart);
            final boolean isFD = (int)(Math.abs(status)/1000) == 2;

            // check that trigger particle is high-momentum electron:
            if (ipart==0) {
                if (!isFD || pid!=11 || 
                    this.getMomentum(ipart,particles) < ELE_MOM_LOW ||
                    this.getMomentum(ipart,particles) > ELE_MOM_HIGH) {
                    return false;
                }
            }
            // find other high-momentum pi+/e+:
            else if (isFD && (pid==211 || pid==-11)) {
                if (this.getMomentum(ipart,particles) > OTHER_MOM_LOW) {
                    posHiCandi.add(ipart);
                }
            }
            // find other high-momentum pi-/e-:
            else if (isFD && (pid==-211 || pid==11)) {
                if (this.getMomentum(ipart,particles) > OTHER_MOM_LOW) {
                    negHiCandi.add(ipart);
                }
            }
        }

        // keep if a high-momentum trigger-e- and negative:
        if (!negHiCandi.isEmpty()) return true;
	    
        // keep if a high-momentum trigger e- and positive, with good missing mass:
        for (int jj : posHiCandi) {
            final double mm = this.getMissingMass(0,jj,0,0.13957,particles);
            if (mm>NEUTRON_MASS_LOW && mm<NEUTRON_MASS_HIGH) {
                return true;
            }
        }

        return false;
    }

}
