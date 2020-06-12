package org.jlab.jnp.grapes.services;

import java.io.*;
import java.util.*;
import java.text.SimpleDateFormat;

import org.jlab.io.hipo.*;
import org.jlab.clas.physics.Vector3;
import org.jlab.clas.physics.LorentzVector;

import org.jlab.jnp.hipo4.data.Bank;
import org.jlab.jnp.hipo4.data.Event;
import org.jlab.jnp.hipo4.data.SchemaFactory;
import org.jlab.jnp.hipo4.io.HipoReader;
import org.jlab.jnp.hipo4.io.HipoWriter;


/**
* 
* DVCS Skimming
*
* @author fxgirod
*/

public class DVCSWagon_10p6 extends Wagon {

    static final double BeamEnergy = 10.603f;
    static final double TargetMass = 0.93827f;

    public LorentzVector VB, VT, VE, VP, VG;

    public DVCSWagon_10p6() {
        super("DVCSWagon","fxgirod","0.0");

        VB = new LorentzVector(0,0,BeamEnergy,BeamEnergy);
        VT = new LorentzVector(0,0,0,TargetMass);
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

            Bank RecPart = new Bank(factory.getSchema("REC::Particle"));
            event.read(RecPart);

            boolean hasDVCS = false;
            if( RecPart!=null && RecPart.getRows()>2 ){

                    for (int ie = 0; ie < RecPart.getRows()-2; ie++) {
                            int is_e     = RecPart.getInt("pid", ie);
                            double e_px  = RecPart.getFloat("px", ie);
                            double e_py  = RecPart.getFloat("py", ie);
                            double e_pz  = RecPart.getFloat("pz", ie);
                            int e_stat   = Math.abs(RecPart.getShort("status", ie));

                            double e_mom = Math.sqrt(e_px*e_px+e_py*e_py+e_pz*e_pz);
                            if( is_e==11 && e_stat>2000 && e_stat<4000 && e_mom>0.1*BeamEnergy ){
                                    VE = new LorentzVector(e_px,e_py,e_pz,e_mom);
                                    for (int ip = ie+1; ip < RecPart.getRows()-1; ip++) {
                                            int is_p     = RecPart.getInt("pid", ip);
                                            double p_px  = RecPart.getFloat("px", ip);
                                            double p_py  = RecPart.getFloat("py", ip);
                                            double p_pz  = RecPart.getFloat("pz", ip);
                                            int p_stat   = Math.abs(RecPart.getShort("status", ip));

                                            if( is_p==2212 && p_stat>2000 ){
                                                    double p_ene = Math.sqrt(p_px*p_px+p_py*p_py+p_pz*p_pz);
                                                    VP = new LorentzVector(p_px,p_py,p_pz,p_ene);
                                                    for (int ig = ip+1; ig < RecPart.getRows(); ig++) {
                                                            int is_g     = RecPart.getInt("pid", ig);
                                                            double g_px  = RecPart.getFloat("px", ig);
                                                            double g_py  = RecPart.getFloat("py", ig);
                                                            double g_pz  = RecPart.getFloat("pz", ig);
                                                            int g_stat   = Math.abs(RecPart.getShort("status", ig));

                                                            double g_mom = Math.sqrt(g_px*g_px+g_py*g_py+g_pz*g_pz);
                                                            if( is_g==22 && g_stat<4000 && g_mom>0.15*BeamEnergy){
                                                                    VG = new LorentzVector(g_px,g_py,g_pz,g_mom);

                                                                    LorentzVector Q = new LorentzVector(0,0,0,0);
                                                                    Q.add(VB);
                                                                    Q.sub(VE);
                                                                    LorentzVector W = new LorentzVector(0,0,0,0);
                                                                    W.add(Q);
                                                                    W.add(this.VT);

                                                                    if( -Q.mass2()>0.8 && W.mass()>1.8 ){
                                                                            //String strKine = String.format("kine : Q2=%1.2f , W=%1.2f",-Q.mass2(),W.mass());
                                                                            //System.out.println("FOUND ELECTRON AND PROTON AND PHOTON , "+strKine);
                                                                            LorentzVector VmissP = new LorentzVector(0,0,0,0);
                                                                            VmissP.add(W);
                                                                            VmissP.sub(VG);
                                                                            LorentzVector VmissG = new LorentzVector(0,0,0,0);
                                                                            VmissG.add(W);
                                                                            VmissG.sub(VP);
                                                                            LorentzVector VmissAll = new LorentzVector(0,0,0,0);
                                                                            VmissAll.add(VmissG);
                                                                            VmissAll.sub(VG);

                                                                            hasDVCS = true
                                                                                    && VmissAll.e() > -1 && VmissAll.e() < 2.0
                                                                                    && VmissP.mass() > 0.25 && VmissP.mass() < 2.0
                                                                                    && VmissAll.mass2() > -0.1 &&  VmissAll.mass2() < 0.1
                                                                                    && VmissAll.px()*VmissAll.px() + VmissAll.py()*VmissAll.py() < 0.75
                                                                                    && Vangle( VG.vect() , VmissG.vect() ) < 7.5
                                                                                    ;

                                                                    }
                                                            }
                                                    }
                                            }
                                    }

                            }

                    }
            }
            return hasDVCS;
    }
}
