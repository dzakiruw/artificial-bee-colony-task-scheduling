package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.Vm;
import java.util.List;

/**
 * A simplified implementation of the Artificial Bee Colony (ABC) algorithm
 * that consolidates all bee-related code into a single file.
 */
public class ABC {
    private PopulationABC population;
    private int maxIterations;
    private int currentIteration;
    private List<Cloudlet> cloudletList;
    private List<Vm> vmList;
    
    // ABC parameters
    private int numBees;
    private int numInactive;
    private int numActive;
    private int numScout;
    private int maxVisits;
    private double probMistake;
    private double probPersuasion;
    
    /**
     * Constructor for ABC algorithm
     * 
     * @param cloudletList List of cloudlets to be scheduled
     * @param vmList List of VMs available
     * @param numBees Total number of bees
     * @param numInactive Number of inactive bees
     * @param numActive Number of active bees
     * @param numScout Number of scout bees
     * @param maxVisits Maximum visit count before abandoning a solution
     * @param maxIterations Maximum number of iterations
     * @param probMistake Probability of making a mistake
     * @param probPersuasion Probability of persuasion for inactive bees
     */
    public ABC(List<Cloudlet> cloudletList, List<Vm> vmList, int numBees, int numInactive, 
               int numActive, int numScout, int maxVisits, int maxIterations, 
               double probMistake, double probPersuasion) {
        this.cloudletList = cloudletList;
        this.vmList = vmList;
        this.numBees = numBees;
        this.numInactive = numInactive;
        this.numActive = numActive;
        this.numScout = numScout;
        this.maxVisits = maxVisits;
        this.maxIterations = maxIterations;
        this.probMistake = probMistake;
        this.probPersuasion = probPersuasion;
        this.currentIteration = 0;
        
        // Verify parameters
        if (numBees != (numActive + numInactive + numScout)) {
            throw new IllegalArgumentException("Sum of active, inactive and scout bees must equal total bees");
            }
        
        // Initialize population
        this.population = new PopulationABC(numBees, numInactive, numActive, numScout, 
                                           maxVisits, probMistake, probPersuasion,
                                           cloudletList.size(), vmList.size());
    }
    
    /**
     * Run the ABC algorithm
     * 
     * @return The optimized mapping of cloudlets to VMs
     */
    public int[] run() {
        // Initialize by calculating fitness for all bees
        population.calculateFitness(cloudletList, vmList);
        
        // Main loop
        while (currentIteration < maxIterations) {
            // Perform one iteration
            population.iterate(cloudletList, vmList);
                currentIteration++;
        }
        
        // Return the best solution found
        return population.getGlobalBest().getVmAllocation();
    }
    
    /**
     * Get the global best solution
     * 
     * @return The global best solution
     */
    public IndividualABC getGlobalBest() {
        return population.getGlobalBest();
        }
        
        /**
     * Get the global best fitness
     * 
     * @return The global best fitness
         */
    public double getGlobalBestFitness() {
        return population.getGlobalBest().getFitness();
        }
        
        /**
     * Get the makespan of the best solution
     * 
     * @return The makespan value
         */
    public double getBestMakespan() {
        // Since fitness is 1/makespan, we need to invert it
        return 1.0 / getGlobalBestFitness();
    }
} 