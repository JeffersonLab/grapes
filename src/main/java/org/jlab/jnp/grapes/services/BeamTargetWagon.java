package org.jlab.jnp.grapes.services;

import org.jlab.jnp.pdg.PDGDatabase;
import org.jlab.jnp.physics.LorentzVector;
import org.jlab.jnp.utils.json.Json;
import org.jlab.jnp.utils.json.JsonObject;

/**
 *
 * Just to generalize custom wagons requiring beam and target info,
 * until we have a better way to retrieve that info at run time.
 * 
 * Requires parameters "beamEnergy" and "targetPDG" to be defined in YAML.
 * 
 * @author baltzell
 */
public abstract class BeamTargetWagon extends Wagon {
    
    int targetPDG = 0;
    double targetMass = -1;
    double beamEnergy = -1;
    LorentzVector targetVector = null;
    LorentzVector beamVector = null;
    
    public BeamTargetWagon(String name, String author, String version){        
        super(name,author,version);
    }

    @Override
    public boolean init(String jsonString) {
        JsonObject jsonObj = Json.parse(jsonString).asObject();
        beamEnergy = jsonObj.getDouble("beamEnergy",-1.0);
        targetPDG = jsonObj.getInt("targetPDG",0);
        if (beamEnergy>0 && targetPDG!=0) {
            targetMass = PDGDatabase.getParticleById(targetPDG).mass();
            if (targetMass>0) {
                targetVector = new LorentzVector(0,0,0,targetMass);
                beamVector = new LorentzVector(0,0,beamEnergy,beamEnergy);
                String beamTargetSetup = "EB="+beamEnergy+" , TID="+targetPDG+" , TM="+targetMass;
                System.out.println(engineName +" READY with "+beamTargetSetup);
                return true;
            }
        }
        System.out.println("Error initializing "+engineName+" due to beamEnergy or targetPDG YAML parameters.");
        return false;
    }

    public LorentzVector getMissingVector(LorentzVector... finalState) {
        LorentzVector missing = new LorentzVector(targetVector);
        missing.add(beamVector);
        for (int i=0; i<finalState.length; i++) missing.sub(finalState[i]);
        return missing;
    }

}
