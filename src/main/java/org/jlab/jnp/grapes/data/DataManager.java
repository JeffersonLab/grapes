/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jlab.jnp.grapes.data;

import org.jlab.jnp.hipo4.data.Bank;
import org.jlab.jnp.hipo4.data.Event;
import org.jlab.jnp.hipo4.data.SchemaFactory;
import org.jlab.jnp.hipo4.io.HipoReader;
import org.jlab.jnp.physics.EventFilter;
import org.jlab.jnp.physics.Particle;
import org.jlab.jnp.physics.ParticleList;
import org.jlab.jnp.physics.PhysicsEvent;

/**
 *
 * @author gavalian
 */
public class DataManager {
    
    public static final int  TAGGER  = 1;
    public static final int FORWARD  = 2;
    public static final int CENTREAL = 3;
    public static final int      ANY = -1;
    
    
    public static final int    ECAL_TIME = 1001;
    public static final int  ECAL_ENERGY = 1002;
    public static final int    FTOF_TIME = 1003;
    public static final int  FTOF_ENERGY = 1004;
    
    public static final String  PARTICLE_BANK = "REC::Particle";
    public static final String      ECAL_BANK = "REC::Calorimeter";
    public static final String       TOF_BANK = "REC::Scintillator";
    public static final String  CHERENKOV_BANK = "REC::Cherenkov";
        
    
    public static ParticleList getParticleList(Event event, SchemaFactory factory){
        
        Bank group = new Bank(factory.getSchema("REC::Particle"));
        ParticleList pList = new ParticleList();
        event.read(group);
        
        int nrows = group.getRows();
        
        for(int i = 0; i < nrows; i++){
            int      pid = group.getInt("pid", i);
            int   status = group.getInt("status", i);
            status = Math.abs(status);
            int detector = DataManager.TAGGER;
            if(status>=2000&&status<3000) detector = DataManager.FORWARD;
            if(status>=4000) detector = DataManager.CENTREAL;
            
            Particle p = new Particle();
            if(pid!=0){
                p.initParticle(pid,
                        group.getFloat("px",i), 
                        group.getFloat("py",i), 
                        group.getFloat("pz",i),
                        group.getFloat("vx",i), 
                        group.getFloat("vy",i), 
                        group.getFloat("vz",i)
                );
            } else {
                p.initParticleWithPidMassSquare(pid, 0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
            }
            p.setStatus(detector);
            pList.add(p);
        }
        
        /*if(event.hasGroup("REC::Particle")==false) return pList;
        HipoGroup group  = event.getGroup("REC::Particle");
        HipoNode  nodePx = group.getNode("px");
        HipoNode  nodePy = group.getNode("py");
        HipoNode  nodePz = group.getNode("pz");
        HipoNode  nodeVx = group.getNode("vx");
        HipoNode  nodeVy = group.getNode("vy");
        HipoNode  nodeVz = group.getNode("vz");
        
        HipoNode  nodeStatus = group.getNode("status");
        
        
        int nrows = group.getMaxSize();
        
        for(int i = 0; i < nrows; i++){
            int pid = group.getNode("pid").getInt(i);
            int status = nodeStatus.getInt(i);
            int detector = DataManager.TAGGER;
            if(status>=2000&&status<3000) detector = DataManager.FORWARD;
            if(status>=4000) detector = DataManager.CENTREAL;
            
            Particle p = new Particle();
            if(pid!=0){
                p.initParticle(pid,
                        nodePx.getFloat(i), 
                        nodePy.getFloat(i), 
                        nodePz.getFloat(i),
                        nodeVx.getFloat(i), 
                        nodeVy.getFloat(i), 
                        nodeVz.getFloat(i)
                );
            } else {
                p.initParticleWithPidMassSquare(pid, 0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
            }
            p.setStatus(detector);
            pList.add(p);
        }*/
        return pList;
    }
    
    
    
    public PhysicsEvent getPhysicsEvent(double energy, Event event, SchemaFactory factory){
        
        ParticleList pList = DataManager.getParticleList(event,factory);
        PhysicsEvent pEvent = new PhysicsEvent();
        for(int i = 0; i < pList.count(); i++){
            pEvent.addParticle(pList.get(i));            
        }                
        return pEvent;
    }
    
    public static void main(String[] args){
        
        
        HipoReader reader = new HipoReader();
        //reader.open( "/Users/gavalian/Work/Software/project-4a.0.0/data/dst/clas_run_3222.hipo");
        //reader.open( "/Users/gavalian/Work/DataSpace/clas12/4013/out_clas_004013.evio.00000-00009.hipo");
        reader.open( "/Users/gavalian/Work/DataSpace/clas12/4152/clas_004152.evio.93.recon.hipo");
        
        SchemaFactory factory = reader.getSchemaFactory();
        
        EventFilter  filter = new EventFilter();
        EventFilter  forward = new EventFilter();
        
        filter.setFilter("22:22");
        //forward.setFilter("1+:2-:X+:X-:Xn");
        forward.setFilter("11:X+:X-:Xn");
        System.out.println(forward.summary());
        System.out.println(forward.toString());
        
        Event event = new Event();
        
        
        while(reader.hasNext()){
            reader.nextEvent(event);
            ParticleList pList = DataManager.getParticleList(event,factory);
            
//System.out.println(pList.toString());
            
            pList.setStatusWord(DataManager.TAGGER);
            boolean statusT = filter.checkFinalState(pList);
            pList.setStatusWord(DataManager.FORWARD);
            boolean statusF = forward.checkFinalState(pList);
            int countE = pList.countByPid(11);
            
            
            //if(countE>0){
            //    System.out.println("FILTERED = ");
            //    System.out.println(pList.toString());
            //}
            
            if(statusF==true){
                System.out.println("FILTERED = ");
                System.out.println(pList.toString());
            }
            
            if(statusT==true){
                //System.out.println("TAGGER = ");
                //System.out.println(pList.toString());
            }
            /*
            if(statusT==true&&statusF==true){
                System.out.println("--------");
                System.out.println(pList.toLundString());
            }*/
        }
    }
}
