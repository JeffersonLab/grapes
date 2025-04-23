/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jlab.jnp.grapes.services;

import org.jlab.jnp.hipo4.data.Event;
import org.jlab.jnp.hipo4.data.SchemaFactory;
import org.jlab.jnp.physics.ParticleList;

import org.jlab.jnp.hipo4.data.Bank;
import org.jlab.jnp.hipo4.io.HipoReader;
import org.jlab.jnp.physics.EventFilter;
import org.jlab.jnp.physics.Particle;
import org.jlab.jnp.physics.PhysicsEvent;

import org.jlab.jnp.utils.json.Json;
import org.jlab.jnp.utils.json.JsonObject;

/**
 *
 * @author gotra
 */

public class LambdaWagon extends Wagon {
    volatile String filterStringForward = null;
    volatile String filterStringTagger = null;
    volatile String filterStringCentral = null;
    volatile String filterStringAny = null;
    private ExtendedEventFilter eventFilterForward  = new ExtendedEventFilter();
    private ExtendedEventFilter eventFilterCentral  = new ExtendedEventFilter();
    private ExtendedEventFilter eventFilterTagger   = new ExtendedEventFilter();
    private ExtendedEventFilter eventFilterAny      = new ExtendedEventFilter();
    public static final int  TAGGER  = 1;
    public static final int FORWARD  = 2;
    public static final int CENTRAL  = 3;
    public static final int     ANY  = 4;

    public LambdaWagon(){
        super("LambdaWagon","myname","0.1");
    }

    @Override
    public boolean processDataEvent(Event event, SchemaFactory factory) {
        ParticleList pl = this.getParticleList(event,factory);

        pl.setStatusWord(this.FORWARD);
        if(eventFilterForward.checkFinalState(pl)==false) return false;

        pl.setStatusWord(this.TAGGER);
        if(eventFilterTagger.checkFinalState(pl)==false) return false;

        pl.setStatusWord(this.CENTRAL);
        if(eventFilterCentral.checkFinalState(pl)==false) return false;

        pl.setStatusWord(this.ANY);
        if(eventFilterAny.checkFinalState(pl)==false) return false;
        return true;
    }

    @Override
    public boolean init(String jsonString) {
        // Initialization code goes here, this shows
        // how JASON can be parsed.
        JsonObject jsonObj = Json.parse(jsonString).asObject();
        filterStringForward  = jsonObj.getString("forward","X+:X-:Xn");
        filterStringTagger   = jsonObj.getString("tagger","X+:X-:Xn");
        filterStringCentral   = jsonObj.getString("central","X+:X-:Xn");
        filterStringAny   = jsonObj.getString("noetrig","X+:X-:Xn");
        this.eventFilterForward.setFilter(filterStringForward);
        this.eventFilterTagger.setFilter(filterStringTagger);
        this.eventFilterCentral.setFilter(filterStringCentral);
        this.eventFilterAny.setFilter(filterStringAny);
        return true;
    }

    public static ParticleList getParticleList(Event event, SchemaFactory factory){

      boolean triggerFT = false;
      boolean triggerFD = false;
      Bank recFTParticleBank = new Bank(factory.getSchema("RECFT::Particle"));
      Bank recParticleBank =  new Bank(factory.getSchema("REC::Particle"));
      ParticleList pList = new ParticleList();
      event.read(recFTParticleBank);
      event.read(recParticleBank);
      int nrows = recParticleBank.getRows();
      if(nrows > 0 && recParticleBank.getInt("pid", 0) == 11) triggerFD = true;
      int nrowsFT = recFTParticleBank.getRows();
      if(nrows == 0 && nrowsFT == 0) return pList;
      if(nrowsFT > 0) triggerFT = true;
      int detector = TAGGER;
      if(triggerFT == true) {
        for(int i = 0; i < nrowsFT; i++){
          int      pid = recFTParticleBank.getInt("pid", i);
          int   status = recFTParticleBank.getInt("status", i);
          status = Math.abs(status);
          detector = TAGGER;
          if(status>=4000) detector = CENTRAL; // status 3

          Particle p = new Particle();
          if(pid!=0){
            p.initParticle(pid,
                  recFTParticleBank.getFloat("px",i),
                  recFTParticleBank.getFloat("py",i),
                  recFTParticleBank.getFloat("pz",i),
                  0.0,
                  0.0,
                  0.0
                  );
          }
          else {
            p.initParticleWithPidMassSquare(pid, 0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
          }
          p.setStatus(detector);
          pList.add(p);
        }
        pList.setStatusWord(TAGGER);
      }
      else { // no RECFT::Particle bank
        for(int i = 0; i < nrows; i++){
          int      pid = recParticleBank.getInt("pid", i);
          int   status = recParticleBank.getInt("status", i);
          status = Math.abs(status);
          if(triggerFD) detector = FORWARD;
          else detector = ANY;
          if(status>=4000) detector = CENTRAL; // status 3
          Particle p = new Particle();
          if(pid!=0){
            p.initParticle(pid,
                    recParticleBank.getFloat("px",i),
                    recParticleBank.getFloat("py",i),
                    recParticleBank.getFloat("pz",i),
                    recParticleBank.getFloat("vx",i),
                    recParticleBank.getFloat("vy",i),
                    recParticleBank.getFloat("vz",i)
                    );
          }
          else {
            p.initParticleWithPidMassSquare(pid, 0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
          }
          p.setStatus(detector);
          pList.add(p);
        }
        if(triggerFD) {
          pList.setStatusWord(FORWARD);
        }
        else {
          pList.setStatusWord(ANY);
        }
      }

      return pList;
    }

    public static void main(String[] args){

        HipoReader reader = new HipoReader();
        reader.open( "/work/clas12/users/gotra/LAMBDASKIM/Workflow/testwfft/rec_clas_005419.evio.00060-00062_.hipo");
        SchemaFactory factory = reader.getSchemaFactory();

        EventFilter  tagger = new EventFilter();
        EventFilter  forward = new EventFilter();
        EventFilter  any = new EventFilter();

        tagger.setFilter("11:2212:-211:X+:X-:Xn");
        System.out.println("ftsummary " + tagger.summary());
        System.out.println(tagger.toString());

        forward.setFilter("11:2212:-211:X+:X-:Xn");
        System.out.println("fdsummary " + forward.summary());
        System.out.println(forward.toString());

        any.setFilter("11:2212:-211:X+:X-:Xn");
        System.out.println("any " + any.summary());
        System.out.println(any.toString());

        Event event = new Event();

        int eventNumber = 0;
        while(reader.hasNext()){
            eventNumber++;
            reader.nextEvent(event);
            ParticleList pList = LambdaWagon.getParticleList(event,factory);
            System.out.println("event: " + eventNumber);

            pList.setStatusWord(LambdaWagon.TAGGER);
            boolean statusT = tagger.checkFinalState(pList);
            pList.setStatusWord(LambdaWagon.FORWARD);
            boolean statusF = forward.checkFinalState(pList);
            pList.setStatusWord(LambdaWagon.ANY);
            boolean statusA = any.checkFinalState(pList);

//            int statusWord = pList.getStatusWord();
//            System.out.println("StatusWord " + statusWord + " FT: " + statusT + " FD: " + statusF);

            if(statusF==true){
                System.out.println("FORWARD = ");
                System.out.println(pList.toString());
            }

            if(statusT==true){
                System.out.println("TAGGER = ");
                System.out.println(pList.toString());
            }

            if(statusA==true){
                System.out.println("ANY = ");
                System.out.println(pList.toString());
            }
        }
    }
}
