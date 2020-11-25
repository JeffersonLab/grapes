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
    HashMap<String, ParticlePropertyFilter> kineFilters = new HashMap();
    
    
       
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
        kineFilters.put("Q2", Q2);
        kineFilters.put("W",  W);
    }
    
    public boolean checkElectronKinematics(ParticleList plist)
    {
        boolean filter = false;
        
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
                if(kineFilters.get("Q2").chackRange(-q.mass2()) && this.kineFilters.get("W").chackRange(w.mass())) {
                    filter = true;
                    break;
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
                    if (!kineFilters.containsKey(name)) {
                        throw new RuntimeException("Invalid filter variable name ("+name+") in "+opt);
                    }
                    try {
                        if (cut.contains("<")) {
                            kineFilters.get(name).propertyMax = Double.parseDouble(value);
                        }
                        else {
                            kineFilters.get(name).propertyMin = Double.parseDouble(value);
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
        for(String key : kineFilters.keySet()) {
            str.append(String.format("\n                " +
                    key + " RANGE : %.2f-%.2f",kineFilters.get(key).propertyMin,kineFilters.get(key).propertyMax));
        }
        return str.toString();
    }
    
    public static void main(String[] args) {
        ExtendedEventFilter filter = new ExtendedEventFilter();
        filter.setElectronFilter(11, 2212, "Q2>0.9&&Q2<1.5&&W>1&&W<2.5");
    }
}
