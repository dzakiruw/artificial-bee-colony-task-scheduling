package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.Cloudlet;
import java.util.Random;

public class IndividualABC {
    private int id;
    private int type; // 0=onlooker, 1=employed, 2=scout
    private int[] vmAllocation;
    private int visitCount;
    private double fitness;
    
    // Constants for bee types
    public static final int ONLOOKER = 0;  // was INACTIVE
    public static final int EMPLOYED = 1;  // was ACTIVE
    public static final int SCOUT = 2;     // stays the same
    
    /**
     * Constructor for an individual bee solution
     * 
     * @param id Unique identifier for the bee
     * @param type Type of bee (ONLOOKER, EMPLOYED, or SCOUT)
     * @param cloudletCount Number of cloudlets to allocate
     */
    public IndividualABC(int id, int type, int cloudletCount) {
        this.id = id;
        this.type = type;
        this.vmAllocation = new int[cloudletCount];
        this.visitCount = 0;
        this.fitness = 0.0;
    }
    
    /**
     * Generate a random solution by randomly assigning cloudlets to VMs
     * 
     * @param vmCount Number of VMs available
     */
    public void generateRandomSolution(int vmCount) {
        Random random = new Random();
        for (int i = 0; i < vmAllocation.length; i++) {
            vmAllocation[i] = random.nextInt(vmCount);
        }
    }
    
    /**
     * Get the VM allocation array
     */
    public int[] getVmAllocation() {
        return vmAllocation;
    }
    
    /**
     * Set the VM allocation array
     */
    public void setVmAllocation(int[] vmAllocation) {
        this.vmAllocation = vmAllocation.clone();
    }
    
    /**
     * Get the visit count
     */
    public int getVisitCount() {
        return visitCount;
    }
    
    /**
     * Increment the visit count
     */
    public void incrementVisitCount() {
        visitCount++;
    }
    
    /**
     * Reset the visit count
     */
    public void resetVisitCount() {
        visitCount = 0;
    }
    
    /**
     * Get the bee ID
     */
    public int getId() {
        return id;
    }
    
    /**
     * Get the bee type
     */
    public int getType() {
        return type;
    }
    
    /**
     * Set the bee type
     */
    public void setType(int type) {
        this.type = type;
    }
    
    /**
     * Get the fitness of the individual
     */
    public double getFitness() {
        return fitness;
    }
    
    /**
     * Set the fitness of the individual
     */
    public void setFitness(double fitness) {
        this.fitness = fitness;
    }
}
