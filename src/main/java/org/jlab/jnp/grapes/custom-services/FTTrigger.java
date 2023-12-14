package org.jlab.jnp.grapes.services;

import org.jlab.jnp.hipo4.data.*;
import org.jlab.jnp.hipo.data.HipoNode;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * 
 * Skim for 2pi channel with e- in FT for MesonX
 *
 * @author devita
 */

public class FTTrigger extends Wagon {

        final static int ftTriggerBit=27;
        
	public FTTrigger(){
            super("FTTrigger","devita","0.1");
        }

	@Override
	public boolean init(String jsonString) {
		System.out.println("FTTrigger READY.");
		return true;
	}

	@Override
	public boolean processDataEvent(Event event, SchemaFactory factory) {

		Bank bank = new Bank(factory.getSchema("RUN::config"));

		event.read(bank);

                boolean wagonStatus = false;
                
                if(bank!= null) {
                    long triggerWord = bank.getLong("trigger",0) & 0xFFFFFFFF;
                    boolean[] trigger_bits = new boolean[32];
                    for (int i=31; i>=0; i--) {
                        trigger_bits[i] = (triggerWord & (1 << i)) != 0;	      
                    }
                    if(trigger_bits[ftTriggerBit]) wagonStatus=true;
                }
                
                return wagonStatus;
	}

}