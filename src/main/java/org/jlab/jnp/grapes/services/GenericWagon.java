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
import org.jlab.jnp.utils.json.Json;
import org.jlab.jnp.utils.json.JsonObject;

/**
 *
 * @author gavalian
 */
public class GenericWagon extends Wagon {
    
    private ExtendedEventFilter eventFilter         = new ExtendedEventFilter();
    private ExtendedEventFilter eventFilterForward  = new ExtendedEventFilter();
    private ExtendedEventFilter eventFilterCentral  = new ExtendedEventFilter();
    private ExtendedEventFilter eventFilterTagger   = new ExtendedEventFilter();
    private ExtendedEventFilter eventFilterElectron = new ExtendedEventFilter();
    private long        eventMask           = 0xFFFFFFFFFFFFFFFFL;
    
    public GenericWagon() {
        super("GenericWagon", "gavalian", "1.0");
    }
    public GenericWagon(String name, String author, String version) {
        super(name,author,version);
    }

    @Override
    public boolean processDataEvent(Event event, SchemaFactory factory) {        
        //HipoNode node = event.getNode(331, 1);
        
        //return true;
        
        ParticleList list = DataManager.getParticleList(event,factory);
        
        list.setStatusWord(DataManager.FORWARD);
        if(eventFilterForward.checkFinalState(list)==false) return false;
        
        list.setStatusWord(DataManager.CENTREAL);
        if(eventFilterCentral.checkFinalState(list)==false) return false;
        
        list.setStatusWord(DataManager.TAGGER);
        if(eventFilterTagger.checkFinalState(list)==false) return false;
        
        list.setStatusWord(DataManager.ANY);
        if( eventFilter.checkFinalState(list) ==false) return false;
        
        list.setStatusWord(DataManager.FORWARD);
        if( eventFilterElectron.checkElectronKinematics(list) ==false) return false;
        
        /*if(eventMask!=0xFFFFFFFFFFFFFFFFL){
            Bank trigger = new Bank(factory.getSchema("RUN::config"));
            event.read(trigger);
            long triggerWord = trigger.getLong("trigger", 0);
            if((triggerWord&eventMask)==0L) return false;
        }*/
        return true;
    }

    @Override
    public boolean init(String jsonString) {
        JsonObject jsonObj = Json.parse(jsonString).asObject();
        double beamEnergy           = jsonObj.getDouble("beamEnergy",11);
        int    targetPDG            = jsonObj.getInt("targetPDG",2212);
        String filterString         = jsonObj.getString("filter","X+:X-:Xn");
        String filterStringForward  = jsonObj.getString("forward","X+:X-:Xn");
        String filterStringCentral  = jsonObj.getString("central","X+:X-:Xn");
        String filterStringTagger   = jsonObj.getString("tagger","X+:X-:Xn");
        String filterStringElectron = jsonObj.getString("electron","");
        String eventMaskString      = jsonObj.getString("trigger","FFFFFFFF");
        
        this.eventFilter.setFilter(filterString);
        this.eventFilterForward.setFilter(filterStringForward);
        this.eventFilterCentral.setFilter(filterStringCentral);
        this.eventFilterTagger.setFilter(filterStringTagger);
        this.eventFilterElectron.setElectronFilter(beamEnergy, targetPDG, filterStringElectron);
        
        //eventMask = Long.parseLong(eventMaskString,16);
        
        System.out.println("SETTING FILTER [CENTRAL]  : " + filterStringCentral);
        System.out.println("SETTING FILTER [FORWARD]  : " + filterStringForward);
        System.out.println("SETTING FILTER [ TAGGER]  : " + filterStringTagger);
        System.out.println("SETTING FILTER [OVERALL]  : " + filterString);
        System.out.println("SETTING FILTER [ELECTRON] : " + filterStringElectron);
        //System.out.printf("SETTING EVENT MASK       : %X \n",eventMask);
        
        //System.out.println(" WAGON CONFIGURATION : set filter = " + filterString);
        return true;
    }
    
}
