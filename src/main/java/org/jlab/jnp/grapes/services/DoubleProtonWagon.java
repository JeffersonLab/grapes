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
public class DoubleProtonWagon extends BeamTargetWagon {

    
    double q2_cut       = 0;
    double xb_cut       = 0; //x-borken cut                                                   
    static final double mass_nucl  = PDGDatabase.getParticleById(2212).mass();
    static final double pi = 3.141592653;


    public DoubleProtonWagon() {
        super("DoubleProtonWagon","Bobcat","0.0");
    }

    @Override
    public boolean init(String jsonString) {
        JsonObject jsonObj = Json.parse(jsonString).asObject();
        beamEnergy = jsonObj.getDouble("beamEnergy",-1.0);
        targetPDG = jsonObj.getInt("targetPDG",0);

        q2_cut = jsonObj.getDouble("Q2Cut",0);
        xb_cut = jsonObj.getDouble("XBCut",0);

        if (beamEnergy>0 && targetPDG!=0) {
            targetMass = PDGDatabase.getParticleById(targetPDG).mass();
            if (targetMass>0) {
                targetVector = new LorentzVector(0,0,0,targetMass);
                beamVector = new LorentzVector(0,0,beamEnergy,beamEnergy);
                String beamTargetSetup = "BeamEnergy="+beamEnergy+" , TargetPDG="+targetPDG+" , TargetMass="+targetMass;
                String cutSetup = "Q2 >"+q2_cut+" , Xb >"+xb_cut;

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

	int num_protons = 0;
        for (int i=1; i<particles.getRows(); ++i) {
            if (particles.getInt("pid",i) == 2212) {
		num_protons++;
	    }
	}

	if(num_protons >= 2)
	    return true;
	else
	    return false;
    }
}
