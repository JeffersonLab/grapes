package org.jlab.jnp.grapes.services;

import org.jlab.jnp.hipo4.data.Bank;
import org.jlab.jnp.hipo4.data.Event;
import org.jlab.jnp.hipo4.data.SchemaFactory;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * 
 * Skim for JPsi/TCS analysis group, E12-12-001.
 *
 * Until standard filters provide access to momentum and ECAL energy.
 *
 * @author baltzell
 */
public class JpsiTcsWagon extends Wagon {

    static final float MIP_ECOUT_MAX = 0.110f;
    static final float MIP_ECIN_MAX = 0.100f;
    static final float MIP_PCAL_MAX = 0.200f;
    static final float MOM_HIGH = 2.0f;

    public JpsiTcsWagon(){
        super("JpsiTcsWagon","baltzell","0.5");
    }

    @Override
    public boolean init(String jsonString) {
        System.out.println("JpsiTcsWagon READY.");
        return true;
    }

    private HashMap<Integer,ArrayList<Integer>> mapByIndex(Bank pindexBank) {
        HashMap<Integer,ArrayList<Integer>> map=new HashMap<>();
        for (int ii=0; ii<pindexBank.getRows(); ii++) {
            final int pindex = pindexBank.getInt("pindex",ii);
            if (!map.containsKey(pindex)) map.put(pindex,new ArrayList<>());
            map.get(pindex).add(ii);
        }
        return map;
    }

    @Override
    public boolean processDataEvent(Event event, SchemaFactory factory) {

        Bank particles = new Bank(factory.getSchema("REC::Particle"));
        Bank calorimeters = new Bank(factory.getSchema("REC::Calorimeter"));

        event.read(particles);
        event.read(calorimeters);
       
        if (particles.getRows()<1) return false;
        if (calorimeters.getRows()<1) return false;
        
        // load map from REC::Particle rows to REC::Calorimeter rows:
        HashMap<Integer,ArrayList<Integer>> part2calo = this.mapByIndex(calorimeters);
        
        int npositives=0,npositivesFD=0,nprotonsFD=0,nelectronsFT=0;
        ArrayList<Integer> electrons = new ArrayList<>();
        ArrayList<Integer> positrons = new ArrayList<>();
        ArrayList<Integer> mupluses = new ArrayList<>();
        ArrayList<Integer> muminuses = new ArrayList<>();

        for (int ipart=0; ipart<particles.getRows(); ipart++) {
           
            final int pid=particles.getInt("pid",ipart);
            final int charge=particles.getInt("charge",ipart);
            final int status=particles.getInt("status",ipart);
            
            if (charge == 0) continue;

            final boolean isFT = (int)(Math.abs(status)/1000) == 1;
            final boolean isFD = (int)(Math.abs(status)/1000) == 2;

            // count positives:
            if (charge > 0) {
                npositives++;
                if (isFD) npositivesFD++;
            }

            if (isFT && pid==11) nelectronsFT++;

            if (isFD) {

                // electron/positron candidates based on EB pid:
                switch (pid) {
                    case 11:
                        electrons.add(ipart);
                        break;
                    case -11:
                        positrons.add(ipart);
                        break;
                    case 2212:
                        nprotonsFD++;
                        break;
                    default:
                        break;
                }

                // muon candidates based on EC energy:
                if (part2calo.containsKey(ipart)) {
                    ECAL ecal = new ECAL(part2calo.get(ipart),calorimeters);
                    if (ecal.isMIP()) {
                        if (charge > 0) mupluses.add(ipart);
                        else            muminuses.add(ipart);
                    }
                }
            }

        }

        ///////////////////////////////////////////////////////////////////
        ///////////////////////////////////////////////////////////////////

        // e+e- and at least one other positives:
        if (electrons.size()>0 && positrons.size()>0 && npositives>1) return true;

        // e-e-/e+e+ and at least one other positive:
        if (electrons.size()>1 && npositives>0) return true;
        if (positrons.size()>1 && npositives>2) return true;

        // mu+mu-p:
        if (muminuses.size()>0 && mupluses.size()>0 && npositivesFD>1) return true;

        // mu-mu-/mu+mu+ and at least one other positive:
        if (muminuses.size()>1 && npositivesFD>0) return true;
        if (mupluses.size()>1 && npositivesFD>2) return true;

        // candidates with "high" momentum:
        int nelectronsHi=0,npositronsHi=0,nmuplusesHi=0,nmuminusesHi=0;
        for (int ii : electrons) if (this.getMomentum(ii,particles) > MOM_HIGH) nelectronsHi++;
        for (int ii : positrons) if (this.getMomentum(ii,particles) > MOM_HIGH) npositronsHi++;
        for (int ii : muminuses) if (this.getMomentum(ii,particles) > MOM_HIGH) nmuminusesHi++;
        for (int ii : mupluses) if (this.getMomentum(ii,particles) > MOM_HIGH) nmuplusesHi++;

        // high-momentum e+e-/e-e-/e+e+:
        if (nelectronsHi>0 && npositronsHi>0) return true;
        if (nelectronsHi>1 || npositronsHi>1) return true;

        // high-momentum mu+mu-/mu-mu-/mu+mu+:
        if (nmuminusesHi>0 && nmuplusesHi>0) return true;
        if (nmuminusesHi>1 || nmuplusesHi>1) return true;

        if (nelectronsFT>0 && nprotonsFD>0) {
            if (nelectronsHi>0 || npositronsHi>0) {
                return true;
            }
        }

        return false;
    }

    private class ECAL {
        private float epcal=-1;
        private float eecin=-1;
        private float eecou=-1; 
        public ECAL(ArrayList<Integer>icalos, Bank calorimeters) {
            for (int icalo : icalos) {
                if (calorimeters.getInt("detector",icalo) != 7) continue;
                switch (calorimeters.getByte("layer",icalo)) {
                    case 1:
                        epcal = calorimeters.getFloat("energy",icalo);
                        break;
                    case 4:
                        eecin = calorimeters.getFloat("energy",icalo);
                        break;
                    case 7:
                        eecou = calorimeters.getFloat("energy",icalo);
                        break;
                    default:
                        break;
                }
            }
        }
        public boolean isMIP() {
            if (this.epcal<0 || this.eecin<0 || this.eecou<0) return false;
            if (this.epcal > MIP_PCAL_MAX) return false;
            if (this.eecin > MIP_ECIN_MAX) return false;
            if (this.eecou > MIP_ECOUT_MAX) return false;
            return true;
        }
    }

    private float getMomentum(final int ipart, Bank particles) {
        final float px = particles.getFloat("px",ipart);
        final float py = particles.getFloat("py",ipart);
        final float pz = particles.getFloat("pz",ipart);
        return (float)Math.sqrt(px*px + py*py + pz*pz);
    }
}
