/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jlab.jnp.grapes.services;

import org.jlab.jnp.hipo.data.HipoEvent;
import org.jlab.jnp.hipo.data.HipoGroup;
import org.jlab.jnp.hipo.data.HipoNode;
import org.jlab.jnp.hipo.io.HipoReader;
import org.jlab.jnp.physics.EventFilter;
import org.jlab.jnp.physics.Particle;
import org.jlab.jnp.physics.ParticleList;
import org.jlab.jnp.utils.file.FileUtils;

/**
 *
 * @author gavalian
 */
public class DataManager {
    
    public static final int  TAGGER  = 1;
    public static final int FORWARD  = 2;
    public static final int CENTREAL = 3;
    public static final int      ANY = -1;
    
    public static ParticleList getParticleList(HipoEvent event){
        
        ParticleList pList = new ParticleList();
        if(event.hasGroup("REC::Particle")==false) return pList;
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
        }
        return pList;
    }
    
    
    public static void main(String[] args){
        HipoReader reader = new HipoReader();
        //reader.open( "/Users/gavalian/Work/Software/project-4a.0.0/data/dst/clas_run_3222.hipo");
        reader.open( "/Users/gavalian/Work/Software/project-4a.0.0/data/dst/clasrun_004013_all.hipo");
        
        EventFilter  filter = new EventFilter();
        EventFilter  forward = new EventFilter();
        
        filter.setFilter("22:22");
        forward.setFilter("1+:2-:X+:X-:Xn");
        while(reader.hasNext()){
            HipoEvent event = reader.readNextEvent();
            ParticleList pList = DataManager.getParticleList(event);
            pList.setStatusWord(DataManager.TAGGER);
            boolean statusT = filter.checkFinalState(pList);
            pList.setStatusWord(DataManager.FORWARD);
            boolean statusF = forward.checkFinalState(pList);
            
            if(statusT==true&&statusF==true){
                System.out.println("--------");
                System.out.println(pList.toLundString());
            }
        }
    }
}
