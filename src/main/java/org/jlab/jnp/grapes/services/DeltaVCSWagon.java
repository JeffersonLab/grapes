package org.jlab.jnp.grapes.services;

import org.jlab.jnp.physics.Vector3;
import org.jlab.jnp.physics.LorentzVector;

import org.jlab.jnp.hipo4.data.Bank;
import org.jlab.jnp.hipo4.data.Event;
import org.jlab.jnp.hipo4.data.SchemaFactory;

/**
 * 
 * DeltaVCS Skimming
 *
 * @author fxgirod
 */

public class DeltaVCSWagon extends BeamTargetWagon {

	public double prot_mass, neut_mass, pip_mass, pi0_mass;

	public DeltaVCSWagon() {
		super("DeltaVCSWagon","fxgirod","0.0");
		prot_mass = 0.93827;
		neut_mass = 0.93957;
		pip_mass = 0.13957;
		pi0_mass = 0.13498;
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

		boolean hasDeltaVCS = false;
		if(RecPart.getRows()>3 ){
			int n_e = 0;
			int n_p = 0;
			int n_n = 0;
			int n_g = 0;
			int n_pip = 0;
			for (int ii = 0; ii < RecPart.getRows() ; ii++) {
				int is_pid = RecPart.getInt("pid", ii);
				int stat   = Math.abs(RecPart.getShort("status", ii));
				if(stat>2000  && stat<4000 && is_pid==11)n_e++;
				if(stat>2000  && stat!=4000 && is_pid==2212)n_p++;
				if(stat>2000  && stat!=4000 && is_pid==2112)n_n++;
				if(stat!=2000 && stat<4000 && is_pid==22)n_g++;
				if(stat>2000  && stat!=4000 && is_pid==211)n_pip++;
			}
			boolean is_candidate = ( n_e*n_p*n_g>0 && n_g>2 ) || ( n_e*n_n*n_pip*n_g>0 );
			if(is_candidate){
				int[] e_ind   = new int[n_e];
				int[] p_ind   = new int[n_p];
				int[] n_ind   = new int[n_n];
				int[] g_ind   = new int[n_g];
				int[] pip_ind   = new int[n_pip];
				n_e = 0;
				n_p = 0;
				n_n = 0;
				n_g = 0;
				n_pip = 0;
				for (int ii = 0; ii < RecPart.getRows() ; ii++) {
					int is_pid = RecPart.getInt("pid", ii);
					int stat   = Math.abs(RecPart.getShort("status", ii));
					if(stat>2000  && stat<4000 && is_pid==11){e_ind[n_e]=ii;n_e++;}
					if(stat>2000  && stat!=4000 && is_pid==2212){p_ind[n_p]=ii;n_p++;}
					if(stat>2000  && stat!=4000 && is_pid==2112){n_ind[n_n]=ii;n_n++;}
					if(stat!=2000 && stat<4000 && is_pid==22){g_ind[n_g]=ii;n_g++;}
					if(stat>2000  && stat!=4000 && is_pid==211){pip_ind[n_pip]=ii;n_pip++;}
				}
				if( n_e*n_p*n_g>0 && n_g>2 ){

					boolean[] g_rad_cut = new boolean[n_g];
					for (int ig = 0; ig < n_g; ig++) {
						g_rad_cut[ig] = true;
						double g_px  = RecPart.getFloat("px", g_ind[ig]);
						double g_py  = RecPart.getFloat("py", g_ind[ig]);
						double g_pz  = RecPart.getFloat("pz", g_ind[ig]);
						double g_mom = Math.sqrt(g_px*g_px+g_py*g_py+g_pz*g_pz);
						LorentzVector VG = new LorentzVector(g_px,g_py,g_pz,g_mom);
						for (int ie = 0; ie < n_e && g_rad_cut[ig]; ie++) {
							double e_px  = RecPart.getFloat("px", e_ind[ie]);
							double e_py  = RecPart.getFloat("py", e_ind[ie]);
							double e_pz  = RecPart.getFloat("pz", e_ind[ie]);
							double e_mom = Math.sqrt(e_px*e_px+e_py*e_py+e_pz*e_pz);
							LorentzVector VE = new LorentzVector(e_px,e_py,e_pz,e_mom);
							double e_g_rad_angle = Vangle( VE.vect() , VG.vect() );
							g_rad_cut[ig] = g_rad_cut[ig] && (e_g_rad_angle>5);
						}
					}
					int n_pi0 = 0;
					int[] g1_ind   = new int[n_g*(n_g-1)];
					int[] g2_ind   = new int[n_g*(n_g-1)];
					for (int ig1 = 0; ig1 < n_g-1; ig1++) {
						double g1_px  = RecPart.getFloat("px", g_ind[ig1]);
						double g1_py  = RecPart.getFloat("py", g_ind[ig1]);
						double g1_pz  = RecPart.getFloat("pz", g_ind[ig1]);
						double g1_mom = Math.sqrt(g1_px*g1_px+g1_py*g1_py+g1_pz*g1_pz);
						if(  g1_mom>0.1){
							LorentzVector VG1 = new LorentzVector(g1_px,g1_py,g1_pz,g1_mom);
							for (int ig2 = ig1+1; ig2 < n_g; ig2++) {
								double g2_px  = RecPart.getFloat("px", g_ind[ig2]);
								double g2_py  = RecPart.getFloat("py", g_ind[ig2]);
								double g2_pz  = RecPart.getFloat("pz", g_ind[ig2]);
								double g2_mom = Math.sqrt(g2_px*g2_px+g2_py*g2_py+g2_pz*g2_pz);
								if(  g2_mom>0.1){
									LorentzVector VG2 = new LorentzVector(g2_px,g2_py,g2_pz,g2_mom);
									LorentzVector VPI0 = new LorentzVector(0,0,0,0);
									VPI0.add(VG1);
									VPI0.add(VG2);

									double g1_g2_angle = Vangle( VG1.vect() , VG2.vect() );
									double pi0mass = VPI0.mass();
									if(true
									   && g_rad_cut[ig1]
									   && g_rad_cut[ig2]
									   && g1_g2_angle>8*(1-VPI0.e())
									   && pi0mass>pi0_mass-0.085 && pi0mass<pi0_mass+0.085
									   ){
										g1_ind[n_pi0] = g_ind[ig1];
										g2_ind[n_pi0] = g_ind[ig2];
										n_pi0++;
									   }
								}//g2 energy
							}// g2 loop
						}// g1 energy
					}// g1 loop

					for (int ie = 0; ie < n_e && !hasDeltaVCS; ie++) {
						double e_px  = RecPart.getFloat("px", e_ind[ie]);
						double e_py  = RecPart.getFloat("py", e_ind[ie]);
						double e_pz  = RecPart.getFloat("pz", e_ind[ie]);

						double e_mom = Math.sqrt(e_px*e_px+e_py*e_py+e_pz*e_pz);
						LorentzVector VE = new LorentzVector(e_px,e_py,e_pz,e_mom);
						LorentzVector Q = new LorentzVector(0,0,0,0);
						Q.add(VB);
						Q.sub(VE);
						LorentzVector W = new LorentzVector(0,0,0,0);
						W.add(Q);
						W.add(VT);

						if( -Q.mass2()>0.8 && W.mass()>1.8 && e_mom>0.1*beamEnergy ){
							for (int ip = 0; ip < n_p && !hasDeltaVCS; ip++) {
								double p_px  = RecPart.getFloat("px", p_ind[ip]);
								double p_py  = RecPart.getFloat("py", p_ind[ip]);
								double p_pz  = RecPart.getFloat("pz", p_ind[ip]);

								double p_ene = Math.sqrt(p_px*p_px+p_py*p_py+p_pz*p_pz+prot_mass*prot_mass);
								if( p_ene>0.94358 ){
									LorentzVector VP = new LorentzVector(p_px,p_py,p_pz,p_ene);
									for (int ig = 0; ig < n_g && !hasDeltaVCS; ig++) {
										double g_px  = RecPart.getFloat("px", g_ind[ig]);
										double g_py  = RecPart.getFloat("py", g_ind[ig]);
										double g_pz  = RecPart.getFloat("pz", g_ind[ig]);
										double g_mom = Math.sqrt(g_px*g_px+g_py*g_py+g_pz*g_pz);
										if(  g_mom>0.15*beamEnergy && g_rad_cut[ig]){
											LorentzVector VG = new LorentzVector(g_px,g_py,g_pz,g_mom);

											for(int ipi0=0;ipi0<n_pi0 && !hasDeltaVCS; ipi0++){
												if( g_ind[ig]!=g1_ind[ipi0] && g_ind[ig]!=g2_ind[ipi0] ){
													double g1_px  = RecPart.getFloat("px", g1_ind[ipi0]);
													double g1_py  = RecPart.getFloat("py", g1_ind[ipi0]);
													double g1_pz  = RecPart.getFloat("pz", g1_ind[ipi0]);
													double g1_mom = Math.sqrt(g1_px*g1_px+g1_py*g1_py+g1_pz*g1_pz);
													LorentzVector VG1 = new LorentzVector(g1_px,g1_py,g1_pz,g1_mom);

													double g2_px  = RecPart.getFloat("px", g2_ind[ipi0]);
													double g2_py  = RecPart.getFloat("py", g2_ind[ipi0]);
													double g2_pz  = RecPart.getFloat("pz", g2_ind[ipi0]);
													double g2_mom = Math.sqrt(g2_px*g2_px+g2_py*g2_py+g2_pz*g2_pz);
													LorentzVector VG2 = new LorentzVector(g2_px,g2_py,g2_pz,g2_mom);

													LorentzVector VPI0 = new LorentzVector(0,0,0,0);
													VPI0.add(VG1);
													VPI0.add(VG2);

													LorentzVector VmissP = new LorentzVector(0,0,0,0);
													VmissP.add(W);
													VmissP.sub(VG);
													VmissP.sub(VPI0);
													LorentzVector VmissPi = new LorentzVector(0,0,0,0);
													VmissPi.add(W);
													VmissPi.sub(VP);
													VmissPi.sub(VG);
													LorentzVector VmissG = new LorentzVector(0,0,0,0);
													VmissG.add(W);
													VmissG.sub(VP);
													VmissG.sub(VPI0);
													LorentzVector VmissAll = new LorentzVector(0,0,0,0);
													VmissAll.add(VmissG);
													VmissAll.sub(VG);

													hasDeltaVCS = true
														&& VmissG.mass()>-0.5 && VmissG.mass()<0.7
														&& VmissP.mass()>0 && VmissP.mass()<2.2
														&& VmissPi.mass()>-0.3 && VmissPi.mass()<0.4
														&& VmissAll.mass2() > -0.1 &&  VmissAll.mass2() < 0.1
														&& VmissAll.e() > -1 && VmissAll.e() < 1.5
														&& VmissAll.px()*VmissAll.px() + VmissAll.py()*VmissAll.py() < 0.75
														&& Vangle( VG.vect() , VmissG.vect() ) < 7.5
														&& Vangle( VP.vect() , VmissP.vect() ) < 32
														&& Vangle( VPI0.vect() , VmissPi.vect() ) < 23
														;
												}// unique photons condition
											}// pion loop
										}// DVCS g E condition
									}// DVCS g loop
								}// p mom condition
							}// loop over p
						}// e mom condition
					}// loop over e
				}// has e p g pi0 configurtion
			}// is candidate
		}// more than 3 particles
		return hasDeltaVCS;
	}
}
