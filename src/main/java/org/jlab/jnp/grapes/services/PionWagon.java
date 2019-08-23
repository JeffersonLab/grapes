package org.jlab.jnp.grapes.services;

import org.jlab.jnp.hipo4.data.Bank;
import org.jlab.jnp.hipo4.data.Event;
import org.jlab.jnp.hipo4.data.SchemaFactory;
import java.util.ArrayList;
import java.util.HashMap;
//import org.jlab.jnp.hipo4.data.Event;
//import org.jlab.jnp.hipo4.data.SchemaFactory;

/**
 * 
 * Skim for studying high-energy pion rejection for improving electron identification 
 *
 * Until standard filters provide access to momentum and ECAL energy.
 *
 * @author baltzell,jnewton
 */
public class PionWagon extends Wagon {

    static final float ELE_MOM_LOW = 0.5f;
    static final float ELE_MOM_HIGH = 4.5f;
    static final float PION_LOW = 5.0f;
    static final float NEUTRON_LOW = 0.7f;
    static final float NEUTRON_HIGH = 1.3f;
    
    public PionWagon(){
        super("PionWagon","baltzell","0.2");
    }

    @Override
    public boolean init(String jsonString) {
        System.out.println("PionWagon READY.");
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
        
        int nposFD=0, nnegFD=0;
        ArrayList<Integer> eleCandi = new ArrayList<>();
        ArrayList<Integer> posCandi = new ArrayList<>();
	ArrayList<Integer> negCandi = new ArrayList<>();

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

            // count negatives:
            if (charge < 0) {
                if (isFD) nnegFD++;
                nneg++;
            }

            // electron/positive candidates based on EB pid:
            if (isFD) {
                if      (pid==11) eleCandi.add(ipart);
                else if (pid==211 || pid==-11) posCandi.add(ipart);
            }

            // electron/negative candidates based on EB pid:                                                                                                                                        
            if (isFD) {
                if      (pid== 11) eleCandi.add(ipart);
                else if (pid==-211 || (pid==11 && ipart>0)) negCandi.add(ipart);
            }

        }

        // abort asap:
        if ( (eleCandi.isEmpty() && posCandi.isEmpty() && negCandi.isEmpty()) return false;

        ///////////////////////////////////////////////////////////////////
        ///////////////////////////////////////////////////////////////////

  
        // Positive Pion:
        if (eleCandi.size()>0 && posCandi.size()>0) return true;
	
	//Negative Pion:
        if (eleCandi.size()>0 && negCandi.size()>0) return true;

        // electrons, negatives, and positives within momentum cuts:
        ArrayList<Integer> eleHiCandi = new ArrayList<>();
        ArrayList<Integer> posHiCandi = new ArrayList<>();
	ArrayList<Integer> negHiCandi = new ArrayList<>();
  
        for (int ii : eleCandi) if (this.getMomentum(ii,particles) > ELE_MOM_LOW && this.getMomentum(ii,particles) < ELE_MOM_HIGH) eleHiCandi.add(ii);
        for (int ii : posCandi) if (this.getMomentum(ii,particles) > PION_LOW) posHiCandi.add(ii);
        for (int ii : negCandi) if (this.getMomentum(ii,particles) > PION_LOW) negHiCandi.add(ii);

  
        // e-, negative
	if (eleHiCandi.size()>0 && negHiCandi.size()>0) return true;
	
       ArrayList<Integer> posHiNeutron = new ArrayList<>();
	     
       for(int ii : eleCandi) {
	      for(int jj:  posCandi) {
		  if (this.getMissingMass(ii,jj,particles))>NEUTRON_LOW && this.getMissingMass(ii;jj)<NEUTRON_HIGH) posHiNeutron.add(jj);
	       }
	   }

	//electron, positive track, and correct missing mass 
	if(eleHiCandi.size()>0 && posHiNeutron.size()>0) return true;

        return false;
    }



    private float getMomentum(final int ipart, Bank particles) {
        final float px = particles.getFloat("px",ipart);
        final float py = particles.getFloat("py",ipart);
        final float pz = particles.getFloat("pz",ipart);
        return (float)Math.sqrt(px*px + py*py + pz*pz);
    }

    private float getMissingMass(final int ipart1, final int ipart2, Bank particles) {
	//Calculate Missing Mass of Neutron
	 
	final float beam_energy = 10.6;
	final float mass_proton = 0.938;

	final float electron_px = particles.getFloat("px",ipart1);
        final float electron_py = particles.getFloat("py",ipart1);
	final float electron_pz = particles.getFloat("pz",ipart1);
        final float electron_energy = Math.sqrt(electron_px*electron_px + electron_py*electron_py + electron_pz*electron_pz);

	final float positive_px = particles.getFloat("px",ipart2);
	final float positive_py = particles.getFloat("py",ipart2);
	final float positive_pz = particles.getFloat("pz",ipart2);
	final float positive_energy = Math.sqrt(positive_px*positive_px + positive_py*positive_py + positive_pz*positive_pz);

	float missing_energy = beam_energy + mass_proton - (electron_energy + positive_energy);
	float missing_px = electron_px + positive_px;
	float missing_py = electron_py + positive_py;
	float missing_pz = beam_energy - (electron_pz + positive_pz);
	float missing_p = Math.sqrt(missing_px*missing_px + missing_py*missing_py + missing_pz*missing_pz);
	float missing_mass = Math.sqrt(missing_energy*missing_energy - missing_p*missing_p);
	 
	return missing_mass;
    }

}
