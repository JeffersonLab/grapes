package org.jlab.jnp.grapes.services;

import org.jlab.jnp.grapes.services.BeamTargetWagon;
import org.jlab.jnp.physics.Vector3;
import org.jlab.jnp.physics.LorentzVector;

import org.jlab.jnp.hipo4.data.Bank;
import org.jlab.jnp.hipo4.data.Event;
import org.jlab.jnp.hipo4.data.SchemaFactory;

/**
 *
 * DVPipPimPi0P Skimming
 *
 * @author fxgirod
 */

public class DVPipPimPi0PWagon2 extends BeamTargetWagon {

    static final double PionMass   = 0.13957f;

    public DVPipPimPi0PWagon2() {
        super("DVPipPimPi0PWagon2","fxgirod","0.0");
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

        boolean hasDVPipPimPi0P = false;
        if( RecPart!=null && RecPart.getRows()>5 ){
            int n_e = 0;
            int n_p = 0;
            int n_pip = 0;
            int n_pim = 0;
            int n_g = 0;
            for (int ii = 0; ii < RecPart.getRows() ; ii++) {
                int is_pid = RecPart.getInt("pid", ii);
                int stat   = Math.abs(RecPart.getShort("status", ii));
                if(stat>2000 && stat<4000 && is_pid==11)n_e++;
                if(stat>2000 && stat!=4000 && is_pid==2212)n_p++;
                if(stat>2000 && stat<4000 && is_pid==211)n_pip++;
                if(stat>2000 && stat<4000 && is_pid==-211)n_pim++;
                if(stat!=2000 && stat<4000 && is_pid==22)n_g++;
            }
            boolean is_candidate = (n_e*n_p*n_pip*n_pim*n_g>0)&&(n_g>1);
            if(is_candidate){
                int[] e_ind   = new int[n_e];
                int[] p_ind   = new int[n_p];
                int[] pip_ind = new int[n_pip];
                int[] pim_ind = new int[n_pim];
                int[] g_ind   = new int[n_g];
                n_e = 0;
                n_p = 0;
                n_pip = 0;
                n_pim = 0;
                n_g=0;
                for (int ii = 0; ii < RecPart.getRows() ; ii++) {
                    int is_pid = RecPart.getInt("pid", ii);
                    int stat   = Math.abs(RecPart.getShort("status", ii));
                    if(stat>2000 && stat<4000 && is_pid==11){e_ind[n_e]=ii;n_e++;}
                    if(stat>2000 && stat!=4000 && is_pid==2212){p_ind[n_p]=ii;n_p++;}
                    if(stat>2000 && stat<4000 && is_pid==211){pip_ind[n_pip]=ii;n_pip++;};
                    if(stat>2000 && stat<4000 && is_pid==-211){pim_ind[n_pim]=ii;n_pim++;}
                    if(stat!=2000 && stat<4000 && is_pid==22){g_ind[n_g]=ii;n_g++;}
                }
                for (int ie = 0; ie < n_e && !hasDVPipPimPi0P; ie++) {
                    double e_px  = RecPart.getFloat("px", e_ind[ie]);
                    double e_py  = RecPart.getFloat("py", e_ind[ie]);
                    double e_pz  = RecPart.getFloat("pz", e_ind[ie]);

                    double e_mom = Math.sqrt(e_px*e_px+e_py*e_py+e_pz*e_pz);
                    if( e_mom>0.1*beamEnergy ){
                        LorentzVector VE = new LorentzVector(e_px,e_py,e_pz,e_mom);
                        for (int ip = 0; ip < n_p && !hasDVPipPimPi0P; ip++) {
                            double p_px  = RecPart.getFloat("px", p_ind[ip]);
                            double p_py  = RecPart.getFloat("py", p_ind[ip]);
                            double p_pz  = RecPart.getFloat("pz", p_ind[ip]);

                            double p_ene = Math.sqrt(p_px*p_px+p_py*p_py+p_pz*p_pz+targetMass*targetMass);
                            if( p_ene>0.94358 ){
                                LorentzVector VP = new LorentzVector(p_px,p_py,p_pz,p_ene);
                                for (int ipip = 0; ipip < n_pip && !hasDVPipPimPi0P ; ipip++) {
                                    double pip_px  = RecPart.getFloat("px", pip_ind[ipip]);
                                    double pip_py  = RecPart.getFloat("py", pip_ind[ipip]);
                                    double pip_pz  = RecPart.getFloat("pz", pip_ind[ipip]);

                                    double pip_ene = Math.sqrt(pip_px*pip_px+pip_py*pip_py+pip_pz*pip_pz+PionMass*PionMass);
                                    if( pip_ene>0.3){
                                        LorentzVector VPIP = new LorentzVector(pip_px,pip_py,pip_pz,pip_ene);
                                        for (int ipim = 0; ipim < n_pim && !hasDVPipPimPi0P ; ipim++) {
                                            double pim_px  = RecPart.getFloat("px", pim_ind[ipim]);
                                            double pim_py  = RecPart.getFloat("py", pim_ind[ipim]);
                                            double pim_pz  = RecPart.getFloat("pz", pim_ind[ipim]);

                                            double pim_ene = Math.sqrt(pim_px*pim_px+pim_py*pim_py+pim_pz*pim_pz+PionMass*PionMass);
                                            if( pim_ene>0.3){
                                                LorentzVector VPIM = new LorentzVector(pim_px,pim_py,pim_pz,pim_ene);
                                                for(int ig1 = 0; ig1 < n_g-1 && !hasDVPipPimPi0P ; ig1++){
                                                    double g1_px  = RecPart.getFloat("px", g_ind[ig1]);
                                                    double g1_py  = RecPart.getFloat("py", g_ind[ig1]);
                                                    double g1_pz  = RecPart.getFloat("pz", g_ind[ig1]);

                                                    double g1_mom = Math.sqrt(g1_px*g1_px+g1_py*g1_py+g1_pz*g1_pz);
                                                    LorentzVector VG1 = new LorentzVector(g1_px,g1_py,g1_pz,g1_mom);
                                                    double e_g1_angle = Vangle( VE.vect() , VG1.vect() );
                                                    if( g1_mom>0.15){

                                                        for(int ig2 = ig1+1; ig2 < n_g && !hasDVPipPimPi0P ; ig2++){
                                                            double g2_px  = RecPart.getFloat("px", g_ind[ig2]);
                                                            double g2_py  = RecPart.getFloat("py", g_ind[ig2]);
                                                            double g2_pz  = RecPart.getFloat("pz", g_ind[ig2]);

                                                            double g2_mom = Math.sqrt(g2_px*g2_px+g2_py*g2_py+g2_pz*g2_pz);
                                                            LorentzVector VG2 = new LorentzVector(g2_px,g2_py,g2_pz,g2_mom);
                                                            double e_g2_angle = Vangle( VE.vect() , VG2.vect() );
                                                            if( g2_mom>0.15){

                                                                LorentzVector VPI0 = new LorentzVector(0,0,0,0);
                                                                VPI0.add(VG1);
                                                                VPI0.add(VG2);

                                                                LorentzVector Q = new LorentzVector(0,0,0,0);
                                                                Q.add(VB);
                                                                Q.sub(VE);
                                                                LorentzVector W = new LorentzVector(0,0,0,0);
                                                                W.add(Q);
                                                                W.add(VT);

                                                                double g1_g2_angle = Vangle( VG1.vect() , VG2.vect() );

                                                                if( -Q.mass2()>0.8 && W.mass()>1.8 && g1_g2_angle>1 && VPI0.mass()>0.025 && VPI0.mass()<0.3 ){
                                                                    LorentzVector VETA = new LorentzVector(0,0,0,0);
                                                                    VETA.add(VPIP);
                                                                    VETA.add(VPIM);
                                                                    VETA.add(VPI0);

                                                                    LorentzVector VmissP = new LorentzVector(0,0,0,0);
                                                                    VmissP.add(W);
                                                                    VmissP.sub(VETA);
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
        }
        return hasDVPipPimPi0P;
    }
}