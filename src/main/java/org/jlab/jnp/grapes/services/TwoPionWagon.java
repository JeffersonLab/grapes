package org.jlab.jnp.grapes.services;

import org.jlab.jnp.hipo4.data.*;
import org.jlab.jnp.hipo.data.HipoNode;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * 
 * Skim for 2pion channel with e- in FD
 *
 * @author devita
 */

public class TwoPionWagon extends Wagon {

	public TwoPionWagon(){
        super("TwoPionWagon","devita","0.1");
    }

	@Override
	public boolean init(String jsonString) {
		System.out.println("TwoPionWagon READY.");
		return true;
	}

	@Override
	public boolean processDataEvent(Event event, SchemaFactory factory) {

//		Bank bank = new Bank(factory.getSchema("REC::Particle"));
		Bank bankREC   = new Bank(factory.getSchema("REC::Particle"));

//		event.read(bank);

		event.read(bankREC);

		boolean flag_TwoPion = false;
		int     nelec  = 0;
        int     nprot = 0;
        int     npip = 0;
        int     npim = 0;

                
                if( bankREC!=null) {
                    for (int ii = 0; ii < bankREC.getRows(); ii++) {
                            int pid    = bankREC.getInt("pid", ii);
                            int charge = bankREC.getByte("charge", ii);
                            double  px = bankREC.getFloat("px", ii);
                            double  py = bankREC.getFloat("py", ii);
                            double  pz = bankREC.getFloat("pz", ii);
                            double   p = Math.sqrt(px*px+py*py+pz*pz);
                            int status = bankREC.getShort("status", ii);

                            if (pid == 11 && (2000 <= Math.abs(status) < 4000) && p>1) nelec++; 
                            if(status>=2000) {
                                if (pid == 2212 && charge >0 )    nprot++;
                                else if(pid == 211 &&  charge>0) npip++;
                                else if(pid == -211 &&  charge<0) npim++;
                            }
                    }

                    if (nelec>=1 && ((nprot+npip)>=2 || (nprot+npip)>=2 || (npip+npim)>=2 || (nprot+npip+npim)>=3)) && (nprot+npip+npim)<=7) flag_TwoPion=true;
                }
                
                return flag_TwoPion;
	}

	private HashMap<Integer, ArrayList<Integer>> mapByIndex(HipoNode indices) {
		HashMap<Integer, ArrayList<Integer>> map = new HashMap<Integer, ArrayList<Integer>>();
		for (int ii = 0; ii < indices.getDataSize(); ii++) {
			final int index = indices.getInt(ii);
			if (!map.containsKey(index))
				map.put(index, new ArrayList<Integer>());
			map.get(index).add(ii);
		}
		return map;
	}

}
