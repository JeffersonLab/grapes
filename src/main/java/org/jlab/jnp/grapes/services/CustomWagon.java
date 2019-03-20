/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jlab.jnp.grapes.services;

import org.jlab.jnp.grapes.data.DataManager;
import org.jlab.jnp.hipo4.data.Event;
import org.jlab.jnp.hipo4.data.SchemaFactory;
import org.jlab.jnp.physics.ParticleList;

/**
 *
 * @author gavalian
 */
public class CustomWagon extends Wagon {

    public CustomWagon(){
        super("CustomWagon","myname","0.1");
    }
    
    @Override
    public boolean processDataEvent(Event event, SchemaFactory factory) {
        ParticleList pl = DataManager.getParticleList(event,factory);
        if(pl.countByCharge(-1)<1) return false;
        if(pl.countByCharge(+1)<1) return false;
        if(pl.countByChargeMinMom(-1, 1.5)<1) return false;
        if(pl.countByChargeMinMom(+1, 1.5)<1) return false;
        return true;
    }

    @Override
    public boolean init(String jsonString) {
        // Initialization code goes here, this shows
        // how JASON can be parsed.
        // JsonObject jsonObj = Json.parse(jsonString).asObject();
        // String filterString = jsonObj.getString("myconfig","11:35:56");
        return true;
    }
    
}
