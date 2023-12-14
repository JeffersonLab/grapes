package org.jlab.jnp.grapes.services;


import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import org.jlab.jnp.hipo4.data.Event;
import org.jlab.jnp.hipo4.data.SchemaFactory;

/**
 * 
 * Hack fix CCDB mistakes in first RG-A cooking:
 *
 * 1. photon sampling fraction (modify REC::Particle.px/py/pz for FD photons)
 * 2. FTOF resolution units (modify REC::Particle.chi2pid for FD hadrons)
 *
 * This wagon never keeps events for itself (i.e. processDataEvent always returns false).
 *
 * @author baltzell
 */
public class TemporaryCorrRGA extends Wagon {

    static final float phoSamp[]={0.250f,1.029f,-0.015f,0.00012f};
   
    static final boolean retVal=false;

    public TemporaryCorrRGA(){
        super("TemporaryCorrRGA","baltzell","0.1");
    }

    @Override
    public boolean init(String jsonString) {
        System.out.println("TemporaryCorrRGA READY.");
        return true;
    }

    @Override
    public boolean processDataEvent(Event event, SchemaFactory factory) {
        /*
        if (!event.hasGroup("REC::Event") ||
            !event.hasGroup("REC::Particle") ||
            !event.hasGroup("REC::Calorimeter")) return retVal;

        // check run number:
        //if (event.getNode(330,1).getInt() < 1234) return retVal;

        HipoGroup recParticle = event.getGroup("REC::Particle");
        
        HipoNode pindices = event.getNode(332,2);

        // abort if either bank is empty:
        if (recParticle==null || pindices==null || pindices.getDataSize()<1) return retVal;

        // load map from REC::Particle rows to REC::Calorimeter rows:
        HashMap<Integer,ArrayList<Integer>> part2calo = this.mapByIndex(pindices);

        boolean modified=false;

        for (int ipart=0; ipart<recParticle.getMaxSize(); ipart++) {
         
            // ignore all but FD:
            if ((int)(recParticle.getNode("status").getInt(ipart)/1000) != 2) continue;
           
            final int pid = recParticle.getNode("pid").getInt(ipart);

            // the chi2pid correction:
            if (Math.abs(pid) > 22) {
                final float chi2pid = recParticle.getNode("chi2pid").getFloat(ipart);
                recParticle.getNode("chi2pid").setFloat(ipart,chi2pid/1000);
                modified=true;
            }

            // the sampling fraction correction:
            else if (pid == 22 && part2calo.containsKey(ipart)) {
                ECAL ecal = new ECAL(part2calo.get(ipart),event);
                scaleMomentum(ecal.getCorrectedPhotonEnergy(),ipart,recParticle);
                modified=true;
            }
        }

        if (modified) {
            event.removeGroup(331);
            event.writeGroup(recParticle);
        }
        */
        return retVal;
    }
    /*
    private HashMap<Integer,ArrayList<Integer>> mapByIndex(HipoNode indices) {
        HashMap<Integer,ArrayList<Integer>> map=new HashMap<Integer,ArrayList<Integer>>();
        for (int ii=0; ii<indices.getDataSize(); ii++) {
            final int index = indices.getInt(ii);
            if (!map.containsKey(index)) map.put(index,new ArrayList<Integer>());
            map.get(index).add(ii);
        }
        return map;
    }

    private class ECAL {
        private float epcal=-1;
        private float eecin=-1;
        private float eecou=-1;
        private float etot=0;
        public ECAL(ArrayList<Integer>icalos, HipoEvent event) {
            HipoNode detectors = event.getNode(332,3);
            HipoNode layers    = event.getNode(332,5);
            HipoNode energies  = event.getNode(332,6);
            for (int icalo : icalos) {
                if (detectors.getInt(icalo) != 7) continue;
                switch (layers.getByte(icalo)) {
                    case 1:
                        epcal = energies.getFloat(icalo);
                        etot += epcal;
                        break;
                    case 4:
                        eecin = energies.getFloat(icalo);
                        etot += eecin;
                        break;
                    case 7:
                        eecou = energies.getFloat(icalo);
                        etot += eecou;
                        break;
                    default:
                        break;
                }
            }
        }
        public float getCorrectedPhotonEnergy() {
            if (etot > 0)
                return etot /= phoSamp[0] * (phoSamp[1] + phoSamp[2]/etot + phoSamp[3]/etot/etot);
            return -1;
        }
    }

    private void scaleMomentum(final float pcorr,final int ipart, HipoGroup recParticle) {
        if (pcorr<=0) return;
        final float px = recParticle.getNode("px").getFloat(ipart);
        final float py = recParticle.getNode("py").getFloat(ipart);
        final float pz = recParticle.getNode("pz").getFloat(ipart);
        final float scale = pcorr / (float)Math.sqrt(px*px+py*py+pz*pz);
        recParticle.getNode("px").setFloat(ipart,px*scale);
        recParticle.getNode("py").setFloat(ipart,py*scale);
        recParticle.getNode("pz").setFloat(ipart,pz*scale);
    }*/

}
