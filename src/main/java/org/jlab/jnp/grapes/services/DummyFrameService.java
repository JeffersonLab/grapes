/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jlab.jnp.grapes.services;

import java.util.HashSet;
import java.util.Set;
import org.jlab.clara.base.ClaraUtil;
import org.jlab.clara.engine.Engine;
import org.jlab.clara.engine.EngineData;
import org.jlab.clara.engine.EngineDataType;
import org.jlab.jnp.grapes.io.Clas12Types;

/**
 *
 * @author gavalian
 */
public class DummyFrameService implements Engine {

    @Override
    public EngineData configure(EngineData ed) {
        return ed;
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public EngineData execute(EngineData ed) {
        return ed;
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public EngineData executeGroup(Set<EngineData> set) {
        return null;
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Set<EngineDataType> getInputDataTypes() {
        return ClaraUtil.buildDataTypes(
                 EngineDataType.JSON,
                Clas12Types.HIPOFRAME,
                EngineDataType.STRING);
    }

    @Override
    public Set<EngineDataType> getOutputDataTypes() {
        return ClaraUtil.buildDataTypes(
                 EngineDataType.JSON,
                Clas12Types.HIPOFRAME,
                EngineDataType.STRING);
    }

    @Override
    public Set<String> getStates() {
        return new HashSet<String>();        
    }

    @Override
    public String getDescription() {
        return "Dummy Frame Service";
    }

    @Override
    public String getVersion() {
        return "1.0";
    }

    @Override
    public String getAuthor() {
        return "gavalian";
    }

    @Override
    public void reset() {
        
    }

    @Override
    public void destroy() {
        
    }
    
}
