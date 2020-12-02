package org.jlab.jnp.grapes.services;

import java.util.HashMap;
import org.jlab.jnp.physics.EventFilter;
import org.jlab.jnp.physics.Particle;
import org.jlab.jnp.physics.ParticleFilter.ParticlePropertyFilter;
import org.jlab.jnp.physics.ParticleList;

/**
 *
 * @author devita
 */
public class ExtendedEventFilter extends EventFilter {
    

    double beamEnergy = 11;
    int    targetPDG  = 2212;
    HashMap<String, ParticlePropertyFilter> allKineFilters    = new HashMap();
    HashMap<String, ParticlePropertyFilter> activeKineFilters = new HashMap();
    
    
       
    public ExtendedEventFilter(String filter){
        super(filter);
        this.init();
    }
    
    public ExtendedEventFilter()
    {
        super();
        this.init();
    }
        
    private void init() {
        ParticlePropertyFilter Q2 = new ParticlePropertyFilter(1, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
        ParticlePropertyFilter W  = new ParticlePropertyFilter(1, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
        ParticlePropertyFilter P  = new ParticlePropertyFilter(1, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
        ParticlePropertyFilter VZ = new ParticlePropertyFilter(1, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
        allKineFilters.put("Q2", Q2);
        allKineFilters.put("W",  W);
        allKineFilters.put("P",  P);
        allKineFilters.put("VZ", VZ);
    }
    
    public boolean checkElectronKinematics(ParticleList plist)
    {
        boolean filter = false;
        
        if(activeKineFilters.isEmpty()) { 
            filter = true;
        }
        else {
            Particle beam     = new Particle(11,0,0,beamEnergy,0,0,0);
            Particle target   = Particle.createWithPid(targetPDG, 0,0,0, 0,0,0);
            for(int i=0; i<plist.count(); i++) {
                Particle electron = plist.getByPid(11, i);
                if(electron != null) { 
                    Particle q = new Particle();
                    q.copy(beam);
                    q.combine(electron, -1);
                    Particle w = new Particle();
                    w.copy(target);
                    w.combine(q, +1);    
                    if(this.activeKineFilters.get("Q2").chackRange(-q.mass2())  && 
                       this.activeKineFilters.get("W").chackRange(w.mass())     && 
                       this.activeKineFilters.get("P").chackRange(electron.p()) && 
                       this.activeKineFilters.get("VZ").chackRange(electron.vz())) {
                        filter = true;
                        break;
                    }
                }
            }
        }
 
        return filter;
    }
  
    public void setElectronFilter(double energy, int pdg, String opt) {
        this.beamEnergy = energy;
        this.targetPDG  = pdg;
        clear();
        if(!opt.isEmpty()) {
            for(String cut : opt.split("&&")) {
                String[] nameValuePair = cut.split("[<>]");
                if (nameValuePair.length == 2) {
                    String name  = nameValuePair[0].trim();
                    String value = nameValuePair[1].trim();
                    if (!allKineFilters.containsKey(name)) {
                        throw new RuntimeException("Invalid filter variable name ("+name+") in "+opt);
                    }
                    else {
                        activeKineFilters.put(name,allKineFilters.get(name));
                    }
                    try {
                        if (cut.contains("<")) {
                            activeKineFilters.get(name).propertyMax = Double.parseDouble(value);
                        }
                        else {
                            activeKineFilters.get(name).propertyMin = Double.parseDouble(value);
                        }
                    }
                    catch (Exception e) {
                        throw new RuntimeException("Invalid filter variable value ("+value+") in "+opt);
                    }
                }
                else {
                    throw new RuntimeException("Invalid filter syntax: "+opt);
                }
            }
            System.out.println(this.toString());
        }
    }
    
    @Override
    public String toString()
    {
        StringBuilder str = new StringBuilder();
        str.append(String.format("KINEMATICS:---> BEAM ENERGY : %8.3f GeV\n" +
                                 "                TARGET PDG : %6d",
                                this.beamEnergy,this.targetPDG));
        for(String key : activeKineFilters.keySet()) {
            str.append(String.format("\n                " +
                    key + " RANGE : %.2f-%.2f",activeKineFilters.get(key).propertyMin,activeKineFilters.get(key).propertyMax));
        }
        return str.toString();
    }
    
    public static void main(String[] args) {
        ExtendedEventFilter filter = new ExtendedEventFilter();
        filter.setElectronFilter(11, 2212, "Q2>0.9&&Q2<1.5&&W>1&&W<2.5");
    }
}
