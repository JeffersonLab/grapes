package org.jlab.jnp.grapes.services;
import org.jlab.jnp.physics.Vector3;
import org.jlab.jnp.physics.LorentzVector;
import org.jlab.jnp.hipo4.data.*;
import org.jlab.jnp.hipo.data.HipoNode;
import org.jlab.jnp.hipo4.data.Bank;
import org.jlab.jnp.hipo4.data.Event;
import org.jlab.jnp.hipo4.data.SchemaFactory;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * 
 * Skim for elec FD Kaon plus analysis.
 *
 */
public class ElecFDKPlusWagon extends BeamTargetWagon {
    
    public ElecFDKPlusWagon(){
	super("ElecFDKPlusWagon","markov","0.1");
    }
    
    
    @Override
    public boolean processDataEvent(Event event, SchemaFactory factory) {

	LorentzVector VB = new LorentzVector(0,0,beamEnergy,beamEnergy);
	LorentzVector VT = new LorentzVector(0,0,0,targetMass);
	LorentzVector Elec = new LorentzVector(0,0,0,0);
	Bank RecPart = new Bank(factory.getSchema("REC::Particle"));	
	
	event.read(RecPart);
	
	int pid = 0;
	short status = 0;
	float chi2 = 0;
	float w = 0;
	boolean flag_elec = false;
	boolean flag_kp = false;
	
	for (int ii = 0; ii < RecPart.getRows(); ii++) {
	    pid = RecPart.getInt("pid", ii);
	    status = (short) Math.abs(RecPart.getShort("status", ii));
	    chi2 = (float) RecPart.getFloat("chi2pid", ii);
	    if (pid == 11 && status > 2000 && status < 4000){
		double e_px  = RecPart.getFloat("px", ii);
		double e_py  = RecPart.getFloat("py", ii);
		double e_pz  = RecPart.getFloat("pz", ii);
		double e_mom = Math.sqrt(e_px*e_px+e_py*e_py+e_pz*e_pz);
		LorentzVector VE = new LorentzVector(e_px,e_py,e_pz,e_mom);
		LorentzVector Q = new LorentzVector(0,0,0,0);
		Q.add(VB);
		Q.sub(VE);
		LorentzVector W = new LorentzVector(0,0,0,0);
		W.add(Q);
		W.add(VT);
		if (W.mass() > 1.3 && W.mass() < 3.2){
		    flag_elec = true;
		}
	    }
	    if (pid == 321 && Math.abs(chi2) < 10)
		flag_kp = true;	    
	}
	
	if (flag_elec && flag_kp) {
	    return true;
	} else {
	    return false;
	}
    }
    
    private HashMap<Integer, ArrayList<Integer>> mapByIndex(HipoNode indices) {
	HashMap<Integer, ArrayList<Integer>> map = new HashMap<Integer, ArrayList<Integer>>();
	for (int ii = 0; ii < indices.getDataSize(); ii++) {
	    final int index = indices.getInt(ii);
	    if (!map.containsKey(index))
		map.put(index, new ArrayList<Integer>());
	    map.get(index).add(ii);
	}
	return map;
    }
    
}
