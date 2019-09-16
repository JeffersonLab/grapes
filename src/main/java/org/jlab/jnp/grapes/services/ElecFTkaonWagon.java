package org.jlab.jnp.grapes.services;

import org.jlab.jnp.hipo4.data.*;
import org.jlab.jnp.hipo.data.HipoNode;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * 
 * Skim for elec FT Kaon plus analysis.
 *
 */
public class ElecFTkaonWagon extends Wagon {

	public ElecFTkaonWagon(){
        super("ElecFTkaonWagon","lanza","0.1");
    }

	@Override
	public boolean init(String jsonString) {
		System.out.println("elecFTkaonWagon READY.");
		return true;
	}

	@Override
	public boolean processDataEvent(Event event, SchemaFactory factory) {

//		Bank bank = new Bank(factory.getSchema("REC::Particle"));
		Bank bankRECFT = new Bank(factory.getSchema("RECFT::Particle"));

//		event.read(bank);

		event.read(bankRECFT);

		int pid = 0;
		short status = 0;

		boolean flag_FTelec = false;
		boolean flag_kp = false;

		for (int ii = 0; ii < bankRECFT.getRows(); ii++) {
			pid = bankRECFT.getInt("pid", ii);
			status = bankRECFT.getShort("status", ii);
			
			if (pid == 11 && (int) (status / 1000) == 1)
				flag_FTelec = true;
			if (pid == 321)
				flag_kp = true;

		}

		if (flag_FTelec && flag_kp) {
			return true;
		} else {
			return false;
		}
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