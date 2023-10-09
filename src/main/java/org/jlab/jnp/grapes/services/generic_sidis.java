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
 * Skim for elec FD for sidis analysis
 * @author thayward
 *
 */
public class generic_sidis extends BeamTargetWagon {
    
    public generic_sidis(){
	super("generic_sidis","thayward","0.0");
    }
    
    @Override
    public boolean processDataEvent(Event event, SchemaFactory factory) {

	// beamEnergy and targetMass defined in yaml
	LorentzVector lv_beam = new LorentzVector(0,0,beamEnergy,beamEnergy);
	LorentzVector lv_target = new LorentzVector(0,0,0,targetMass);
	Bank RecPart = new Bank(factory.getSchema("REC::Particle"));
	
	event.read(RecPart);
	
	int pid = 0;
	short status = 0;
	boolean flag_elec = false;
	
	for (int ii = 0; ii < RecPart.getRows(); ii++) {
	    pid = RecPart.getInt("pid", ii);
	    status = (short) Math.abs(RecPart.getShort("status", ii));
	    if (pid == 11 && status > 2000 && status < 4000){
		float e_px  = RecPart.getFloat("px", ii);
		float e_py  = RecPart.getFloat("py", ii);
		float e_pz  = RecPart.getFloat("pz", ii);
		float e_p =(float) Math.sqrt(e_px*e_px+e_py*e_py+e_pz*e_pz);
		float e_vz  = RecPart.getFloat("vz", ii);
		// define Lorentz vector for scattered electron
		LorentzVector lv_e = new LorentzVector(e_px,e_py,e_pz,e_p);

		// define Lorentz vector for virtual photon
		LorentzVector lv_q = new LorentzVector(0,0,0,0); 
		lv_q.add(lv_beam); lv_q.sub(lv_e);
		// define DIS variables
		float Q2 = (float)(-lv_q.mass2());
		float nu = (float)(lv_beam.e()-lv_e.e());
		float y =(float)( nu/lv_beam.e());
		// define Lorentz vector for W, the "hadronic mass"
		LorentzVector lv_W = new LorentzVector(0,0,0,0); 
                lv_W.add(lv_q); lv_W.add(lv_target);
		float W = (float)lv_W.mass();
		if (Q2 > 1.0 && y < 0.8 && W > 2.0 && e_vz > -25 && e_vz < 20){
		    flag_elec = true;
		}
	    }    
	}
	return flag_elec;
    }    
}
