package org.jlab.jnp.grapes.services;

import org.jlab.jnp.physics.Vector3;
import org.jlab.jnp.physics.LorentzVector;

import org.jlab.jnp.hipo4.data.Bank;
import org.jlab.jnp.hipo4.data.Event;
import org.jlab.jnp.hipo4.data.SchemaFactory;


//import org.jlab.jnp.grapes.services.*;

/**
 * 
 * DVPipPimP Skimming
 *
 * @author fxgirod
 */

public class DVKpKmPWagon extends BeamTargetWagon {

	static final double KaonMass   = 0.493677f;

	public DVKpKmPWagon() {
		super("DVKpKmPWagon","fxgirod","0.0");
	}

	public double Vangle(Vector3 v1, Vector3 v2){ 
		double res=0;
		double l1 = v1.mag();
		double l2 = v2.mag();
		if( l1*l2 > 0)res = Math.toDegrees( Math.acos( v1.dot(v2)/(l1*l2) ) );
		return res;
	}

	@Override
	public boolean processDataEvent(Event event, SchemaFactory factory) {
		LorentzVector VB = new LorentzVector(0,0,beamEnergy,beamEnergy);
		LorentzVector VT = new LorentzVector(0,0,0,targetMass);

		Bank RecPart = new Bank(factory.getSchema("REC::Particle"));
		event.read(RecPart);

		boolean hasDVKapKamP = false;
		if( RecPart!=null && RecPart.getRows()>3 ){
			int n_e = 0;
			int n_p = 0;
			int n_kap = 0;
			int n_kam = 0;
			for (int ii = 0; ii < RecPart.getRows() ; ii++) {
				int is_pid = RecPart.getInt("pid", ii);
				int stat   = Math.abs(RecPart.getShort("status", ii));
				if(stat>2000 && stat<4000 && is_pid==11)n_e++;
				if(stat>2000 && stat!=4000 && is_pid==2212)n_p++;
				if(stat>2000 && stat<4000 && is_pid==321)n_kap++;
				if(stat>2000 && stat<4000 && is_pid==-321)n_kam++;
			}
			boolean is_candidate = n_e*n_p*n_kap*n_kam>0;
			if(is_candidate){
				int[] e_ind   = new int[n_e];
				int[] p_ind   = new int[n_p];
				int[] kap_ind = new int[n_kap];
				int[] kam_ind = new int[n_kam];
				n_e = 0;
				n_p = 0;
				n_kap = 0;
				n_kam = 0;
				for (int ii = 0; ii < RecPart.getRows() ; ii++) {
					int is_pid = RecPart.getInt("pid", ii);
					int stat   = Math.abs(RecPart.getShort("status", ii));
					if(stat>2000 && stat<4000 && is_pid==11){e_ind[n_e]=ii;n_e++;}
					if(stat>2000 && stat!=4000 && is_pid==2212){p_ind[n_p]=ii;n_p++;}
					if(stat>2000 && stat<4000 && is_pid==321){kap_ind[n_kap]=ii;n_kap++;};
					if(stat>2000 && stat<4000 && is_pid==-321){kam_ind[n_kam]=ii;n_kam++;}
				}
				for (int ie = 0; ie < n_e && !hasDVKapKamP; ie++) {
					double e_px  = RecPart.getFloat("px", e_ind[ie]);
					double e_py  = RecPart.getFloat("py", e_ind[ie]);
					double e_pz  = RecPart.getFloat("pz", e_ind[ie]);

					double e_mom = Math.sqrt(e_px*e_px+e_py*e_py+e_pz*e_pz);
					if( e_mom>0.1*beamEnergy ){
						LorentzVector VE = new LorentzVector(e_px,e_py,e_pz,e_mom);
						for (int ip = 0; ip < n_p && !hasDVKapKamP; ip++) {
							double p_px  = RecPart.getFloat("px", p_ind[ip]);
							double p_py  = RecPart.getFloat("py", p_ind[ip]);
							double p_pz  = RecPart.getFloat("pz", p_ind[ip]);

							double p_ene = Math.sqrt(p_px*p_px+p_py*p_py+p_pz*p_pz+targetMass*targetMass);
							if( p_ene>0.94358 ){
								LorentzVector VP = new LorentzVector(p_px,p_py,p_pz,p_ene);
								for (int ikap = 0; ikap < n_kap && !hasDVKapKamP ; ikap++) {
									double kap_px  = RecPart.getFloat("px", kap_ind[ikap]);
									double kap_py  = RecPart.getFloat("py", kap_ind[ikap]);
									double kap_pz  = RecPart.getFloat("pz", kap_ind[ikap]);

									double kap_ene = Math.sqrt(kap_px*kap_px+kap_py*kap_py+kap_pz*kap_pz+KaonMass*KaonMass);
									if( kap_ene>0.523){
										LorentzVector VKAP = new LorentzVector(kap_px,kap_py,kap_pz,kap_ene);
										for (int ikam = 0; ikam < n_kam && !hasDVKapKamP ; ikam++) {
											double kam_px  = RecPart.getFloat("px", kam_ind[ikam]);
											double kam_py  = RecPart.getFloat("py", kam_ind[ikam]);
											double kam_pz  = RecPart.getFloat("pz", kam_ind[ikam]);

											double kam_ene = Math.sqrt(kam_px*kam_px+kam_py*kam_py+kam_pz*kam_pz+KaonMass*KaonMass);
											if(  kam_ene>0.523){
												LorentzVector VKAM = new LorentzVector(kam_px,kam_py,kam_pz,kam_ene);

												LorentzVector VPHI = new LorentzVector(0,0,0,0);
												VPHI.add(VKAP);
												VPHI.add(VKAM);

												LorentzVector Q = new LorentzVector(0,0,0,0);
												Q.add(VB);
												Q.sub(VE);
												LorentzVector W = new LorentzVector(0,0,0,0);
												W.add(Q);
												W.add(VT);

												if( -Q.mass2()>0.8 && W.mass()>1.8 ){
													LorentzVector VmissP = new LorentzVector(0,0,0,0);
													VmissP.add(W);
													VmissP.sub(VPHI);
													LorentzVector VmissKAP = new LorentzVector(0,0,0,0);
													VmissKAP.add(W);
													VmissKAP.sub(VP);
													VmissKAP.sub(VKAM);
													LorentzVector VmissKAM = new LorentzVector(0,0,0,0);
													VmissKAM.add(W);
													VmissKAM.sub(VP);
													VmissKAM.sub(VKAP);
													LorentzVector VmissAll = new LorentzVector(0,0,0,0);
													VmissAll.add(VmissP);
													VmissAll.sub(VP);

													hasDVKapKamP = true
														&& VmissAll.e() > -0.5 && VmissAll.e() < 0.5
														&& VmissAll.mass2() > -0.05 &&  VmissAll.mass2() < 0.05
														&& VmissAll.px()*VmissAll.px() + VmissAll.py()*VmissAll.py() < 0.25
														&& Vangle( VP.vect() , VmissP.vect() ) < 12
														&& Vangle( VKAP.vect() , VmissKAP.vect() ) < 12
														&& Vangle( VKAM.vect() , VmissKAM.vect() ) < 12
														&& VmissP.mass2() > 0.5 && VmissP.mass2() < 1.5
														&& VmissKAP.mass2() > 0.035 && VmissKAP.mass2() < 0.6
														&& VmissKAM.mass2() > 0.035 && VmissKAM.mass2() < 0.6
														;
												}
											}
										}
									}
								}
							}
						}

					}

				}
			}
		}
		return hasDVKapKamP;
	}
}
