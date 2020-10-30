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
import org.jlab.jnp.hipo4.data.Bank;
import org.jlab.jnp.hipo4.data.DataFrame;
import org.jlab.jnp.hipo4.data.DataFrameBuilder;
import org.jlab.jnp.hipo4.data.Event;
import org.jlab.jnp.hipo4.data.SchemaFactory;
import org.jlab.jnp.utils.file.FileUtils;
import org.jlab.jnp.utils.json.Json;
import org.jlab.jnp.utils.json.JsonObject;

/**
 *
 * @author gavalian
 */
public abstract class Wagon implements Engine {

    
    String             engineName        = "UnknownEngine";
    String             engineAuthor      = "N.T.";
    String             engineVersion     = "0.0";
    String             engineDescription = "CLARA Engine";
    
    private  int                  id = 0;
    private  String       dataFilter = "X+:X-:Xn";
    private  int     processedEvents = 0;
    private  long          eventMask = 0xFFFFFFFFFFFFFFFFL;
    volatile SchemaFactory   engineDictionary = new SchemaFactory();
    
    
    public Wagon(String name, String author, String version){        
        engineName    = name;
        engineAuthor  = author;
        engineVersion = version;
        String path   = FileUtils.getEnvironmentPath("CLAS12DIR", "etc/bankdefs/hipo4");
        engineDictionary.initFromDirectory(path);
    }
    
    abstract public boolean processDataEvent(Event event, SchemaFactory factory);
    abstract public boolean init(String jsonString);
    
    @Override
    public EngineData configure(EngineData config) {
        
        String mt = config.getMimeType();
        System.out.println(" CONFIGURATION " + this.engineName + " : type = " + mt );
        String engineConfiguration = (String) config.getData();
        System.out.println(" DATA = " + engineConfiguration);
        JsonObject jsonObj = Json.parse(engineConfiguration).asObject();
        id = jsonObj.getInt("id", 0);
        System.out.println(" CONFIG: set id = " + id);
        this.init(engineConfiguration);
        String eventMaskString      = jsonObj.getString("trigger","0x7FFFFFFFFFFFFFFF");
        if(eventMaskString.startsWith("0x")==true){
            eventMaskString = eventMaskString.substring(2);
        }
        eventMask = Long.parseLong(eventMaskString,16);
        return config;
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public EngineData execute(EngineData input) {
        
        EngineData output = input;
        String mt = input.getMimeType();
        boolean  processStatus = false;
        
        //System.out.println(" DATA TYPE = [" + mt + "]");
        Event hipoEvent = new Event();
        
        if(mt.compareTo("binary/data-hipo-frame")==0){
            
            DataFrame  frame = (DataFrame) input.getData();
            int count = frame.getEntries();
            int size  = frame.getEventBufferSize();
            DataFrameBuilder  outBuilder = new DataFrameBuilder(count, size);
            
            for(int i = 0; i < count; i++){
                byte[] data = frame.getEventCopy(i);
                hipoEvent.initFrom(data);
                
                
                //event.setSchemaFactory(engineDictionary,false);
                Bank configBank = new Bank(engineDictionary.getSchema("RUN::config"));
                hipoEvent.read(configBank);
                long triggerWord  = configBank.getLong("trigger", 0);
                
                processStatus = false;
                if((triggerWord&eventMask)!=0L){
                    processStatus = processDataEvent(hipoEvent,engineDictionary);
                } 
                
                if(processedEvents==0){
                    hipoEvent.setEventBitMask(id);
                }
                
                if(processStatus==true){
                    //event.setEventStatusBit(id);
                    hipoEvent.setEventBitMask(id);
                } else {
                    //event.unsetEventStatusBit(id);
                }
                
                if(hipoEvent.getEventTag()==1001){
                    hipoEvent.setEventBitMask(id);
                    //hipoEvent.setEventTag(0);
                    System.out.println("[WAGON] --> RECEIVED START FILE EVENT... MARKING : " + id);
                }
                
                processedEvents++;
                                
                outBuilder.addEvent(hipoEvent.getEventBuffer().array(), 
                        0, hipoEvent.getEventBufferSize());
                //outFrame.add(event.getDataBuffer());
            }
            DataFrame  outframe = outBuilder.build();
            output.setData(mt, outframe);
            return output;
        }
        
                /*
        if(mt.compareTo("binary/data-hipo")==0){
            try {
                //ByteBuffer bb = (ByteBuffer) input.getData();
               // hipoEvent = (HipoEvent) input.getData();
               // hipoEvent.setSchemaFactory(engineDictionary, false);               
                
                //dataEventHipo.initDictionary(engineDictionary);
                //dataEventHipo = new HipoDataEvent(bb.array(),this.engineDictionary);
            } catch (Exception e) {
                String msg = String.format("Error reading input event%n%n%s", ClaraUtil.reportException(e));
                output.setStatus(EngineStatus.ERROR);
                output.setDescription(msg);
                return output;
            }

            try {
                processStatus = processDataEvent(hipoEvent);
                if(processStatus==true){
                    hipoEvent.setEventStatusBit(id);
                } else {
                   // hipoEvent.unsetEventStatusBit(id);
                }
                
                output.setData(mt, hipoEvent);
            } catch (Exception e) {
                String msg = String.format("Error processing input event%n%n%s", ClaraUtil.reportException(e));
                output.setStatus(EngineStatus.ERROR);
                output.setDescription(msg);
                return output;
            }
        }*/
        return output;
    }

    @Override
    public EngineData executeGroup(Set<EngineData> set) {
        return null;
    }

    @Override
    public Set<EngineDataType> getInputDataTypes() {
        return ClaraUtil.buildDataTypes(
                Clas12Types.HIPO,
                EngineDataType.JSON,
                EngineDataType.STRING);
    }

    @Override
    public Set<EngineDataType> getOutputDataTypes() {
         return ClaraUtil.buildDataTypes(
                 EngineDataType.JSON,
                Clas12Types.HIPO,
                EngineDataType.STRING);
    }
    
    
    @Override
    public Set<String> getStates() {
        return new HashSet<String>();
    }

    @Override
    public String getDescription() {
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        return this.engineDescription;
    }

    @Override
    public String getVersion() {
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        return this.engineVersion;
    }

    @Override
    public String getAuthor() {
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        return this.engineAuthor;
    }

    @Override
    public void reset() {
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void destroy() {
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
