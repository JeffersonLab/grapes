package org.jlab.jnp.grapes.services;

import org.jlab.jnp.hipo4.data.*;

import org.jlab.jnp.physics.LorentzVector;

/**
 * 
 * Skim for pi0p channel:
 * 1 electron, 1 proton, >=0 photons, >=0 neutrons, 
 * -0.25 <= MM^2_{ep->e'p'X} <= 0.25 GeV^2, 
 * W >= 1.1 GeV, 
 * Q^2 >= 0.45 GeV^2
 *
 * @author ryzhkov
 *
 */

public class Pi0PWagon extends BeamTargetWagon 
{

	static final double MM2_MIN = -0.25d;
	static final double MM2_MAX = 0.25d;
	static final double W_MIN = 1.1d;
    static final double Q2_MIN = 0.45d;
    static final double electronMass = 0.000511d;
    //beamEnergy and targetMass are from BeamTargetWagon

	public Pi0PWagon()
	{
        super("Pi0PWagon","ryzhkov","0.1");
    }

	@Override
	public boolean processDataEvent(Event event, SchemaFactory factory) 
	{
		Bank bankREC = new Bank(factory.getSchema("REC::Particle"));
		event.read(bankREC);

        // If the bank is empty don't save the event
        if (bankREC.getRows() == 0) return false;
        
        LorentzVector Lbeam = new LorentzVector(0d, 0d, beamEnergy, beamEnergy); //beam (beamEnergy from BeamTargetWagon)	
		LorentzVector Ltarget = new LorentzVector(0d, 0d, 0d, targetMass); //target=proton (targetMass from BeamTargetWagon)
		LorentzVector Le = new LorentzVector(); //final state electron
		LorentzVector Lp = new LorentzVector(); //final state proton

        int e_count = 0; //electron counter
        int p_count = 0; //proton counter
        int other_count = 0; //other particle counter (not electron, proton, photon, neutron or unidentified)
        
        for (int ii = 0; ii < bankREC.getRows(); ii++) 
        {
        	int pID = bankREC.getInt("pid", ii);

        	if (pID == 11 /*Electron*/) 
        	{
        		e_count++; 
        		if (e_count > 1) return false; 
        		Le.setPxPyPzM((double)bankREC.getFloat("px",ii), (double)bankREC.getFloat("py",ii), (double)bankREC.getFloat("pz",ii), electronMass);
        	} 
            else if (pID == 2212 /*Proton*/) 
            {
            	p_count++; 
            	if (p_count > 1) return false; 
            	Lp.setPxPyPzM((double)bankREC.getFloat("px",ii), (double)bankREC.getFloat("py",ii), (double)bankREC.getFloat("pz",ii), targetMass);
            }
            else if ( (pID != 22 /*Gamma*/) && (pID != 2112 /*Neutron*/) && (pID != 0 /*Unidentified*/) ) 
            {
            	other_count++; 
            	if (other_count > 0) return false;
            }
 		}

    	if ( (e_count != 1) || (p_count != 1) || (other_count != 0) ) return false;
    	
    	LorentzVector Lq = new LorentzVector(Lbeam);
    	Lq.sub(Le);
    	double Q2 = -Lq.mass2();
    	
    	LorentzVector LW = new LorentzVector(Ltarget);
    	LW.add(Lq);
    	double W = LW.mass();
    	
    	LorentzVector LMM = new LorentzVector(LW);
    	LMM.sub(Lp);
    	double MM2 = LMM.mass2();
    	
    	if ( (W < W_MIN) || (Q2 < Q2_MIN) || (MM2 < MM2_MIN) || (MM2 > MM2_MAX) ) return false;
    	else return true;
	}

}
