package org.jlab.jnp.grapes.services;



import java.util.ArrayList;
import java.util.HashMap;
import org.jlab.jnp.hipo4.data.Event;
import org.jlab.jnp.hipo4.data.SchemaFactory;

/**
 * 
 * Skim for JPsi/TCS analysis group, E12-12-001.
 *
 * Until standard filters provide access to momentum and ECAL energy.
 *
 * @author baltzell
 */
public class JpsiTcsWagon extends Wagon {

    static final float muEcoutMax = 0.085f;
    static final float muEcinMax = 0.055f;
    static final float muPcalMax = 0.045f;
    static final float momHigh = 2.0f;
    
    public JpsiTcsWagon(){
        super("JpsiTcsWagon","baltzell","0.1");
    }

    @Override
    public boolean init(String jsonString) {
        System.out.println("JpsiTcsWagon READY.");
        return true;
    }

    @Override
    public boolean processDataEvent(Event event, SchemaFactory factory) {

       /* if (!event.hasGroup("REC::Particle") ||
            !event.hasGroup("REC::Calorimeter")) return false;

        // REC::Particle:
        HipoNode pids    = event.getNode(331,1);
        HipoNode charges = event.getNode(331,8);
        HipoNode statii  = event.getNode(331,11);

        // REC::Calorimeter:
        HipoNode pindices = event.getNode(332,2);

        // abort if either bank is empty:
        if (pids==null || pindices==null || pids.getDataSize()<1 || pindices.getDataSize()<1) return false;

        // load map from REC::Particle rows to REC::Calorimeter rows:
        HashMap<Integer,ArrayList<Integer>> part2calo = this.mapByIndex(pindices);
        
        int npos=0,nposFD=0;
        ArrayList<Integer> eleCandi = new ArrayList<Integer>();
        ArrayList<Integer> posCandi = new ArrayList<Integer>();
        ArrayList<Integer> mupCandi = new ArrayList<Integer>();
        ArrayList<Integer> mumCandi = new ArrayList<Integer>();

        for (int ipart=0; ipart<pids.getDataSize(); ipart++) {
           
            if (charges.getInt(ipart) == 0) continue;

            final boolean isFD = (int)(statii.getInt(ipart)/1000) == 2;

            // count positives:
            if (charges.getInt(ipart) > 0) {
                if (isFD) nposFD++;
                npos++;
            }

            // electron/positron candidates based on EB pid:
            if (isFD) {
                if      (pids.getInt(ipart)== 11) eleCandi.add(ipart);
                else if (pids.getInt(ipart)==-11) posCandi.add(ipart);
            }

            // muon candidates based on EC energy:
            if (part2calo.containsKey(ipart)) {
                ECAL ecal = new ECAL(part2calo.get(ipart),event);
                if (ecal.isMIP()) {
                    if (charges.getInt(ipart) > 0) mupCandi.add(ipart);
                    else                           mumCandi.add(ipart);
                }
            }
        }

        // abort asap:
        if ( (eleCandi.size()==0 || posCandi.size()==0) &&
             (mumCandi.size()==0 || mupCandi.size()==0) ) return false;

        ///////////////////////////////////////////////////////////////////
        ///////////////////////////////////////////////////////////////////

        // TCS, e+e- and 2 positives:
        if (eleCandi.size()>0 && posCandi.size()>0 && npos>1) return true;

        // J/Psi, mu+mu-p:
        if (mumCandi.size()>0 && mupCandi.size()>0 && nposFD>1) return true;

        // e/mu candidates with "high" momentum:
        ArrayList<Integer> eleHiCandi = new ArrayList<Integer>();
        ArrayList<Integer> posHiCandi = new ArrayList<Integer>();
        ArrayList<Integer> mupHiCandi = new ArrayList<Integer>();
        ArrayList<Integer> mumHiCandi = new ArrayList<Integer>();
        for (int ii : eleCandi) if (this.getMomentum(ii,event) > momHigh) eleHiCandi.add(ii);
        for (int ii : posCandi) if (this.getMomentum(ii,event) > momHigh) posHiCandi.add(ii);
        for (int ii : mumCandi) if (this.getMomentum(ii,event) > momHigh) mumHiCandi.add(ii);
        for (int ii : mupCandi) if (this.getMomentum(ii,event) > momHigh) mupHiCandi.add(ii);

        // J/Psi, high-momentum e+e-:
        if (eleHiCandi.size()>0 && posHiCandi.size()>0) return true;

        // J/Psi, high-momentum mu+mu-:
        if (mumHiCandi.size()>0 && mupHiCandi.size()>0) return true;
        */
        return false;
    }

    /*private HashMap<Integer,ArrayList<Integer>> mapByIndex(HipoNode indices) {
        HashMap<Integer,ArrayList<Integer>> map=new HashMap<Integer,ArrayList<Integer>>();
        for (int ii=0; ii<indices.getDataSize(); ii++) {
            final int index = indices.getInt(ii);
            if (!map.containsKey(index)) map.put(index,new ArrayList<Integer>());
            map.get(index).add(ii);
        }
        return map;
    }*/
    /*
    private class ECAL {
        private float epcal=-1;
        private float eecin=-1;
        private float eecou=-1; 
        public ECAL(ArrayList<Integer>icalos, HipoEvent event) {
            HipoNode detectors = event.getNode(332,3);
            HipoNode layers    = event.getNode(332,5);
            HipoNode energies  = event.getNode(332,6);
            for (int icalo : icalos) {
                if (detectors.getInt(icalo) != 7) continue;
                switch (layers.getByte(icalo)) {
                    case 1:
                        epcal = energies.getFloat(icalo);
                        break;
                    case 4:
                        eecin = energies.getFloat(icalo);
                        break;
                    case 7:
                        eecou = energies.getFloat(icalo);
                        break;
                    default:
                        break;
                }
            }
        }
        public boolean isMIP() {
            if (this.epcal<0 || this.eecin<0 || this.eecou<0) return false;
            if (this.epcal > muPcalMax) return false;
            if (this.eecin > muEcinMax) return false;
            if (this.eecou > muEcoutMax) return false;
            return true;
        }
    }

    private float getMomentum(final int ipart, HipoEvent event) {
        final float px = event.getNode(331,2).getFloat(ipart);
        final float py = event.getNode(331,3).getFloat(ipart);
        final float pz = event.getNode(331,4).getFloat(ipart);
        return (float)Math.sqrt(px*px + py*py + pz*pz);
    }
    */
}
