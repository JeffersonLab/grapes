package org.jlab.jnp.grapes.services;

import org.jlab.jnp.pdg.PDGDatabase;
import org.jlab.jnp.utils.json.Json;
import org.jlab.jnp.utils.json.JsonObject;

import org.jlab.jnp.physics.Vector3;
import org.jlab.jnp.physics.LorentzVector;
import org.jlab.jnp.physics.Particle;

import org.jlab.jnp.hipo4.data.Bank;
import org.jlab.jnp.hipo4.data.Event;
import org.jlab.jnp.hipo4.data.SchemaFactory;


//import org.jlab.jnp.grapes.services.*;


/**
 * 2 pi Skimming
 *
 * @author golovach
 */

public class TwoPionWagonRGK extends BeamTargetWagon {

  	static final double ElectronMass = 0.000510998928f;
	static final double ProtonMass   = 0.938272046f;
	static final double PionMass     = 0.13957018f;
	//beamEnergy comes from the configuration .yaml file


	public TwoPionWagonRGK() {
		super("TwoPionWagonRGK","fxgirod,golovach","0.0");
	}


	@Override
	public boolean processDataEvent(Event event, SchemaFactory factory) {
		
		//LorentzVector VB = new LorentzVector(0,0,beamEnergy,beamEnergy);
		//LorentzVector VT = new LorentzVector(0,0,0,targetMass);
		//System.out.println(" EBEAM " + beamEnergy);
 
		Bank bank = new Bank(factory.getSchema("REC::Particle"));
		event.read(bank);
		    		if( bank==null || bank.getRows()==0) return false; // events with some info but no particles

                if( bank.getRows() < 3) return false;


                LorentzVector vE   = new LorentzVector();
                LorentzVector vP   = new LorentzVector();
                LorentzVector vPip = new LorentzVector();
                LorentzVector vPim = new LorentzVector();

		Particle elecDATA = new Particle(0,-10000,-10000,-10000,-10000,-10000,-10000);
		Particle protDATA = new Particle(0,-10000,-10000,-10000,-10000,-10000,-10000);
		Particle pipDATA = new Particle(0,-10000,-10000,-10000,-10000,-10000,-10000);
		Particle pimDATA = new Particle(0,-10000,-10000,-10000,-10000,-10000,-10000);
		 

		Particle elecDATA_array[] = new Particle[100];for(int i=0;i<100;i++){elecDATA_array[i]=new Particle(0,-10000,-10000,-10000,-10000,-10000,-10000);}
		Particle protDATA_array[] = new Particle[100];for(int i=0;i<100;i++){protDATA_array[i]=new Particle(0,-10000,-10000,-10000,-10000,-10000,-10000);}
		Particle pipDATA_array[] = new Particle[100];for(int i=0;i<100;i++){pipDATA_array[i]=new Particle(0,-10000,-10000,-10000,-10000,-10000,-10000);}
		Particle pimDATA_array[] = new Particle[100];for(int i=0;i<100;i++){pimDATA_array[i]=new Particle(0,-10000,-10000,-10000,-10000,-10000,-10000);}
		  
		  
		//DATI RICOSTRUITI
		byte charge = 0; int pid = 0; short status = 0; float beta = 0; float px = 0; float py = 0; float pz = 0; float vx = 0; float vy = 0; float vz = 0; float p = 0; float chi2pid=0;
		//double beamEnergy=7.5460;
		//		double beamEnergy=6.53536;
		
		int n_e = 0;
		int n_p = 0;
		int n_pip = 0;
		int n_pim = 0;

		
		for (int ii=0; ii<bank.getRows(); ii++) {
		    charge = bank.getByte("charge", ii);   
		    pid    = bank.getInt("pid",ii);
		    status = bank.getShort("status",ii);
		    beta = bank.getFloat("beta",ii); 
		    px   = bank.getFloat("px",ii); 
		    py   = bank.getFloat("py",ii); 
		    pz   = bank.getFloat("pz",ii); 
		    vx   = bank.getFloat("vx",ii); 
		    vy   = bank.getFloat("vy",ii); 
		    vz   = bank.getFloat("vz",ii); 
		    p    = (float) Math.sqrt(px*px+py*py+pz*pz);
		           
		           
		    chi2pid= bank.getFloat("chi2pid",ii);
		    Particle recParticle = new Particle(pid,px,py,pz,vx,vy,vz);

		    //  System.out.println(recParticle.pid()+" pid, px "+recParticle.px()+", py "+recParticle.py()+" pid, pz "+recParticle.pz());
		    switch(pid){
		    case 211:if(chi2pid<=5){pipDATA_array[n_pip] = recParticle;n_pip++;pipDATA = recParticle;}break;
		    case 11:if(chi2pid<=5 && Math.abs(status)>=2000 && Math.abs(status)<4000){elecDATA_array[n_e] = recParticle; n_e++;}break;
		    case 2212:if(chi2pid<=5){protDATA_array[n_p] = recParticle;n_p++; protDATA = recParticle;}break;
		    case -211:if(chi2pid<=5){pimDATA_array[n_pim] = recParticle;n_pim++; pimDATA = recParticle;}break;
		    }		                     
		}  
		  
		boolean notTooMany = ((n_p+n_pip+n_pim)<=7);	
		  
		if(!notTooMany)return false;

		if(n_e>0 && n_pip>0 && n_p>0 && n_pim>0){
		    for(int y=0; y<n_e;y++)
			for(int yy=0; yy<n_pip;yy++)
			    for(int yyy=0; yyy<n_p;yyy++)
				for(int yyyy=0;yyyy<n_pim;yyyy++){
				    vE.setPxPyPzM(elecDATA_array[y].px(),
                         			  elecDATA_array[y].py(),
						  elecDATA_array[y].pz(),
						  ElectronMass);
				    vP.setPxPyPzM(protDATA_array[yyy].px(),
						  protDATA_array[yyy].py(),
						  protDATA_array[yyy].pz(),
						  ProtonMass);
				    vPip.setPxPyPzM(pipDATA_array[yy].px(),
						    pipDATA_array[yy].py(),
						    pipDATA_array[yy].pz(),
						    PionMass);
				    vPim.setPxPyPzM(pimDATA_array[yyyy].px(),
						    pimDATA_array[yyyy].py(),
						    pimDATA_array[yyyy].pz(),
						    PionMass);

		    double mm2_e_p_pip_pim = 100.;

		    double mm2_e_pip_pim = 100.;

		    double mm2_e_p_pim = 100.;

		    double mm2_e_p_pip = 100.;



                                    if(elecDATA_array[y].pid()==11 && protDATA_array[yyy].pid()==2212 && pipDATA_array[yy].pid()==211 && pimDATA_array[yyyy].pid()==-211) mm2_e_p_pip_pim = calcMissMass4part(beamEnergy, vE, vP, vPip, vPim);

                                    if(elecDATA_array[y].pid()==11 && pipDATA_array[yy].pid()==211 && pimDATA_array[yyyy].pid()==-211) mm2_e_pip_pim = calcMissMass3part(beamEnergy, vE, vPip, vPim);

                                    if(elecDATA_array[y].pid()==11 && protDATA_array[yyy].pid()==2212 && pimDATA_array[yyyy].pid()==-211) mm2_e_p_pim = calcMissMass3part(beamEnergy, vE, vP, vPim);

                                    if(elecDATA_array[y].pid()==11 && protDATA_array[yyy].pid()==2212 && pipDATA_array[yy].pid()==211) mm2_e_p_pip = calcMissMass3part(beamEnergy, vE, vP, vPip);




 if(elecDATA_array[y].pid()==11 && protDATA_array[yyy].pid()==2212 && pipDATA_array[yy].pid()==211 && pimDATA_array[yyyy].pid()==-211 && mm2_e_p_pip_pim >= -0.02 && mm2_e_p_pip_pim <= 0.02) return true;
 else if(elecDATA_array[y].pid()==11 && protDATA_array[yyy].pid()==2212 && pimDATA_array[yyyy].pid()==-211 && -0.2<=mm2_e_p_pim && mm2_e_p_pim<=0.5)return true;
 else if(elecDATA_array[y].pid()==11 && pipDATA_array[yy].pid()==211 && pimDATA_array[yyyy].pid()==-211 && 0.4<=mm2_e_pip_pim && mm2_e_pip_pim<=1.6)return true;
 else if(elecDATA_array[y].pid()==11 && protDATA_array[yyy].pid()==2212 && pipDATA_array[yy].pid()==211 && -0.2<=mm2_e_p_pip && mm2_e_p_pip<=0.5)return true;
		}}
	else if(n_e>0 && n_pip>0 && n_p>0 && n_pim==0){
		    for(int y=0;y<n_e;y++)
			for(int yy=0;yy<n_pip;yy++)
			    for(int yyy=0;yyy<n_p;yyy++){
				vE.setPxPyPzM(elecDATA_array[y].px(),
					      elecDATA_array[y].py(),
					      elecDATA_array[y].pz(),
					      ElectronMass);
				vP.setPxPyPzM(protDATA_array[yyy].px(),
					      protDATA_array[yyy].py(),
					      protDATA_array[yyy].pz(),
					      ProtonMass);
				vPip.setPxPyPzM(pipDATA_array[yy].px(),
						pipDATA_array[yy].py(),
						pipDATA_array[yy].pz(),
						PionMass);
				double mm2_e_p_pip = calcMissMass3part(beamEnergy, vE, vP, vPip);

				if(elecDATA_array[y].pid()==11 && protDATA_array[yyy].pid()==2212 && pipDATA_array[yy].pid()==211 && -0.2<=mm2_e_p_pip && mm2_e_p_pip<=0.5)return true; 
			    } 
		}
		else if(n_e>0 && n_pip>0 && n_p==0 && n_pim>0){
		    for(int y=0;y<n_e;y++)
			for(int yy=0;yy<n_pip;yy++)
			    for(int yyyy=0;yyyy<n_pim;yyyy++){
				 
                                vE.setPxPyPzM(elecDATA_array[y].px(),
                                              elecDATA_array[y].py(),
                                              elecDATA_array[y].pz(),
                                              ElectronMass);
                                vPim.setPxPyPzM(pimDATA_array[yyyy].px(),
                                              pimDATA_array[yyyy].py(),
                                              pimDATA_array[yyyy].pz(),
                                              PionMass);
                                vPip.setPxPyPzM(pipDATA_array[yy].px(),
                                                pipDATA_array[yy].py(),
                                                pipDATA_array[yy].pz(),
                                                PionMass);
                                double mm2_e_pip_pim = calcMissMass3part(beamEnergy, vE, vPip, vPim);

				if(elecDATA_array[y].pid()==11 && pipDATA_array[yy].pid()==211 && pimDATA_array[yyyy].pid()==-211 && 0.4<=mm2_e_pip_pim && mm2_e_pip_pim<=1.6)return true; 
			    } 
		}
		else if(n_e>0 && n_pip==0 && n_p>0 && n_pim>0){
		    for(int y=0;y<n_e;y++)
			for(int yyy=0;yyy<n_p;yyy++)
			    for(int yyyy=0;yyyy<n_pim;yyyy++){
				vE.setPxPyPzM(elecDATA_array[y].px(),
                                              elecDATA_array[y].py(),
                                              elecDATA_array[y].pz(),
                                              ElectronMass);
                                vP.setPxPyPzM(protDATA_array[yyy].px(),
                                              protDATA_array[yyy].py(),
                                              protDATA_array[yyy].pz(),
                                              ProtonMass);
                                vPim.setPxPyPzM(pimDATA_array[yyyy].px(),
                                                pimDATA_array[yyyy].py(),
                                                pimDATA_array[yyyy].pz(),
                                                PionMass);
				double mm2_e_p_pim = calcMissMass3part(beamEnergy, vE, vP, vPim);

				if(elecDATA_array[y].pid()==11 && protDATA_array[yyy].pid()==2212 && pimDATA_array[yyyy].pid()==-211 && -0.2<=mm2_e_p_pim && mm2_e_p_pim<=0.5)return true;
		  
			    }}


                return false;
	} // end processDataEvent method
	
	// calculation of the missing mass squared
  	public double calcMissMass3part(double Ebeam, LorentzVector part1, LorentzVector part2, 
	                                              LorentzVector part3) {
		double E  = Ebeam+ProtonMass  - part1.e()  - part2.e()  - part3.e();
		double px = 0.                - part1.px() - part2.px() - part3.px();
		double py = 0.                - part1.py() - part2.py() - part3.py();
		double pz = Ebeam             - part1.pz() - part2.pz() - part3.pz();
		return E*E - px*px - py*py - pz*pz;
  	}
  	public double calcMissMass4part(double Ebeam, LorentzVector part1, LorentzVector part2, 
	                                              LorentzVector part3, LorentzVector part4) {
		double E  = Ebeam+ProtonMass  - part1.e()  - part2.e()  - part3.e()  - part4.e() ;
		double px = 0.                - part1.px() - part2.px() - part3.px() - part4.px();
		double py = 0.                - part1.py() - part2.py() - part3.py() - part4.py();
		double pz = Ebeam             - part1.pz() - part2.pz() - part3.pz() - part4.pz();
		return E*E - px*px - py*py - pz*pz;
  	}


	
}  // end class TWOPIONskim
