package org.jlab.jnp.grapes.services;

import java.util.List;
import java.util.ArrayList;

import org.jlab.jnp.hipo4.data.Bank;
import org.jlab.jnp.hipo4.data.Event;
import org.jlab.jnp.hipo4.data.SchemaFactory;
import org.jlab.jnp.pdg.PDGDatabase;
import org.jlab.jnp.physics.LorentzVector;
import org.jlab.jnp.utils.json.Json;
import org.jlab.jnp.utils.json.JsonObject;

/**
 * targetPDG and beamEnergy are defined in YAML
 *
 * return from processDataEvent as early as possible
 */
public class SRCWagon extends BeamTargetWagon {

    
    double q2_cut       = 0;
    double xb_cut       = 0; //x-borken cut                                                   
    double pmiss_cut    = 0;   //missing momentum cut                                             
    double prec_cut     = 0; //recoil momentum cut                                              
    double missm_cut    = 999; //missing mass cut
    double pq_angle_cut = 999;  //angle between pmiss & q degs.
    double pq_cut_min   = 0; // |p|/|q| cut
    double pq_cut_max   = 999; // |p|/|q| cut
    static final double mass_nucl  = PDGDatabase.getParticleById(2212).mass();
    static final double pi = 3.141592653;


    public SRCWagon() {
        super("SRCWagon","Bobcat","0.0");
    }

    @Override
    public boolean init(String jsonString) {
        JsonObject jsonObj = Json.parse(jsonString).asObject();
        beamEnergy = jsonObj.getDouble("beamEnergy",-1.0);
        targetPDG = jsonObj.getInt("targetPDG",0);

        q2_cut = jsonObj.getDouble("Q2Cut",0);
        xb_cut = jsonObj.getDouble("XBCut",0);
        pmiss_cut    = jsonObj.getDouble("MissMomCut",0);
        missm_cut    = jsonObj.getDouble("MissMassCut",999);
        pq_angle_cut = jsonObj.getDouble("pqAngleCut",999);
        pq_cut_min   = jsonObj.getDouble("pqCut_min",0);
        pq_cut_max   = jsonObj.getDouble("pqCut_max",999);
	prec_cut     = jsonObj.getDouble("RecoilMomCut",0);

        if (beamEnergy>0 && targetPDG!=0) {
            targetMass = PDGDatabase.getParticleById(targetPDG).mass();
            if (targetMass>0) {
                targetVector = new LorentzVector(0,0,0,targetMass);
                beamVector = new LorentzVector(0,0,beamEnergy,beamEnergy);
                String beamTargetSetup = "BeamEnergy="+beamEnergy+" , TargetPDG="+targetPDG+" , TargetMass="+targetMass;
                String cutSetup = "Q2 >"+q2_cut+" , Xb >"+xb_cut+" , Pmiss >"+pmiss_cut+" , MissM <"+missm_cut+" , pq Angle <"+pq_angle_cut+" , |p|/|q| >"+pq_cut_min+" , |p|/|q| <"+pq_cut_max+" , Precoil >"+prec_cut;

                System.out.println(engineName +" READY with "+beamTargetSetup);
                System.out.println(engineName +" READY with Cuts "+cutSetup);
                return true;
            }
        }
        System.out.println("Error initializing "+engineName+" due to beamEnergy or targetPDG YAML parameters.");
        return false;
    }


    @Override
    public boolean processDataEvent(Event event, SchemaFactory factory) {

        Bank particles = new Bank(factory.getSchema("REC::Particle"));
	event.read(particles);
        // get scattered electron, requiring it to be the trigger particle:
	if(particles.getRows() == 0) return false;
        if (particles.getInt("pid",0) != 11) return false;
	if (particles.getInt("status",0)/1000 != -2) return false;
	LorentzVector electron = Util.getLorentzVector(particles,0);
        // check Q2:
	LorentzVector q = new LorentzVector(electron);
	q.sub(beamVector);
        final double Q2 = -q.mass2();
	if (Q2 < q2_cut) return false;

        // check xB:
        final double xB = Q2 /(2*mass_nucl*(beamEnergy-electron.e()));
	if (xB < xb_cut) return false;

	boolean lead_proton = false;
	int lead_idx = -1;
	int num_protons = 0;

        for (int i=1; i<particles.getRows(); ++i) {
            if (particles.getInt("pid",i) == 2212) {
                // check this proton with all previous ones:
		LorentzVector proton = Util.getLorentzVector(particles, i);
		LorentzVector missing_mom = getMissingVector(electron, proton);

		num_protons++;
		if(missing_mom.mass() < missm_cut){
                    if(missing_mom.p() > pmiss_cut){
                        if(missing_mom.angle(proton) < pq_angle_cut*(pi/180)){
			    if(missing_mom.p()/q.p() > pq_cut_min){
				if(missing_mom.p()/q.p() < pq_cut_max){
				    lead_proton = true;
				    lead_idx = i;
				} 	
			    }
			}
                    }
                }

	    }
	}

	if(lead_proton && num_protons >= 2)
	    return true;
	else
	    return false;
    }
}
