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
        super("JpsiTcsWagon","baltzell","0.3");
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
        
        int npos=0,nposFD=0;
        ArrayList<Integer> eleCandi = new ArrayList<>();
        ArrayList<Integer> posCandi = new ArrayList<>();
        ArrayList<Integer> mupCandi = new ArrayList<>();
        ArrayList<Integer> mumCandi = new ArrayList<>();

        for (int ipart=0; ipart<particles.getRows(); ipart++) {
           
            final int pid=particles.getInt("pid",ipart);
            final int charge=particles.getInt("charge",ipart);
            final int status=particles.getInt("status",ipart);
            
            if (charge == 0) continue;

            final boolean isFD = (int)(Math.abs(status)/1000) == 2;

            // count positives:
            if (charge > 0) {
                if (isFD) nposFD++;
                npos++;
            }

            // electron/positron candidates based on EB pid:
            if (isFD) {
                if      (pid== 11) eleCandi.add(ipart);
                else if (pid==-11) posCandi.add(ipart);
            }

            // muon candidates based on EC energy:
            if (part2calo.containsKey(ipart)) {
                ECAL ecal = new ECAL(part2calo.get(ipart),calorimeters);
                if (ecal.isMIP()) {
                    if (charge > 0) mupCandi.add(ipart);
                    else            mumCandi.add(ipart);
                }
            }
        }

        // abort asap:
        if ( (eleCandi.isEmpty() || posCandi.isEmpty()) &&
             (mumCandi.isEmpty() || mupCandi.isEmpty()) ) return false;

        ///////////////////////////////////////////////////////////////////
        ///////////////////////////////////////////////////////////////////

        // e+e- and at least one other positives:
        if (eleCandi.size()>0 && posCandi.size()>0 && npos>1) return true;

        // e-e-/e+e+ and at least one other positive:
        if (eleCandi.size()>1 && npos>0) return true;
        if (posCandi.size()>1 && npos>2) return true;

        // mu+mu-p:
        if (mumCandi.size()>0 && mupCandi.size()>0 && nposFD>1) return true;

        // mu-mu-/mu+mu+ and at least one other positive:
        if (mumCandi.size()>1 && nposFD>0) return true;
        if (mupCandi.size()>1 && nposFD>2) return true;

        // candidates with "high" momentum:
        ArrayList<Integer> eleHiCandi = new ArrayList<>();
        ArrayList<Integer> posHiCandi = new ArrayList<>();
        ArrayList<Integer> mupHiCandi = new ArrayList<>();
        ArrayList<Integer> mumHiCandi = new ArrayList<>();
        for (int ii : eleCandi) if (this.getMomentum(ii,particles) > MOM_HIGH) eleHiCandi.add(ii);
        for (int ii : posCandi) if (this.getMomentum(ii,particles) > MOM_HIGH) posHiCandi.add(ii);
        for (int ii : mumCandi) if (this.getMomentum(ii,particles) > MOM_HIGH) mumHiCandi.add(ii);
        for (int ii : mupCandi) if (this.getMomentum(ii,particles) > MOM_HIGH) mupHiCandi.add(ii);

        // high-momentum e+e-/e-e-/e+e+:
        if (eleHiCandi.size()>0 && posHiCandi.size()>0) return true;
        if (eleHiCandi.size()>1 || posHiCandi.size()>1) return true;

        // high-momentum mu+mu-/mu-mu-/mu+mu+:
        if (mumHiCandi.size()>0 && mupHiCandi.size()>0) return true;
        if (mumHiCandi.size()>1 || mupHiCandi.size()>1) return true;

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
