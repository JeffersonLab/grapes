package org.jlab.jnp.grapes.services;

import org.jlab.jnp.physics.Vector3;
import org.jlab.jnp.physics.LorentzVector;

import org.jlab.jnp.hipo4.data.Bank;
import org.jlab.jnp.hipo4.data.Event;
import org.jlab.jnp.hipo4.data.SchemaFactory;


//import org.jlab.jnp.grapes.services.*;

/**
* 
* DVPipPimPi0P Skimming
*
* @author fxgirod
*/
 
public class DVPipPimPi0PWagon extends BeamTargetWagon {
 
    static final double PionMass   = 0.13957f;

    public LorentzVector VB, VT;

    public DVPipPimPi0PWagon() {
        super("DVPipPimPi0PWagon","fxgirod","0.0");

	VB = new LorentzVector(0,0,beamEnergy,beamEnergy);
	VT = new LorentzVector(0,0,0,targetMass);
    }

    public double Vangle(Vector3 v1, Vector3 v2){ 
	    double res=0;
	    double l1 = v1.mag();
	    double l2 = v2.mag();
	    if( l1*l2 > 0)res = Math.toDegrees( Math.acos( v1.dot(v2)/(l1*l2) ) );
	    return res;
    }

    @Override
    public boolean init(String jsonString) {
        System.out.println("DVPipPimPi0PWagon READY.");
        return true;
    }   

