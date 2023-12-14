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

public class DVPipPimPWagon extends BeamTargetWagon {

	static final double PionMass   = 0.13957f;

	public DVPipPimPWagon() {
		super("DVPipPimPWagon","fxgirod","0.0");
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

		boolean hasDVPipPimP = false;
		if( RecPart!=null && RecPart.getRows()>3 ){
			int n_e = 0;
			int n_p = 0;
			int n_pip = 0;
			int n_pim = 0;
			for (int ii = 0; ii < RecPart.getRows() ; ii++) {
				int is_pid = RecPart.getInt("pid", ii);
				int stat   = Math.abs(RecPart.getShort("status", ii));
				if(stat>2000 && stat<4000 && is_pid==11)n_e++;
				if(stat>2000 && stat!=4000 && is_pid==2212)n_p++;
				if(stat>2000 && stat<4000 && is_pid==211)n_pip++;
				if(stat>2000 && stat<4000 && is_pid==-211)n_pim++;
			}
			boolean is_candidate = n_e*n_p*n_pip*n_pim>0;
			if(is_candidate){
				int[] e_ind   = new int[n_e];
				int[] p_ind   = new int[n_p];
				int[] pip_ind = new int[n_pip];
				int[] pim_ind = new int[n_pim];
				n_e = 0;
				n_p = 0;
				n_pip = 0;
				n_pim = 0;
				for (int ii = 0; ii < RecPart.getRows() ; ii++) {
					int is_pid = RecPart.getInt("pid", ii);
					int stat   = Math.abs(RecPart.getShort("status", ii));
					if(stat>2000 && stat<4000 && is_pid==11){e_ind[n_e]=ii;n_e++;}
					if(stat>2000 && stat!=4000 && is_pid==2212){p_ind[n_p]=ii;n_p++;}
					if(stat>2000 && stat<4000 && is_pid==211){pip_ind[n_pip]=ii;n_pip++;};
					if(stat>2000 && stat<4000 && is_pid==-211){pim_ind[n_pim]=ii;n_pim++;}
				}
				for (int ie = 0; ie < n_e && !hasDVPipPimP; ie++) {
					double e_px  = RecPart.getFloat("px", e_ind[ie]);
					double e_py  = RecPart.getFloat("py", e_ind[ie]);
					double e_pz  = RecPart.getFloat("pz", e_ind[ie]);

					double e_mom = Math.sqrt(e_px*e_px+e_py*e_py+e_pz*e_pz);
					if( e_mom>0.1*beamEnergy ){
						LorentzVector VE = new LorentzVector(e_px,e_py,e_pz,e_mom);
						for (int ip = 0; ip < n_p && !hasDVPipPimP; ip++) {
							double p_px  = RecPart.getFloat("px", p_ind[ip]);
							double p_py  = RecPart.getFloat("py", p_ind[ip]);
							double p_pz  = RecPart.getFloat("pz", p_ind[ip]);

							double p_ene = Math.sqrt(p_px*p_px+p_py*p_py+p_pz*p_pz+targetMass*targetMass);
							if( p_ene>0.94358){
								LorentzVector VP = new LorentzVector(p_px,p_py,p_pz,p_ene);
								for (int ipip = 0; ipip < n_pip && !hasDVPipPimP ; ipip++) {
									double pip_px  = RecPart.getFloat("px", pip_ind[ipip]);
									double pip_py  = RecPart.getFloat("py", pip_ind[ipip]);
									double pip_pz  = RecPart.getFloat("pz", pip_ind[ipip]);

									double pip_ene = Math.sqrt(pip_px*pip_px+pip_py*pip_py+pip_pz*pip_pz+PionMass*PionMass);

									if( pip_ene>0.3){
										LorentzVector VPIP = new LorentzVector(pip_px,pip_py,pip_pz,pip_ene);
										for (int ipim = 0; ipim < n_pim && !hasDVPipPimP ; ipim++) {
											double pim_px  = RecPart.getFloat("px", pim_ind[ipim]);
											double pim_py  = RecPart.getFloat("py", pim_ind[ipim]);
											double pim_pz  = RecPart.getFloat("pz", pim_ind[ipim]);

											double pim_ene = Math.sqrt(pim_px*pim_px+pim_py*pim_py+pim_pz*pim_pz+PionMass*PionMass);

											if( pim_ene>0.3){
												LorentzVector VPIM = new LorentzVector(pim_px,pim_py,pim_pz,pim_ene);

												LorentzVector VRHO0 = new LorentzVector(0,0,0,0);
												VRHO0.add(VPIP);
												VRHO0.add(VPIM);

												LorentzVector Q = new LorentzVector(0,0,0,0);
												Q.add(VB);
												Q.sub(VE);
												LorentzVector W = new LorentzVector(0,0,0,0);
												W.add(Q);
												W.add(VT);

												if( -Q.mass2()>0.8 && W.mass()>1.8 ){
													LorentzVector VmissP = new LorentzVector(0,0,0,0);
													VmissP.add(W);
													VmissP.sub(VRHO0);
													LorentzVector VmissPIP = new LorentzVector(0,0,0,0);
													VmissPIP.add(W);
													VmissPIP.sub(VP);
													VmissPIP.sub(VPIM);
													LorentzVector VmissPIM = new LorentzVector(0,0,0,0);
													VmissPIM.add(W);
													VmissPIM.sub(VP);
													VmissPIM.sub(VPIP);
													LorentzVector VmissAll = new LorentzVector(0,0,0,0);
													VmissAll.add(VmissPIM);
													VmissAll.sub(VPIM);

													hasDVPipPimP = true
														&& VmissAll.e() > -0.5 && VmissAll.e() < 0.5
														&& VmissAll.mass2() > -0.1 &&  VmissAll.mass2() < 0.1
														&& VmissAll.px()*VmissAll.px() + VmissAll.py()*VmissAll.py() < 0.5
														&& Vangle( VP.vect() , VmissP.vect() ) < 12
														&& Vangle( VPIP.vect() , VmissPIP.vect() ) < 12
														&& Vangle( VPIM.vect() , VmissPIM.vect() ) < 12
														&& VmissP.mass2() > 0.5 && VmissP.mass2() < 1.5
														&& VmissPIP.mass2() > -0.3 && VmissPIP.mass2() < 0.5
														&& VmissPIM.mass2() > -0.3 && VmissPIM.mass2() < 0.5
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
		return hasDVPipPimP;
	}
}
