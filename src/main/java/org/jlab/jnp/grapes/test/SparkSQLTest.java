/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jlab.jnp.grapes.test;

/**
 *
 * @author gavalian
 */
public class SparkSQLTest {
    public static void main(String[] args){
        System.out.println("*** Running test with apache-spark.......");
        String format = "0FFFFFFFFFFFFFFF";
        long trigger = Long.parseLong(format,16);
        System.out.printf("trigger = %X\n",trigger);
    }
}