    @Override
    public boolean processDataEvent(Event event, SchemaFactory factory) {

	    Bank RecPart = new Bank(factory.getSchema("REC::Particle"));
	    event.read(RecPart);

	    boolean hasDVPipPimPi0P = false;
	    if( RecPart!=null && RecPart.getRows()>5 ){

		    for (int ie = 0; ie < RecPart.getRows()-5 && !hasDVPipPimPi0P; ie++) {
			    int is_e     = RecPart.getInt("pid", ie);
			    double e_px  = RecPart.getFloat("px", ie);
			    double e_py  = RecPart.getFloat("py", ie);
			    double e_pz  = RecPart.getFloat("pz", ie);
			    int e_stat   = Math.abs(RecPart.getShort("status", ie));

			    double e_mom = Math.sqrt(e_px*e_px+e_py*e_py+e_pz*e_pz);
			    if( is_e==11 && e_stat>2000 && e_stat<4000 && e_mom>0.1*beamEnergy ){
				    LorentzVector VE = new LorentzVector(e_px,e_py,e_pz,e_mom);
				    for (int ip = ie+1; ip < RecPart.getRows()-4 && !hasDVPipPimPi0P; ip++) {
					    int is_p     = RecPart.getInt("pid", ip);
					    double p_px  = RecPart.getFloat("px", ip);
					    double p_py  = RecPart.getFloat("py", ip);
					    double p_pz  = RecPart.getFloat("pz", ip);
					    int p_stat   = Math.abs(RecPart.getShort("status", ip));

					    if( is_p==2212 && p_stat>2000 ){
						    double p_ene = Math.sqrt(p_px*p_px+p_py*p_py+p_pz*p_pz+targetMass*targetMass);
						    LorentzVector VP = new LorentzVector(p_px,p_py,p_pz,p_ene);
						    for (int ipip = ip+1; ipip < RecPart.getRows()-3 && !hasDVPipPimPi0P ; ipip++) {
							    int is_pip     = RecPart.getInt("pid", ipip);
							    double pip_px  = RecPart.getFloat("px", ipip);
							    double pip_py  = RecPart.getFloat("py", ipip);
							    double pip_pz  = RecPart.getFloat("pz", ipip);
							    int pip_stat   = Math.abs(RecPart.getShort("status", ipip));

							    double pip_ene = Math.sqrt(pip_px*pip_px+pip_py*pip_py+pip_pz*pip_pz+PionMass*PionMass);
							    if( is_pip==211 && pip_stat>2000 && pip_stat<4000 && pip_ene>0.3){
								    LorentzVector VPIP = new LorentzVector(pip_px,pip_py,pip_pz,pip_ene);
								    for (int ipim = ipip+1; ipim < RecPart.getRows()-2 && !hasDVPipPimPi0P ; ipim++) {
									    int is_pim     = RecPart.getInt("pid", ipim);
									    double pim_px  = RecPart.getFloat("px", ipim);
									    double pim_py  = RecPart.getFloat("py", ipim);
									    double pim_pz  = RecPart.getFloat("pz", ipim);
									    int pim_stat   = Math.abs(RecPart.getShort("status", ipim));

									    double pim_ene = Math.sqrt(pim_px*pim_px+pim_py*pim_py+pim_pz*pim_pz+PionMass*PionMass);
									    if( is_pim==-211 && pim_stat>2000 && pim_stat<4000 && pim_ene>0.3){
										    LorentzVector VPIM = new LorentzVector(pim_px,pim_py,pim_pz,pim_ene);
										    for(int ig1 = ipim+1; ig1 < RecPart.getRows()-1 && !hasDVPipPimPi0P ; ig1++){
											    int is_g1     = RecPart.getInt("pid", ig1);
											    double g1_px  = RecPart.getFloat("px", ig1);
											    double g1_py  = RecPart.getFloat("py", ig1);
											    double g1_pz  = RecPart.getFloat("pz", ig1);
											    int g1_stat   = Math.abs(RecPart.getShort("status", ig1));

											    double g1_mom = Math.sqrt(g1_px*g1_px+g1_py*g1_py+g1_pz*g1_pz);
											    LorentzVector VG1 = new LorentzVector(g1_px,g1_py,g1_pz,g1_mom);
											    double e_g1_angle = Vangle( VE.vect() , VG1.vect() );
											    if( is_g1==22 && e_g1_angle>4 && g1_stat<4000 && g1_mom>0.15){
										    
												    for(int ig2 = ig1+1; ig2 < RecPart.getRows() && !hasDVPipPimPi0P ; ig2++){
													    int is_g2     = RecPart.getInt("pid", ig2);
													    double g2_px  = RecPart.getFloat("px", ig2);
													    double g2_py  = RecPart.getFloat("py", ig2);
													    double g2_pz  = RecPart.getFloat("pz", ig2);
													    int g2_stat   = Math.abs(RecPart.getShort("status", ig2));

													    double g2_mom = Math.sqrt(g2_px*g2_px+g2_py*g2_py+g2_pz*g2_pz);
													    LorentzVector VG2 = new LorentzVector(g2_px,g2_py,g2_pz,g2_mom);
													    double e_g2_angle = Vangle( VE.vect() , VG2.vect() );
													    if( is_g2==22 && e_g2_angle>4 && g2_stat<4000 && g2_mom>0.15){

														    LorentzVector VPI0 = new LorentzVector(0,0,0,0);
														    VPI0.add(VG1);
														    VPI0.add(VG2);

														    LorentzVector Q = new LorentzVector(0,0,0,0);
														    Q.add(VB);
														    Q.sub(VE);
														    LorentzVector W = new LorentzVector(0,0,0,0);
														    W.add(Q);
														    W.add(this.VT);

														    double g1_g2_angle = Vangle( VG1.vect() , VG2.vect() );

														    if( -Q.mass2()>0.8 && W.mass()>1.8 && g1_g2_angle>1 && VPI0.mass()>0.025 && VPI0.mass()<0.3 ){
															    LorentzVector VOMEGA = new LorentzVector(0,0,0,0);
															    VOMEGA.add(VPIP);
															    VOMEGA.add(VPIM);
															    VOMEGA.add(VPI0);

															    LorentzVector VmissP = new LorentzVector(0,0,0,0);
															    VmissP.add(W);
															    VmissP.sub(VOMEGA);
															    LorentzVector VmissPIP = new LorentzVector(0,0,0,0);
															    VmissPIP.add(W);
															    VmissPIP.sub(VP);
															    VmissPIP.sub(VPIM);
															    VmissPIP.sub(VPI0);
															    LorentzVector VmissPIM = new LorentzVector(0,0,0,0);
															    VmissPIM.add(W);
															    VmissPIM.sub(VP);
															    VmissPIM.sub(VPIP);
															    VmissPIM.sub(VPI0);
															    LorentzVector VmissPI0 = new LorentzVector(0,0,0,0);
															    VmissPI0.add(W);
															    VmissPI0.sub(VP);
															    VmissPI0.sub(VPIP);
															    VmissPI0.sub(VPIM);
															    LorentzVector VmissAll = new LorentzVector(0,0,0,0);
															    VmissAll.add(VmissP);
															    VmissAll.sub(VP);

															    hasDVPipPimPi0P = true
																    && VmissAll.e() > -1.0 && VmissAll.e() < 1.5
																    && VmissAll.mass2() > -0.175 &&  VmissAll.mass2() < 0.175
																    && VmissAll.px()*VmissAll.px() + VmissAll.py()*VmissAll.py() < 0.75
																    && Vangle( VP.vect() , VmissP.vect() ) < 17
																    && Vangle( VPIP.vect() , VmissPIP.vect() ) < 35
																    && Vangle( VPIM.vect() , VmissPIM.vect() ) < 35
																    && Vangle( VPI0.vect() , VmissPI0.vect() ) < 35
																    && VmissP.mass2() > -0.5 && VmissP.mass2() < 3.0
																    && VmissPIP.mass2() > -0.6 && VmissPIP.mass2() < 0.75
																    && VmissPIM.mass2() > -0.6 && VmissPIM.mass2() < 0.75
																    && VmissPI0.mass2() > -0.6 && VmissPI0.mass2() < 0.75
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
			    }
		    }
	    }
	    return hasDVPipPimPi0P;
    }
}
