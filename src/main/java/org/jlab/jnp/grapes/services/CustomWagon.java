/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jlab.jnp.grapes.services;

import org.jlab.jnp.hipo.data.HipoEvent;
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
    public boolean processDataEvent(HipoEvent event) {
        ParticleList pl = DataManager.getParticleList(event);
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
