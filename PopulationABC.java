package org.cloudbus.cloudsim.examples;

import java.util.ArrayList;
import java.util.List;

/**
 * Simplified PopulationABC class that focuses only on data storage
 * Most algorithm logic has been moved to ABC.java
 */
public class PopulationABC {
    private List<IndividualABC> bees;
    private int numBees;
    private int cloudletCount;
    
    /**
     * Constructor for ABC population
     * 
     * @param numBees Total number of bees
     * @param cloudletCount Number of cloudlets to schedule
     */
    public PopulationABC(int numBees, int cloudletCount) {
        this.numBees = numBees;
        this.cloudletCount = cloudletCount;
        this.bees = new ArrayList<>();
        
        initializePopulation();
    }
    
    /**
     * Initialize the bee population
     */
    private void initializePopulation() {
        for (int i = 0; i < numBees; i++) {
            // Create bees with default type ONLOOKER (will be set by ABC)
            IndividualABC bee = new IndividualABC(i, IndividualABC.ONLOOKER, cloudletCount);
            bees.add(bee);
        }
    }
    
    /**
     * Get all bees in the population
     * 
     * @return List of all bees
     */
    public List<IndividualABC> getBees() {
        return bees;
    }
    
    /**
     * Get a specific bee from the population
     * 
     * @param index Index of the bee
     * @return The bee at the specified index
     */
    public IndividualABC getBee(int index) {
        return bees.get(index);
    }
    
    /**
     * Get the count of bees in the population
     * 
     * @return Number of bees
     */
    public int getSize() {
        return bees.size();
    }
} 