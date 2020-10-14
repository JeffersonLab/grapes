package org.jlab.jnp.grapes.services;

import org.jlab.jnp.physics.Vector3;
import org.jlab.jnp.physics.LorentzVector;

import org.jlab.jnp.hipo4.data.Bank;
import org.jlab.jnp.hipo4.data.Event;
import org.jlab.jnp.hipo4.data.SchemaFactory;

/**
 *
 * DVEtaP Skimming
 *
 * @author fxgirod
 */

public class DVEtaPWagon extends BeamTargetWagon {

    public DVEtaPWagon() {
        super("DVEtaPWagon","fxgirod","0.0");
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

        boolean hasDVEtaP = false;
        if( RecPart!=null && RecPart.getRows()>3 ){

            int n_e = 0;
            int n_p = 0;
            int n_g = 0;
            for (int ii = 0; ii < RecPart.getRows() ; ii++) {
                int is_pid = RecPart.getInt("pid", ii);
                int stat   = Math.abs(RecPart.getShort("status", ii));
                if(stat>2000  && stat<4000 && is_pid==11)n_e++;
                if(stat>2000  && stat!=4000 && is_pid==2212)n_p++;
                if(stat!=2000 && stat<4000 && is_pid==22)n_g++;
            }
            boolean is_candidate = (n_e*n_p*n_g>0)&&(n_g>1);
            if(is_candidate){
                int[] e_ind   = new int[n_e];
                int[] p_ind   = new int[n_p];
                int[] g_ind   = new int[n_g];
                n_e = 0;
                n_p = 0;
                n_g = 0;
                for (int ii = 0; ii < RecPart.getRows() ; ii++) {
                    int is_pid = RecPart.getInt("pid", ii);
                    int stat   = Math.abs(RecPart.getShort("status", ii));
                    if(stat>2000  && stat<4000 && is_pid==11){e_ind[n_e]=ii;n_e++;}
                    if(stat>2000  && stat!=4000 && is_pid==2212){p_ind[n_p]=ii;n_p++;}
                    if(stat!=2000 && stat<4000 && is_pid==22){g_ind[n_g]=ii;n_g++;}
                }
                for (int ie = 0; ie < n_e && !hasDVEtaP; ie++) {
                    double e_px  = RecPart.getFloat("px", e_ind[ie]);
                    double e_py  = RecPart.getFloat("py", e_ind[ie]);
                    double e_pz  = RecPart.getFloat("pz", e_ind[ie]);

                    double e_mom = Math.sqrt(e_px*e_px+e_py*e_py+e_pz*e_pz);
                    if( e_mom>0.1*beamEnergy ){
                        LorentzVector VE = new LorentzVector(e_px,e_py,e_pz,e_mom);
                        for (int ip = 0; ip < n_p && !hasDVEtaP; ip++) {
                            double p_px  = RecPart.getFloat("px", p_ind[ip]);
                            double p_py  = RecPart.getFloat("py", p_ind[ip]);
                            double p_pz  = RecPart.getFloat("pz", p_ind[ip]);

                            double p_ene = Math.sqrt(p_px*p_px+p_py*p_py+p_pz*p_pz+targetMass*targetMass);
                            if(  p_ene>0.94358 ){
                                LorentzVector VP = new LorentzVector(p_px,p_py,p_pz,p_ene);
                                for (int ig1 = 0; ig1 < n_g-1 && !hasDVEtaP ; ig1++) {
                                    double g1_px  = RecPart.getFloat("px", g_ind[ig1]);
                                    double g1_py  = RecPart.getFloat("py", g_ind[ig1]);
                                    double g1_pz  = RecPart.getFloat("pz", g_ind[ig1]);

                                    double g1_mom = Math.sqrt(g1_px*g1_px+g1_py*g1_py+g1_pz*g1_pz);
                                    if( g1_mom>0.15){
                                        LorentzVector VG1 = new LorentzVector(g1_px,g1_py,g1_pz,g1_mom);
                                        for (int ig2 = ig1+1; ig2 < n_g && !hasDVEtaP ; ig2++) {
                                            double g2_px  = RecPart.getFloat("px", g_ind[ig2]);
                                            double g2_py  = RecPart.getFloat("py", g_ind[ig2]);
                                            double g2_pz  = RecPart.getFloat("pz", g_ind[ig2]);

                                            double g2_mom = Math.sqrt(g2_px*g2_px+g2_py*g2_py+g2_pz*g2_pz);
                                            if( g2_mom>0.15){
                                                LorentzVector VG2 = new LorentzVector(g2_px,g2_py,g2_pz,g2_mom);

                                                LorentzVector VETA = new LorentzVector(0,0,0,0);
                                                VETA.add(VG1);
                                                VETA.add(VG2);

                                                LorentzVector Q = new LorentzVector(0,0,0,0);
                                                Q.add(VB);
                                                Q.sub(VE);
                                                LorentzVector W = new LorentzVector(0,0,0,0);
                                                W.add(Q);
                                                W.add(VT);

                                                double e_g1_angle = Vangle( VE.vect() , VG1.vect() );
                                                double e_g2_angle = Vangle( VE.vect() , VG2.vect() );
                                                double g1_g2_angle = Vangle( VG1.vect() , VG2.vect() );

                                                if( -Q.mass2()>0.8 && W.mass()>1.8 && e_g1_angle>4 && e_g2_angle>4 && g1_g2_angle>1 ){
                                                    LorentzVector VmissP = new LorentzVector(0,0,0,0);
                                                    VmissP.add(W);
                                                    VmissP.sub(VETA);
                                                    LorentzVector VmissETA = new LorentzVector(0,0,0,0);
                                                    VmissETA.add(W);
                                                    VmissETA.sub(VP);
                                                    LorentzVector VmissAll = new LorentzVector(0,0,0,0);
                                                    VmissAll.add(VmissETA);
                                                    VmissAll.sub(VETA);

                                                    // cuts
                                                    hasDVEtaP = true
                                                            && VmissAll.e() > -1.5 && VmissAll.e() < 2.0
                                                            && VmissP.mass() > 0 && VmissP.mass() < 2.5
                                                            && VmissAll.mass2() > -0.1 &&  VmissAll.mass2() < 0.1
                                                            && VmissAll.px()*VmissAll.px() + VmissAll.py()*VmissAll.py() < 1.0
                                                            && VETA.mass()>0.05 && VETA.mass()<1.0 // could cut out pi0 by increasing lower limit
                                                            && Vangle( VETA.vect() , VmissETA.vect() ) < 7.5
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
        return hasDVEtaP;
    }
}